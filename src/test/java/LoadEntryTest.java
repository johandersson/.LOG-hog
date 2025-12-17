import javax.swing.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import filehandling.LogFileHandler;
import filehandling.EntryLoader;

/**
 * Simple test to simulate loading entries after encryption unlock
 */
public class LoadEntryTest {

    public static void main(String[] args) throws Exception {
        // Use temp file
        Path testFile = Files.createTempFile("loghog_test", ".txt");
        LogFileHandler.setTestFilePath(testFile);

        LogFileHandler logFileHandler = new LogFileHandler();
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();

        try {
            // Create some test data
            createTestLogFile(logFileHandler, listModel);

            // Load entries to get a timestamp
            entryLoader.loadLogEntries(listModel);
            if (listModel.getSize() == 0) {
                System.out.println("FAIL: No entries loaded");
                return;
            }
            String originalTimestamp = listModel.getElementAt(0);
            System.out.println("Original timestamp: " + originalTimestamp);

            // Load the entry content before encryption
            String originalContent = entryLoader.loadEntry(originalTimestamp);
            if (originalContent == null || originalContent.isEmpty()) {
                System.out.println("FAIL: Original content empty");
                return;
            }
            System.out.println("Original content: '" + originalContent + "'");

            // Now enable encryption (simulate locking)
            logFileHandler.enableEncryption("testpassword".toCharArray());
            System.out.println("Encryption enabled");

            // Clear list and reload (simulate unlock)
            listModel.clear();
            entryLoader.loadLogEntries(listModel);
            if (listModel.getSize() == 0) {
                System.out.println("FAIL: No entries after encryption");
                return;
            }

            // The timestamp should be the same (display format)
            String encryptedTimestamp = listModel.getElementAt(0);
            System.out.println("Encrypted timestamp: " + encryptedTimestamp);
            if (!originalTimestamp.equals(encryptedTimestamp)) {
                System.out.println("FAIL: Timestamps don't match");
                return;
            }

            // Load the entry content after encryption
            String encryptedContent = entryLoader.loadEntry(encryptedTimestamp);
            if (encryptedContent == null || encryptedContent.isEmpty()) {
                System.out.println("FAIL: Encrypted content empty");
                return;
            }
            System.out.println("Encrypted content: '" + encryptedContent + "'");

            // Content should be the same
            if (!originalContent.equals(encryptedContent)) {
                System.out.println("FAIL: Contents don't match");
                return;
            }

            System.out.println("SUCCESS: LoadEntry works after encryption unlock");

        } finally {
            // Clean up
            Files.deleteIfExists(testFile);
            logFileHandler.clearSensitiveData();
        }
    }

    private static void createTestLogFile(LogFileHandler logFileHandler, DefaultListModel<String> listModel) throws Exception {
        // Create some test entries
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");

        // Save a few entries
        logFileHandler.saveText("First test entry\nWith multiple lines\nAnd content that might look like timestamps: 15:30 2025-12-17", listModel);
        logFileHandler.saveText("Second entry", listModel);
        logFileHandler.saveText("Third entry with timestamp-like content: Meeting at 14:00 2025-12-17", listModel);
    }
}