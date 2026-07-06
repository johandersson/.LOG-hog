package filehandling;

import utils.DateHandler;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
// Unused import removed for PMD compliance
import java.util.*;

/**
 * Comprehensive tests for the filehandling package
 * Tests EntryLoader and LogFileHandler functionality
 * Converted from JUnit to pure Java main method
 */
public class FileHandlingTest {

    private static Path testFilePath;
    private LogFileHandler logFileHandler;
    private EntryLoader entryLoader;
    private DefaultListModel<String> listModel;

    public static void main(String[] args) throws Exception {
        testsupport.TestLog.out("=== FileHandling Test Suite ===\n");

        FileHandlingTest test = new FileHandlingTest();
        test.runAllTests();
    }

    private void runAllTests() throws Exception {
        // Setup
        testFilePath = Files.createTempFile("filehandling_test", ".txt");

        try {
            setup();

            // Run all tests
            testLoadLogEntriesEmptyFile();
            testLoadLogEntriesWithData();
            testLoadFilteredEntries();
            testFilterModelByYearMonth();
            testLoadEntry();
            testLoadEntryNonExistent();
            testLoadEntryWithDuplicateSuffix();
            testLoadEntryRawVsDisplayTimestamp();
            testGetRecentLogEntries();
            testLoadEntryNullEmpty();
            testLoadEntryComplexContent();
            testLoadEntryEncrypted();
            testTimestampHandling();
            testSaveText();
            testSaveTextEmpty();
            testSaveTextDuplicateTimestamps();
            testUpdateEntry();
            testChangeTimestamp();
            testDeleteEntry();
            testGetLines();
            testGetParsedEntries();
            testFileOperationsWithNonExistentFile();
            testLoadEntriesWithWrongLogHeader();
            testLoadEntriesWithLogHeaderInWrongPosition();
            testLoadEntriesWithLogHeaderAfterContent();
            testLoadEntriesWithCaseVariationsOfLog();
            testLoadEntriesWithLogHeaderWithWhitespace();
            testLoadEntriesWithMultipleLogHeadersDifferentPositions();
            testLoadEntriesWithLogHeaderNotAtBeginning();
            testBehaviorWhenLogHeaderMissingOrMisplaced();
            testLoadEntriesWithMultipleLogHeaders();
            testLoadEntriesWithLogInContent();
            testLoadEntriesWithMalformedTimestamps();
            testLoadEntriesWithoutLogHeader();
            testLoadEntriesEmptyFile();
            testLoadEntriesOnlyHeader();
            testGetLinesWithMalformedFile();
            testGetParsedEntriesWithMalformedData();
            testEncryptedFilePreservesLogHeader();
            testDecryptOldEncryptedFileAddsLogHeader();
            testEntryLoaderFiltersLogFromEncryptedFiles();
            testEncryptedFileMaintainsLogThroughCycles();
            testLoadEntryAfterEncryptionUnlock();

            testsupport.TestLog.out("\n=== All FileHandling tests completed successfully! ===");

        } finally {
            // Cleanup
            cleanup();
        }
    }

    private void setup() throws Exception {
        LogFileHandler.setTestFilePath(testFilePath);
        logFileHandler = new LogFileHandler();
        entryLoader = new EntryLoader(logFileHandler);
        listModel = new DefaultListModel<>();

        // Ensure clean state
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
    }

    private void cleanup() throws Exception {
        if (Files.exists(testFilePath)) {
            Files.delete(testFilePath);
        }
        logFileHandler.clearSensitiveData();
    }

    // ===== TEST METHODS =====

