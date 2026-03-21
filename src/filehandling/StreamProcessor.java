package filehandling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Stream-based processing helpers for parsing very large log files without
 * allocating large intermediate collections.
 */
public final class StreamProcessor {
    private StreamProcessor() {}

    private static final java.util.regex.Pattern TS_PATTERN = java.util.regex.Pattern.compile("^\\d{2}:\\d{2} \\\\d{4}-\\d{2}-\\d{2}( \\\\([0-9]+\\\\))?$");

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

    public static List<List<String>> parseEntriesForFullLogStream(java.util.stream.Stream<String> linesStream) {
        var entries = parseAllEntriesStream(linesStream);

        // Sort oldest first
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);
        entries.sort((a, b) -> {
            try {
                String dateStrA = a.get(0).trim().replaceAll(" \\\([0-9]+\\\)", "");
                String dateStrB = b.get(0).trim().replaceAll(" \\\([0-9]+\\\)", "");
                LocalDateTime dateA = LocalDateTime.parse(dateStrA, formatter);
                LocalDateTime dateB = LocalDateTime.parse(dateStrB, formatter);
                return dateA.compareTo(dateB);
            } catch (Exception e) {
                return 0;
            }
        });

        return entries;
    }
}
