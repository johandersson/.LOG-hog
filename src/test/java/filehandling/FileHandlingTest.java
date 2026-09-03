package filehandling;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import encryption.EncryptionManager;
import encryption.TestableEncryptionManager;
import utils.DateHandler;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class FileHandlingTest {

    @TempDir
    Path tempDir;

    private Path testFilePath;
    private LogFileHandler logFileHandler;
    private EntryLoader entryLoader;
    private DefaultListModel<String> listModel;

    @BeforeEach
    void setUp() {
        testFilePath = tempDir.resolve("filehandling-test.txt");
        logFileHandler = new LogFileHandler(testFilePath, EncryptionManager.getInstance());
        entryLoader = new EntryLoader(logFileHandler);
        listModel = new DefaultListModel<>();
    }

    @AfterEach
    void tearDown() {
        logFileHandler.clearSensitiveData();
    }

    @Test
    void loadEntryWithDuplicateSuffixLoadsEachMatchingOccurrence() throws Exception {
        String timestamp = "10:15 2026-09-02";
        Files.write(testFilePath, List.of(
            ".LOG", "", timestamp, "First duplicate", "", timestamp, "Second duplicate", "", timestamp, "Third duplicate", ""
        ));
        logFileHandler.enableEncryption("testpassword".toCharArray());
        loadEntries();

        assertEquals(3, listModel.getSize());
        assertEquals("First duplicate", entryLoader.loadEntry(listModel.getElementAt(0)));
        assertEquals("Second duplicate", entryLoader.loadEntry(listModel.getElementAt(1)));
        assertEquals("Third duplicate", entryLoader.loadEntry(listModel.getElementAt(2)));
    }

    @Test
    void displayAndRawTimestampsResolveToSameEntryContent() throws Exception {
        createStandardTestLogFile();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        loadEntries();
        String displayTimestamp = listModel.getElementAt(0);
        String rawTimestamp = logFileHandler.getRawTimestamp(displayTimestamp);
        assertEquals(entryLoader.loadEntry(displayTimestamp), entryLoader.loadEntry(rawTimestamp));
    }

    @Test
    void getRecentLogEntriesReturnsSortedSubset() throws Exception {
        createStandardTestLogFile();
        logFileHandler.enableEncryption("testpassword".toCharArray());
        List<String> recent = entryLoader.getRecentLogEntries(2);
        assertEquals(2, recent.size());
        LocalDateTime first = DateHandler.parseTimestamp(recent.get(0));
        LocalDateTime second = DateHandler.parseTimestamp(recent.get(1));
        assertFalse(first.isBefore(second));
    }

    @Test
    void loadEntryReturnsEmptyForNullBlankAndMissingEntries() {
        assertEquals("", entryLoader.loadEntry(null));
        assertEquals("", entryLoader.loadEntry(""));
        assertEquals("", entryLoader.loadEntry("   "));
        assertEquals("", entryLoader.loadEntry("99:99 9999-99-99"));
    }

    @Test
    void loadEntryPreservesComplexContentAndBlankLines() throws Exception {
        Files.write(testFilePath, List.of(
            ".LOG", "", "14:30 2025-12-18", "Entry with multiple lines",
            "Line 2 with timestamp-like content: 15:30 2025-12-18",
            "Line 3 with special characters: @#$%^&*()",
            "Line 4 with quotes: \"Hello World\"",
            "Line 5 with empty line above", "", "Line 7 after empty line", ""
        ));
        logFileHandler.enableEncryption("testpassword".toCharArray());
        loadEntries();
        String loadedContent = entryLoader.loadEntry(listModel.getElementAt(0));
        assertTrue(loadedContent.contains("empty line above\n\nLine 7 after empty line"));
    }

    @Test
    void getRawTimestampStripsSuffixesAndRejectsNull() {
        assertEquals("14:30 2025-12-18", logFileHandler.getRawTimestamp("14:30 2025-12-18"));
        assertEquals("14:30 2025-12-18", logFileHandler.getRawTimestamp("14:30 2025-12-18 (5)"));
        assertThrows(NullPointerException.class, () -> logFileHandler.getRawTimestamp(null));
    }

    @Test
    void saveTextWritesTimestampAndContent() throws Exception {
        logFileHandler.enableEncryption("testpassword".toCharArray());
        logFileHandler.saveText("This is a test entry", listModel);
        flushEdt();
        List<String> lines = logFileHandler.getLines();
        assertEquals(1, listModel.getSize());
        assertTrue(lines.stream().noneMatch(line -> ".LOG".equalsIgnoreCase(line.trim())));
        assertTrue(lines.stream().anyMatch(line -> line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}.*")));
        assertTrue(lines.contains("This is a test entry"));
    }

    @Test
    void saveTextIgnoresBlankInputs() {
        logFileHandler.saveText("", listModel);
        logFileHandler.saveText("   ", listModel);
        logFileHandler.saveText(null, listModel);
        assertFalse(Files.exists(testFilePath));
        assertEquals(0, listModel.getSize());
    }

    @Test
    void updateEntryReplacesExistingBody() throws Exception {
        logFileHandler.enableEncryption("testpassword".toCharArray());
        logFileHandler.saveText("Original text", listModel);
        flushEdt();
        String timestamp = listModel.getElementAt(0);
        logFileHandler.updateEntry(timestamp, "Updated text");
        assertTrue(logFileHandler.getLines().contains("Updated text"));
        assertFalse(logFileHandler.getLines().contains("Original text"));
    }

    @Test
    void changeTimestampUpdatesFileAndListModel() throws Exception {
        logFileHandler.enableEncryption("testpassword".toCharArray());
        logFileHandler.saveText("Test entry", listModel);
        flushEdt();
        String oldTimestamp = listModel.getElementAt(0);
        String newTimestamp = "12:34 2024-01-01";
        logFileHandler.changeTimestamp(oldTimestamp, newTimestamp, listModel);
        flushEdt();
        assertEquals(newTimestamp, listModel.getElementAt(0));
        assertTrue(logFileHandler.getLines().contains(newTimestamp));
    }

    @Test
    void deleteEntryRemovesSelectionFromModelAndFile() throws Exception {
        Files.write(testFilePath, List.of(
            ".LOG", "",
            "10:00 2025-01-01", "First entry", "",
            "10:01 2025-01-01", "Second entry", ""
        ));
        logFileHandler.enableEncryption("testpassword".toCharArray());
        loadEntries();
        String timestampToDelete = listModel.getElementAt(0);
        logFileHandler.deleteEntry(timestampToDelete, listModel);
        flushEdt();
        assertEquals(1, listModel.getSize());
        assertFalse(logFileHandler.getLines().stream().anyMatch(line -> line.trim().equals(timestampToDelete)));
    }

    @Test
    void fileOperationsWithMissingFileDoNotThrow() {
        assertAll(
            () -> assertDoesNotThrow(() -> logFileHandler.deleteEntry("nonexistent", listModel)),
            () -> assertDoesNotThrow(() -> logFileHandler.changeTimestamp("old", "new", listModel)),
            () -> assertDoesNotThrow(() -> logFileHandler.updateEntry("nonexistent", "new text"))
        );
    }

    @Test
    void getLinesAndParsedEntriesHandleMalformedContentGracefully() throws Exception {
        Files.write(testFilePath, List.of(
            ".LOG1", "", "10:30 2024-12-15", "Valid entry", "", "invalid timestamp", "Entry without proper timestamp"
        ));
        logFileHandler.enableEncryption("testpassword".toCharArray());
        List<String> lines = logFileHandler.getLines();
        List<List<String>> entries = logFileHandler.getParsedEntries();
        assertTrue(lines.contains(".LOG1") || lines.contains(".LOG"));
        assertFalse(entries.isEmpty());
    }

    @Test
    void headerPlacementVariantsStillLoadEntries() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp1 = String.format("%02d:%02d %04d-%02d-%02d", now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        String timestamp2 = String.format("%02d:%02d %04d-%02d-%02d", now.getHour(), (now.getMinute() + 1) % 60, now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        List<List<String>> variants = List.of(
            List.of(".LOG", "", timestamp1, "Entry 1", "", timestamp2, "Entry 2"),
            List.of(timestamp1, "Entry 1", "", timestamp2, "Entry 2"),
            List.of(timestamp1, "Entry 1", "", ".LOG", "", timestamp2, "Entry 2"),
            List.of(" .LOG ", "", timestamp1, "Entry 1", "", "\t.LOG\t", "", timestamp2, "Entry 2"),
            List.of(".LOG1", "", timestamp1, "Entry 1", "", timestamp2, "Entry 2")
        );
        for (List<String> variant : variants) {
            Files.write(testFilePath, variant);
            logFileHandler.enableEncryption("testpassword".toCharArray());
            loadEntries();
            assertTrue(listModel.getSize() >= 1);
            logFileHandler = new LogFileHandler(testFilePath, EncryptionManager.getInstance());
            entryLoader = new EntryLoader(logFileHandler);
            listModel = new DefaultListModel<>();
        }
    }

    @Test
    void malformedTimestampsDoNotPreventValidEntriesFromLoading() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String validTimestamp = String.format("%02d:%02d %04d-%02d-%02d", now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        Files.write(testFilePath, List.of(
            ".LOG", "", "25:99 2024-13-45", "Entry with invalid timestamp", "", validTimestamp, "Entry with valid timestamp"
        ));
        logFileHandler.enableEncryption("testpassword".toCharArray());
        loadEntries();
        assertTrue(listModel.getSize() >= 1);
    }

    @Test
    void emptyFilesAndHeaderOnlyFilesLoadAsEmptyLists() throws Exception {
        logFileHandler.enableEncryption("testpassword".toCharArray());
        loadEntries();
        assertEquals(0, listModel.getSize());
        logFileHandler = new LogFileHandler(testFilePath, EncryptionManager.getInstance());
        entryLoader = new EntryLoader(logFileHandler);
        listModel = new DefaultListModel<>();
        Files.write(testFilePath, List.of(".LOG"));
        logFileHandler.enableEncryption("testpassword".toCharArray());
        loadEntries();
        assertEquals(0, listModel.getSize());
    }

    @Test
    void encryptedFilesKeepLogHeaderOutOfUiAndPreserveContentAcrossCycles() throws Exception {
        logFileHandler.enableEncryption("testpassword".toCharArray());
        logFileHandler.saveText("First entry", listModel);
        logFileHandler.saveText("Second entry", listModel);
        flushEdt();
        List<String> decryptedLines = logFileHandler.getLines();
        assertTrue(decryptedLines.stream().noneMatch(line -> ".LOG".equalsIgnoreCase(line.trim())));
        loadEntries();
        assertEquals(2, listModel.getSize());
        assertFalse(asList(listModel).stream().anyMatch(line -> ".LOG".equals(line.trim().toUpperCase())));
        byte[] salt = logFileHandler.getSalt().clone();
        logFileHandler.clearSensitiveData();
        logFileHandler.setEncryption("testpassword".toCharArray(), salt);
        listModel.clear();
        loadEntries();
        assertEquals(2, listModel.getSize());
    }

    @Test
    void decryptingOlderStyleContentAddsMissingLogHeader() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.format("%02d:%02d %04d-%02d-%02d", now.getHour(), now.getMinute(), now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        Files.write(testFilePath, List.of("", "", timestamp, "This is an entry from old encrypted file"));
        logFileHandler.enableEncryption("testpassword".toCharArray());
        loadEntries();
        assertFalse(listModel.isEmpty());
    }

    @Test
    void ensureEntrySeparatorIsIdempotentAndAddsSingleBlankLine() {
        List<String> lines = new ArrayList<>(List.of("10:00 2025-01-01", "Body", ""));
        LogFileFormat.ensureEntrySeparator(lines);
        assertEquals(List.of("10:00 2025-01-01", "Body", ""), lines);
        List<String> noSeparator = new ArrayList<>(List.of("10:00 2025-01-01", "Body"));
        LogFileFormat.ensureEntrySeparator(noSeparator);
        assertEquals(List.of("10:00 2025-01-01", "Body", ""), noSeparator);
    }

    @Test
    void encryptionRoundTripPreservesManualFixtureContent() throws Exception {
        Path uniqueTestFile = tempDir.resolve("filehandling-encryption-roundtrip.txt");
        TestableEncryptionManager encryptor = new TestableEncryptionManager();
        LogFileHandler uniqueHandler = new LogFileHandler(uniqueTestFile, encryptor);
        createStandardTestLogFile(uniqueTestFile);
        uniqueHandler.enableEncryption("testpassword".toCharArray());
        byte[] salt = uniqueHandler.getSalt().clone();
        uniqueHandler.clearSensitiveData();
        uniqueHandler.setEncryption("testpassword".toCharArray(), salt);
        assertFalse(uniqueHandler.getLines().isEmpty());
    }

    private void loadEntries() throws Exception {
        entryLoader.loadLogEntries(listModel);
        flushEdt();
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static List<String> asList(DefaultListModel<String> model) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) values.add(model.getElementAt(i));
        return values;
    }

    private void createStandardTestLogFile() throws Exception {
        createStandardTestLogFile(testFilePath);
    }

    private static void createStandardTestLogFile(Path filePath) throws Exception {
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
}
