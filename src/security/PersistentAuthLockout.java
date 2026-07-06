package security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final int MAX_FAILED_ATTEMPTS = 4;
    private static final long LOCKOUT_MS = 10 * 60 * 1000L;
    private static final String LOCKOUT_DIR_NAME = ".loghog";
    private static final String LOCKOUT_STATE_FILE = "auth-lockout.properties";
    private static final String LOCKOUT_KEY_FILE = "auth-lockout.key";

    private PersistentAuthLockout() {}

    public static long getRemainingLockoutMillis(Properties settings) {
        purgeLegacyKeys(settings);
        try {
            return Math.max(0L, readState().lockedUntil - System.currentTimeMillis());
        } catch (Exception e) {
            return LOCKOUT_MS;
        }
    }

    public static void registerFailure(Properties settings) {
        purgeLegacyKeys(settings);
        try {
            LockoutState state = readState();
            int failed = state.failedAttempts + 1;
            if (failed >= MAX_FAILED_ATTEMPTS) {
                state.failedAttempts = 0;
                state.lockedUntil = System.currentTimeMillis() + LOCKOUT_MS;
            } else {
                state.failedAttempts = failed;
            }
            writeState(state);
        } catch (Exception ignored) {
            // Fail closed by leaving existing lockout data untouched.
        }
    }

    public static void clear(Properties settings) {
        purgeLegacyKeys(settings);
        try {
            writeState(new LockoutState(0, 0L));
        } catch (Exception ignored) {
        }
    }

    private static void purgeLegacyKeys(Properties settings) {
        if (settings == null) {
            return;
        }
        settings.remove(KEY_FAILED);
        settings.remove(KEY_LOCKED_UNTIL);
        settings.remove(KEY_MAC);
    }

    private static LockoutState readState() throws IOException {
        Path statePath = getStatePath();
        Path keyPath = getKeyPath();
        boolean stateExists = Files.exists(statePath);
        boolean keyExists = Files.exists(keyPath);

        if (!stateExists && !keyExists) {
            byte[] key = generateKey();
            try {
                writeKey(key);
                writeState(new LockoutState(0, 0L), key);
            } finally {
                zeroize(key);
            }
            return new LockoutState(0, 0L);
        }

        if (!keyExists) {
            byte[] key = generateKey();
            try {
                writeKey(key);
                LockoutState locked = new LockoutState(0, System.currentTimeMillis() + LOCKOUT_MS);
                writeState(locked, key);
                return locked;
            } finally {
                zeroize(key);
            }
        }

        byte[] key = Files.readAllBytes(keyPath);
        try {
            if (!stateExists) {
                LockoutState locked = new LockoutState(0, System.currentTimeMillis() + LOCKOUT_MS);
                writeState(locked, key);
                return locked;
            }

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
                    LockoutState locked = new LockoutState(0, System.currentTimeMillis() + LOCKOUT_MS);
                    writeState(locked, key);
                    return locked;
                }
            } finally {
                zeroize(expectedMac);
                zeroize(actualMac);
            }

            return new LockoutState(
                parseInt(props.getProperty(KEY_FAILED), 0),
                parseLong(props.getProperty(KEY_LOCKED_UNTIL), 0L)
            );
        } finally {
            zeroize(key);
        }
    }

    private static void writeState(LockoutState state) throws IOException {
        byte[] key = Files.exists(getKeyPath()) ? Files.readAllBytes(getKeyPath()) : generateKey();
        try {
            writeState(state, key);
        } finally {
            zeroize(key);
        }
    }

    private static void writeState(LockoutState state, byte[] key) throws IOException {
        ensureStorageDir();
        Properties props = new Properties();
        props.setProperty(KEY_FAILED, Integer.toString(state.failedAttempts));
        props.setProperty(KEY_LOCKED_UNTIL, Long.toString(state.lockedUntil));

        byte[] payload = serializeState(props);
        byte[] mac = null;
        try {
            mac = HmacUtils.computeHmacSha256(key, payload);
            props.setProperty(KEY_MAC, Base64.getEncoder().encodeToString(mac));
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                props.store(out, "LogHog lockout state");
                Files.write(getStatePath(), out.toByteArray());
            }
            SecurityFilePolicy.ensureOwnerOnlyPermissions(getStatePath());
            SecurityFilePolicy.ensureOwnerOnlyPermissions(getKeyPath());
        } finally {
            zeroize(payload);
            zeroize(mac);
        }
    }

    private static void writeKey(byte[] key) throws IOException {
        ensureStorageDir();
        Files.write(getKeyPath(), Base64.getEncoder().encode(key));
        SecurityFilePolicy.ensureOwnerOnlyPermissions(getKeyPath());
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

    private static void ensureStorageDir() throws IOException {
        Files.createDirectories(getStorageDir());
        SecurityFilePolicy.ensureOwnerOnlyPermissions(getStorageDir());
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

        private LockoutState(int failedAttempts, long lockedUntil) {
            this.failedAttempts = failedAttempts;
            this.lockedUntil = lockedUntil;
        }
    }
}