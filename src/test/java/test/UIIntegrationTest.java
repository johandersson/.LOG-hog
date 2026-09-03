package test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import encryption.EncryptionManager;
import filehandling.EntryLoader;
import filehandling.LogFileHandler;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import java.nio.file.Path;

class UIIntegrationTest {

    @TempDir
    Path tempDir;

    private LogFileHandler logFileHandler;
    private EntryLoader entryLoader;
    private DefaultListModel<String> listModel;

    @BeforeEach
    void setUp() throws Exception {
        Path testFile = tempDir.resolve("ui-integration-test.txt");
        logFileHandler = new LogFileHandler(testFile, EncryptionManager.getInstance());
        entryLoader = new EntryLoader(logFileHandler);
        listModel = new DefaultListModel<>();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        createTestEntries();
        flushEdt();
        entryLoader.loadLogEntries(listModel);
        flushEdt();
    }

    @AfterEach
    void tearDown() {
        logFileHandler.clearSensitiveData();
    }

    @Test
    void logTextEditorRelatedLoadingLogicFindsContent() {
        assertFalse(listModel.isEmpty());
        String timestamp = listModel.getElementAt(0);
        String expectedContent = entryLoader.loadEntry(timestamp);
        assertNotNull(expectedContent);
        assertFalse(expectedContent.isEmpty());
    }

    @Test
    void logListPanelRelatedLoadingLogicHandlesMissingSelections() {
        String timestamp = listModel.getElementAt(0);
        String content = logFileHandler.loadEntry(timestamp);

        assertNotNull(content);
        assertFalse(content.isEmpty());
        assertEquals("", logFileHandler.loadEntry(null));
        assertEquals("", logFileHandler.loadEntry(""));
    }

    @Test
    void listSelectionAndEntryLoadingStayInSync() {
        JList<String> mockList = new JList<>(listModel);
        mockList.setSelectedIndex(0);

        String firstItem = listModel.getElementAt(0);
        String selectedItem = mockList.getSelectedValue();
        assertEquals(firstItem, selectedItem);
        assertEquals(logFileHandler.loadEntry(firstItem), logFileHandler.loadEntry(selectedItem));
    }

    private void createTestEntries() {
        logFileHandler.saveText("Test entry 1\nWith multiple lines\nAnd content", listModel);
        logFileHandler.saveText("Test entry 2 - simple", listModel);
        logFileHandler.saveText("Test entry 3\nWith timestamp-like content: 14:30 2025-12-18", listModel);
        logFileHandler.saveText("Duplicate timestamp entry", listModel);
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
