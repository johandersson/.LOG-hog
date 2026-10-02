package filehandling;

import static org.junit.jupiter.api.Assertions.*;

import encryption.TestableEncryptionManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import utils.LogEntryLink;

class AlternateTimestampLinkTest {
    @TempDir Path tempDir;

    private static final List<String> HEADERS = List.of("13:23 2022-12-19",
            "2022-12-19 13:23", "19/12/2022 13:23", "12/19/2022 13:23",
            "19.12.2022 13:23", "19-12-2022 13:23");

    private List<String> lines() {
        var lines = new ArrayList<String>(List.of(".LOG", ""));
        for (String header : HEADERS) {
            lines.add(header);
            lines.add("Body for " + header);
            lines.add("2023-01-01 00:00");
            lines.add("");
        }
        return lines;
    }

    @Test
    void normalAndStreamingParsersRecognizeAllHeaderFormatsWithoutSplittingBodyDates() {
        var entries = LogParser.parseAllEntries(lines());
        assertEquals(HEADERS.size(), entries.size());
        for (int i = 0; i < HEADERS.size(); i++) {
            assertEquals(HEADERS.get(i), entries.get(i).get(0));
            assertTrue(entries.get(i).contains("2023-01-01 00:00"));
        }
        try (var stream = lines().stream()) {
            var result = StreamProcessor.parseEntriesForFullLogStreamWithStats(stream);
            assertEquals(HEADERS.size(), result.totalEntries);
            assertEquals(HEADERS.size(), result.entriesNewestFirst.size());
        }
    }

    @Test
    void encryptedEntryLookupListFilteringAndContentLoadingSupportAllHeaderFormats() throws Exception {
        Path file = tempDir.resolve("alternate-timestamps.log");
        Files.write(file, lines());
        var handler = new LogFileHandler(file, new TestableEncryptionManager());
        try {
            handler.enableEncryption("testpassword".toCharArray());
            var model = new DefaultListModel<String>();
            handler.loadLogEntries(model);
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(HEADERS.size(), model.size());
            var filtered = handler.getEntryLoader().searchEntries(2022, 12, null);
            assertEquals(HEADERS.size(), filtered.size());
            for (String header : HEADERS) {
                assertTrue(filtered.contains(header), header);
                assertTrue(handler.loadEntry(header).contains("Body for " + header), header);
                assertEquals(header, LogEntryLink.findTimestamp("13:23 2022-12-19",
                        List.of(List.of(header, handler.loadEntry(header)))), header);
            }
            assertEquals(HEADERS.size(), handler.getParsedEntries().size());
        } finally {
            handler.clearSensitiveData();
        }
    }

    @Test
    void fullLogAndStorageSortAlternateHeaderFormatsChronologically() {
        var lines = List.of(".LOG", "", "2023-01-01 00:00", "Newest", "",
                "19/12/2022 13:23", "Middle", "", "00:00 2020-01-01", "Oldest");
        var fullLog = LogParser.parseEntriesForFullLog(lines);
        assertEquals(List.of("00:00 2020-01-01", "19/12/2022 13:23", "2023-01-01 00:00"),
                fullLog.stream().map(entry -> entry.get(0)).toList());
        var sorted = LogParser.parseAllEntries(EntrySorter.sortEntriesByTimestamp(lines));
        assertEquals(fullLog, sorted);
    }
}
