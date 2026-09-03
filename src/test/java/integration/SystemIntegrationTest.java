package integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import encryption.EncryptionException;
import encryption.EncryptionManager;
import filehandling.LogFileHandler;
import main.BackupManager;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class SystemIntegrationTest {

    private EncryptionManager encryptionManager;
    private LogFileHandler logFileHandler;
    private BackupManager backupManager;
    private DefaultListModel<String> listModel;
    private Properties testSettings;
    private Path tempLogFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        System.setProperty("user.home", tempDir.toString());
        tempLogFile = tempDir.resolve("log.txt");
        encryptionManager = EncryptionManager.getInstance();
        logFileHandler = new LogFileHandler(tempLogFile, encryptionManager);
        testSettings = new Properties();
        backupManager = new BackupManager(testSettings);
        listModel = new DefaultListModel<>();
        testSettings.setProperty("autoBackupEnabled", "true");
        testSettings.setProperty("backupDirectory", tempDir.resolve("backups").toString());
        Files.createDirectories(tempDir.resolve("backups"));
        backupManager.deriveAndSetHmacKey("backup-test-password".toCharArray(), new byte[16]);
        logFileHandler.enableEncryption("integration-password".toCharArray());
        backupManager.deriveAndSetHmacKey("backup-test-password".toCharArray(), new byte[16]);
    }

    @AfterEach
    void cleanup() {
        logFileHandler.clearSensitiveData();
    }

    @Test
    void testEncryptionFileHandlingIntegration() throws Exception {
        logFileHandler.saveText("This is a test entry for encryption integration", listModel);
        flushEdt();

        assertEquals(1, listModel.getSize());
        String timestamp = listModel.getElementAt(0);
        assertTrue(timestamp.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?"));
        assertTrue(Files.exists(tempLogFile));
    }

    @Test
    void testBackupWithEncryptionIntegration() throws Exception {
        logFileHandler.saveText("Test content for backup", listModel);
        flushEdt();

        backupManager.performAutomaticBackup();

        Path backupDir = tempDir.resolve("backups");
        assertTrue(Files.list(backupDir).findFirst().isPresent(), "Should have created backup files");
        Path backupFile = Files.list(backupDir).findFirst().orElseThrow();
        assertTrue(backupFile.getFileName().toString().endsWith(".enc"));
        assertTrue(Files.size(backupFile) > Files.size(tempLogFile), "Backup should include integrity metadata");
    }

    @Test
    void testEncryptionDecryptionRoundTrip() throws EncryptionException {
        String originalText = "Sensitive information that needs encryption";
        char[] password = "testPassword123!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        byte[] encrypted = encryptionManager.encrypt(originalText, password, salt);
        String decrypted = encryptionManager.decrypt(encrypted, password);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testFileHandlingWithLargeContent() throws Exception {
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("This is line ").append(i).append(" of test content. ");
        }

        logFileHandler.saveText(largeContent.toString(), listModel);
        flushEdt();

        assertEquals(1, listModel.getSize());
        assertNotNull(listModel.getElementAt(0));
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
