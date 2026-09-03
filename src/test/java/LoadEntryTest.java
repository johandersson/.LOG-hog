import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import javax.swing.DefaultListModel;
import java.nio.file.Path;

import encryption.EncryptionManager;
import filehandling.EntryLoader;
import filehandling.LogFileHandler;

class LoadEntryTest {

    @TempDir
    Path tempDir;

    private Path testFile;
    private LogFileHandler logFileHandler;
    private EntryLoader entryLoader;
    private DefaultListModel<String> listModel;

    @BeforeEach
    void setUp() {
        testFile = tempDir.resolve("load-entry-test.txt");
        logFileHandler = new LogFileHandler(testFile, EncryptionManager.getInstance());
        entryLoader = new EntryLoader(logFileHandler);
        listModel = new DefaultListModel<>();
    }

    @AfterEach
    void tearDown() {
        logFileHandler.clearSensitiveData();
    }

    @Test
    void loadEntryStillWorksAfterEnablingEncryption() throws Exception {
        logFileHandler.enableEncryption("testpassword".toCharArray());
        createTestEntries();
        flushEdt();

        entryLoader.loadLogEntries(listModel);
        flushEdt();
        assertFalse(listModel.isEmpty());

        String originalTimestamp = listModel.getElementAt(0);
        String originalContent = entryLoader.loadEntry(originalTimestamp);

        byte[] salt = logFileHandler.getSalt().clone();
        logFileHandler.clearSensitiveData();
        logFileHandler.setEncryption("testpassword".toCharArray(), salt);
        listModel.clear();
        entryLoader.loadLogEntries(listModel);
        flushEdt();

        String encryptedTimestamp = listModel.getElementAt(0);
        String encryptedContent = entryLoader.loadEntry(encryptedTimestamp);
        assertEquals(originalTimestamp, encryptedTimestamp);
        assertEquals(originalContent, encryptedContent);
    }

    private void createTestEntries() {
        logFileHandler.saveText("First test entry\nWith multiple lines\nAnd content that might look like timestamps: 15:30 2025-12-17", listModel);
        logFileHandler.saveText("Second entry", listModel);
        logFileHandler.saveText("Third entry with timestamp-like content: Meeting at 14:00 2025-12-17", listModel);
    }

    private static void flushEdt() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
    }
}
