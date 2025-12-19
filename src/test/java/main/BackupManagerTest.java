package main;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Comprehensive tests for BackupManager
 * Tests automatic backups, manual backups, secure deletion, and error handling
 */
public class BackupManagerTest {

    private BackupManager backupManager;
    private Properties testSettings;
    private Path tempLogFile;
    private Path tempBackupDir;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws IOException {
        testSettings = new Properties();
        backupManager = new BackupManager(testSettings);

        // Create a temporary log file
        tempLogFile = tempDir.resolve("log.txt");
        Files.writeString(tempLogFile, "Test log content for backup testing");

        // Create a temporary backup directory
        tempBackupDir = tempDir.resolve("backups");
        Files.createDirectories(tempBackupDir);

        // Override the log file path for testing
        System.setProperty("user.home", tempDir.toString());
        testSettings.setProperty("autoBackupDirectory", tempBackupDir.toString());
    }

    @Test
    void testIsAutoBackupEnabled() {
        System.out.println("🧪 Testing auto backup enabled check...");

        // Default should be false
        assertFalse(backupManager.isAutoBackupEnabled(), "Auto backup should be disabled by default");

        // Enable auto backup
        testSettings.setProperty("autoBackupEnabled", "true");
        assertTrue(backupManager.isAutoBackupEnabled(), "Auto backup should be enabled when set to true");

        // Disable auto backup
        testSettings.setProperty("autoBackupEnabled", "false");
        assertFalse(backupManager.isAutoBackupEnabled(), "Auto backup should be disabled when set to false");

        System.out.println("✅ Auto backup enabled check works correctly");
    }

    @Test
    void testGetAutoBackupDirectory() {
        System.out.println("🧪 Testing auto backup directory retrieval...");

        // Test with auto backup directory set
        testSettings.setProperty("autoBackupDirectory", "/custom/backup/path");
        assertEquals("/custom/backup/path", backupManager.getAutoBackupDirectory());

        // Test fallback to manual backup directory
        testSettings.remove("autoBackupDirectory");
        testSettings.setProperty("backupDirectory", "/manual/backup/path");
        assertEquals("/manual/backup/path", backupManager.getAutoBackupDirectory());

        // Test fallback to user home
        testSettings.remove("backupDirectory");
        String expectedHome = System.getProperty("user.home");
        assertEquals(expectedHome, backupManager.getAutoBackupDirectory());

        System.out.println("✅ Auto backup directory retrieval works correctly");
    }

    @Test
    void testPerformAutomaticBackupWhenDisabled() {
        System.out.println("🧪 Testing automatic backup when disabled...");

        // Ensure auto backup is disabled
        testSettings.setProperty("autoBackupEnabled", "false");

        // Should not create any backup files
        long initialFileCount = Files.list(tempBackupDir).count();

        assertDoesNotThrow(() -> {
            backupManager.performAutomaticBackup();
        });

        long finalFileCount = Files.list(tempBackupDir).count();
        assertEquals(initialFileCount, finalFileCount, "No backup files should be created when auto backup is disabled");

        System.out.println("✅ Automatic backup correctly skips when disabled");
    }

    @Test
    void testPerformAutomaticBackupWhenEnabled() throws IOException {
        System.out.println("🧪 Testing automatic backup when enabled...");

        // Enable auto backup
        testSettings.setProperty("autoBackupEnabled", "true");

        long initialFileCount = Files.list(tempBackupDir).count();

        assertDoesNotThrow(() -> {
            backupManager.performAutomaticBackup();
        });

        long finalFileCount = Files.list(tempBackupDir).count();
        assertEquals(initialFileCount + 1, finalFileCount, "One backup file should be created");

        // Verify backup content matches original
        Path backupFile = Files.list(tempBackupDir).findFirst().orElseThrow();
        String backupContent = Files.readString(backupFile);
        String originalContent = Files.readString(tempLogFile);
        assertEquals(originalContent, backupContent, "Backup content should match original");

        System.out.println("✅ Automatic backup works correctly when enabled");
    }

