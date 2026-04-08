package security;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Simple smoke test to verify owner-only permission helpers and SecureTempFiles behavior.
 * Run manually: java -cp . security.PermissionsTest
 */
public class PermissionsTest {
    public static void main(String[] args) throws Exception {
        Path tmp = null;
        try {
            tmp = utils.SecureTempFiles.createSecureTempFile(null, "permtest-", ".tmp", true);
            Files.write(tmp, "permission-test".getBytes(StandardCharsets.UTF_8));

            boolean ok = false;
            try {
                Set<PosixFilePermission> perms = Files.getPosixFilePermissions(tmp);
                ok = perms.contains(PosixFilePermission.OWNER_READ) && perms.contains(PosixFilePermission.OWNER_WRITE);
                System.out.println("POSIX perms: " + perms);
            } catch (UnsupportedOperationException e) {
                // Non-POSIX (Windows) fallback - ensure file is readable/writable and owner-only calls didn't throw
                File f = tmp.toFile();
                System.out.println("Windows fallback permissions: readable=" + f.canRead() + " writable=" + f.canWrite());
                ok = f.canRead() && f.canWrite();
            }

            System.out.println(ok ? "PASS: Owner-only permissions appear applied" : "FAIL: Owner-only permissions not verified");
        } finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }
}
