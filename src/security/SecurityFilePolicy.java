package security;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Best-effort file-system hardening helpers kept separate from UI and business logic.
 */
public final class SecurityFilePolicy {
    private SecurityFilePolicy() {}

    public static void ensureOwnerOnlyPermissions(Path path) {
        if (path == null) return;

        try {
            Set<PosixFilePermission> perms = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(path, perms);
            return;
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // Not POSIX or blocked.
        } catch (Exception ignored) {
            // Best-effort fallback below.
        }

        try {
            File f = path.toFile();
            f.setReadable(true, true);
            f.setWritable(true, true);
            f.setExecutable(false, false);
        } catch (Exception ignored) {
            // Best-effort only.
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
}
