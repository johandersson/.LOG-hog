package security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Streaming digest helpers to avoid loading whole files into memory.
 */
public final class StreamingHash {
    private static final int HASH_BUFFER_SIZE = 8192;

    private StreamingHash() {}

    public static byte[] sha256(Path file) throws IOException {
        MessageDigest digest = newSha256();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[HASH_BUFFER_SIZE];
            int read = in.read(buffer);
            while (read != -1) {
                digest.update(buffer, 0, read);
                read = in.read(buffer);
            }
        }
        return digest.digest();
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
