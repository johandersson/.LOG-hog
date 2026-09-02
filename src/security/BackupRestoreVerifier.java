package security;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates backup files before restore operations.
 */
public final class BackupRestoreVerifier {
    private BackupRestoreVerifier() {}

    public static boolean isValidEncryptedBackup(Path backupPath) {
        if (backupPath == null || !Files.exists(backupPath) || !Files.isRegularFile(backupPath)) {
            return false;
        }
        return encryption.EncryptionDetector.isFileEncrypted(backupPath);
    }
}