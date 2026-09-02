package filehandling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Regression test for a bug where editing, deleting or appending entries removed the
 * blank line that separates entries in the file. Since a timestamp line only starts a
 * new entry when it is preceded by a blank line, the missing separator made entries
 * merge into each other in the Full Log view.
 */
public class EntrySeparatorRegressionTest {
    public static void main(String[] args) throws Exception {
        updateKeepsFollowingEntrySeparate();
        deleteKeepsFollowingEntrySeparate();
        appendToFileWithoutTrailingBlankKeepsEntrySeparate();
        System.out.println("EntrySeparatorRegressionTest passed");
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
        if (entries.size() != expected) {
            throw new AssertionError(context + ": expected " + expected + " entries, got " + entries.size()
                + " (entries merged because the blank separator line was lost)");
        }
    }

    private static void updateKeepsFollowingEntrySeparate() {
        List<String> updated = newEditor(Path.of("unused.txt"))
            .updateEntry("10:29 2026-09-02", 0, "Second entry edited", sampleLines());
        assertEntryCount("after update", updated, 3);
    }

    private static void deleteKeepsFollowingEntrySeparate() {
        List<String> updated = newEditor(Path.of("unused.txt"))
            .deleteEntries(List.of("10:29 2026-09-02"), sampleLines());
        assertEntryCount("after delete", updated, 2);
    }

    private static void appendToFileWithoutTrailingBlankKeepsEntrySeparate() throws Exception {
        Path tempFile = Files.createTempFile("loghog_separator_test", ".txt");
        try {
            // A file whose last entry has no trailing blank line (e.g. edited outside LogHog)
            Files.writeString(tempFile, ".LOG" + System.lineSeparator() + System.lineSeparator()
                + "09:16 2026-09-02" + System.lineSeparator() + "First entry");

            newEditor(tempFile).saveEntry("Second entry", "10:29 2026-09-02", false);

            assertEntryCount("after append", Files.readAllLines(tempFile), 2);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
