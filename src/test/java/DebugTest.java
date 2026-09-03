import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import javax.swing.DefaultListModel;
import java.nio.file.Path;

import encryption.EncryptionManager;
import filehandling.EntryLoader;
import filehandling.LogFileHandler;

class DebugTest {

    @TempDir
    Path tempDir;

    @Test
    void reloadingEntriesKeepsAllSavedEntriesLoadable() throws Exception {
        Path testFile = tempDir.resolve("debug-test.txt");
        LogFileHandler logFileHandler = new LogFileHandler(testFile, EncryptionManager.getInstance());
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();

        logFileHandler.enableEncryption("testpassword".toCharArray());
        logFileHandler.saveText("First entry content", listModel);
        logFileHandler.saveText("Second entry content", listModel);
        logFileHandler.saveText("Third entry content", listModel);
        flushEdt();

        listModel.clear();
        entryLoader.loadLogEntries(listModel);
        flushEdt();

        assertEquals(3, listModel.getSize());
        for (int i = 0; i < listModel.getSize(); i++) {
            String timestamp = listModel.getElementAt(i);
            String content = entryLoader.loadEntry(timestamp);
            assertFalse(content.isEmpty());
        }
    }

    private static void flushEdt() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
    }
}
