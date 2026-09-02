import javax.swing.*;
import java.nio.file.*;
// Unused imports removed for PMD compliance

import filehandling.LogFileHandler;
import filehandling.EntryLoader;

/**
 * Debug test to see what's happening with log loading
 */
public class DebugTest {

    public static void main(String[] args) throws Exception {
        // Use temp file
        Path testFile = Files.createTempFile("loghog_debug", ".txt");
        LogFileHandler.setTestFilePath(testFile);

        try {
            LogFileHandler logFileHandler = new LogFileHandler();
            EntryLoader entryLoader = new EntryLoader(logFileHandler);
            DefaultListModel<String> listModel = new DefaultListModel<>();

            // Create test entries
            testsupport.TestLog.out("Creating test entries...");
            logFileHandler.saveText("First entry content", listModel);
            logFileHandler.saveText("Second entry content", listModel);
            logFileHandler.saveText("Third entry content", listModel);

            testsupport.TestLog.out("List model size: " + listModel.getSize());
            for (int i = 0; i < listModel.getSize(); i++) {
                testsupport.TestLog.out("List item " + i + ": '" + listModel.getElementAt(i) + "'");
            }

            // Clear and reload
            testsupport.TestLog.out("\nClearing and reloading...");
            listModel.clear();
            entryLoader.loadLogEntries(listModel);

            testsupport.TestLog.out("After reload - List model size: " + listModel.getSize());
            for (int i = 0; i < listModel.getSize(); i++) {
                testsupport.TestLog.out("List item " + i + ": '" + listModel.getElementAt(i) + "'");
            }

            // Try to load each entry
            testsupport.TestLog.out("\nTrying to load each entry:");
            for (int i = 0; i < listModel.getSize(); i++) {
                String timestamp = listModel.getElementAt(i);
                testsupport.TestLog.out("Loading entry for timestamp: '" + timestamp + "'");
                String content = entryLoader.loadEntry(timestamp);
                testsupport.TestLog.out("Content length: " + content.length());
                testsupport.TestLog.out("Content: '" + content + "'");
                testsupport.TestLog.out();
            }

        } finally {
            Files.deleteIfExists(testFile);
        }
    }
}