package gui;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import encryption.EncryptionManager;
import encryption.FileEncryptionManager;

class BackupRestoreWorkflowTest {
    @TempDir
    Path tempDir;

    @Test
    void confirmsTwiceShowsBothFileSizesAndRestoresOnlyAfterBothYesAnswers() throws Exception {
        Path active = Files.writeString(tempDir.resolve("log.txt"), "current log");
        Path backup = encryptedBackup("backup");
        List<String> prompts = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int[] confirmations = {0};
        int[] restores = {0};
        int[] successes = {0};

        boolean restored = BackupRestoreWorkflow.restore(
            active,
            backup,
            (title, message) -> {
                confirmations[0]++;
                prompts.add(message);
                return true;
            },
            (title, message) -> errors.add(message),
            () -> successes[0]++,
            selected -> {
                assertEquals(backup, selected);
                restores[0]++;
            }
        );

        assertTrue(restored);
        assertEquals(2, confirmations[0]);
        assertEquals(1, restores[0]);
        assertEquals(1, successes[0]);
        assertTrue(errors.isEmpty());
        assertTrue(prompts.get(0).contains("Are you sure you want to replace the whole file?"));
        assertTrue(prompts.get(0).contains("Current log.txt: " + Files.size(active) + " bytes"));
        assertTrue(prompts.get(0).contains("Selected backup: " + Files.size(backup) + " bytes"));
        assertTrue(prompts.get(1).contains("cannot be undone"));
    }

    @Test
    void decliningEitherConfirmationNeverRestores() throws Exception {
        Path active = Files.writeString(tempDir.resolve("log.txt"), "current");
        Path backup = encryptedBackup("backup");

        for (int declineAt = 1; declineAt <= 2; declineAt++) {
            int[] confirmations = {0};
            int[] restores = {0};
            boolean restored = BackupRestoreWorkflow.restore(
                active,
                backup,
                (title, message) -> ++confirmations[0] != declineAt,
                (title, message) -> fail(message),
                () -> fail("Cancellation must not report success"),
                selected -> restores[0]++
            );
            assertFalse(restored);
            assertEquals(declineAt, confirmations[0]);
            assertEquals(0, restores[0]);
        }
    }

    private Path encryptedBackup(String content) throws Exception {
        Path backup = tempDir.resolve("backup.enc");
        byte[] salt = EncryptionManager.getInstance().generateSalt();
        FileEncryptionManager manager = new FileEncryptionManager(backup, EncryptionManager.getInstance());
        try {
            manager.setEncryption("restore-workflow-test-password".toCharArray(), salt);
            manager.encryptFileFromLines(List.of(".LOG", "", content));
            return backup;
        } finally {
            manager.clearSensitiveData();
            java.util.Arrays.fill(salt, (byte) 0);
        }
    }
}
