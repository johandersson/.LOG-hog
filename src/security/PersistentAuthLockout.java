package security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

import main.HmacUtils;

/**
 * Persistent authentication lockout tracker stored outside the plaintext settings file.
 * The state file is signed with an owner-only secret, and tampering or missing state fails closed.
 */
public final class PersistentAuthLockout {
    private static final String KEY_FAILED = "authFailedAttempts";
    private static final String KEY_LOCKED_UNTIL = "authLockedUntilEpochMs";
    private static final String KEY_MAC = "authLockoutMac";
    private static final String KEY_LOCKOUT_LEVEL = "authLockoutLevel";
    private static final String KEY_SEQ = "authStateSeq";
    private static final String KEY_STATE_HASH = "authStateHash";
    private static final String KEY_ANCHOR_MAC = "authAnchorMac";
    private static final int MAX_FAILED_SESSIONS_BEFORE_LOCKOUT = 10;
    private static final int MAX_SESSION_ATTEMPTS = 3;
    private static final long[] LOCKOUT_SCHEDULE_MS = new long[] {
        30L * 60L * 1000L,
        30L * 60L * 1000L,
        30L * 60L * 1000L
    };
    private static final String LOCKOUT_DIR_NAME = ".loghog";
    private static final String LOCKOUT_STATE_FILE = "auth-lockout.properties";
    private static final String LOCKOUT_KEY_FILE = "auth-lockout.key";
    private static final String LOCKOUT_ANCHOR_FILE = "auth-lockout.anchor";

    private PersistentAuthLockout() {}

    public static long getRemainingLockoutMillis(Properties settings) {
        purgeLegacyKeys(settings);
        try {
            LockoutState state = readState();
            long remaining = Math.max(0L, state.lockedUntil - System.currentTimeMillis());
            if (remaining > getMaxLockoutMillis() + 5000L) {
                audit("LOCKOUT_INVALID_STATE", "remainingMs=" + remaining);
                clear(settings);
                return 0L;
            }
            return remaining;
        } catch (Exception e) {
            audit("LOCKOUT_READ_ERROR", e.getClass().getSimpleName());
            return getMaxLockoutMillis();
        }
    }

    public static void registerFailure(Properties settings) {
        purgeLegacyKeys(settings);
        try {
            LockoutState state = readState();
            int failedSessions = state.failedAttempts + 1;
            if (failedSessions >= MAX_FAILED_SESSIONS_BEFORE_LOCKOUT) {
                state.failedAttempts = 0;
                state.lockoutLevel = 1;
                long lockoutDurationMs = getLockoutDurationMillis(state.lockoutLevel);
                state.lockedUntil = System.currentTimeMillis() + lockoutDurationMs;
                long lockoutMinutes = lockoutDurationMs / (60L * 1000L);
                audit("LOCKOUT_TRIGGERED", "level=" + state.lockoutLevel + ",minutes=" + lockoutMinutes + ",until=" + state.lockedUntil);
            } else {
                state.failedAttempts = failedSessions;
                audit("AUTH_FAILURE", "failedSessions=" + failedSessions);
            }

            writeState(state);
        } catch (Exception ex) {
            audit("LOCKOUT_WRITE_ERROR", ex.getClass().getSimpleName());
            // Fail closed by leaving existing lockout data untouched.
        }
    }

    public static void clear(Properties settings) {
        purgeLegacyKeys(settings);
        try {
            writeState(new LockoutState(0, 0L, 0L, 0));
            audit("LOCKOUT_CLEARED", "ok");
        } catch (Exception ex) {
            audit("LOCKOUT_CLEAR_ERROR", ex.getClass().getSimpleName());
        }
    }

    public static int getMaxSessionAttempts() {
        return MAX_SESSION_ATTEMPTS;
    }

    public static int getMaxFailedSessionsBeforeLockout() {
        return MAX_FAILED_SESSIONS_BEFORE_LOCKOUT;
    }

