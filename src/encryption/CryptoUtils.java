package encryption;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import security.SecurityFilePolicy;

public class CryptoUtils {
    /**
     * Constant-time comparison for MAC/tag values.
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a, b);
    }

    /**
     * Set restrictive file permissions (owner read/write only) if possible.
     * Best effort: works on Unix, ignored on Windows.
     */
    public static void setOwnerOnlyPermissions(Path path) {
        SecurityFilePolicy.ensureOwnerOnlyPermissions(path);
    }

    /**
     * Validate IV and tag lengths for GCM before decryption.
     * Throws IllegalArgumentException if invalid.
     */
    public static void validateGcmParams(byte[] iv, byte[] tag) {
        if (iv == null || iv.length != 12) throw new IllegalArgumentException("IV must be 12 bytes for AES-GCM");
        if (tag == null || tag.length != 16) throw new IllegalArgumentException("Tag must be 16 bytes for AES-GCM");
    }

    /**
     * Overwrite a byte array with zeros.
     */
    public static void zeroize(byte[] arr) {
        if (arr != null) Arrays.fill(arr, (byte)0);
    }

    /**
     * Overwrite a char array with zeros (for password cleanup).
     */
    public static void zeroize(char[] arr) {
        if (arr != null) Arrays.fill(arr, '\0');
    }
}
