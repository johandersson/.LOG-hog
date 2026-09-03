package filehandling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class EntrySeparatorRegressionTest {

    @TempDir
    Path tempDir;

    @Test
    void updateKeepsFollowingEntrySeparate() {
        List<String> updated = newEditor(tempDir.resolve("unused.txt"))
            .updateEntry("10:29 2026-09-02", 0, "Second entry edited", sampleLines());
        assertEntryCount("after update", updated, 3);
    }

    @Test
    void deleteKeepsFollowingEntrySeparate() {
        List<String> updated = newEditor(tempDir.resolve("unused.txt"))
            .deleteEntries(List.of("10:29 2026-09-02"), sampleLines());
        assertEntryCount("after delete", updated, 2);
    }

    @Test
    void appendToFileWithoutTrailingBlankKeepsEntrySeparate() throws Exception {
        Path tempFile = tempDir.resolve("entry-separator-regression.txt");
        Files.writeString(tempFile, ".LOG" + System.lineSeparator() + System.lineSeparator()
            + "09:16 2026-09-02" + System.lineSeparator() + "First entry");

        newEditor(tempFile).saveEntry("Second entry", "10:29 2026-09-02", false);

        assertEntryCount("after append", Files.readAllLines(tempFile), 2);
    }

    private static List<String> sampleLines() {
        StringBuilder sb = new StringBuilder(".LOG" + System.lineSeparator() + System.lineSeparator());
        sb.append(LogFileFormat.createEntry("09:16 2026-09-02", "First entry"));
        sb.append(LogFileFormat.createEntry("10:29 2026-09-02", "Second entry"));
        sb.append(LogFileFormat.createEntry("13:01 2026-09-02", "Third entry"));
        return new ArrayList<>(Arrays.asList(sb.toString().split("\r?\n", -1)));
    }

    private static EntryEditor newEditor(Path path) {
        return new EntryEditor(path, null, null);
    }

    private static void assertEntryCount(String context, List<String> lines, int expected) {
        List<List<String>> entries = LogParser.parseAllEntries(lines);
        assertEquals(expected, entries.size(), context + ": entries merged because the blank separator line was lost");
    }
}
