package filehandling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parses log file lines into entries.
 * Handles both encrypted and plain text logs.
 */
public class LogParser {

    private static final Pattern TS_PATTERN = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$");

    /**
     * Parses the lines into entries, filtering by year and month, and sorting.
     */
    public static List<List<String>> parseEntries(List<String> lines, int year, int month) {
        var entries = new ArrayList<List<String>>();
        var currentEntry = new ArrayList<String>();
        for (String line : lines) {
            var trimmed = line.trim();
            if (trimmed.equalsIgnoreCase(".LOG")) continue;
            if (TS_PATTERN.matcher(trimmed).matches()) {
                if (!currentEntry.isEmpty()) {
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
            entries.add(currentEntry);
        }

        // Filter by year and month
        var filtered = new ArrayList<List<String>>();
        for (List<String> entry : entries) {
            if (!entry.isEmpty()) {
                try {
                    var dt = utils.DateHandler.parseTimestamp(entry.get(0));
                    if (dt.getYear() == year && dt.getMonthValue() == month) {
                        filtered.add(entry);
                    }
                } catch (Exception e) {
                    // Security: Don't log parsing errors to console
                    // Silently skip malformed entries
                }
            }
        }

        // Sort by timestamp descending
        filtered.sort((a, b) -> {
            try {
                LocalDateTime dateA = utils.DateHandler.parseTimestamp(a.get(0));
                LocalDateTime dateB = utils.DateHandler.parseTimestamp(b.get(0));
                return dateB.compareTo(dateA);
            } catch (Exception e) {
                return b.get(0).compareTo(a.get(0));
            }
        });

        return filtered;
    }

    /**
     * Parses all entries without filtering or sorting.
     * Enforces MAX_COLLECTION_SIZE to prevent DoS attacks.
     */
    public static List<List<String>> parseAllEntries(List<String> lines) {
        var entries = new ArrayList<List<String>>();
        var currentEntry = new ArrayList<String>();
        final int MAX_COLLECTION_SIZE = ResourceLimits.MAX_COLLECTION_SIZE; // DoS protection
        
        for (String line : lines) {
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
                    "The log file contains too many entries to process safely (max " + MAX_COLLECTION_SIZE + ")"
                );
                throw new IllegalStateException("Too many entries (max " + MAX_COLLECTION_SIZE + ")");
            }
            entries.add(currentEntry);
        }
        return entries;
    }

    /**
     * Parses entries for full log view, sorted oldest first.
     */
    public static List<List<String>> parseEntriesForFullLog(List<String> lines) {
        var entries = parseAllEntries(lines);

        // Sort oldest first
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);
        entries.sort((a, b) -> {
            try {
                String dateStrA = a.get(0).trim().replaceAll(" \\(\\d+\\)", "");
                String dateStrB = b.get(0).trim().replaceAll(" \\(\\d+\\)", "");
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