    private void testLoadLogEntriesEmptyFile() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should load empty list when file doesn't exist...");
        entryLoader.loadLogEntries(listModel);
        if (listModel.getSize() == 0) {
            testsupport.TestLog.out("✓ PASS");
        } else {
            testsupport.TestLog.out("✗ FAIL: Expected empty list, got " + listModel.getSize() + " items");
        }
    }

    private void testLoadLogEntriesWithData() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should load entries from file...");
        createTestLogFile();
        entryLoader.loadLogEntries(listModel);

        if (listModel.getSize() > 0) {
            testsupport.TestLog.out("✓ PASS: Loaded " + listModel.getSize() + " entries");

            // Check sorting (most recent first)
            if (listModel.getSize() >= 2) {
                LocalDateTime first = DateHandler.parseTimestamp(listModel.getElementAt(0));
                LocalDateTime second = DateHandler.parseTimestamp(listModel.getElementAt(1));
                if (first.isAfter(second) || first.isEqual(second)) {
                    testsupport.TestLog.out("✓ PASS: Entries are properly sorted");
                } else {
                    testsupport.TestLog.out("✗ FAIL: Entries are not properly sorted");
                }
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: No entries loaded");
        }
    }

    private void testLoadFilteredEntries() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should filter entries by year and month...");
        createTestLogFile();

        LocalDateTime now = LocalDateTime.now();
        entryLoader.loadFilteredEntries(listModel, now.getYear(), now.getMonthValue());

        boolean allCorrectMonth = true;
        for (int i = 0; i < listModel.getSize(); i++) {
            String displayTs = listModel.getElementAt(i);
            // Strip suffix for parsing
            String rawTs = displayTs.replaceAll(" \\([0-9]+\\)$", "");
            LocalDateTime dt = DateHandler.parseTimestamp(rawTs);
            if (dt.getYear() != now.getYear() || dt.getMonthValue() != now.getMonthValue()) {
                allCorrectMonth = false;
                break;
            }
        }

        if (allCorrectMonth) {
            testsupport.TestLog.out("✓ PASS: All entries are from current month");
        } else {
            testsupport.TestLog.out("✗ FAIL: Some entries are not from current month");
        }
    }

    private void testFilterModelByYearMonth() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should filter model by year and month...");
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

        if (allCorrectMonth) {
            testsupport.TestLog.out("✓ PASS: Filtered model contains only current month entries");
        } else {
            testsupport.TestLog.out("✗ FAIL: Filtered model contains entries from other months");
        }
    }

    private void testLoadEntry() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should load specific entry by timestamp...");
        createTestLogFile();

        entryLoader.loadLogEntries(listModel);
        if (listModel.getSize() == 0) {
            testsupport.TestLog.out("✗ FAIL: No entries to test loading");
            return;
        }

        String timestamp = listModel.getElementAt(0);
        String entry = entryLoader.loadEntry(timestamp);

        if (entry != null && !entry.isEmpty()) {
            testsupport.TestLog.out("✓ PASS: Entry loaded successfully");
            if (!entry.contains(timestamp.trim())) {
                testsupport.TestLog.out("✓ PASS: Entry content excludes timestamp line");
            } else {
                testsupport.TestLog.out("✗ FAIL: Entry content includes timestamp line");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: Entry content is empty");
        }
    }

    private void testLoadEntryNonExistent() {
        testsupport.TestLog.out("Test: EntryLoader should return empty string for non-existent entry...");
        String entry = entryLoader.loadEntry("99:99 9999-99-99");
        if ("".equals(entry)) {
            testsupport.TestLog.out("✓ PASS: Correctly returned empty string");
        } else {
            testsupport.TestLog.out("✗ FAIL: Should return empty string for non-existent entry");
        }
    }

    private void testLoadEntryWithDuplicateSuffix() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should load entry with duplicate timestamp suffix...");
        // Create entries that will have duplicate timestamps
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        // Create file with duplicate timestamps
        List<String> testData = Arrays.asList(
            timestamp,
            "First entry",
            "",
            timestamp,
            "Second entry with same timestamp",
            "",
            timestamp,
            "Third entry with same timestamp",
            ""
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);
        testsupport.TestLog.out("Display timestamps:");
        for (int i = 0; i < listModel.getSize(); i++) {
            testsupport.TestLog.out("  " + i + ": '" + listModel.getElementAt(i) + "'");
        }

        if (listModel.getSize() < 3) {
            testsupport.TestLog.out("✗ FAIL: Should have at least 3 entries");
            return;
        }

        boolean allLoaded = true;
        for (int i = 0; i < listModel.getSize(); i++) {
            String displayTimestamp = listModel.getElementAt(i);
            testsupport.TestLog.out("Loading: '" + displayTimestamp + "'");
            String content = entryLoader.loadEntry(displayTimestamp);
            testsupport.TestLog.out("  Content length: " + content.length());
            if (content == null || content.isEmpty()) {
                allLoaded = false;
                testsupport.TestLog.out("  ✗ FAIL: Empty content");
                break;
            }
            if (content.contains(displayTimestamp.trim())) {
                allLoaded = false;
                testsupport.TestLog.out("  ✗ FAIL: Content contains timestamp");
                break;
            }
            testsupport.TestLog.out("  ✓ OK");
        }

        if (allLoaded) {
            testsupport.TestLog.out("✓ PASS: All entries with duplicate suffixes loaded correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: Some entries with duplicate suffixes failed to load");
        }
    }

    private void testLoadEntryRawVsDisplayTimestamp() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle raw vs display timestamp consistency...");
        createTestLogFile();

        entryLoader.loadLogEntries(listModel);
        if (listModel.getSize() == 0) {
            testsupport.TestLog.out("✗ FAIL: No entries to test");
            return;
        }

        String displayTimestamp = listModel.getElementAt(0);
        String rawTimestamp = logFileHandler.getRawTimestamp(displayTimestamp);

        String content1 = entryLoader.loadEntry(displayTimestamp);
        String content2 = entryLoader.loadEntry(rawTimestamp);

        if (content1 != null && content2 != null && content1.equals(content2)) {
            testsupport.TestLog.out("✓ PASS: Both display and raw timestamps work for loading");
        } else {
            testsupport.TestLog.out("✗ FAIL: Display and raw timestamps return different content");
        }
    }

    private void testGetRecentLogEntries() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should get recent log entries...");
        createTestLogFile();

        List<String> recent = entryLoader.getRecentLogEntries(5);
        if (recent != null && recent.size() <= 5) {
            testsupport.TestLog.out("✓ PASS: Got " + recent.size() + " recent entries");

            if (recent.size() >= 2) {
                LocalDateTime current = DateHandler.parseTimestamp(recent.get(0));
                LocalDateTime next = DateHandler.parseTimestamp(recent.get(1));
                if (current.isAfter(next) || current.isEqual(next)) {
                    testsupport.TestLog.out("✓ PASS: Recent entries are properly sorted");
                } else {
                    testsupport.TestLog.out("✗ FAIL: Recent entries are not properly sorted");
                }
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: getRecentLogEntries returned null or too many entries");
        }
    }

    private void testLoadEntryNullEmpty() {
        testsupport.TestLog.out("Test: EntryLoader should handle null and empty timestamps gracefully...");
        String result1 = entryLoader.loadEntry(null);
        String result2 = entryLoader.loadEntry("");
        String result3 = entryLoader.loadEntry("   ");

        if ("".equals(result1) && "".equals(result2) && "".equals(result3)) {
            testsupport.TestLog.out("✓ PASS: Correctly handled null and empty timestamps");
        } else {
            testsupport.TestLog.out("✗ FAIL: Should return empty string for null/empty timestamps");
        }
    }

    private void testLoadEntryComplexContent() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should load entries with complex content...");
        List<String> testData = Arrays.asList(
            "14:30 2025-12-18",
            "Entry with multiple lines",
            "Line 2 with timestamp-like content: 15:30 2025-12-18",
            "Line 3 with special characters: @#$%^&*()",
            "Line 4 with quotes: \"Hello World\"",
            "Line 5 with empty line above",
            "",
            "Line 7 after empty line",
            ""
        );
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);
        if (listModel.getSize() != 1) {
            testsupport.TestLog.out("✗ FAIL: Should have exactly 1 entry");
            return;
        }

        String loadedContent = entryLoader.loadEntry(listModel.getElementAt(0));
        if (loadedContent != null &&
            loadedContent.contains("Entry with multiple lines") &&
            loadedContent.contains("timestamp-like content") &&
            loadedContent.contains("special characters") &&
            loadedContent.contains("quotes") &&
            loadedContent.contains("empty line above") &&
            loadedContent.contains("after empty line")) {
            testsupport.TestLog.out("✓ PASS: Complex content loaded correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: Complex content not loaded correctly");
        }
    }

    private void testLoadEntryEncrypted() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should load entry correctly when encrypted...");
        // Ensure clean state
        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }
        createTestLogFile();

        logFileHandler.enableEncryption("testpassword".toCharArray());

        listModel.clear();
        entryLoader.loadLogEntries(listModel);
        if (listModel.getSize() == 0) {
            testsupport.TestLog.out("✗ FAIL: No entries loaded with encryption");
            return;
        }

        String timestamp = listModel.getElementAt(0);
        String content = entryLoader.loadEntry(timestamp);

        if (content != null && !content.isEmpty() && !content.contains(timestamp.trim())) {
            testsupport.TestLog.out("✓ PASS: Entry loaded correctly with encryption");
        } else {
            testsupport.TestLog.out("✗ FAIL: Entry not loaded correctly with encryption");
        }

        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }
    }

    private void testTimestampHandling() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should correctly handle raw vs display timestamps...");
        String displayTs1 = "14:30 2025-12-18";
        String rawTs1 = logFileHandler.getRawTimestamp(displayTs1);
        if (displayTs1.equals(rawTs1)) {
            testsupport.TestLog.out("✓ PASS: Simple timestamp handled correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: Simple timestamp not handled correctly");
        }

        String displayTs2 = "14:30 2025-12-18 (1)";
        String rawTs2 = logFileHandler.getRawTimestamp(displayTs2);
        if ("14:30 2025-12-18".equals(rawTs2)) {
            testsupport.TestLog.out("✓ PASS: Timestamp with suffix (1) handled correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: Timestamp with suffix (1) not handled correctly");
        }

        String displayTs3 = "14:30 2025-12-18 (5)";
        String rawTs3 = logFileHandler.getRawTimestamp(displayTs3);
        if ("14:30 2025-12-18".equals(rawTs3)) {
            testsupport.TestLog.out("✓ PASS: Timestamp with suffix (5) handled correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: Timestamp with suffix (5) not handled correctly");
        }

        String noSuffix = "14:30 2025-12-18";
        String rawNoSuffix = logFileHandler.getRawTimestamp(noSuffix);
        if (noSuffix.equals(rawNoSuffix)) {
            testsupport.TestLog.out("✓ PASS: Timestamp without suffix handled correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: Timestamp without suffix not handled correctly");
        }

        String empty = "";
        String rawEmpty = logFileHandler.getRawTimestamp(empty);
        if ("".equals(rawEmpty)) {
            testsupport.TestLog.out("✓ PASS: Empty string handled correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: Empty string not handled correctly");
        }

        String nullInput = null;
        try {
            String rawNull = logFileHandler.getRawTimestamp(nullInput);
            if (rawNull == null) {
                testsupport.TestLog.out("✓ PASS: Null input handled correctly");
            } else {
                testsupport.TestLog.out("✗ FAIL: Null input not handled correctly");
            }
        } catch (Exception e) {
            testsupport.TestLog.out("✓ PASS: Null input handled correctly (exception caught)");
        }
    }

    private void testSaveText() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should save text and create timestamp...");
        // Ensure clean state
        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }

        String testText = "This is a test entry";

        logFileHandler.saveText(testText, listModel);

        if (Files.exists(testFilePath) && listModel.getSize() == 1) {
            testsupport.TestLog.out("✓ PASS: Text saved and list model updated");

            List<String> lines = Files.readAllLines(testFilePath);
            if (lines.size() > 0 && lines.get(0).matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}.*") && lines.contains(testText)) {
                testsupport.TestLog.out("✓ PASS: File contains timestamp and text");
            } else {
                testsupport.TestLog.out("✗ FAIL: File does not contain expected timestamp and text");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: File not created or list model not updated");
        }
    }

    private void testSaveTextEmpty() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should not save empty text...");
        // Ensure clean state
        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }

        logFileHandler.saveText("", listModel);
        logFileHandler.saveText("   ", listModel);
        logFileHandler.saveText(null, listModel);

        if (!Files.exists(testFilePath) && listModel.getSize() == 0) {
            testsupport.TestLog.out("✓ PASS: Empty text not saved");
        } else {
            testsupport.TestLog.out("✗ FAIL: Empty text was saved");
        }
    }

    private void testSaveTextDuplicateTimestamps() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should handle duplicate timestamps...");
        // Ensure clean state
        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }

        String testText1 = "First entry";
        String testText2 = "Second entry";

        logFileHandler.saveText(testText1, listModel);
        logFileHandler.saveText(testText2, listModel);

        if (listModel.getSize() == 2) {
            testsupport.TestLog.out("✓ PASS: Both entries saved");

            List<String> lines = Files.readAllLines(testFilePath);
            boolean foundFirst = false, foundSecond = false;
            for (String line : lines) {
                if (line.contains(testText1)) foundFirst = true;
                if (line.contains(testText2)) foundSecond = true;
            }
            if (foundFirst && foundSecond) {
                testsupport.TestLog.out("✓ PASS: Both entries found in file");
            } else {
                testsupport.TestLog.out("✗ FAIL: One or both entries missing from file");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: Expected 2 entries, got " + listModel.getSize());
        }
    }

    private void testUpdateEntry() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should update existing entry...");
        String originalText = "Original text";
        logFileHandler.saveText(originalText, listModel);

        if (listModel.getSize() != 1) {
            testsupport.TestLog.out("✗ FAIL: Initial save failed");
            return;
        }

        String timestamp = listModel.getElementAt(0);
        String updatedText = "Updated text";
        logFileHandler.updateEntry(timestamp, updatedText);

        List<String> lines = Files.readAllLines(testFilePath);
        boolean hasUpdated = false, hasOriginal = false;
        for (String line : lines) {
            if (line.contains(updatedText)) hasUpdated = true;
            if (line.contains(originalText)) hasOriginal = true;
        }

        if (hasUpdated && !hasOriginal) {
            testsupport.TestLog.out("✓ PASS: Entry updated correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: Entry not updated correctly");
        }
    }

    private void testChangeTimestamp() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should change timestamp...");
        String testText = "Test entry";
        logFileHandler.saveText(testText, listModel);

        if (listModel.getSize() != 1) {
            testsupport.TestLog.out("✗ FAIL: Initial save failed");
            return;
        }

        String oldTimestamp = listModel.getElementAt(0);
        String newTimestamp = "12:34 2024-01-01";

        logFileHandler.changeTimestamp(oldTimestamp, newTimestamp, listModel);

        if (listModel.getSize() == 1 && newTimestamp.equals(listModel.getElementAt(0))) {
            testsupport.TestLog.out("✓ PASS: Timestamp changed in list model");

            List<String> lines = Files.readAllLines(testFilePath);
            if (lines.contains(newTimestamp)) {
                testsupport.TestLog.out("✓ PASS: New timestamp found in file");
            } else {
                testsupport.TestLog.out("✗ FAIL: New timestamp not found in file");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: Timestamp not changed in list model");
        }
    }

    private void testDeleteEntry() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should delete entry...");
        logFileHandler.saveText("First entry", listModel);
        logFileHandler.saveText("Second entry", listModel);

        if (listModel.getSize() != 2) {
            testsupport.TestLog.out("✗ FAIL: Initial saves failed");
            return;
        }

        String timestampToDelete = listModel.getElementAt(0);
        logFileHandler.deleteEntry(timestampToDelete, listModel);

        if (listModel.getSize() == 1 && !timestampToDelete.equals(listModel.getElementAt(0))) {
            testsupport.TestLog.out("✓ PASS: Entry deleted from list model");

            List<String> lines = Files.readAllLines(testFilePath);
            boolean foundDeleted = false;
            for (String line : lines) {
                if (line.trim().equals(timestampToDelete.trim())) {
                    foundDeleted = true;
                    break;
                }
            }
            if (!foundDeleted) {
                testsupport.TestLog.out("✓ PASS: Entry deleted from file");
            } else {
                testsupport.TestLog.out("✗ FAIL: Entry still found in file");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: Entry not deleted from list model");
        }
    }

    private void testGetLines() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should get lines from file...");
        createTestLogFile();

        List<String> lines = logFileHandler.getLines();
        if (lines != null && lines.size() > 0) {
            testsupport.TestLog.out("✓ PASS: Got " + lines.size() + " lines");

            boolean hasLogHeader = false;
            for (String line : lines) {
                if (".LOG".equals(line.trim().toUpperCase())) {
                    hasLogHeader = true;
                    break;
                }
            }
            if (hasLogHeader) {
                testsupport.TestLog.out("✓ PASS: .LOG header preserved");
            } else {
                testsupport.TestLog.out("✗ FAIL: .LOG header not found");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: No lines returned");
        }
    }

    private void testGetParsedEntries() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should parse entries correctly...");
        createTestLogFile();

        List<List<String>> entries = logFileHandler.getParsedEntries();
        if (entries != null && entries.size() > 0) {
            testsupport.TestLog.out("✓ PASS: Parsed " + entries.size() + " entries");

            boolean allValid = true;
            for (List<String> entry : entries) {
                if (entry == null || entry.isEmpty() || !entry.get(0).matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}.*")) {
                    allValid = false;
                    break;
                }
            }
            if (allValid) {
                testsupport.TestLog.out("✓ PASS: All entries have valid timestamps");
            } else {
                testsupport.TestLog.out("✗ FAIL: Some entries have invalid timestamps");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: No entries parsed");
        }
    }

    private void testFileOperationsWithNonExistentFile() {
        testsupport.TestLog.out("Test: LogFileHandler should handle file operations gracefully with non-existent file...");
        try {
            logFileHandler.deleteEntry("nonexistent", listModel);
            logFileHandler.changeTimestamp("old", "new", listModel);
            logFileHandler.updateEntry("nonexistent", "new text");
            testsupport.TestLog.out("✓ PASS: File operations handled gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: File operations threw exception: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithWrongLogHeader() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with .LOG1 instead of .LOG...");
        List<String> testData = Arrays.asList(
            ".LOG1",
            "",
            "10:30 2024-12-15",
            "This entry should be loaded despite wrong header",
            "",
            "11:45 2024-12-15",
            "Another entry"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            if (listModel.getSize() > 0) {
                testsupport.TestLog.out("✓ PASS: Entries loaded despite wrong header");
            } else {
                testsupport.TestLog.out("✗ FAIL: No entries loaded with wrong header");
            }
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with wrong header: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithLogHeaderInWrongPosition() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with .LOG header in wrong position...");
        LocalDateTime now = LocalDateTime.now();
        String timestamp1 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String timestamp2 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), (now.getMinute() + 1) % 60, now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            "",
            timestamp1,
            "First entry",
            "",
            ".LOG",
            "",
            timestamp2,
            "Second entry after misplaced header"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            if (listModel.getSize() >= 1) {
                testsupport.TestLog.out("✓ PASS: Entries loaded despite misplaced header");
            } else {
                testsupport.TestLog.out("✗ FAIL: No entries loaded with misplaced header");
            }
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with misplaced header: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithLogHeaderAfterContent() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with .LOG header after content...");
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            timestamp,
            "Entry before header",
            "",
            ".LOG",
            "",
            "Some content after header"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            testsupport.TestLog.out("✓ PASS: Handled .LOG header after content gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with header after content: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithCaseVariationsOfLog() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with case variations of .LOG...");
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".log",
            "",
            timestamp,
            "Entry with lowercase header",
            "",
            ".Log",
            "",
            "Content after mixed case header"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            testsupport.TestLog.out("✓ PASS: Handled case variations of .LOG gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with case variations: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithLogHeaderWithWhitespace() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with .LOG header with whitespace...");
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            " .LOG ",
            "",
            timestamp,
            "Entry with spaced header",
            "",
            "\t.LOG\t",
            "",
            "Content after tabbed header"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            testsupport.TestLog.out("✓ PASS: Handled .LOG with whitespace gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with whitespace in header: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithMultipleLogHeadersDifferentPositions() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with multiple .LOG headers in different positions...");
        LocalDateTime now = LocalDateTime.now();
        String timestamp1 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String timestamp2 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), (now.getMinute() + 1) % 60, now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".LOG",
            "",
            timestamp1,
            "First entry",
            "",
            ".LOG",
            "",
            timestamp2,
            "Second entry",
            "",
            ".LOG",
            "Content after last header"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            testsupport.TestLog.out("✓ PASS: Handled multiple headers in different positions gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with multiple headers: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithLogHeaderNotAtBeginning() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with .LOG header not at the very beginning...");
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            "Some random text at top",
            "",
            ".LOG",
            "",
            timestamp,
            "Entry after header",
            "",
            "More content"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            testsupport.TestLog.out("✓ PASS: Handled .LOG header not at beginning gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with header not at beginning: " + e.getMessage());
        }
    }

    private void testBehaviorWhenLogHeaderMissingOrMisplaced() throws Exception {
        testsupport.TestLog.out("Test: Demonstrate what happens when .LOG is missing or misplaced...");
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

        if (normalCount == noHeaderCount && normalCount == middleHeaderCount && normalCount == endHeaderCount && normalCount >= 1) {
            testsupport.TestLog.out("✓ PASS: All scenarios work the same and load entries");
        } else {
            testsupport.TestLog.out("✗ FAIL: Different scenarios behave differently");
        }
    }

    private void testLoadEntriesWithMultipleLogHeaders() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with multiple .LOG headers...");
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".LOG",
            ".LOG",
            "",
            timestamp,
            "Entry after duplicate headers",
            "",
            ".LOG",
            "",
            "Another entry"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            testsupport.TestLog.out("✓ PASS: Handled multiple .LOG headers gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with multiple headers: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithLogInContent() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with .LOG in middle of content...");
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".LOG",
            "",
            timestamp,
            "This is a normal entry",
            ".LOG",
            "This line follows .LOG in content",
            "",
            "12:34 2024-12-15",
            "Another entry"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            testsupport.TestLog.out("✓ PASS: Handled .LOG in content gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with .LOG in content: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithMalformedTimestamps() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with malformed timestamps...");
        LocalDateTime now = LocalDateTime.now();
        String validTimestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            ".LOG",
            "",
            "25:99 2024-13-45",
            "Entry with invalid timestamp",
            "",
            validTimestamp,
            "Entry with valid timestamp",
            "",
            "invalid timestamp format",
            "Entry without timestamp"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            if (listModel.getSize() >= 1) {
                testsupport.TestLog.out("✓ PASS: Handled malformed timestamps and loaded valid entries");
            } else {
                testsupport.TestLog.out("✗ FAIL: No entries loaded despite valid timestamp");
            }
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown with malformed timestamps: " + e.getMessage());
        }
    }

    private void testLoadEntriesWithoutLogHeader() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files without .LOG header...");
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            "",
            timestamp,
            "Entry without header",
            "",
            "Another line"
        );
        Files.write(testFilePath, testData);

        try {
            entryLoader.loadLogEntries(listModel);
            testsupport.TestLog.out("✓ PASS: Handled missing header gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Exception thrown without header: " + e.getMessage());
        }
    }

    private void testLoadEntriesEmptyFile() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle empty files...");
        Files.write(testFilePath, new byte[0]);

        entryLoader.loadLogEntries(listModel);
        if (listModel.getSize() == 0) {
            testsupport.TestLog.out("✓ PASS: Empty file handled correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: Empty file should result in empty list");
        }
    }

    private void testLoadEntriesOnlyHeader() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should handle files with only .LOG header...");
        List<String> testData = Arrays.asList(".LOG");
        Files.write(testFilePath, testData);

        entryLoader.loadLogEntries(listModel);
        if (listModel.getSize() == 0) {
            testsupport.TestLog.out("✓ PASS: File with only header handled correctly");
        } else {
            testsupport.TestLog.out("✗ FAIL: File with only header should result in empty list");
        }
    }

    private void testGetLinesWithMalformedFile() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should handle malformed files in getLines...");
        List<String> testData = Arrays.asList(
            ".LOG1",
            "",
            "10:30 2024-12-15",
            "Normal entry",
            "",
            "malformed line with no timestamp"
        );
        Files.write(testFilePath, testData);

        List<String> lines = logFileHandler.getLines();
        if (lines != null && lines.size() > 0 && lines.contains(".LOG1")) {
            testsupport.TestLog.out("✓ PASS: Malformed file handled without crashing");
        } else {
            testsupport.TestLog.out("✗ FAIL: Malformed file caused issues");
        }
    }

    private void testGetParsedEntriesWithMalformedData() throws Exception {
        testsupport.TestLog.out("Test: LogFileHandler should handle getParsedEntries with malformed data...");
        List<String> testData = Arrays.asList(
            ".LOG",
            "",
            "10:30 2024-12-15",
            "Valid entry",
            "",
            "invalid timestamp",
            "Entry without proper timestamp",
            "",
            "25:99 2024-13-45",
            "Entry with invalid timestamp"
        );
        Files.write(testFilePath, testData);

        List<List<String>> entries = logFileHandler.getParsedEntries();
        if (entries != null && entries.size() >= 1) {
            testsupport.TestLog.out("✓ PASS: Malformed data parsed without crashing");
        } else {
            testsupport.TestLog.out("✗ FAIL: Malformed data caused parsing to fail");
        }
    }

    private void testEncryptedFilePreservesLogHeader() throws Exception {
        testsupport.TestLog.out("Test: Encrypted files should preserve .LOG header...");
        // Ensure clean state
        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }

        logFileHandler.enableEncryption("testpassword".toCharArray());

        logFileHandler.saveText("Test entry", listModel);

        List<String> lines = logFileHandler.getLines();
        if (lines != null && lines.size() > 0) {
            boolean hasLogHeader = false;
            for (String line : lines) {
                if (".LOG".equals(line.trim().toUpperCase())) {
                    hasLogHeader = true;
                    break;
                }
            }
            if (hasLogHeader) {
                testsupport.TestLog.out("✓ PASS: Encrypted file preserves .LOG header");
            } else {
                testsupport.TestLog.out("✗ FAIL: Encrypted file missing .LOG header");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: No lines returned from encrypted file");
        }

        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }
    }

    private void testDecryptOldEncryptedFileAddsLogHeader() throws Exception {
        testsupport.TestLog.out("Test: Decrypting old encrypted files should add .LOG header...");
        // Ensure clean state
        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }

        LocalDateTime now = LocalDateTime.now();
        String currentMonthTimestamp1 = String.format("%02d:%02d %04d-%02d-%02d",
            now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        List<String> testData = Arrays.asList(
            "",
            "",
            currentMonthTimestamp1,
            "This is an entry from old encrypted file",
            "",
            "14:45 2024-11-10",
            "This is an old entry from 2024"
        );
        Files.write(testFilePath, testData);

        logFileHandler.enableEncryption("testpassword".toCharArray());
        logFileHandler.disableEncryption();

        List<String> lines = Files.readAllLines(testFilePath);
        if (lines.size() > 0 && ".LOG".equals(lines.get(0).trim().toUpperCase())) {
            testsupport.TestLog.out("✓ PASS: .LOG header added during decryption");
        } else {
            testsupport.TestLog.out("✗ FAIL: .LOG header not added during decryption");
        }
    }

    private void testEntryLoaderFiltersLogFromEncryptedFiles() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should filter .LOG from UI for encrypted files...");
        // Ensure clean state
        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }

        logFileHandler.enableEncryption("testpassword".toCharArray());

        logFileHandler.saveText("Test entry", listModel);

        listModel.clear();
        entryLoader.loadLogEntries(listModel);

        if (listModel.getSize() > 0) {
            boolean hasLogHeader = false;
            for (int i = 0; i < listModel.getSize(); i++) {
                if (".LOG".equals(listModel.getElementAt(i).trim().toUpperCase())) {
                    hasLogHeader = true;
                    break;
                }
            }
            if (!hasLogHeader) {
                testsupport.TestLog.out("✓ PASS: .LOG filtered from UI for encrypted files");
            } else {
                testsupport.TestLog.out("✗ FAIL: .LOG not filtered from UI for encrypted files");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: No entries loaded for encrypted file");
        }

        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }
    }

    private void testEncryptedFileMaintainsLogThroughCycles() throws Exception {
        testsupport.TestLog.out("Test: Encrypted files should maintain .LOG through save/load cycles...");
        // Ensure clean state
        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }

        logFileHandler.enableEncryption("testpassword".toCharArray());

        logFileHandler.saveText("First entry", listModel);
        logFileHandler.saveText("Second entry", listModel);

        List<String> lines = logFileHandler.getLines();
        if (lines != null && lines.size() > 0) {
            boolean hasLogHeader = false;
            for (String line : lines) {
                if (".LOG".equals(line.trim().toUpperCase())) {
                    hasLogHeader = true;
                    break;
                }
            }
            if (hasLogHeader) {
                testsupport.TestLog.out("✓ PASS: .LOG header maintained through multiple saves");
            } else {
                testsupport.TestLog.out("✗ FAIL: .LOG header lost during multiple saves");
            }
        } else {
            testsupport.TestLog.out("✗ FAIL: No lines returned after multiple saves");
        }

        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }
    }

    private void testLoadEntryAfterEncryptionUnlock() throws Exception {
        testsupport.TestLog.out("Test: EntryLoader should load entry correctly after encryption unlock...");
        // Ensure clean state
        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }
        createTestLogFile();

        entryLoader.loadLogEntries(listModel);
        if (listModel.getSize() == 0) {
            testsupport.TestLog.out("✗ FAIL: No entries to test");
            return;
        }

        String originalTimestamp = listModel.getElementAt(0);
        String originalContent = entryLoader.loadEntry(originalTimestamp);
        if (originalContent == null || originalContent.isEmpty()) {
            testsupport.TestLog.out("✗ FAIL: Could not load original content");
            return;
        }

        logFileHandler.enableEncryption("testpassword".toCharArray());

        listModel.clear();
        entryLoader.loadLogEntries(listModel);
        if (listModel.getSize() == 0) {
            testsupport.TestLog.out("✗ FAIL: No entries after encryption");
            return;
        }

        String encryptedTimestamp = listModel.getElementAt(0);
        String encryptedContent = entryLoader.loadEntry(encryptedTimestamp);

        if (originalTimestamp.equals(encryptedTimestamp) &&
            originalContent != null && encryptedContent != null &&
            originalContent.equals(encryptedContent)) {
            testsupport.TestLog.out("✓ PASS: Content preserved through encryption cycle");
        } else {
            testsupport.TestLog.out("✗ FAIL: Content not preserved through encryption cycle");
        }

        try {
            logFileHandler.disableEncryption();
        } catch (Exception e) {
            // Ignore if not encrypted
        }
    }

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
}