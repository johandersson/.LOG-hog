package filehandling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import encryption.EncryptionManager;
import encryption.FileEncryptionManager;

public class EncryptedIncrementalWriteTest {
    @org.junit.jupiter.api.Test
    void appendsEncryptedEntriesWithoutRewritingBaseSnapshot() throws Exception {
        Path baseFile = Files.createTempFile("loghog-incremental-", ".enc");
        Path journalFile = baseFile.resolveSibling(baseFile.getFileName().toString() + ".journal.enc");
        try {
            EncryptionManager encryptionManager = EncryptionManager.getInstance();
            byte[] salt = encryptionManager.generateSalt();
            char[] password = "testPassword123!".toCharArray();
            FileEncryptionManager fileEncryptionManager = new FileEncryptionManager(baseFile, encryptionManager);
            fileEncryptionManager.setEncryption(password, salt);

            List<String> seedLines = List.of(
                ".LOG",
                "",
                LogFileFormat.createEntry("01:00 2026-07-06", "first entry")
            );
            fileEncryptionManager.encryptFileFromLines(seedLines);
            long baseSizeBefore = Files.size(baseFile);

            FileCache cache = new FileCache();
            EntryEditor editor = new EntryEditor(baseFile, fileEncryptionManager, cache);
            editor.saveEntry("second entry", "01:01 2026-07-06", true);

            assertEquals(baseSizeBefore, Files.size(baseFile));
            assertTrue(Files.exists(journalFile));

            editor.compactEncryptedJournal();

            assertFalse(Files.exists(journalFile));

            List<String> merged = editor.getMergedEncryptedWorkingLines();
            assertTrue(merged.stream().anyMatch(line -> line.contains("first entry")));
            assertTrue(merged.stream().anyMatch(line -> line.contains("second entry")));
        } finally {
            Files.deleteIfExists(journalFile);
            Files.deleteIfExists(baseFile);
        }
    }
}