    /**
     * Formats the remaining lockout duration in a user-friendly way.
     * Shows the most significant non-zero unit (hours, minutes, seconds) and
     * rounds up so "18 minutes left" is shown instead of "about 30 minutes".
     *
     * @param remainingMs remaining lockout time in milliseconds (must be >= 0)
     * @return human-readable string such as "18 minutes" or "1 hour 5 minutes"
     */
    public static String formatRemainingLockout(long remainingMs) {
        if (remainingMs <= 0) {
            return "0 seconds";
        }
        long totalSeconds = (remainingMs + 999L) / 1000L;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            if (minutes > 0) {
                return hours + " hour" + (hours == 1 ? "" : "s") + " " + minutes + " minute" + (minutes == 1 ? "" : "s");
            }
            return hours + " hour" + (hours == 1 ? "" : "s");
        }
        if (minutes > 0) {
            if (seconds > 0) {
                return minutes + " minute" + (minutes == 1 ? "" : "s") + " " + seconds + " second" + (seconds == 1 ? "" : "s");
            }
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        }
        return seconds + " second" + (seconds == 1 ? "" : "s");
    }

    private static void purgeLegacyKeys(Properties settings) {
        if (settings == null) {
            return;
        }
        settings.remove(KEY_FAILED);
        settings.remove(KEY_LOCKED_UNTIL);
        settings.remove(KEY_MAC);
        settings.remove(KEY_LOCKOUT_LEVEL);
    }

    private static LockoutState readState() throws IOException {
        Path statePath = getStatePath();
        Path keyPath = getKeyPath();
        Path anchorPath = getAnchorPath();
        boolean stateExists = Files.exists(statePath);
        boolean keyExists = Files.exists(keyPath);
        boolean anchorExists = Files.exists(anchorPath);

        if (!stateExists && !keyExists && !anchorExists) {
            byte[] key = generateKey();
            try {
                writeKey(key);
                LockoutState reset = new LockoutState(0, 0L, 0L, 0);
                writeState(reset, key);
                return reset;
            } finally {
                zeroize(key);
            }
        }

        if (stateExists && keyExists && !anchorExists) {
            byte[] key = readKey();
            try {
                Properties props = new Properties();
                try (ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(statePath))) {
                    props.load(in);
                }

                String mac = props.getProperty(KEY_MAC, "");
                props.remove(KEY_MAC);
                byte[] expectedMac = HmacUtils.computeHmacSha256(key, serializeState(props));
                byte[] actualMac = null;
                try {
                    if (mac.isEmpty()) {
                        // State file is missing MAC - treat as migration failure
                        throw new IllegalArgumentException("MAC property is missing from legacy state file");
                    }
                    actualMac = Base64.getDecoder().decode(mac);
                } catch (Exception macDecodeEx) {
                    // If MAC is invalid or can't be decoded, fail closed for security
                    audit("LOCKOUT_LEGACY_STATE_MAC_DECODE_FAILED", macDecodeEx.getClass().getSimpleName());
                    throw macDecodeEx;
                }
                try {
                    if (!java.security.MessageDigest.isEqual(expectedMac, actualMac)) {
                        LockoutState failClosed = failClosedState();
                        writeState(failClosed, key);
                        audit("LOCKOUT_TAMPER_DETECTED", "legacy_state_mac_mismatch");
                        return failClosed;
                    }
                } finally {
                    zeroize(expectedMac);
                    zeroize(actualMac);
                }

                LockoutState migrated = new LockoutState(
                    parseInt(props.getProperty(KEY_FAILED), 0),
                    parseLong(props.getProperty(KEY_LOCKED_UNTIL), 0L),
                    parseLong(props.getProperty(KEY_SEQ), 0L),
                    parseInt(props.getProperty(KEY_LOCKOUT_LEVEL), 0)
                );
                
                try {
                    writeState(migrated, key);
                    audit("LOCKOUT_ANCHOR_MIGRATED", "legacy_state");
                } catch (Exception writeEx) {
                    // If anchor write fails but state is clean, allow migration to proceed
                    // The anchor can be re-created on next successful authentication
                    if (migrated.failedAttempts == 0 && migrated.lockedUntil == 0L) {
                        audit("LOCKOUT_ANCHOR_WRITE_FAILED_BUT_STATE_CLEAN", writeEx.getClass().getSimpleName());
                        // Continue with migration even if anchor write failed
                    } else {
                        // State has active failures/lockout, fail closed for security
                        audit("LOCKOUT_ANCHOR_WRITE_FAILED_WITH_ACTIVE_STATE", writeEx.getClass().getSimpleName());
                        return failClosedState();
                    }
                }
                return migrated;
            } catch (Exception ex) {
                audit("LOCKOUT_MIGRATION_FAILED", ex.getClass().getSimpleName());
                return failClosedState();
            } finally {
                zeroize(key);
            }
        }

        if (!stateExists || !keyExists || !anchorExists) {
            // Partial or corrupted state detected. Handle different scenarios:
            // 1. If state file exists but key is missing, read state to check if there's any security-relevant info
            // 2. If there are active lockouts or registered failures, fail-closed (can't verify)
            // 3. If completely clean state, recover
            
            if (stateExists && !keyExists) {
                try {
                    // Try to read the state file without verification to check security status
                    Properties props = new Properties();
                    try (ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(getStatePath()))) {
                        props.load(in);
                    }
                    long currentLockedUntil = parseLong(props.getProperty(KEY_LOCKED_UNTIL), 0L);
                    int failedAttempts = parseInt(props.getProperty(KEY_FAILED), 0);
                    long remaining = Math.max(0L, currentLockedUntil - System.currentTimeMillis());
                    
                    // Fail-closed if there's any active lockout OR if there are registered failures
                    // (failures indicate prior authentication attempts we can't verify)
                    if (remaining > 0 || failedAttempts > 0) {
                        // Active lockout or failures exist but key is missing - fail closed (can't verify)
                        LockoutState failClosed = failClosedState();
                        audit("LOCKOUT_ACTIVE_OR_FAILURES_BUT_KEY_MISSING", "remaining=" + remaining + ",failed=" + failedAttempts);
                        try {
                            byte[] newKey = generateKey();
                            try {
                                writeKey(newKey);
                                writeState(failClosed, newKey);
                            } finally {
                                zeroize(newKey);
                            }
                        } catch (Exception ex) {
                            audit("LOCKOUT_RECOVERY_WRITE_FAILED", ex.getClass().getSimpleName());
                        }
                        return failClosed;
                    }
                    
                    // Completely clean state - safe to recover
                    byte[] newKey = generateKey();
                    try {
                        writeKey(newKey);
                        LockoutState recovered = new LockoutState(0, 0L, 0L, 0);
                        writeState(recovered, newKey);
                        audit("LOCKOUT_RECOVERED_AFTER_KEY_LOSS", "clean_state");
                        return recovered;
                    } finally {
                        zeroize(newKey);
                    }
                } catch (Exception ex) {
                    // If we can't even read the state file, fail closed
                    audit("LOCKOUT_CANNOT_READ_STATE", ex.getClass().getSimpleName());
                    LockoutState failClosed = failClosedState();
                    try {
                        byte[] newKey = generateKey();
                        try {
                            writeKey(newKey);
                            writeState(failClosed, newKey);
                        } finally {
                            zeroize(newKey);
                        }
                    } catch (Exception ignored) {
                        audit("LOCKOUT_RECOVERY_WRITE_FAILED_2", ignored.getClass().getSimpleName());
                    }
                    return failClosed;
                }
            }
            
            // For other partial states (state missing or only key/anchor present),
            // attempt clean recovery instead of immediate lockout
            byte[] key = null;
            try {
                key = keyExists ? readKey() : generateKey();
                try {
                    if (!keyExists) {
                        writeKey(key);
                    }
                    // Start with a clean state (no lockout) for recovery scenarios
                    LockoutState recovered = new LockoutState(0, 0L, 0L, 0);
                    writeState(recovered, key);
                    audit("LOCKOUT_PARTIAL_STATE_RECOVERED", "state=" + stateExists + ",key=" + keyExists + ",anchor=" + anchorExists);
                    return recovered;
                } finally {
                    zeroize(key);
                }
            } catch (Exception ex) {
                // If we can't read or write recovery state, fail closed
                audit("LOCKOUT_PARTIAL_RECOVERY_FAILED", ex.getClass().getSimpleName());
                return failClosedState();
            }
        }

        byte[] key = readKey();
        try {
            Properties props = new Properties();
            try (ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(statePath))) {
                props.load(in);
            }

            String mac = props.getProperty(KEY_MAC, "");
            props.remove(KEY_MAC);
            byte[] expectedMac = HmacUtils.computeHmacSha256(key, serializeState(props));
            byte[] actualMac = null;
            try {
                if (mac.isEmpty()) {
                    // State file is missing MAC - likely corrupted or from old version
                    throw new IllegalArgumentException("MAC property is missing from state file");
                }
                actualMac = Base64.getDecoder().decode(mac);
            } catch (Exception macDecodeEx) {
                // If MAC is invalid or can't be decoded, fail closed for security
                audit("LOCKOUT_STATE_MAC_DECODE_FAILED", macDecodeEx.getClass().getSimpleName());
                LockoutState failClosed = failClosedState();
                writeState(failClosed, key);
                return failClosed;
            }
            try {
                if (!java.security.MessageDigest.isEqual(expectedMac, actualMac)) {
                    LockoutState failClosed = failClosedState();
                    writeState(failClosed, key);
                    audit("LOCKOUT_TAMPER_DETECTED", "state_mac_mismatch");
                    return failClosed;
                }
            } finally {
                zeroize(expectedMac);
                zeroize(actualMac);
            }

            LockoutState state = new LockoutState(
                parseInt(props.getProperty(KEY_FAILED), 0),
                parseLong(props.getProperty(KEY_LOCKED_UNTIL), 0L),
                parseLong(props.getProperty(KEY_SEQ), 0L),
                parseInt(props.getProperty(KEY_LOCKOUT_LEVEL), 0)
            );

            if (!verifyAnchor(state, key, props.getProperty(KEY_STATE_HASH, ""))) {
                LockoutState failClosed = failClosedState();
                writeState(failClosed, key);
                audit("LOCKOUT_ROLLBACK_DETECTED", "anchor_mismatch");
                return failClosed;
            }

            return state;
        } finally {
            zeroize(key);
        }
    }

    private static void writeState(LockoutState state) throws IOException {
        byte[] key = Files.exists(getKeyPath()) ? readKey() : generateKey();
        try {
            writeState(state, key);
        } finally {
            zeroize(key);
        }
    }

    private static void writeState(LockoutState state, byte[] key) throws IOException {
        ensureStorageDir();
        long nextSeq = Math.max(state.sequence + 1L, readAnchorSequence() + 1L);
        Properties props = new Properties();
        props.setProperty(KEY_FAILED, Integer.toString(state.failedAttempts));
        props.setProperty(KEY_LOCKED_UNTIL, Long.toString(state.lockedUntil));
        props.setProperty(KEY_LOCKOUT_LEVEL, Integer.toString(state.lockoutLevel));
        props.setProperty(KEY_SEQ, Long.toString(nextSeq));

        byte[] stateHashRaw = null;
        String stateHash = "";
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            stateHashRaw = digest.digest(serializeState(props));
            stateHash = Base64.getEncoder().encodeToString(stateHashRaw);
        } catch (java.security.GeneralSecurityException ex) {
            throw new IOException("Unable to hash lockout state", ex);
        } finally {
            zeroize(stateHashRaw);
        }
        props.setProperty(KEY_STATE_HASH, stateHash);

        byte[] payload = serializeState(props);
        byte[] mac = null;
        try {
            mac = HmacUtils.computeHmacSha256(key, payload);
            props.setProperty(KEY_MAC, Base64.getEncoder().encodeToString(mac));
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                props.store(out, "LogHog lockout state");
                Files.write(getStatePath(), out.toByteArray());
            }
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(getStatePath());
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(getKeyPath());
            writeAnchor(nextSeq, stateHash, key);
        } finally {
            zeroize(payload);
            zeroize(mac);
        }
    }

    private static void writeKey(byte[] key) throws IOException {
        ensureStorageDir();
        SensitiveKeyProtector.writeProtected(getKeyPath(), key, "auth-lockout-key");
    }

    private static byte[] readKey() throws IOException {
        Path keyPath = getKeyPath();
        try {
            return SensitiveKeyProtector.readProtected(keyPath, "auth-lockout-key");
        } catch (IOException protectedReadFailed) {
            byte[] raw = Files.readAllBytes(keyPath);
            byte[] decoded = tryDecodeLegacyKey(raw);
            try {
                writeKey(decoded);
                audit("LOCKOUT_KEY_MIGRATED", "legacy_format");
                return decoded;
            } finally {
                zeroize(raw);
            }
        }
    }

    private static byte[] tryDecodeLegacyKey(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return generateKey();
        }
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException ex) {
            return raw.clone();
        }
    }

    private static Path getStorageDir() {
        return Paths.get(System.getProperty("user.home"), LOCKOUT_DIR_NAME);
    }

    private static Path getStatePath() {
        return getStorageDir().resolve(LOCKOUT_STATE_FILE);
    }

    private static Path getKeyPath() {
        return getStorageDir().resolve(LOCKOUT_KEY_FILE);
    }

    private static Path getAnchorPath() {
        return getStorageDir().resolve(LOCKOUT_ANCHOR_FILE);
    }

    private static void ensureStorageDir() throws IOException {
        Files.createDirectories(getStorageDir());
        // Directory permission APIs are inconsistent across platforms; keep this best-effort
        // while retaining strict enforcement for security-critical files created inside it.
        SecurityFilePolicy.ensureOwnerOnlyPermissions(getStorageDir());
    }

    private static void writeAnchor(long sequence, String stateHash, byte[] key) throws IOException {
        String payload = sequence + "|" + stateHash;
        byte[] mac = HmacUtils.computeHmacSha256(key, payload.getBytes(StandardCharsets.UTF_8));
        try {
            Properties anchor = new Properties();
            anchor.setProperty(KEY_SEQ, Long.toString(sequence));
            anchor.setProperty(KEY_STATE_HASH, stateHash);
            anchor.setProperty(KEY_ANCHOR_MAC, Base64.getEncoder().encodeToString(mac));
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                anchor.store(out, "LogHog lockout anchor");
                Files.write(getAnchorPath(), out.toByteArray());
            }
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(getAnchorPath());
        } finally {
            zeroize(mac);
        }
    }

    private static long readAnchorSequence() {
        Path anchorPath = getAnchorPath();
        if (!Files.exists(anchorPath)) {
            return 0L;
        }
        try {
            Properties props = new Properties();
            try (ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(anchorPath))) {
                props.load(in);
            }
            return parseLong(props.getProperty(KEY_SEQ), 0L);
        } catch (Exception ex) {
            return 0L;
        }
    }

    private static boolean verifyAnchor(LockoutState state, byte[] key, String stateHash) {
        Path anchorPath = getAnchorPath();
        if (!Files.exists(anchorPath)) {
            return false;
        }
        try {
            Properties anchor = new Properties();
            try (ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(anchorPath))) {
                anchor.load(in);
            }

            long anchorSeq = parseLong(anchor.getProperty(KEY_SEQ), -1L);
            String anchorHash = anchor.getProperty(KEY_STATE_HASH, "");
            String anchorMac = anchor.getProperty(KEY_ANCHOR_MAC, "");
            String payload = anchorSeq + "|" + anchorHash;

            byte[] expected = HmacUtils.computeHmacSha256(key, payload.getBytes(StandardCharsets.UTF_8));
            byte[] actual = Base64.getDecoder().decode(anchorMac);
            try {
                if (!java.security.MessageDigest.isEqual(expected, actual)) {
                    return false;
                }
            } finally {
                zeroize(expected);
                zeroize(actual);
            }

            if (state.sequence < anchorSeq) {
                return false;
            }
            if (state.sequence > anchorSeq) {
                // Anchor is stale; accept only if state hash matches and anchor can be advanced by next write.
                return stateHash != null && !stateHash.isEmpty();
            }
            return anchorHash.equals(stateHash);
        } catch (Exception ex) {
            return false;
        }
    }

    private static void audit(String eventType, String details) {
        try {
            SecurityEventLog.appendEvent(eventType, details);
        } catch (Exception ignored) {
            // Never block auth flow on audit write failures.
        }
    }

    private static byte[] serializeState(Properties props) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            props.store(out, "LogHog lockout payload");
            return out.toByteArray();
        }
    }

    private static byte[] generateKey() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return key;
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static long parseLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static void zeroize(byte[] data) {
        if (data != null) {
            java.util.Arrays.fill(data, (byte) 0);
        }
    }

    private static final class LockoutState {
        private int failedAttempts;
        private long lockedUntil;
        private long sequence;
        private int lockoutLevel;

        private LockoutState(int failedAttempts, long lockedUntil, long sequence, int lockoutLevel) {
            this.failedAttempts = failedAttempts;
            this.lockedUntil = lockedUntil;
            this.sequence = sequence;
            this.lockoutLevel = lockoutLevel;
        }
    }

    private static long getLockoutDurationMillis(int lockoutLevel) {
        int normalizedLevel = lockoutLevel <= 0 ? 1 : lockoutLevel;
        int index = normalizedLevel - 1;
        if (index >= LOCKOUT_SCHEDULE_MS.length) {
            index = LOCKOUT_SCHEDULE_MS.length - 1;
        }
        return LOCKOUT_SCHEDULE_MS[index];
    }

    private static long getMaxLockoutMillis() {
        return LOCKOUT_SCHEDULE_MS[LOCKOUT_SCHEDULE_MS.length - 1];
    }

    private static LockoutState failClosedState() {
        return new LockoutState(0, System.currentTimeMillis() + getMaxLockoutMillis(), 0L, LOCKOUT_SCHEDULE_MS.length);
    }
}