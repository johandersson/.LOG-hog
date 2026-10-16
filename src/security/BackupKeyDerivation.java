package security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Centralized backup-signing key derivation policies.
 */
public final class BackupKeyDerivation {
    private static final int KEY_LENGTH_BYTES = 32;
    // Align with primary encryption KDF hardness to avoid weaker derived backup key paths.
    private static final int PBKDF2_ITERATIONS = 600000;
    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";
    private static final byte[] CONTEXT_V2 = "loghog-backup-hmac-v2".getBytes(StandardCharsets.UTF_8);

    private BackupKeyDerivation() {}

    public static byte[] deriveV2(char[] password, byte[] salt) {
        return deriveV2(password, salt, PBKDF2_ITERATIONS);
    }

    private static byte[] deriveV2(char[] password, byte[] salt, int iterations) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt are required");
        }

        PBEKeySpec spec = null;
        try {
            byte[] contextualSalt = new byte[salt.length + CONTEXT_V2.length];
            System.arraycopy(salt, 0, contextualSalt, 0, salt.length);
            System.arraycopy(CONTEXT_V2, 0, contextualSalt, salt.length, CONTEXT_V2.length);

            spec = new PBEKeySpec(password, contextualSalt, iterations, KEY_LENGTH_BYTES * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGO);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive backup key", e);
        } finally {
            if (spec != null) {
                try {
                    spec.clearPassword();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Read-only compatibility keys for backups written before July 6, 2026.
     * New backups must continue to use deriveV2 with the current work factor.
     */
    public static byte[][] deriveRestoreKeys(char[] password, byte[] salt) {
        byte[][] keys = new byte[3][];
        boolean completed = false;
        try {
            keys[0] = deriveV2(password, salt);
            keys[1] = deriveV2(password, salt, 120000);
            java.nio.ByteBuffer encoded = StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(password));
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                digest.update(encoded);
                digest.update(salt);
                keys[2] = digest.digest("loghog-backup-hmac-v1".getBytes(StandardCharsets.UTF_8));
            } finally {
                encoded.clear();
                while (encoded.hasRemaining()) encoded.put((byte) 0);
            }
            completed = true;
            return keys;
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to derive legacy backup verification key", e);
        } finally {
            if (!completed) {
                for (byte[] key : keys) {
                    if (key != null) java.util.Arrays.fill(key, (byte) 0);
                }
            }
        }
    }

    public static byte[] randomSessionKey() {
        byte[] key = new byte[KEY_LENGTH_BYTES];
        new SecureRandom().nextBytes(key);
        return key;
    }
}
