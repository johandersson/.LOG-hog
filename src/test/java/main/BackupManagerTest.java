package main;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class BackupManagerTest {

    private BackupManager backupManager;
    private Properties testSettings;
    private Path tempLogFile;
    private Path tempBackupDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        testSettings = new Properties();
        backupManager = new BackupManager(testSettings);
        tempLogFile = tempDir.resolve("log.txt");
        Files.writeString(tempLogFile, "Test log content for backup testing");
        tempBackupDir = tempDir.resolve("backups");
        Files.createDirectories(tempBackupDir);
        System.setProperty("user.home", tempDir.toString());
        testSettings.setProperty("backupDirectory", tempBackupDir.toString());
    }

    @Test
    void testIsAutoBackupEnabled() {
        assertFalse(backupManager.isAutoBackupEnabled());
        testSettings.setProperty("autoBackupEnabled", "true");
        assertTrue(backupManager.isAutoBackupEnabled());
        testSettings.setProperty("autoBackupEnabled", "false");
        assertFalse(backupManager.isAutoBackupEnabled());
    }

    @Test
    void testGetAutoBackupDirectory() {
        testSettings.setProperty("backupDirectory", "/custom/backup/path");
        assertEquals("/custom/backup/path", backupManager.getAutoBackupDirectory());
        testSettings.remove("backupDirectory");
        assertEquals(System.getProperty("user.home"), backupManager.getAutoBackupDirectory());
    }

    @Test
    void testPerformAutomaticBackupWhenDisabled() throws Exception {
        testSettings.setProperty("autoBackupEnabled", "false");
        long initialFileCount = Files.list(tempBackupDir).count();
        backupManager.performAutomaticBackup();
        assertEquals(initialFileCount, Files.list(tempBackupDir).count());
    }

    @Test
    void testPerformAutomaticBackupWhenEnabled() throws Exception {
        testSettings.setProperty("autoBackupEnabled", "true");
        backupManager.performAutomaticBackup();

        Path backupFile = Files.list(tempBackupDir).findFirst().orElseThrow();
        assertTrue(backupFile.getFileName().toString().endsWith(".enc"));

        byte[] original = Files.readAllBytes(tempLogFile);
        byte[] backup = Files.readAllBytes(backupFile);
        assertTrue(backup.length >= original.length);
        assertArrayEquals(original, Arrays.copyOf(backup, original.length));
    }

    @Test
    void testPerformAutomaticBackupWithNonexistentLogFile() throws Exception {
        Files.deleteIfExists(tempLogFile);
        testSettings.setProperty("autoBackupEnabled", "true");
        assertDoesNotThrow(() -> backupManager.performAutomaticBackup());
    }

    @Test
    void testPerformAutomaticBackupWithInvalidBackupDirectory() {
        testSettings.setProperty("autoBackupEnabled", "true");
        testSettings.setProperty("backupDirectory", "/invalid/path/that/does/not/exist");
        assertDoesNotThrow(() -> backupManager.performAutomaticBackup());
    }

    @Test
    void testCreateManualBackup() {
        assertNull(backupManager.createManualBackup());
    }

    @Test
    void testBackupFileNaming() throws Exception {
        testSettings.setProperty("autoBackupEnabled", "true");
        backupManager.performAutomaticBackup();

        String filename = Files.list(tempBackupDir).findFirst().orElseThrow().getFileName().toString();
        assertTrue(filename.startsWith("loghog-auto-backup-"));
        assertTrue(filename.endsWith(".enc"));
        assertTrue(filename.matches("loghog-auto-backup-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.enc"));
    }

    @Test
    void testMultipleBackupsCreateSeparateFiles() throws Exception {
        testSettings.setProperty("autoBackupEnabled", "true");
        backupManager.performAutomaticBackup();
        Thread.sleep(1100);
        backupManager.performAutomaticBackup();
        assertEquals(2, Files.list(tempBackupDir).count());
    }

    @Test
    void testBackupOverwritesExistingFile() throws Exception {
        testSettings.setProperty("autoBackupEnabled", "true");
        backupManager.performAutomaticBackup();
        Thread.sleep(1100);
        Files.writeString(tempLogFile, "Modified content", StandardCharsets.UTF_8);
        backupManager.performAutomaticBackup();

        Path mostRecentBackup = Files.list(tempBackupDir)
            .max((p1, p2) -> p1.getFileName().compareTo(p2.getFileName()))
            .orElseThrow();
        byte[] backup = Files.readAllBytes(mostRecentBackup);
        byte[] modified = "Modified content".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(modified, Arrays.copyOf(backup, modified.length));
    }
}
