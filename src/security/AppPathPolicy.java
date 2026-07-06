package security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Central path policy helpers for normalization and pre-I/O safety assertions.
 */
public final class AppPathPolicy {
    private AppPathPolicy() {}

    public static Path userHomePath() {
        String home = System.getProperty("user.home", ".");
        return Path.of(home).toAbsolutePath().normalize();
    }

    public static Path currentWorkingDirectory() {
        String cwd = System.getProperty("user.dir", ".");
        return Path.of(cwd).toAbsolutePath().normalize();
    }

    public static Path normalizeUserPath(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            Path normalized = Path.of(raw).toAbsolutePath().normalize();
            String text = normalized.toString();
            if (text.contains("\n") || text.contains("\r") || text.contains("\u0000")) {
                return null;
            }
            return normalized;
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean isWithinUserControlledRoots(Path path) {
        if (path == null) {
            return false;
        }
        try {
            Path normalized = path.toAbsolutePath().normalize();
            return normalized.startsWith(userHomePath()) || normalized.startsWith(currentWorkingDirectory());
        } catch (Exception ex) {
            return false;
        }
    }

    public static void assertSafeRegularFile(Path path) throws IOException {
        if (path == null) {
            throw new IOException("Null path is not allowed");
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(normalized)) {
                throw new IOException("Refusing symbolic link file path: " + normalized);
            }
            if (!Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Refusing non-regular file path: " + normalized);
            }
        }
    }

    public static void assertSafeDirectory(Path path) throws IOException {
        if (path == null) {
            throw new IOException("Null directory path is not allowed");
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(normalized)) {
                throw new IOException("Refusing symbolic link directory path: " + normalized);
            }
            if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Refusing non-directory path: " + normalized);
            }
        }
    }
}
