/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package filehandling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Handles sorting and organizing log entries by timestamp.
 * Extracted from LogFileHandler to separate sorting logic from file operations.
 */
public class EntrySorter {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$", Pattern.MULTILINE);
    
    /**
     * Sorts log entries by timestamp in ascending order (oldest first).
     * Non-timestamp entries are preserved at the beginning.
     * 
     * @param lines the raw lines from the log file
     * @return sorted lines with consistent spacing
     */
    public static List<String> sortEntriesByTimestamp(List<String> lines) {
        // Check if .LOG header exists in the input
        boolean hasLogHeader = lines.stream().anyMatch(line -> line.trim().equalsIgnoreCase(".LOG"));
        
        List<List<String>> entries = new ArrayList<>();
        List<String> currentEntry = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase(".LOG")) continue; // Skip .LOG during processing
            if (TIMESTAMP_PATTERN.matcher(trimmed).matches()) {
                if (!currentEntry.isEmpty()) {
                    entries.add(new ArrayList<>(currentEntry));
                    currentEntry.clear();
                }
                currentEntry.add(line);
            } else {
                // Only add non-blank lines to the entry body
                // Use centralized format rules
                if (LogFileFormat.shouldIncludeInEntry(line, !currentEntry.isEmpty())) {
                    currentEntry.add(line);
                }
            }
        }
        if (!currentEntry.isEmpty()) {
            entries.add(currentEntry);
        }

        // Separate timestamp entries from non-timestamp entries
        List<List<String>> timestampEntries = new ArrayList<>();
        List<List<String>> nonTimestampEntries = new ArrayList<>();
        for (List<String> entry : entries) {
            if (!entry.isEmpty() && TIMESTAMP_PATTERN.matcher(entry.get(0).trim()).matches()) {
                timestampEntries.add(entry);
            } else {
                nonTimestampEntries.add(entry);
            }
        }

        // Sort timestamp entries by date ascending (oldest first)
        timestampEntries.sort((a, b) -> {
            try {
                LocalDateTime dateA = parseDateForSorting(a.get(0));
                LocalDateTime dateB = parseDateForSorting(b.get(0));
                return dateA.compareTo(dateB);
            } catch (Exception e) {
                return 0; // keep original order if parsing fails
            }
        });

        // Combine: non-timestamp entries first, then sorted timestamp entries
        List<List<String>> sortedEntries = new ArrayList<>();
        sortedEntries.addAll(nonTimestampEntries);
        sortedEntries.addAll(timestampEntries);

        // Flatten back to lines with consistent spacing
        List<String> sortedLines = new ArrayList<>();
        
        // Add .LOG header at the top if it existed in the input
        if (hasLogHeader) {
            sortedLines.add(".LOG");
            sortedLines.add(""); // Blank line after header
        }
        
        for (int i = 0; i < sortedEntries.size(); i++) {
            List<String> entry = sortedEntries.get(i);
            
            // Remove trailing blank lines from entry
            while (!entry.isEmpty() && entry.get(entry.size() - 1).trim().isEmpty()) {
                entry.remove(entry.size() - 1);
            }
            
            sortedLines.addAll(entry);
            
            // Add exactly one blank line after each entry except the last one
            if (i < sortedEntries.size() - 1) {
                sortedLines.add("");
            }
        }

        return sortedLines;
    }
    
    /**
     * Parses a timestamp line into LocalDateTime for sorting.
     * Removes duplicate counter suffix if present.
     */
    public static LocalDateTime parseDateForSorting(String timestampLine) {
        String dateStr = timestampLine.trim().replaceAll(" \\(\\d+\\)", "");
        return LocalDateTime.parse(dateStr, FORMATTER);
    }
    
    /**
     * Removes a specific log entry from the lines list.
     * 
     * @param timeStamp the timestamp of the entry to remove
     * @param lines the complete list of lines
     * @return updated lines with the entry removed
     */
    public static List<String> removeEntry(String timeStamp, List<String> lines) {
        List<String> updatedLines = new ArrayList<>();
        boolean skipping = false;

        for (String line : lines) {
            // timestamp lines are exact matches (whitespace trimmed)
            if (!skipping && line.trim().equals(timeStamp.trim())) {
                skipping = true; // start skipping this timestamp and its body
                continue;
            }

            if (skipping) {
                // stop skipping when we hit the next timestamp line
                if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                    skipping = false;
                    // This line is the next timestamp; it should be kept
                    updatedLines.add(line);
                } else {
                    // while skipping, simply continue (this drops blank lines and body lines)
                    continue;
                }
            } else {
                updatedLines.add(line);
            }
        }
        return updatedLines;
    }
}
