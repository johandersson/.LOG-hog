package filehandling;

import org.junit.jupiter.api.*;
import encryption.EncryptionManager;
import utils.DateHandler;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit tests for the filehandling package
 * Tests EntryLoader and LogFileHandler functionality
 */
public class FileHandlingJUnitTest {

    private static Path testFilePath;
    private LogFileHandler logFileHandler;
    private EntryLoader entryLoader;
    private DefaultListModel<String> listModel;

    @BeforeAll
    static void setupAll() throws Exception {
        testFilePath = Files.createTempFile("filehandling_junit_test", ".txt");
    }

    @AfterAll
    static void cleanupAll() throws Exception {
        Files.deleteIfExists(testFilePath);
    }

    @BeforeEach
    void setup() throws Exception {
        LogFileHandler.setTestFilePath(testFilePath);
        logFileHandler = new LogFileHandler();
        entryLoader = new EntryLoader(logFileHandler);
        listModel = new DefaultListModel<>();

        // Ensure clean state
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
    }

    @Test
    void testLoadLogEntriesEmptyFile() throws Exception {
        System.out.println("Test: EntryLoader should handle empty file...");
        entryLoader.loadLogEntries(listModel);
        assertEquals(0, listModel.getSize(), "Empty file should result in empty list");
        System.out.println("✓ PASS: Empty file handled correctly");
    }

    @Test
    void testLoadLogEntriesWithData() throws Exception {
        System.out.println("Test: EntryLoader should load entries with data...");
        createTestLogFile();
        entryLoader.loadLogEntries(listModel);
        assertTrue(listModel.getSize() > 0, "Should load entries from file with data");
        System.out.println("✓ PASS: Entries loaded successfully");
    }

    @Test
    void testLoadFilteredEntries() throws Exception {
        System.out.println("Test: EntryLoader should filter entries by year and month...");
        createTestLogFile();
        entryLoader.loadFilteredEntries(listModel, LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue());

        boolean allCorrectMonth = true;
        for (int i = 0; i < listModel.getSize(); i++) {
            String displayTs = listModel.getElementAt(i);
            // Strip suffix for parsing
            String rawTs = displayTs.replaceAll(" \\([0-9]+\\)$", "");
            LocalDateTime dt = DateHandler.parseTimestamp(rawTs);
            if (dt.getYear() != LocalDateTime.now().getYear() ||
                dt.getMonthValue() != LocalDateTime.now().getMonthValue()) {
                allCorrectMonth = false;
                break;
            }
        }

        assertTrue(allCorrectMonth, "All entries should be from current month");
        System.out.println("✓ PASS: Filtered entries are from current month");
    }

    @Test
    void testFilterModelByYearMonth() throws Exception {
        System.out.println("Test: EntryLoader should filter model by year and month...");
        createTestLogFile();
        entryLoader.loadLogEntries(listModel);

        LocalDateTime now = LocalDateTime.now();
        DefaultListModel<String> filtered = entryLoader.filterModelByYearMonth(listModel, now.getYear(), now.getMonthValue());

        boolean allCorrectMonth = true;
        for (int i = 0; i < filtered.getSize(); i++) {
            LocalDateTime dt = DateHandler.parseTimestamp(filtered.getElementAt(i));
            if (dt.getYear() != now.getYear() || dt.getMonthValue() != now.getMonthValue()) {
                allCorrectMonth = false;
                break;
            }
        }

        assertTrue(allCorrectMonth, "Filtered model should contain only current month entries");
        System.out.println("✓ PASS: Filtered model contains only current month entries");
    }

    @Test
    void testLoadEntry() throws Exception {
        System.out.println("Test: EntryLoader should load specific entry by timestamp...");
        createTestLogFile();

        entryLoader.loadLogEntries(listModel);
        assertTrue(listModel.getSize() > 0, "Should have entries to test");

        String timestamp = listModel.getElementAt(0);
        String content = entryLoader.loadEntry(timestamp);
        assertNotNull(content, "Should load content for valid timestamp");
        assertFalse(content.isEmpty(), "Content should not be empty");
        System.out.println("✓ PASS: Entry loaded successfully");
    }

    @Test
    void testLoadEntryNonExistent() throws Exception {
        System.out.println("Test: EntryLoader should return empty string for non-existent timestamp...");
        createTestLogFile();

        String nonExistentTimestamp = "25:99 9999-99-99";
        String content = entryLoader.loadEntry(nonExistentTimestamp);
        assertEquals("", content, "Should return empty string for non-existent timestamp");
        System.out.println("✓ PASS: Non-existent entry handled correctly");
    }

