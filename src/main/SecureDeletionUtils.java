package main;

import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;

public class SecureDeletionUtils {
    public static void wipeFile(Path file) throws IOException {
        if (!Files.exists(file)) return;
        long size = Files.size(file);
        SecureRandom random = new SecureRandom();
        byte[] zeros = new byte[4096];
        byte[] randomBytes = new byte[4096];
        try (var channel = Files.newByteChannel(file, StandardOpenOption.WRITE)) {
            long written = 0;
            while (written < size) {
                random.nextBytes(randomBytes);
                channel.write(java.nio.ByteBuffer.wrap(randomBytes));
                channel.position(written);
                channel.write(java.nio.ByteBuffer.wrap(zeros));
                written += zeros.length;
            }
        }
        Files.delete(file);
    }
}
