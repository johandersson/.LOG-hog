package security;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Best-effort file-system hardening helpers kept separate from UI and business logic.
 */
public final class SecurityFilePolicy {
    private SecurityFilePolicy() {}

    public static void ensureOwnerOnlyPermissions(Path path) {
        if (path == null) return;

        tryApplyOwnerOnlyPermissions(path);
    }

    /**
     * Fail-secure variant for security-critical files (keys, auth state, audit metadata).
     */
    public static void ensureOwnerOnlyPermissionsOrThrow(Path path) {
        if (path == null) {
            throw new SecurityException("Path cannot be null");
        }
        if (!tryApplyOwnerOnlyPermissions(path)) {
            throw new SecurityException("Failed to enforce owner-only permissions for: " + path);
        }
    }

    /**
     * Best-effort check for owner-only read/write on POSIX systems.
     * Returns true when verifiable as secure, false when insecure or unverifiable.
     */
    public static boolean isOwnerOnlyAccessEnforced(Path path) {
        if (path == null || !Files.exists(path)) return false;
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
            boolean ownerRw = perms.contains(PosixFilePermission.OWNER_READ)
                && perms.contains(PosixFilePermission.OWNER_WRITE);
            boolean othersDenied = !perms.contains(PosixFilePermission.GROUP_READ)
                && !perms.contains(PosixFilePermission.GROUP_WRITE)
                && !perms.contains(PosixFilePermission.GROUP_EXECUTE)
                && !perms.contains(PosixFilePermission.OTHERS_READ)
                && !perms.contains(PosixFilePermission.OTHERS_WRITE)
                && !perms.contains(PosixFilePermission.OTHERS_EXECUTE);
            return ownerRw && othersDenied;
        } catch (UnsupportedOperationException | SecurityException ex) {
            // On non-POSIX platforms this may be unverifiable via standard JDK APIs.
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    public static String sanitizeLogValue(String input) {
        if (input == null) return "";
        String noNewlines = input.replace('\r', ' ').replace('\n', ' ');
        if (noNewlines.length() <= 1000) {
            return noNewlines;
        }
        return noNewlines.substring(0, 1000) + "...";
    }

    private static boolean tryApplyOwnerOnlyPermissions(Path path) {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            if (Files.isDirectory(path)) {
                perms.add(PosixFilePermission.OWNER_EXECUTE);
            }
            Files.setPosixFilePermissions(path, perms);
            return true;
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // Not POSIX or blocked.
        } catch (Exception ignored) {
            // Fall back to basic file attributes below.
        }

        try {
            File f = path.toFile();
            boolean readable = f.setReadable(true, true);
            boolean writable = f.setWritable(true, true);
            // Directories need execute/traverse. For regular files, execute bit is not security-critical
            // and may be unsupported or immutable on some filesystems (especially on Windows).
            boolean executable = true;
            if (f.isDirectory()) {
                executable = f.setExecutable(true, true) || f.canExecute();
            } else {
                try {
                    f.setExecutable(false, false);
                } catch (Exception ignored) {
                    // Best effort only for regular files.
                }
            }
            return readable && writable && executable;
        } catch (Exception ignored) {
            return false;
        }
    }
}
