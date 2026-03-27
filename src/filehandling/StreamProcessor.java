package filehandling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Stream-based processing helpers for parsing very large log files without
 * allocating large intermediate collections.
 */
public final class StreamProcessor {
    private StreamProcessor() {}

    private static final java.util.regex.Pattern TS_PATTERN = java.util.regex.Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( .*|)$");

    public static final class ParseResult {
        public final long totalEntries;
        public final List<List<String>> entriesNewestFirst;

        public ParseResult(long totalEntries, List<List<String>> entriesNewestFirst) {
            this.totalEntries = totalEntries;
            this.entriesNewestFirst = entriesNewestFirst;
        }
    }

    /**
     * Stream-parses entries and returns the total number of entries plus a bounded
     * list containing at most ResourceLimits.MAX_ENTRIES_TO_RENDER of the most
     * recent entries (newest-first). This avoids allocating the full entry list
     * for very large files.
     */
    public static ParseResult parseEntriesForFullLogStreamWithStats(java.util.stream.Stream<String> linesStream) {
        final int RENDER_CAP = ResourceLimits.MAX_ENTRIES_TO_RENDER;
        final int MAX_COLLECTION_SIZE = ResourceLimits.MAX_COLLECTION_SIZE;

        ArrayDeque<List<String>> tail = new ArrayDeque<>(Math.min(RENDER_CAP + 4, 1024));
        var currentEntry = new ArrayList<String>();
        long totalCount = 0;

        var it = linesStream.iterator();
        while (it.hasNext()) {
            String line = it.next();
            String trimmed = line.trim();
            if (".LOG".equalsIgnoreCase(trimmed)) continue;
            boolean isHeader = TS_PATTERN.matcher(trimmed).matches();
            if (isHeader) {
                if (!currentEntry.isEmpty()) {
                    totalCount++;
                    if (totalCount > MAX_COLLECTION_SIZE) {
                        filehandling.DialogHandler.showLimitExceeded(
                            "Too Many Entries",
                            "The log file contains too many entries to process safely (max " + MAX_COLLECTION_SIZE + ")."
                        );
                        throw new IllegalStateException("Too many entries (max " + MAX_COLLECTION_SIZE + ")");
                    }
                    // add to tail buffer
                    tail.addLast(new ArrayList<>(currentEntry));
                    if (tail.size() > RENDER_CAP) tail.removeFirst();
                    currentEntry.clear();
                }
                currentEntry.add(line);
            } else {
                if (!currentEntry.isEmpty() || !trimmed.isEmpty()) {
                    currentEntry.add(line);
                }
            }
        }
        if (!currentEntry.isEmpty()) {
            totalCount++;
            if (totalCount > MAX_COLLECTION_SIZE) {
                filehandling.DialogHandler.showLimitExceeded(
                    "Too Many Entries",
                    "The log file contains too many entries to process safely (max " + MAX_COLLECTION_SIZE + ")."
                );
                throw new IllegalStateException("Too many entries (max " + MAX_COLLECTION_SIZE + ")");
            }
            tail.addLast(currentEntry);
            if (tail.size() > RENDER_CAP) tail.removeFirst();
        }

        // Convert deque to list newest-first
        List<List<String>> result = new ArrayList<>(tail.size());
        var descIt = tail.descendingIterator();
        while (descIt.hasNext()) result.add(descIt.next());

        return new ParseResult(totalCount, result);
    }

    // Keep the old method for compatibility (heavier)
    public static List<List<String>> parseAllEntriesStream(java.util.stream.Stream<String> linesStream) {
        var entries = new ArrayList<List<String>>();
        var currentEntry = new ArrayList<String>();
        final int MAX_COLLECTION_SIZE = ResourceLimits.MAX_COLLECTION_SIZE;

        var it = linesStream.iterator();
        while (it.hasNext()) {
            String line = it.next();
            var trimmed = line.trim();
            if (trimmed.equalsIgnoreCase(".LOG")) continue;
            if (TS_PATTERN.matcher(trimmed).matches()) {
                if (!currentEntry.isEmpty()) {
                    if (entries.size() >= MAX_COLLECTION_SIZE) {
                        filehandling.DialogHandler.showLimitExceeded(
                            "Too Many Entries",
                            "The log file contains too many entries to process safely (max " + MAX_COLLECTION_SIZE + ")."
                        );
                        throw new IllegalStateException("Too many entries (max " + MAX_COLLECTION_SIZE + ")");
                    }
                    entries.add(new ArrayList<>(currentEntry));
                    currentEntry.clear();
                }
                currentEntry.add(line);
            } else {
                if (!currentEntry.isEmpty() || !trimmed.isEmpty()) {
                    currentEntry.add(line);
                }
            }
        }
        if (!currentEntry.isEmpty()) {
            if (entries.size() >= MAX_COLLECTION_SIZE) {
                filehandling.DialogHandler.showLimitExceeded(
                    "Too Many Entries",
                    "The log file contains too many entries to process safely (max " + MAX_COLLECTION_SIZE + ")."
                );
                throw new IllegalStateException("Too many entries (max " + MAX_COLLECTION_SIZE + ")");
            }
            entries.add(currentEntry);
        }
        return entries;
    }
}
