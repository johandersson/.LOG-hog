package security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Centralized backup-signing key derivation policies.
 *
 * <p>Supports a stronger PBKDF2-based derivation (v2) and a legacy SHA-256 derivation
 * for backward compatibility with previously generated backup signatures.</p>
 */
public final class BackupKeyDerivation {
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int PBKDF2_ITERATIONS = 120000;
    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";
    private static final byte[] CONTEXT_V2 = "loghog-backup-hmac-v2".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CONTEXT_V1 = "loghog-backup-hmac-v1".getBytes(StandardCharsets.UTF_8);

    private BackupKeyDerivation() {}

    public static byte[] deriveV2(char[] password, byte[] salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt are required");
        }

        PBEKeySpec spec = null;
        try {
            byte[] contextualSalt = new byte[salt.length + CONTEXT_V2.length];
            System.arraycopy(salt, 0, contextualSalt, 0, salt.length);
            System.arraycopy(CONTEXT_V2, 0, contextualSalt, salt.length, CONTEXT_V2.length);

            spec = new PBEKeySpec(password, contextualSalt, PBKDF2_ITERATIONS, KEY_LENGTH_BYTES * 8);
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

    public static byte[] deriveLegacyV1(char[] password, byte[] salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt are required");
        }

        ByteBuffer pwdBuf = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        byte[] pwdBytes = new byte[pwdBuf.remaining()];
        pwdBuf.get(pwdBytes);

        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(pwdBytes);
            sha256.update(salt);
            sha256.update(CONTEXT_V1);
            return sha256.digest();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive legacy backup key", e);
        } finally {
            java.util.Arrays.fill(pwdBytes, (byte) 0);
        }
    }

    public static byte[] randomSessionKey() {
        byte[] key = new byte[KEY_LENGTH_BYTES];
        new SecureRandom().nextBytes(key);
        return key;
    }
}
