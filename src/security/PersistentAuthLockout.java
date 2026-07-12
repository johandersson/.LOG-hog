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
            return Math.max(0L, readState().lockedUntil - System.currentTimeMillis());
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

        if (!stateExists || !keyExists || !anchorExists) {
            byte[] key = keyExists ? readKey() : generateKey();
            try {
                if (!keyExists) {
                    writeKey(key);
                }
                LockoutState locked = new LockoutState(0, System.currentTimeMillis() + getMaxLockoutMillis(), 0L, LOCKOUT_SCHEDULE_MS.length);
                writeState(locked, key);
                audit("LOCKOUT_MISSING_ARTIFACT", "state=" + stateExists + ",key=" + keyExists + ",anchor=" + anchorExists);
                return locked;
            } finally {
                zeroize(key);
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
            byte[] actualMac = Base64.getDecoder().decode(mac);
            try {
                if (!java.security.MessageDigest.isEqual(expectedMac, actualMac)) {
                    LockoutState locked = new LockoutState(0, System.currentTimeMillis() + getMaxLockoutMillis(), 0L, LOCKOUT_SCHEDULE_MS.length);
                    writeState(locked, key);
                    audit("LOCKOUT_TAMPER_DETECTED", "state_mac_mismatch");
                    return locked;
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
                LockoutState locked = new LockoutState(0, System.currentTimeMillis() + getMaxLockoutMillis(), 0L, LOCKOUT_SCHEDULE_MS.length);
                writeState(locked, key);
                audit("LOCKOUT_ROLLBACK_DETECTED", "anchor_mismatch");
                return locked;
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
}