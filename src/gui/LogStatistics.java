package gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calculates and provides statistics for log files.
 * Optimized to avoid redundant parsing by accepting pre-parsed data.
 */
public class LogStatistics {

    private final int entryCount;
    private final int dayCount;
    private final long fileSize;
    private final String formattedFileSize;

    /**
     * Creates statistics from pre-parsed entries and file path.
     * This is the optimized constructor that avoids re-parsing.
     */
    public LogStatistics(List<List<String>> entries, Path filePath) {
        this.entryCount = entries.size();
        this.dayCount = calculateUniqueDays(entries);
        this.fileSize = getFileSize(filePath);
        this.formattedFileSize = formatFileSize(fileSize);
    }

    /**
     * Creates statistics when the total entry count is known but only a subset
     * of entries are available for date/day calculation (streamed parsing).
     */
    public LogStatistics(int totalEntryCount, List<List<String>> subsetForDays, Path filePath) {
        this.entryCount = totalEntryCount;
        this.dayCount = calculateUniqueDays(subsetForDays);
        this.fileSize = getFileSize(filePath);
        this.formattedFileSize = formatFileSize(fileSize);
    }

    /**
     * Creates statistics from raw lines (fallback for cases where entries aren't pre-parsed).
     * This is O(N) and should be avoided when possible.
     */
    public LogStatistics(Path filePath, List<String> rawLines) {
        List<List<String>> entries = filehandling.LogParser.parseEntriesForFullLog(rawLines);
        this.entryCount = entries.size();
        this.dayCount = calculateUniqueDays(entries);
        this.fileSize = getFileSize(filePath);
        this.formattedFileSize = formatFileSize(fileSize);
    }

    private int calculateUniqueDays(List<List<String>> entries) {
        Set<LocalDate> uniqueDays = new HashSet<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (List<String> entry : entries) {
            if (!entry.isEmpty()) {
                String timestamp = entry.get(0);
                // Extract date part (format: "HH:mm yyyy-MM-dd")
                String[] parts = timestamp.split(" ");
                if (parts.length >= 2) {
                    String dateStr = parts[1];
                    try {
                        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                        uniqueDays.add(date);
                    } catch (Exception e) {
                        // Skip malformed dates
                    }
                }
            }
        }

        return uniqueDays.size();
    }

    private long getFileSize(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
        } catch (Exception e) {
            // File size unavailable
        }
        return 0;
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    public int getEntryCount() {
        return entryCount;
    }

    public int getDayCount() {
        return dayCount;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFormattedFileSize() {
        return formattedFileSize;
    }

    @Override
    public String toString() {
        return String.format("Entries: %d, Days: %d, Size: %s",
                           entryCount, dayCount, formattedFileSize);
    }
}