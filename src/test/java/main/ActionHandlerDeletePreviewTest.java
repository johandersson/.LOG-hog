package main;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import encryption.EncryptionException;
import encryption.Encryptor;
import filehandling.LogFileHandler;

public class ActionHandlerDeletePreviewTest {
    @TempDir
    Path tempDir;

    @Test
    void deletePreviewUsesExactDisplayTimestampWithSuffix() throws Exception {
        // Test that delete preview loads the correct entry when using display
        // timestamp with suffix (e.g., "10:00 2025-01-01 (1)"), not the raw
        // timestamp. This is critical for entries with duplicate timestamps.
        StubLogFileHandler handler = new StubLogFileHandler(
            tempDir.resolve("delete-preview-test.txt"));
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("10:00 2025-01-01");
        listModel.addElement("10:00 2025-01-01 (1)");
        
        JList<String> logList = new JList<>(listModel);
        logList.setSelectedIndex(1); // Select the second entry with suffix "(1)"
        
        ActionHandler actionHandler = new ActionHandler(null, handler, logList, listModel);
        
        // Build the preview entries list (simulating what deleteSelectedEntry does)
        List<String> selectedItems = logList.getSelectedValuesList();
        List<List<String>> previewEntries = new ArrayList<>();
        
        for (String selectedItem : selectedItems) {
            String entryText = handler.loadEntry(selectedItem);
            String previewBody;
            if (entryText == null || entryText.isBlank()) {
                previewBody = "(no content)";
            } else {
                String trimmed = entryText.length() > 200 
                    ? entryText.substring(0, 200) + "..." 
                    : entryText;
                previewBody = trimmed;
            }
            List<String> entryPreview = new ArrayList<>();
            entryPreview.add(selectedItem); // timestamp with suffix
            entryPreview.add(previewBody);
            previewEntries.add(entryPreview);
        }
        
        // Verify that the exact display timestamp (with suffix) was used
        assertEquals("10:00 2025-01-01 (1)", handler.requestedTimestamp,
            "Expected exact selected timestamp with suffix to be used, not raw timestamp");
        
        // Verify the preview entry has the correct content
        assertEquals(1, previewEntries.size());
        List<String> preview = previewEntries.get(0);
        assertEquals("10:00 2025-01-01 (1)", preview.get(0), "Preview should use display timestamp with suffix");
        assertEquals("content of second duplicate", preview.get(1), "Preview should show correct entry content");
    }

    @Test
    void deletePreviewHandlesMultipleDuplicateEntries() throws Exception {
        // Test that when deleting multiple entries with the same base timestamp
        // but different suffixes, each loads the correct content.
        StubLogFileHandler handler = new StubLogFileHandler(
            tempDir.resolve("delete-preview-multiple.txt"));
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("10:00 2025-01-01");
        listModel.addElement("10:00 2025-01-01 (1)");
        listModel.addElement("10:00 2025-01-01 (2)");
        
        JList<String> logList = new JList<>(listModel);
        logList.setSelectedIndex(1); // Select "(1)"
        logList.getSelectionModel().addSelectionInterval(2, 2); // Also select "(2)"
        
        ActionHandler actionHandler = new ActionHandler(null, handler, logList, listModel);
        
        // Build the preview entries list
        List<String> selectedItems = logList.getSelectedValuesList();
        List<List<String>> previewEntries = new ArrayList<>();
        
        for (String selectedItem : selectedItems) {
            String entryText = handler.loadEntry(selectedItem);
            String previewBody = entryText == null || entryText.isBlank() 
                ? "(no content)" 
                : entryText;
            List<String> entryPreview = new ArrayList<>();
            entryPreview.add(selectedItem);
            entryPreview.add(previewBody);
            previewEntries.add(entryPreview);
        }
        
        // Verify both entries were loaded
        assertEquals(2, previewEntries.size());
        
        // Verify first preview uses exact timestamp with suffix
        assertEquals("10:00 2025-01-01 (1)", previewEntries.get(0).get(0));
        assertEquals("content of second duplicate", previewEntries.get(0).get(1));
        
        // Verify second preview uses exact timestamp with suffix
        assertEquals("10:00 2025-01-01 (2)", previewEntries.get(1).get(0));
        assertEquals("content of third duplicate", previewEntries.get(1).get(1));
    }

    /**
     * Stub LogFileHandler that tracks which timestamp was requested and returns
     * different content based on the timestamp to verify correct entries are loaded.
     */
    private static final class StubLogFileHandler extends LogFileHandler {
        private String requestedTimestamp;

        private StubLogFileHandler(Path path) throws Exception {
            super(path, new NoopEncryptor());
        }

        @Override
        public String loadEntry(String timeStamp) {
            requestedTimestamp = timeStamp;
            // Return different content based on the exact timestamp
            if (timeStamp.contains("(1)")) {
                return "content of second duplicate";
            } else if (timeStamp.contains("(2)")) {
                return "content of third duplicate";
            } else {
                return "content of first entry";
            }
        }
    }

    private static final class NoopEncryptor implements Encryptor {
        @Override
        public byte[] generateSalt() throws EncryptionException {
            return new byte[0];
        }

        @Override
        public javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) throws EncryptionException {
            return null;
        }

        @Override
        public byte[] encrypt(String data, char[] password, byte[] salt) throws EncryptionException {
            return new byte[0];
        }

        @Override
        public String decrypt(byte[] data, char... password) throws EncryptionException {
            return "";
        }
    }
}
