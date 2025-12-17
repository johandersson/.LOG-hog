package filehandling;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import utils.DateHandler;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the filehandling package
 * Tests EntryLoader and LogFileHandler functionality
 */
public class FileHandlingTest {

    @TempDir
    static Path tempDir;

    private static Path testFilePath;
    private LogFileHandler logFileHandler;
    private EntryLoader entryLoader;
    private DefaultListModel<String> listModel;

    @BeforeAll
    static void setupTestFile() {
        testFilePath = tempDir.resolve("test_log.txt");
    }

    @BeforeEach
    void setUp() throws Exception {
        // Set the test file path for LogFileHandler
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
    void tearDown() throws Exception {
        // Clean up after each test
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
        logFileHandler.clearSensitiveData();
    }

    // ===== ENTRY LOADER TESTS =====

    @Test
    @DisplayName("EntryLoader should load empty list when file doesn't exist")
    void testLoadLogEntriesEmptyFile() throws Exception {
        entryLoader.loadLogEntries(listModel);
        assertEquals(0, listModel.getSize());
    }

    @Test
    @DisplayName("EntryLoader should load entries from file")
    void testLoadLogEntriesWithData() throws Exception {
        // Create test data
        createTestLogFile();

        entryLoader.loadLogEntries(listModel);

        // Should load entries from current month
        assertTrue(listModel.getSize() > 0);

        // Check that entries are sorted (most recent first)
        if (listModel.getSize() >= 2) {
            LocalDateTime first = DateHandler.parseTimestamp(listModel.getElementAt(0));
            LocalDateTime second = DateHandler.parseTimestamp(listModel.getElementAt(1));
            assertTrue(first.isAfter(second) || first.isEqual(second));
        }
    }

    @Test
    @DisplayName("EntryLoader should filter entries by year and month")
    void testLoadFilteredEntries() throws Exception {
        createTestLogFile();

        LocalDateTime now = LocalDateTime.now();
        entryLoader.loadFilteredEntries(listModel, now.getYear(), now.getMonthValue());

        // Should only contain entries from current month
        for (int i = 0; i < listModel.getSize(); i++) {
            LocalDateTime dt = DateHandler.parseTimestamp(listModel.getElementAt(i));
            assertEquals(now.getYear(), dt.getYear());
            assertEquals(now.getMonthValue(), dt.getMonthValue());
        }
    }

    @Test
    @DisplayName("EntryLoader should filter model by year and month")
    void testFilterModelByYearMonth() throws Exception {
        // Populate list model with test data
        createTestLogFile();
        entryLoader.loadLogEntries(listModel);

        LocalDateTime now = LocalDateTime.now();
        DefaultListModel<String> filtered = entryLoader.filterModelByYearMonth(listModel, now.getYear(), now.getMonthValue());

        // Filtered model should only contain current month entries
        for (int i = 0; i < filtered.getSize(); i++) {
            LocalDateTime dt = DateHandler.parseTimestamp(filtered.getElementAt(i));
            assertEquals(now.getYear(), dt.getYear());
            assertEquals(now.getMonthValue(), dt.getMonthValue());
        }
    }

    @Test
    @DisplayName("EntryLoader should load specific entry by timestamp")
    void testLoadEntry() throws Exception {
        createTestLogFile();

        // Load all entries first to get a timestamp
        entryLoader.loadLogEntries(listModel);
        assertTrue(listModel.getSize() > 0);

        String timestamp = listModel.getElementAt(0);
        String entry = entryLoader.loadEntry(timestamp);

        assertNotNull(entry);
        assertFalse(entry.isEmpty());
        // Entry should not contain the timestamp line itself
        assertFalse(entry.contains(timestamp.trim()));
    }

    @Test
    @DisplayName("EntryLoader should return empty string for non-existent entry")
    void testLoadEntryNonExistent() {
        String entry = entryLoader.loadEntry("99:99 9999-99-99");
        assertEquals("", entry);
    }

    @Test
    @DisplayName("EntryLoader should get recent log entries")
    void testGetRecentLogEntries() throws Exception {
        createTestLogFile();

        List<String> recent = entryLoader.getRecentLogEntries(5);
        assertNotNull(recent);
        assertTrue(recent.size() <= 5); // Should not exceed requested count

        // Should be sorted most recent first
        for (int i = 0; i < recent.size() - 1; i++) {
            LocalDateTime current = DateHandler.parseTimestamp(recent.get(i));
            LocalDateTime next = DateHandler.parseTimestamp(recent.get(i + 1));
            assertTrue(current.isAfter(next) || current.isEqual(next));
        }
    }

    // ===== LOG FILE HANDLER TESTS =====

    @Test
    @DisplayName("LogFileHandler should save text and create timestamp")
    void testSaveText() throws Exception {
        String testText = "This is a test entry";

        logFileHandler.saveText(testText, listModel);

        // File should exist
        assertTrue(Files.exists(testFilePath));

        // List model should contain the new entry
        assertEquals(1, listModel.getSize());

        // Verify file contents
        List<String> lines = Files.readAllLines(testFilePath);
        assertTrue(lines.size() > 0);
        assertTrue(lines.get(0).matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}.*"));
        assertTrue(lines.contains(testText));
    }

    @Test
    @DisplayName("LogFileHandler should not save empty text")
    void testSaveTextEmpty() throws Exception {
        logFileHandler.saveText("", listModel);
        logFileHandler.saveText("   ", listModel);
        logFileHandler.saveText(null, listModel);

        assertFalse(Files.exists(testFilePath));
        assertEquals(0, listModel.getSize());
    }

    @Test
    @DisplayName("LogFileHandler should handle duplicate timestamps")
    void testSaveTextDuplicateTimestamps() throws Exception {
        // Create a scenario where timestamps might collide
        String testText1 = "First entry";
        String testText2 = "Second entry";

        // Save first entry
        logFileHandler.saveText(testText1, listModel);

        // Mock duplicate timestamp by temporarily changing system time concept
        // For testing, we'll just save another entry quickly
        logFileHandler.saveText(testText2, listModel);

        assertEquals(2, listModel.getSize());

        // Verify file has both entries
        List<String> lines = Files.readAllLines(testFilePath);
        boolean foundFirst = false, foundSecond = false;
        for (String line : lines) {
            if (line.contains(testText1)) foundFirst = true;
            if (line.contains(testText2)) foundSecond = true;
        }
        assertTrue(foundFirst && foundSecond);
    }

    @Test
    @DisplayName("LogFileHandler should update existing entry")
    void testUpdateEntry() throws Exception {
        // Save initial entry
        String originalText = "Original text";
        logFileHandler.saveText(originalText, listModel);

        assertEquals(1, listModel.getSize());
        String timestamp = listModel.getElementAt(0);

        // Update the entry
        String updatedText = "Updated text";
        logFileHandler.updateEntry(timestamp, updatedText);

        // Verify the update
        List<String> lines = Files.readAllLines(testFilePath);
        assertTrue(lines.contains(updatedText));
        assertFalse(lines.contains(originalText));
    }

    @Test
    @DisplayName("LogFileHandler should change timestamp")
    void testChangeTimestamp() throws Exception {
        // Save initial entry
        String testText = "Test entry";
        logFileHandler.saveText(testText, listModel);

        String oldTimestamp = listModel.getElementAt(0);
        String newTimestamp = "12:34 2024-01-01";

        logFileHandler.changeTimestamp(oldTimestamp, newTimestamp, listModel);

        // List model should be updated
        assertEquals(1, listModel.getSize());
        assertEquals(newTimestamp, listModel.getElementAt(0));

        // File should contain new timestamp
        List<String> lines = Files.readAllLines(testFilePath);
        assertTrue(lines.contains(newTimestamp));
    }

    @Test
    @DisplayName("LogFileHandler should delete entry")
    void testDeleteEntry() throws Exception {
        // Save two entries
        logFileHandler.saveText("First entry", listModel);
        logFileHandler.saveText("Second entry", listModel);

        assertEquals(2, listModel.getSize());
        String timestampToDelete = listModel.getElementAt(0);

        logFileHandler.deleteEntry(timestampToDelete, listModel);

        // Should have one less entry
        assertEquals(1, listModel.getSize());
        assertNotEquals(timestampToDelete, listModel.getElementAt(0));

        // File should not contain the deleted entry
        List<String> lines = Files.readAllLines(testFilePath);
        boolean foundDeleted = false;
        for (String line : lines) {
            if (line.trim().equals(timestampToDelete.trim())) {
                foundDeleted = true;
                break;
            }
        }
        assertFalse(foundDeleted);
    }

    @Test
    @DisplayName("LogFileHandler should get lines from file")
    void testGetLines() throws Exception {
        createTestLogFile();

        List<String> lines = logFileHandler.getLines();
        assertNotNull(lines);
        assertTrue(lines.size() > 0);

        // For unencrypted files, .LOG header should be preserved
        boolean hasLogHeader = false;
        for (String line : lines) {
            if (".LOG".equals(line.trim().toUpperCase())) {
                hasLogHeader = true;
                break;
            }
        }
        assertTrue(hasLogHeader, "Unencrypted files should preserve .LOG header");
    }

    @Test
    @DisplayName("LogFileHandler should parse entries correctly")
    void testGetParsedEntries() throws Exception {
        createTestLogFile();

        List<List<String>> entries = logFileHandler.getParsedEntries();
        assertNotNull(entries);
        assertTrue(entries.size() > 0);

        // Each entry should be a list of strings
        for (List<String> entry : entries) {
            assertNotNull(entry);
            assertTrue(entry.size() > 0);
            // First line should be a timestamp
            assertTrue(entry.get(0).matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}.*"));
        }
    }

    @Test
    @DisplayName("LogFileHandler should handle file operations gracefully")
    void testFileOperationsWithNonExistentFile() {
        // These operations should not throw exceptions when file doesn't exist
        assertDoesNotThrow(() -> logFileHandler.deleteEntry("nonexistent", listModel));
        assertDoesNotThrow(() -> logFileHandler.changeTimestamp("old", "new", listModel));
        assertDoesNotThrow(() -> logFileHandler.updateEntry("nonexistent", "new text"));
    }

    // ===== EDGE CASE TESTS =====

    @Test
    @DisplayName("EntryLoader should handle files with .LOG1 instead of .LOG")
    void testLoadEntriesWithWrongLogHeader() throws Exception {
        // Create file with .LOG1 instead of .LOG
        List<String> testData = Arrays.asList(
            ".LOG1",  // Wrong header
            "",
            "10:30 2024-12-15",
            "This entry should be loaded despite wrong header",
            "",
            "11:45 2024-12-15",
            "Another entry"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should load entries normally, .LOG1 is not recognized as header
        assertTrue(listModel.getSize() > 0);
    }

    @Test
    @DisplayName("EntryLoader should handle files with .LOG header in wrong position")
    void testLoadEntriesWithLogHeaderInWrongPosition() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp1 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String timestamp2 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), (now.getMinute() + 1) % 60, now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            "",  // Empty first line
            timestamp1,
            "First entry",
            "",
            ".LOG",  // .LOG header in middle of file
            "",
            timestamp2,
            "Second entry after misplaced header"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle misplaced .LOG header gracefully
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
        // Should load entries from current month
        assertTrue(listModel.getSize() >= 1);
    }

