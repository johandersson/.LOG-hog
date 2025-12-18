import javax.swing.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
            System.out.println("Creating test entries...");
            logFileHandler.saveText("First entry content", listModel);
            logFileHandler.saveText("Second entry content", listModel);
            logFileHandler.saveText("Third entry content", listModel);

            System.out.println("List model size: " + listModel.getSize());
            for (int i = 0; i < listModel.getSize(); i++) {
                System.out.println("List item " + i + ": '" + listModel.getElementAt(i) + "'");
            }

            // Clear and reload
            System.out.println("\nClearing and reloading...");
            listModel.clear();
            entryLoader.loadLogEntries(listModel);

            System.out.println("After reload - List model size: " + listModel.getSize());
            for (int i = 0; i < listModel.getSize(); i++) {
                System.out.println("List item " + i + ": '" + listModel.getElementAt(i) + "'");
            }

            // Try to load each entry
            System.out.println("\nTrying to load each entry:");
            for (int i = 0; i < listModel.getSize(); i++) {
                String timestamp = listModel.getElementAt(i);
                System.out.println("Loading entry for timestamp: '" + timestamp + "'");
                String content = entryLoader.loadEntry(timestamp);
                System.out.println("Content length: " + content.length());
                System.out.println("Content: '" + content + "'");
                System.out.println();
            }

        } finally {
            Files.deleteIfExists(testFile);
        }
    }
}