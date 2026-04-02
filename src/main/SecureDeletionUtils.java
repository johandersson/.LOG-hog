package main;

import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;

/**
 * Utilities for secure file deletion with data overwriting.
 * 
 * <h2>Security Properties</h2>
 * <ul>
 *   <li>Overwrites file content with random bytes before deletion</li>
 *   <li>Followed by zero-fill pass to ensure no residual data patterns</li>
 *   <li>Uses {@link SecureRandom} for cryptographically secure random data</li>
 * </ul>
 * 
 * <h2>Important Limitations</h2>
 * <p><b>SSD/Flash Storage:</b> On solid-state drives and flash memory, secure deletion
 * is fundamentally limited due to:</p>
 * <ul>
 *   <li><b>Wear-leveling:</b> SSDs may write data to different physical blocks than
 *       the logical address, leaving old data in spare/retired blocks</li>
 *   <li><b>TRIM behavior:</b> TRIM commands mark blocks as unused but don't guarantee
 *       immediate data erasure</li>
 *   <li><b>Over-provisioned space:</b> SSDs reserve extra capacity that is inaccessible
 *       to the OS but may contain old data</li>
 * </ul>
 * 
 * <p>For maximum security on SSDs, consider:</p>
 * <ul>
 *   <li>Full-disk encryption (BitLocker, FileVault, LUKS)</li>
 *   <li>Manufacturer-provided secure erase utilities</li>
 *   <li>Physical destruction for highly sensitive data</li>
 * </ul>
 * 
 * <p>This implementation provides best-effort secure deletion that is effective on
 * traditional HDDs and provides some protection on SSDs by overwriting logical blocks.</p>
 */
public class SecureDeletionUtils {
    
    /**
     * Securely deletes a file by overwriting its contents before deletion.
     * 
     * <p>Performs a random-data pass followed by a zero-fill pass, then deletes the file.
     * See class documentation for SSD/flash storage limitations.</p>
     * 
     * @param file the file to securely delete
     * @throws IOException if an I/O error occurs during overwriting or deletion
     */
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