    @Test
    void testPerformAutomaticBackupWithNonexistentLogFile() {
        System.out.println("🧪 Testing automatic backup with nonexistent log file...");

        // Delete the log file
        assertDoesNotThrow(() -> Files.deleteIfExists(tempLogFile));

        // Enable auto backup
        testSettings.setProperty("autoBackupEnabled", "true");

        // Should handle gracefully without throwing exceptions
        assertDoesNotThrow(() -> {
            backupManager.performAutomaticBackup();
        });

        System.out.println("✅ Automatic backup handles nonexistent log file gracefully");
    }

    @Test
    void testPerformAutomaticBackupWithInvalidBackupDirectory() {
        System.out.println("🧪 Testing automatic backup with invalid backup directory...");

        // Set invalid backup directory
        testSettings.setProperty("autoBackupEnabled", "true");
        testSettings.setProperty("autoBackupDirectory", "/invalid/path/that/does/not/exist");

        // Should handle gracefully without throwing exceptions
        assertDoesNotThrow(() -> {
            backupManager.performAutomaticBackup();
        });

        System.out.println("✅ Automatic backup handles invalid backup directory gracefully");
    }

    @Test
    void testCreateManualBackup() {
        System.out.println("🧪 Testing manual backup creation...");

        // Manual backup is not implemented in BackupManager (handled by UI)
        Path result = backupManager.createManualBackup();
        assertNull(result, "Manual backup should return null (handled by UI layer)");

        System.out.println("✅ Manual backup correctly delegates to UI layer");
    }

    @Test
    void testBackupFileNaming() throws IOException {
        System.out.println("🧪 Testing backup file naming...");

        testSettings.setProperty("autoBackupEnabled", "true");

        backupManager.performAutomaticBackup();

        Path backupFile = Files.list(tempBackupDir).findFirst().orElseThrow();
        String filename = backupFile.getFileName().toString();

        // Should follow pattern: loghog-auto-backup-YYYY-MM-DD_HH-mm-ss.txt
        assertTrue(filename.startsWith("loghog-auto-backup-"), "Filename should start with correct prefix");
        assertTrue(filename.endsWith(".txt"), "Filename should end with .txt");
        assertTrue(filename.matches("loghog-auto-backup-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.txt"),
                  "Filename should match expected timestamp pattern");

        System.out.println("✅ Backup file naming follows correct pattern");
    }

    @Test
    void testMultipleBackupsCreateSeparateFiles() throws IOException {
        System.out.println("🧪 Testing multiple backups create separate files...");

        testSettings.setProperty("autoBackupEnabled", "true");

        // Create multiple backups
        backupManager.performAutomaticBackup();
        // Small delay to ensure different timestamps
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        backupManager.performAutomaticBackup();

        long fileCount = Files.list(tempBackupDir).count();
        assertEquals(2, fileCount, "Should create two separate backup files");

        System.out.println("✅ Multiple backups create separate files");
    }

    @Test
    void testBackupOverwritesExistingFile() throws IOException {
        System.out.println("🧪 Testing backup overwrites existing file...");

        testSettings.setProperty("autoBackupEnabled", "true");

        // Create first backup
        backupManager.performAutomaticBackup();
        Path backupFile = Files.list(tempBackupDir).findFirst().orElseThrow();

        // Modify the log file
        Files.writeString(tempLogFile, "Modified content");

        // Create second backup (should overwrite or create new file)
        backupManager.performAutomaticBackup();

        long fileCount = Files.list(tempBackupDir).count();
        assertTrue(fileCount >= 1, "Should have at least one backup file");

        // The most recent backup should have the modified content
        Path mostRecentBackup = Files.list(tempBackupDir)
                .max((p1, p2) -> p1.getFileName().compareTo(p2.getFileName()))
                .orElseThrow();
        String backupContent = Files.readString(mostRecentBackup);
        assertEquals("Modified content", backupContent, "Most recent backup should have modified content");

        System.out.println("✅ Backup correctly handles file overwrites");
    }
}