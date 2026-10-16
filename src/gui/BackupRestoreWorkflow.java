package gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import security.AppPathPolicy;
import security.BackupRestoreVerifier;

final class BackupRestoreWorkflow {
    @FunctionalInterface
    interface Confirmation {
        boolean confirm(String title, String message);
    }

    @FunctionalInterface
    interface Message {
        void show(String title, String message);
    }

    @FunctionalInterface
    interface Restorer {
        void restore(Path backupPath) throws Exception;
    }

    private BackupRestoreWorkflow() {}

    static boolean restore(
            Path activeLog,
            Path backup,
            Confirmation confirmation,
            Message errorMessage,
            Runnable successMessage,
            Restorer restorer) {
        try {
            AppPathPolicy.assertSafeRegularFile(activeLog);
            AppPathPolicy.assertSafeRegularFile(backup);
            if (activeLog.toAbsolutePath().normalize().equals(backup.toAbsolutePath().normalize())
                    || !Files.isRegularFile(backup, LinkOption.NOFOLLOW_LINKS)
                    || !BackupRestoreVerifier.isValidEncryptedBackup(backup)) {
                errorMessage.show("Invalid Backup", "Select a valid encrypted backup file.");
                return false;
            }

            long activeSize = Files.exists(activeLog, LinkOption.NOFOLLOW_LINKS) ? Files.size(activeLog) : 0L;
            long backupSize = Files.size(backup);
            String sizeComparison = "Current log.txt: " + formatSize(activeSize)
                + "<br>Selected backup: " + formatSize(backupSize);

            if (!confirmation.confirm(
                    "Restore Backup",
                    "Are you sure you want to replace the whole file?<br><br>" + sizeComparison)) {
                return false;
            }
            if (!confirmation.confirm(
                    "Confirm Restore",
                    "This will replace the entire open log.txt. This cannot be undone. Continue?")) {
                return false;
            }

            restorer.restore(backup);
            successMessage.run();
            return true;
        } catch (Exception e) {
            errorMessage.show("Restore Failed", "Unable to restore the selected backup. The active log was not restored.");
            return false;
        }
    }

    private static String formatSize(long bytes) {
        return String.format(java.util.Locale.ROOT, "%,d bytes", bytes);
    }
}
