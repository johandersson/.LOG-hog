import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import javax.swing.DefaultListModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import encryption.EncryptionManager;
import filehandling.EntryLoader;
import filehandling.LogFileHandler;

class DuplicateTest {

    @TempDir
    Path tempDir;

    @Test
    void duplicateTimestampsWithoutEncryptionReceiveDisplaySuffixes() throws Exception {
        Path testFile = tempDir.resolve("duplicate-plain.txt");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd"));
        Files.write(testFile, List.of(
            ".LOG",
            "",
            timestamp,
            "First duplicate entry",
            "",
            timestamp,
            "Second duplicate entry",
            ""
        ));

        LogFileHandler logFileHandler = new LogFileHandler(testFile, EncryptionManager.getInstance());
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        entryLoader.loadLogEntries(listModel);
        flushEdt();

        assertEquals(2, listModel.size());
        assertTrue(listModel.getElementAt(1).endsWith("(1)"));
        assertEquals("First duplicate entry", entryLoader.loadEntry(listModel.getElementAt(0)));
        assertEquals("Second duplicate entry", entryLoader.loadEntry(listModel.getElementAt(1)));
    }

    @Test
    void duplicateTimestampsRemainReadableWithEncryptionEnabled() throws Exception {
        Path testFile = tempDir.resolve("duplicate-encrypted.txt");
        LogFileHandler logFileHandler = new LogFileHandler(testFile, EncryptionManager.getInstance());
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();

        logFileHandler.enableEncryption("testpassword".toCharArray());
        logFileHandler.saveText("Encrypted first duplicate", listModel);
        logFileHandler.saveText("Encrypted second duplicate", listModel);
        flushEdt();

        listModel.clear();
        entryLoader.loadLogEntries(listModel);
        flushEdt();

        assertEquals(2, listModel.size());
        assertEquals("Encrypted first duplicate", entryLoader.loadEntry(listModel.getElementAt(0)));
        assertEquals("Encrypted second duplicate", entryLoader.loadEntry(listModel.getElementAt(1)));
    }

    @Test
    void distinctTimestampsStayUnsuffixed() throws Exception {
        Path testFile = tempDir.resolve("duplicate-distinct-times.txt");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        Files.write(testFile, List.of(
            ".LOG",
            "",
            now.format(formatter),
            "First entry",
            "",
            now.plusMinutes(1).format(formatter),
            "Second entry",
            ""
        ));

        LogFileHandler logFileHandler = new LogFileHandler(testFile, EncryptionManager.getInstance());
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        entryLoader.loadLogEntries(listModel);
        flushEdt();

        assertEquals(2, listModel.size());
        assertFalse(listModel.getElementAt(0).contains("("));
        assertFalse(listModel.getElementAt(1).contains("("));
        assertNotEquals(logFileHandler.getRawTimestamp(listModel.getElementAt(0)), logFileHandler.getRawTimestamp(listModel.getElementAt(1)));
    }

    @Test
    void enablingEncryptionOnExistingDuplicatesPreservesOccurrenceLabels() throws Exception {
        Path testFile = tempDir.resolve("duplicate-existing-encrypted.txt");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd"));
        Files.write(testFile, List.of(
            ".LOG",
            "",
            timestamp,
            "Pre-encryption first",
            "",
            timestamp,
            "Pre-encryption second",
            ""
        ));

        LogFileHandler logFileHandler = new LogFileHandler(testFile, EncryptionManager.getInstance());
        EntryLoader entryLoader = new EntryLoader(logFileHandler);
        DefaultListModel<String> listModel = new DefaultListModel<>();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        entryLoader.loadLogEntries(listModel);
        flushEdt();

        assertEquals(2, listModel.size());
        assertTrue(listModel.getElementAt(1).endsWith("(1)"));
        assertEquals("Pre-encryption first", entryLoader.loadEntry(listModel.getElementAt(0)));
        assertEquals("Pre-encryption second", entryLoader.loadEntry(listModel.getElementAt(1)));
    }

    private static void flushEdt() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
    }
}