    @Test
    @DisplayName("EntryLoader should handle files with .LOG header after content")
    void testLoadEntriesWithLogHeaderAfterContent() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            timestamp,
            "Entry before header",
            "",
            ".LOG",  // .LOG header after content
            "",
            "Some content after header"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle .LOG header after content gracefully
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
    }

    @Test
    @DisplayName("EntryLoader should handle files with case variations of .LOG")
    void testLoadEntriesWithCaseVariationsOfLog() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".log",  // lowercase
            "",
            timestamp,
            "Entry with lowercase header",
            "",
            ".Log",  // mixed case
            "",
            "Content after mixed case header"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle case variations gracefully
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
    }

    @Test
    @DisplayName("EntryLoader should handle files with .LOG and extra whitespace")
    void testLoadEntriesWithLogHeaderWithWhitespace() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            " .LOG ",  // .LOG with leading/trailing spaces
            "",
            timestamp,
            "Entry with spaced header",
            "",
            "\t.LOG\t",  // .LOG with tabs
            "",
            "Content after tabbed header"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle .LOG with extra whitespace gracefully
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
    }

    @Test
    @DisplayName("EntryLoader should handle files with multiple .LOG headers in different positions")
    void testLoadEntriesWithMultipleLogHeadersDifferentPositions() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp1 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String timestamp2 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), (now.getMinute() + 1) % 60, now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".LOG",  // First header at top
            "",
            timestamp1,
            "First entry",
            "",
            ".LOG",  // Second header in middle
            "",
            timestamp2,
            "Second entry",
            "",
            ".LOG",  // Third header at end
            "Content after last header"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle multiple headers in different positions gracefully
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
    }

    @Test
    @DisplayName("EntryLoader should handle files with .LOG header not at the very beginning")
    void testLoadEntriesWithLogHeaderNotAtBeginning() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            "Some random text at top",
            "",
            ".LOG",  // .LOG header not at the very beginning
            "",
            timestamp,
            "Entry after header",
            "",
            "More content"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle .LOG header not at the very beginning gracefully
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
    }

    @Test
    @DisplayName("Demonstrate what happens when .LOG is missing or misplaced")
    void testBehaviorWhenLogHeaderMissingOrMisplaced() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp1 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String timestamp2 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), (now.getMinute() + 1) % 60, now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        // Test 1: File with .LOG at top (normal case)
        List<String> normalData = Arrays.asList(
            ".LOG",
            "",
            timestamp1,
            "Entry 1",
            "",
            timestamp2,
            "Entry 2"
        );
        Files.write(testFilePath, normalData);
        entryLoader.loadLogEntries(listModel);
        int normalCount = listModel.getSize();
        listModel.clear();

        // Test 2: File without .LOG header
        List<String> noHeaderData = Arrays.asList(
            timestamp1,
            "Entry 1",
            "",
            timestamp2,
            "Entry 2"
        );
        Files.write(testFilePath, noHeaderData);
        entryLoader.loadLogEntries(listModel);
        int noHeaderCount = listModel.getSize();
        listModel.clear();

        // Test 3: File with .LOG in middle
        List<String> middleHeaderData = Arrays.asList(
            timestamp1,
            "Entry 1",
            "",
            ".LOG",
            "",
            timestamp2,
            "Entry 2"
        );
        Files.write(testFilePath, middleHeaderData);
        entryLoader.loadLogEntries(listModel);
        int middleHeaderCount = listModel.getSize();
        listModel.clear();

        // Test 4: File with .LOG at end
        List<String> endHeaderData = Arrays.asList(
            timestamp1,
            "Entry 1",
            "",
            timestamp2,
            "Entry 2",
            "",
            ".LOG"
        );
        Files.write(testFilePath, endHeaderData);
        entryLoader.loadLogEntries(listModel);
        int endHeaderCount = listModel.getSize();

        // All scenarios should work and load the same entries
        assertEquals(normalCount, noHeaderCount, "Missing .LOG should work the same as proper .LOG");
        assertEquals(normalCount, middleHeaderCount, "Misplaced .LOG should work the same");
        assertEquals(normalCount, endHeaderCount, "End-placed .LOG should work the same");
        assertTrue(normalCount >= 1, "Should load entries in all scenarios");
    }

    @Test
    @DisplayName("EntryLoader should handle files with multiple .LOG headers")
    void testLoadEntriesWithMultipleLogHeaders() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".LOG",
            ".LOG",  // Duplicate header
            "",
            timestamp,
            "Entry after duplicate headers",
            "",
            ".LOG",  // Another duplicate
            "",
            "Another entry"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle multiple headers gracefully
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
    }

    @Test
    @DisplayName("EntryLoader should handle files with .LOG in middle of content")
    void testLoadEntriesWithLogInContent() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".LOG",
            "",
            timestamp,
            "This is a normal entry",
            ".LOG",  // .LOG in middle of content
            "This line follows .LOG in content",
            "",
            "12:34 2024-12-15",
            "Another entry"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle .LOG in content gracefully (not skip it as header)
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
    }

    @Test
    @DisplayName("EntryLoader should handle files with malformed timestamps")
    void testLoadEntriesWithMalformedTimestamps() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String validTimestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".LOG",
            "",
            "25:99 2024-13-45",  // Invalid timestamp
            "Entry with invalid timestamp",
            "",
            validTimestamp,  // Valid timestamp for current month
            "Entry with valid timestamp",
            "",
            "invalid timestamp format",  // Not a timestamp at all
            "Entry without timestamp"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle malformed timestamps gracefully
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
        // Should still load the valid timestamp entry
        assertTrue(listModel.getSize() >= 1);
    }

    @Test
    @DisplayName("EntryLoader should handle files without .LOG header")
    void testLoadEntriesWithoutLogHeader() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            "",  // No .LOG header
            timestamp,
            "Entry without header",
            "",
            "Another line"
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle missing header gracefully
        assertDoesNotThrow(() -> entryLoader.loadLogEntries(listModel));
    }

    @Test
    @DisplayName("EntryLoader should handle empty files")
    void testLoadEntriesEmptyFile() throws Exception {
        // Create empty file
        Files.write(testFilePath, new byte[0]);

        entryLoader.loadLogEntries(listModel);

        // Should handle empty files gracefully
        assertEquals(0, listModel.getSize());
    }

    @Test
    @DisplayName("EntryLoader should handle files with only .LOG header")
    void testLoadEntriesOnlyHeader() throws Exception {
        List<String> testData = Arrays.asList(".LOG");
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);

        // Should handle files with only header gracefully
        assertEquals(0, listModel.getSize());
    }

    @Test
    @DisplayName("LogFileHandler should handle malformed files in getLines")
    void testGetLinesWithMalformedFile() throws Exception {
        List<String> testData = Arrays.asList(
            ".LOG1",  // Wrong header - should remain in lines since it's not ".LOG"
            "",
            "10:30 2024-12-15",
            "Normal entry",
            "",
            "malformed line with no timestamp"
        );
        Files.write(testFilePath, testData);

        List<String> lines = logFileHandler.getLines();

        // Should return lines without crashing
        assertNotNull(lines);
        assertTrue(lines.size() > 0);
        // Should contain .LOG1 since it's not recognized as header to remove
        assertTrue(lines.contains(".LOG1"));
    }

    @Test
    @DisplayName("LogFileHandler should handle getParsedEntries with malformed data")
    void testGetParsedEntriesWithMalformedData() throws Exception {
        List<String> testData = Arrays.asList(
            ".LOG",
            "",
            "10:30 2024-12-15",
            "Valid entry",
            "",
            "invalid timestamp",
            "Entry without proper timestamp",
            "",
            "25:99 2024-13-45",  // Invalid timestamp
            "Entry with invalid timestamp"
        );
        Files.write(testFilePath, testData);

        List<List<String>> entries = logFileHandler.getParsedEntries();

        // Should parse without crashing
        assertNotNull(entries);
        // Should contain at least the valid entry
        assertTrue(entries.size() >= 1);
    }

    // ===== HELPER METHODS =====

    private void createTestLogFile() throws IOException {
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

        Files.write(testFilePath, testData);
    }

    // Helper method to enable encryption for testing
    private void enableEncryption() throws Exception {
        logFileHandler.enableEncryption("testpassword".toCharArray());
    }

    // Helper method to create a test file without .LOG header (simulating old encrypted files)
    private void createOldEncryptedFile() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        String currentMonthTimestamp1 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            "",  // No .LOG header - simulates old encrypted file
            "",
            currentMonthTimestamp1,
            "This is an entry from old encrypted file",
            "",
            "14:45 2024-11-10",
            "This is an old entry from 2024"
        );

        Files.write(testFilePath, testData);
    }

    // ===== ENCRYPTION TESTS =====

    @Test
    @DisplayName("Encrypted files should preserve .LOG header")
    void testEncryptedFilePreservesLogHeader() throws Exception {
        enableEncryption();

        logFileHandler.saveText("Test entry", listModel);

        List<String> lines = logFileHandler.getLines();
        assertNotNull(lines);
        assertTrue(lines.size() > 0);

        // Should contain .LOG header
        boolean hasLogHeader = false;
        for (String line : lines) {
            if (".LOG".equals(line.trim().toUpperCase())) {
                hasLogHeader = true;
                break;
            }
        }
        assertTrue(hasLogHeader, "Encrypted files should preserve .LOG header");
    }

    @Test
    @DisplayName("Decrypting old encrypted files should add .LOG header")
    void testDecryptOldEncryptedFileAddsLogHeader() throws Exception {
        // Create file without .LOG header (simulating old unencrypted file)
        createOldEncryptedFile();

        // Enable encryption (this will add .LOG header before encrypting)
        enableEncryption();

        // Now disable encryption - should preserve .LOG header
        logFileHandler.disableEncryption();

        // Check that decrypted file has .LOG header
        List<String> lines = Files.readAllLines(testFilePath);
        assertTrue(lines.size() > 0);
        assertEquals(".LOG", lines.get(0).trim().toUpperCase());
    }

    @Test
    @DisplayName("EntryLoader should filter .LOG from UI for encrypted files")
    void testEntryLoaderFiltersLogFromEncryptedFiles() throws Exception {
        enableEncryption();

        logFileHandler.saveText("Test entry", listModel);

        // Load entries - should filter out .LOG
        entryLoader.loadLogEntries(listModel);

        // Should have entries but .LOG should not appear in list model
        assertTrue(listModel.getSize() > 0);
        for (int i = 0; i < listModel.getSize(); i++) {
            assertNotEquals(".LOG", listModel.getElementAt(i).trim().toUpperCase());
        }
    }

    @Test
    @DisplayName("Encrypted files should maintain .LOG through save/load cycles")
    void testEncryptedFileMaintainsLogThroughCycles() throws Exception {
        enableEncryption();

        // Save first entry
        logFileHandler.saveText("First entry", listModel);

        // Save second entry
        logFileHandler.saveText("Second entry", listModel);

        // Reload lines
        List<String> lines = logFileHandler.getLines();

        // Should still have .LOG header
        boolean hasLogHeader = false;
        for (String line : lines) {
            if (".LOG".equals(line.trim().toUpperCase())) {
                hasLogHeader = true;
                break;
            }
        }
        assertTrue(hasLogHeader, "Encrypted files should maintain .LOG header through multiple saves");
    }

    @Test
    @DisplayName("EntryLoader should load entry correctly after encryption unlock")
    void testLoadEntryAfterEncryptionUnlock() throws Exception {
        // Create test data first
        createTestLogFile();

        // Load entries to get a timestamp
        entryLoader.loadLogEntries(listModel);
        assertTrue(listModel.getSize() > 0);
        String originalTimestamp = listModel.getElementAt(0);

        // Load the entry content before encryption
        String originalContent = entryLoader.loadEntry(originalTimestamp);
        assertNotNull(originalContent);
        assertFalse(originalContent.isEmpty());

        // Now enable encryption (simulate locking)
        enableEncryption();

        // Clear list and reload (simulate unlock)
        listModel.clear();
        entryLoader.loadLogEntries(listModel);
        assertTrue(listModel.getSize() > 0);

        // The timestamp should be the same (display format)
        String encryptedTimestamp = listModel.getElementAt(0);
        assertEquals(originalTimestamp, encryptedTimestamp);

        // Load the entry content after encryption
        String encryptedContent = entryLoader.loadEntry(encryptedTimestamp);
        assertNotNull(encryptedContent);
        assertFalse(encryptedContent.isEmpty());

        // Content should be the same
        assertEquals(originalContent, encryptedContent);
    }
}