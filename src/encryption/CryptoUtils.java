package encryption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;

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
        try {
            Set<PosixFilePermission> perms = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException | IOException e) {
            // Not POSIX or not permitted; ignore
        }
        // On Windows, fallback: try to make file hidden (not secure, but best effort)
        try {
            File file = path.toFile();
            file.setReadable(true, true);
            file.setWritable(true, true);
        } catch (Exception ignored) {}
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
}
