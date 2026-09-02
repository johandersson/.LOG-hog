package filehandling;

import java.util.Arrays;
import java.util.List;

public class LogParserRegressionTest {
    public static void main(String[] args) {
        timestampTextInsideEntryDoesNotCreateDuplicateEntry();
    }

    private static void timestampTextInsideEntryDoesNotCreateDuplicateEntry() {
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
        if (entries.size() != 2) {
            throw new AssertionError("Expected 2 saved entries, got " + entries.size());
        }
        if (!entries.get(0).contains("12:00 2026-09-02")) {
            throw new AssertionError("Timestamp-like content line should stay in the first entry body");
        }
        if (!"12:00 2026-09-02".equals(entries.get(1).get(0))) {
            throw new AssertionError("The real second saved entry should still be parsed at the separator boundary");
        }
    }
}
