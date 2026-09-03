package filehandling;

import encryption.EncryptionManager;
import encryption.TestableEncryptionManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import utils.DateHandler;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileHandlingJUnitTest {

    @TempDir
    Path tempDir;

    private Path testFilePath;
    private LogFileHandler logFileHandler;
    private EntryLoader entryLoader;
    private DefaultListModel<String> listModel;

    @BeforeEach
    void setup() {
        testFilePath = tempDir.resolve("filehandling-junit-test.txt");
        logFileHandler = new LogFileHandler(testFilePath, EncryptionManager.getInstance());
        entryLoader = new EntryLoader(logFileHandler);
        listModel = new DefaultListModel<>();
    }

    @AfterEach
    void cleanup() {
        logFileHandler.clearSensitiveData();
    }

    @Test
    void testLoadLogEntriesEmptyFile() throws Exception {
        logFileHandler.enableEncryption("testpassword".toCharArray());
        entryLoader.loadLogEntries(listModel);
        flushEdt();
        assertEquals(0, listModel.getSize());
    }

    @Test
    void testLoadLogEntriesWithData() throws Exception {
        createTestLogFile();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        entryLoader.loadLogEntries(listModel);
        flushEdt();
        assertTrue(listModel.getSize() > 0);
    }

    @Test
    void testLoadFilteredEntries() throws Exception {
        createTestLogFile();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        entryLoader.loadFilteredEntries(listModel, LocalDateTime.now().getYear(), LocalDateTime.now().getMonthValue());
        flushEdt();
        for (int i = 0; i < listModel.getSize(); i++) {
            String rawTs = listModel.getElementAt(i).replaceAll(" \\([0-9]+\\)$", "");
            LocalDateTime dt = DateHandler.parseTimestamp(rawTs);
            assertEquals(LocalDateTime.now().getYear(), dt.getYear());
            assertEquals(LocalDateTime.now().getMonthValue(), dt.getMonthValue());
        }
    }

    @Test
    void testFilterModelByYearMonth() throws Exception {
        createTestLogFile();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        entryLoader.loadLogEntries(listModel);
        flushEdt();
        LocalDateTime now = LocalDateTime.now();
        DefaultListModel<String> filtered = entryLoader.filterModelByYearMonth(listModel, now.getYear(), now.getMonthValue());
        for (int i = 0; i < filtered.getSize(); i++) {
            LocalDateTime dt = DateHandler.parseTimestamp(filtered.getElementAt(i));
            assertEquals(now.getYear(), dt.getYear());
            assertEquals(now.getMonthValue(), dt.getMonthValue());
        }
    }

    @Test
    void testLoadEntry() throws Exception {
        createTestLogFile();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        entryLoader.loadLogEntries(listModel);
        flushEdt();
        String timestamp = listModel.getElementAt(0);
        String content = entryLoader.loadEntry(timestamp);
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    @Test
    void testLoadEntryNonExistent() {
        assertEquals("", entryLoader.loadEntry("25:99 9999-99-99"));
    }

    @Test
    void testLoadEntryEncrypted() throws Exception {
        Path uniqueTestFile = tempDir.resolve("encrypted-test.txt");
        TestableEncryptionManager encryptor = new TestableEncryptionManager();
        LogFileHandler uniqueHandler = new LogFileHandler(uniqueTestFile, encryptor);
        EntryLoader uniqueLoader = new EntryLoader(uniqueHandler, encryptor);
        createTestLogFile(uniqueTestFile);
        uniqueHandler.enableEncryption("testpassword".toCharArray());
        uniqueLoader.loadLogEntries(listModel);
        flushEdt();
        assertFalse(listModel.isEmpty());
        byte[] salt = uniqueHandler.getSalt().clone();
        uniqueHandler.clearSensitiveData();
        uniqueHandler.setEncryption("testpassword".toCharArray(), salt);
        assertTrue(uniqueHandler.getLines().contains("This is an entry from current month"));
    }

    @Test
    void testEncryptionUnlockBug() throws Exception {
        Path uniqueTestFile = tempDir.resolve("unlock-bug-test.txt");
        TestableEncryptionManager encryptor = new TestableEncryptionManager();
        LogFileHandler uniqueHandler = new LogFileHandler(uniqueTestFile, encryptor);
        createTestLogFile(uniqueTestFile);
        uniqueHandler.enableEncryption("testpassword".toCharArray());
        byte[] salt = uniqueHandler.getSalt().clone();
        uniqueHandler.clearSensitiveData();
        uniqueHandler.setEncryption("testpassword".toCharArray(), salt);
        assertFalse(uniqueHandler.getLines().isEmpty());
    }

    private void createTestLogFile() throws Exception {
        createTestLogFile(testFilePath);
    }

    private static void createTestLogFile(Path filePath) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String currentMonthTimestamp1 = String.format("%02d:%02d %04d-%02d-%02d", now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String currentMonthTimestamp2 = String.format("%02d:%02d %04d-%02d-%02d", now.getHour(), (now.getMinute() + 1) % 60, now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        Files.write(filePath, Arrays.asList(
            ".LOG", "", currentMonthTimestamp1, "This is an entry from current month", "With multiple lines", "",
            currentMonthTimestamp2, "This is another entry from current month", "",
            "14:45 2024-11-10", "This is an entry from November 2024", "",
            "16:20 2023-06-15", "This is an old entry from 2023"
        ));
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
