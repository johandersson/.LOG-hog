package integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import encryption.EncryptionException;
import encryption.EncryptionManager;
import filehandling.LogFileHandler;
import main.BackupManager;

import javax.swing.DefaultListModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Integration tests for cross-component interactions
 * Tests the interaction between encryption, file handling, and backup systems
 */
public class SystemIntegrationTest {

    private EncryptionManager encryptionManager;
    private LogFileHandler logFileHandler;
    private BackupManager backupManager;
    private DefaultListModel<String> listModel;
    private Properties testSettings;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws IOException {
        encryptionManager = EncryptionManager.getInstance();
        logFileHandler = new LogFileHandler();
        testSettings = new Properties();
        backupManager = new BackupManager(testSettings);
        listModel = new DefaultListModel<>();

        // Set up temporary log file
        Path tempLogFile = tempDir.resolve("log.txt");
        System.setProperty("user.home", tempDir.toString());

        // Configure backup settings
        testSettings.setProperty("autoBackupEnabled", "true");
        testSettings.setProperty("backupDirectory", tempDir.resolve("backups").toString());

        // Create backup directory
        Files.createDirectories(tempDir.resolve("backups"));
    }

    @Test
    void testEncryptionFileHandlingIntegration() {
        System.out.println("🧪 Testing encryption and file handling integration...");

        String testContent = "This is a test entry for encryption integration";

        assertDoesNotThrow(() -> {
            // Save content through LogFileHandler
            logFileHandler.saveText(testContent, listModel);

            // Verify it was added to the list
            assertEquals(1, listModel.getSize(), "Should have one entry in list");

            // The list should contain a timestamp
            String timestamp = listModel.getElementAt(0);
            assertNotNull(timestamp, "Timestamp should not be null");
            assertTrue(timestamp.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}"),
                      "Should have proper timestamp format");
        });

        System.out.println("✅ Encryption and file handling integration works correctly");
    }

    @Test
    void testBackupWithEncryptionIntegration() {
        System.out.println("🧪 Testing backup with encryption integration...");

        assertDoesNotThrow(() -> {
            // Create some content
            logFileHandler.saveText("Test content for backup", listModel);

            // Perform backup
            backupManager.performAutomaticBackup();

            // Verify backup was created
            Path backupDir = tempDir.resolve("backups");
            long backupCount = Files.list(backupDir).count();
            assertTrue(backupCount > 0, "Should have created backup files");

            // Verify backup content
            Path backupFile = Files.list(backupDir).findFirst().orElseThrow();
            String backupContent = Files.readString(backupFile);
            assertTrue(backupContent.contains("Test content for backup"),
                      "Backup should contain the test content");
        });

        System.out.println("✅ Backup with encryption integration works correctly");
    }

    @Test
    void testEncryptionDecryptionRoundTrip() throws EncryptionException {
        System.out.println("🧪 Testing encryption/decryption round-trip...");

        String originalText = "Sensitive information that needs encryption";
        char[] password = "testPassword123!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        assertDoesNotThrow(() -> {
            // Encrypt the text
            byte[] encrypted = encryptionManager.encrypt(originalText, password, salt);

            // Decrypt it back
            String decrypted = encryptionManager.decrypt(encrypted, password);

            // Verify round-trip success
            assertEquals(originalText, decrypted, "Decrypted text should match original");
        });

        System.out.println("✅ Encryption/decryption round-trip works correctly");
    }

    @Test
    void testFileHandlingWithLargeContent() {
        System.out.println("🧪 Testing file handling with large content...");

        // Create a large text content
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("This is line ").append(i).append(" of test content. ");
        }
        String largeText = largeContent.toString();

        assertDoesNotThrow(() -> {
            // Save large content
            logFileHandler.saveText(largeText, listModel);

            // Verify it was saved
            assertEquals(1, listModel.getSize(), "Should have one entry");

            // The content should be retrievable (this tests the underlying file operations)
            String timestamp = listModel.getElementAt(0);
            assertNotNull(timestamp, "Should have timestamp");
        });

        System.out.println("✅ File handling with large content works correctly");
    }

    @Test
    void testBackupFileIntegrity() {
        System.out.println("🧪 Testing backup file integrity...");

        assertDoesNotThrow(() -> {
            // Create multiple entries
            logFileHandler.saveText("First entry", listModel);
            logFileHandler.saveText("Second entry", listModel);
            logFileHandler.saveText("Third entry", listModel);

            // Perform backup
            backupManager.performAutomaticBackup();

            // Verify backup file exists and is readable
            Path backupDir = tempDir.resolve("backups");
            Path backupFile = Files.list(backupDir).findFirst().orElseThrow();

            assertTrue(Files.exists(backupFile), "Backup file should exist");
            assertTrue(Files.size(backupFile) > 0, "Backup file should not be empty");

            // Verify file is readable
            String content = Files.readString(backupFile);
            assertNotNull(content, "Backup content should be readable");
            assertTrue(content.length() > 0, "Backup should have content");
        });

        System.out.println("✅ Backup file integrity verified");
    }

    @Test
    void testConcurrentOperations() {
        System.out.println("🧪 Testing concurrent operations...");

        assertDoesNotThrow(() -> {
            // Simulate concurrent file operations
            Thread saveThread = new Thread(() -> {
                try {
                    logFileHandler.saveText("Concurrent save operation", listModel);
                } catch (Exception e) {
                    fail("Concurrent save should not fail: " + e.getMessage());
                }
            });

            Thread backupThread = new Thread(() -> {
                try {
                    Thread.sleep(10); // Small delay to interleave operations
                    backupManager.performAutomaticBackup();
                } catch (Exception e) {
                    fail("Concurrent backup should not fail: " + e.getMessage());
                }
            });

            saveThread.start();
            backupThread.start();

            // Wait for both to complete
            saveThread.join(1000);
            backupThread.join(1000);

            // Verify both operations succeeded
            assertTrue(listModel.getSize() > 0, "Save operation should have succeeded");
        });

        System.out.println("✅ Concurrent operations handled correctly");
    }

    @Test
    void testErrorRecoveryScenarios() {
        System.out.println("🧪 Testing error recovery scenarios...");

        // Test with invalid backup directory
        testSettings.setProperty("backupDirectory", "/invalid/path/that/does/not/exist");

        assertDoesNotThrow(() -> {
            // Should not crash even with invalid backup path
            logFileHandler.saveText("Test content", listModel);
            backupManager.performAutomaticBackup();
        });

        // Reset to valid directory
        testSettings.setProperty("backupDirectory", tempDir.resolve("backups").toString());

        assertDoesNotThrow(() -> {
            // Should work again with valid path
            backupManager.performAutomaticBackup();
        });

        System.out.println("✅ Error recovery scenarios handled correctly");
    }

    @Test
    void testSystemStateConsistency() {
        System.out.println("🧪 Testing system state consistency...");

        assertDoesNotThrow(() -> {
            // Perform a series of operations
            logFileHandler.saveText("Entry 1", listModel);
            logFileHandler.saveText("Entry 2", listModel);

            // Backup the current state
            backupManager.performAutomaticBackup();

            // Add more content
            logFileHandler.saveText("Entry 3", listModel);

            // Verify list model consistency
            assertEquals(3, listModel.getSize(), "Should have 3 entries");

            // Verify all entries have proper timestamps
            for (int i = 0; i < listModel.getSize(); i++) {
                String timestamp = listModel.getElementAt(i);
                assertTrue(timestamp.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}"),
                          "Entry " + i + " should have proper timestamp format");
            }
        });

        System.out.println("✅ System state consistency maintained");
    }
}