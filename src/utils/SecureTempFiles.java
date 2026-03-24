package utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper for creating secure temporary files with restrictive permissions.
 * Best-effort: sets POSIX perms when available, falls back to java.io.File owner-only flags.
 */
public final class SecureTempFiles {
    private SecureTempFiles() {}

    public static Path createSecureTempFile(Path dir, String prefix, String suffix) throws java.io.IOException {
        Path parent = dir != null ? dir : Path.of(System.getProperty("java.io.tmpdir"));
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, prefix, suffix);

        // Try to set restrictive permissions
        try {
            try {
                var perms = java.nio.file.attribute.PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(tmp, perms);
            } catch (UnsupportedOperationException | SecurityException ignored) {
                // Fallback for non-POSIX filesystems
                File f = tmp.toFile();
                f.setReadable(true, true);
                f.setWritable(true, true);
                f.setExecutable(false, true);
            }
        } catch (Exception ignored) {
            // Best-effort only
        }

        return tmp;
    }
}
