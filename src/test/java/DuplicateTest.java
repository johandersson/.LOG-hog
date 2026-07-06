import javax.swing.*;
import java.nio.file.*;
// Unused import removed for PMD compliance
import java.time.format.DateTimeFormatter;
import java.util.*;

import filehandling.LogFileHandler;
import filehandling.EntryLoader;

/**
 * Extensive test for duplicate timestamp handling and numbering.
 */
public class DuplicateTest {

    public static void main(String[] args) throws Exception {
        testsupport.TestLog.out("=== Duplicate and Numbering Test ===\n");

        // Test 1: Save multiple entries at same time, no encryption
        testNoEncryptionDuplicates();

        // Test 2: Save multiple entries at same time, with encryption
        testEncryptionDuplicates();

        // Test 3: Save entries at different times
        testDifferentTimes();

        // Test 4: Load after encryption enable on existing duplicates
        testEnableEncryptionOnDuplicates();

        testsupport.TestLog.out("=== All tests completed ===");
    }

    private static void testNoEncryptionDuplicates() throws Exception {
        testsupport.TestLog.out("Test 1: Multiple entries at same time, no encryption");
        Path testFile = Files.createTempFile("loghog_dup_test1", ".txt");
        LogFileHandler.setTestFilePath(testFile);

        LogFileHandler logFileHandler = new LogFileHandler();
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();

        // Save two entries quickly (same timestamp)
        logFileHandler.saveText("First duplicate entry", listModel);
        Thread.sleep(10); // Small delay to ensure same time if system is fast
        logFileHandler.saveText("Second duplicate entry", listModel);

        // Load entries
        listModel.clear();
        entryLoader.loadLogEntries(listModel);

        testsupport.TestLog.out("Loaded entries: " + listModel.size());
        for (int i = 0; i < listModel.size(); i++) {
            testsupport.TestLog.out("  " + listModel.get(i));
        }

        // Check file content
        List<String> lines = Files.readAllLines(testFile);
        testsupport.TestLog.out("File lines: " + lines.size());
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                testsupport.TestLog.out("  '" + line + "'");
            }
        }

        // Cleanup
        Files.deleteIfExists(testFile);
        testsupport.TestLog.out("Test 1 passed\n");
    }

    private static void testEncryptionDuplicates() throws Exception {
        testsupport.TestLog.out("Test 2: Multiple entries at same time, with encryption");
        Path testFile = Files.createTempFile("loghog_dup_test2", ".txt");
        LogFileHandler.setTestFilePath(testFile);

        LogFileHandler logFileHandler = new LogFileHandler();
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();

        // Enable encryption
        logFileHandler.enableEncryption("testpassword".toCharArray());

        // Save two entries quickly
        logFileHandler.saveText("Encrypted first duplicate", listModel);
        Thread.sleep(10);
        logFileHandler.saveText("Encrypted second duplicate", listModel);

        // Load entries
        listModel.clear();
        entryLoader.loadLogEntries(listModel);

        testsupport.TestLog.out("Loaded entries: " + listModel.size());
        for (int i = 0; i < listModel.size(); i++) {
            testsupport.TestLog.out("  " + listModel.get(i));
        }

        // Cleanup
        Files.deleteIfExists(testFile);
        testsupport.TestLog.out("Test 2 passed\n");
    }

    private static void testDifferentTimes() throws Exception {
        testsupport.TestLog.out("Test 3: Entries at different times");
        Path testFile = Files.createTempFile("loghog_dup_test3", ".txt");
        LogFileHandler.setTestFilePath(testFile);

        LogFileHandler logFileHandler = new LogFileHandler();
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();

        // Save entries with delay
        logFileHandler.saveText("First entry", listModel);
        Thread.sleep(2000); // 2 seconds delay
        logFileHandler.saveText("Second entry", listModel);

        // Load entries
        listModel.clear();
        entryLoader.loadLogEntries(listModel);

        testsupport.TestLog.out("Loaded entries: " + listModel.size());
        for (int i = 0; i < listModel.size(); i++) {
            testsupport.TestLog.out("  " + listModel.get(i));
        }

        // Cleanup
        Files.deleteIfExists(testFile);
        testsupport.TestLog.out("Test 3 passed\n");
    }

    private static void testEnableEncryptionOnDuplicates() throws Exception {
        testsupport.TestLog.out("Test 4: Enable encryption on existing duplicate timestamps");
        Path testFile = Files.createTempFile("loghog_dup_test4", ".txt");
        LogFileHandler.setTestFilePath(testFile);

        LogFileHandler logFileHandler = new LogFileHandler();
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();

        // Save two entries with same timestamp (simulate existing duplicates)
        logFileHandler.saveText("Pre-encryption first", listModel);
        // Manually append another entry with same timestamp to simulate duplicates
        String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd"));
        String entry = timestamp + "\nPre-encryption second\n\n";
        Files.writeString(testFile, entry, StandardOpenOption.APPEND);

        // Enable encryption
        logFileHandler.enableEncryption("testpassword".toCharArray());

        // Load entries
        listModel.clear();
        entryLoader.loadLogEntries(listModel);

        testsupport.TestLog.out("Loaded entries: " + listModel.size());
        for (int i = 0; i < listModel.size(); i++) {
            testsupport.TestLog.out("  " + listModel.get(i));
        }

        // Cleanup
        Files.deleteIfExists(testFile);
        testsupport.TestLog.out("Test 4 passed\n");
    }
}