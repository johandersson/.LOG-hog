package filehandling;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

class LogParserRegressionTest {

    @Test
    void timestampTextInsideEntryDoesNotCreateDuplicateEntry() {
        List<String> lines = Arrays.asList(
            ".LOG",
            "",
            "13:00 2026-09-02",
            "First entry text",
            "12:00 2026-09-02",
            "More text after timestamp-like content",
            "",
            "12:00 2026-09-02",
            "Second saved entry",
            ""
        );

        List<List<String>> entries = LogParser.parseAllEntries(lines);
        assertEquals(2, entries.size());
        assertTrue(entries.get(0).contains("12:00 2026-09-02"),
            "Timestamp-like content line should stay in the first entry body");
        assertEquals("12:00 2026-09-02", entries.get(1).get(0));
    }
}