    @Test
    void testLoadEntryEncrypted() throws Exception {
        System.out.println("Test: EntryLoader should handle encrypted files correctly...");

        // Use a unique test file for this test
        Path uniqueTestFile = Files.createTempFile("encrypted_test", ".txt");
        LogFileHandler uniqueHandler = new LogFileHandler();
        LogFileHandler.setTestFilePath(uniqueTestFile);
        EntryLoader uniqueLoader = new EntryLoader(uniqueHandler);
        DefaultListModel<String> uniqueModel = new DefaultListModel<>();

        try {
            createTestLogFile(uniqueTestFile);

            uniqueLoader.loadLogEntries(uniqueModel);
            assertTrue(uniqueModel.getSize() > 0, "Should have entries to test");

            String originalTimestamp = uniqueModel.getElementAt(0);
            String originalContent = uniqueLoader.loadEntry(originalTimestamp);
            assertNotNull(originalContent, "Should load original content");
            assertFalse(originalContent.isEmpty(), "Original content should not be empty");

            // Enable encryption with proper salt
            byte[] salt = EncryptionManager.getInstance().generateSalt();
            uniqueHandler.setEncryption("testpassword".toCharArray(), salt);

            uniqueModel.clear();
            uniqueLoader.loadLogEntries(uniqueModel);
            assertTrue(uniqueModel.getSize() > 0, "Should have entries after encryption");

            String encryptedTimestamp = uniqueModel.getElementAt(0);
            String encryptedContent = uniqueLoader.loadEntry(encryptedTimestamp);

            assertEquals(originalTimestamp, encryptedTimestamp, "Timestamps should match");
            assertNotNull(encryptedContent, "Should load encrypted content");
            assertEquals(originalContent, encryptedContent, "Content should be preserved through encryption cycle");

            // Clean up
            try {
                uniqueHandler.disableEncryption();
            } catch (Exception e) {
                // Ignore
            }
        } finally {
            // Restore original test file path
            LogFileHandler.setTestFilePath(testFilePath);
            Files.deleteIfExists(uniqueTestFile);
        }

        System.out.println("✓ PASS: Encryption cycle preserved content");
    }

    @Test
    void testEncryptionUnlockBug() throws Exception {
        System.out.println("Test: Investigating encryption unlock bug - timestamps load but no content...");

        // Use a unique test file for this test
        Path uniqueTestFile = Files.createTempFile("unlock_bug_test", ".txt");
        LogFileHandler uniqueHandler = new LogFileHandler();
        LogFileHandler.setTestFilePath(uniqueTestFile);
        EntryLoader uniqueLoader = new EntryLoader(uniqueHandler);
        DefaultListModel<String> uniqueModel = new DefaultListModel<>();

        try {
            // Create test file with content
            createTestLogFile(uniqueTestFile);

            // Load entries without encryption
            uniqueLoader.loadLogEntries(uniqueModel);
            assertTrue(uniqueModel.getSize() > 0, "Should have entries");

            String timestamp = uniqueModel.getElementAt(0);
            String originalContent = uniqueLoader.loadEntry(timestamp);
            assertFalse(originalContent.isEmpty(), "Should have original content");

            // Enable encryption with proper salt
            byte[] salt = EncryptionManager.getInstance().generateSalt();
            uniqueHandler.setEncryption("testpassword".toCharArray(), salt);

            // Clear cache and reload
            uniqueModel.clear();
            uniqueLoader.loadLogEntries(uniqueModel);
            assertTrue(uniqueModel.getSize() > 0, "Should have encrypted entries");

            String encryptedTimestamp = uniqueModel.getElementAt(0);
            String encryptedContent = uniqueLoader.loadEntry(encryptedTimestamp);

            // This is where the bug manifests - content might be empty
            assertFalse(encryptedContent.isEmpty(),
                "BUG: Content should not be empty after encryption - this indicates the unlock bug");

            System.out.println("✓ PASS: Content loaded correctly after encryption");
        } finally {
            // Restore original test file path
            LogFileHandler.setTestFilePath(testFilePath);
            Files.deleteIfExists(uniqueTestFile);
        }
    }

    private void createTestLogFile() throws IOException {
        createTestLogFile(testFilePath);
    }

    private void createTestLogFile(Path filePath) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String currentMonthTimestamp1 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String currentMonthTimestamp2 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), (now.getMinute() + 1) % 60, now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".LOG",
            "",
            currentMonthTimestamp1,
            "This is an entry from current month",
            "With multiple lines",
            "",
            currentMonthTimestamp2,
            "This is another entry from current month",
            "",
            "14:45 2024-11-10",
            "This is an entry from November 2024",
            "",
            "16:20 2023-06-15",
            "This is an old entry from 2023"
        );

        Files.write(filePath, testData);
    }
}