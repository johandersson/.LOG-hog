package security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

class PermissionsTest {

    @TempDir
    Path tempDir;

    @Test
    void secureTempFileAppliesOwnerOnlyPermissions() throws Exception {
        Path tmp = utils.SecureTempFiles.createSecureTempFile(tempDir, "permtest-", ".tmp", true);
        Files.write(tmp, "permission-test".getBytes(StandardCharsets.UTF_8));

        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(tmp);
            assertTrue(perms.contains(PosixFilePermission.OWNER_READ));
            assertTrue(perms.contains(PosixFilePermission.OWNER_WRITE));
            assertFalse(perms.contains(PosixFilePermission.OTHERS_READ), "File should not be world-readable");
        } catch (UnsupportedOperationException e) {
            File f = tmp.toFile();
            assertTrue(f.canRead(), "Fallback file should remain readable by owner");
            assertTrue(f.canWrite(), "Fallback file should remain writable by owner");
        }
    }
}
