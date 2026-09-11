package security;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecurityFilePolicyDirectoryPermissionsTest {
    @TempDir
    Path tempDir;

    @Test
    void ownerExecuteIsPreservedForDirectoriesOnPosix() throws Exception {
        SecurityFilePolicy.ensureOwnerOnlyPermissions(tempDir);
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(tempDir);
            assertTrue(perms.contains(PosixFilePermission.OWNER_READ));
            assertTrue(perms.contains(PosixFilePermission.OWNER_WRITE));
            assertTrue(perms.contains(PosixFilePermission.OWNER_EXECUTE));
            assertFalse(perms.contains(PosixFilePermission.OTHERS_EXECUTE));
        } catch (UnsupportedOperationException ex) {
            assertTrue(Files.isDirectory(tempDir));
        }
    }
}
