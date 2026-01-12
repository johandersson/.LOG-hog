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

import gui.DialogHelper;
import gui.LoadingProgressDialog;
import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Handles formatting operations for log files, including sorting entries
 * by timestamp and normalizing spacing between entries.
 */
public class LogFileFormatter {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);
    
    /**
     * Helper class to store entry with pre-parsed timestamp for efficient sorting.
     */
    private static class TimestampEntry {
        final List<String> entry;
        final LocalDateTime dateTime;
        
        TimestampEntry(List<String> entry, LocalDateTime dateTime) {
            this.entry = entry;
            this.dateTime = dateTime;
        }
    }
    
    // Security: Prevent memory exhaustion attacks
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_COLLECTION_SIZE = 100000; // Max entries
    
    /**
     * Sorts entries by timestamp and ensures consistent spacing.
     */
    public static List<String> sortEntriesByTimestamp(List<String> lines) {
        // Security: Prevent DoS from excessive entries
        if (lines == null) {
            throw new IllegalArgumentException("Lines cannot be null");
        }
        if (lines.size() > 100000) {
            throw new IllegalArgumentException("Too many lines to process (max 100000)");
        }
        
        // Check if .LOG header exists in the input
        boolean hasLogHeader = lines.stream().anyMatch(line -> line.trim().equalsIgnoreCase(".LOG"));
        
        List<List<String>> entries = new ArrayList<>();
        List<String> currentEntry = new ArrayList<>();
        java.util.regex.Pattern tsPattern = java.util.regex.Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$", java.util.regex.Pattern.MULTILINE);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase(".LOG")) continue; // Skip .LOG during processing
            if (tsPattern.matcher(trimmed).matches()) {
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

        // Separate timestamp entries from non-timestamp entries
        // Pre-parse timestamps for O(N) instead of O(N log N) parsing during sort
        List<TimestampEntry> timestampEntriesWithDates = new ArrayList<>();
        List<List<String>> nonTimestampEntries = new ArrayList<>();
        
        for (List<String> entry : entries) {
            if (!entry.isEmpty() && tsPattern.matcher(entry.get(0).trim()).matches()) {
                // Pre-parse timestamp once
                LocalDateTime dateTime = null;
                try {
                    String dateStr = entry.get(0).trim().replaceAll(" \\(\\d+\\)", "");
                    dateTime = LocalDateTime.parse(dateStr, FORMATTER);
                } catch (Exception e) {
                    // Parsing failed, use null for stable sort
                }
                timestampEntriesWithDates.add(new TimestampEntry(entry, dateTime));
            } else {
                nonTimestampEntries.add(entry);
            }
        }

        // Sort by pre-parsed timestamps - O(N log N) comparisons with O(1) per comparison
        timestampEntriesWithDates.sort((a, b) -> {
            if (a.dateTime == null && b.dateTime == null) return 0;
            if (a.dateTime == null) return 1;  // nulls last
            if (b.dateTime == null) return -1;
            return a.dateTime.compareTo(b.dateTime);
        });
        
        // Extract sorted entries
        List<List<String>> timestampEntries = new ArrayList<>(timestampEntriesWithDates.size());
        for (TimestampEntry te : timestampEntriesWithDates) {
            timestampEntries.add(te.entry);
        }

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
            
            // Single-pass entry cleaning: remove excessive blanks and trailing blanks in one go
            int consecutiveBlanks = 0;
            int lastNonBlankIndex = -1;
            
            // First pass: identify last non-blank line
            for (int j = entry.size() - 1; j >= 0; j--) {
                if (!entry.get(j).trim().isEmpty()) {
                    lastNonBlankIndex = j;
                    break;
                }
            }
            
            // Second pass: add lines up to last non-blank, limiting consecutive blanks
            for (int j = 0; j <= lastNonBlankIndex && j < entry.size(); j++) {
                String line = entry.get(j);
                if (line.trim().isEmpty()) {
                    consecutiveBlanks++;
                    // Allow up to 1 blank line within entries (for paragraph breaks)
                    if (consecutiveBlanks <= 1) {
                        sortedLines.add(line);
                    }
                } else {
                    sortedLines.add(line);
                    consecutiveBlanks = 0;
                }
            }
            
            // Use centralized format rules for entry separation
            for (int j = 0; j < LogFileFormat.FILE_ENTRY_SEPARATOR_BLANKS; j++) {
                sortedLines.add("");
            }
        }

        return sortedLines;
    }
    
    /**
     * Normalizes spacing by removing excessive blank lines.
     * Uses centralized format rules from LogFileFormat.
     * @deprecated Use LogFileFormat.normalizeSpacing() instead
     */
    @Deprecated
    public static List<String> getNormalized(List<String> updatedLines) {
        return LogFileFormat.normalizeSpacing(updatedLines);
    }
    
    /**
     * Performs complete formatting operation: sort, normalize, backup, and write.
     * Runs in a background thread with progress dialog.
     * 
     * @param parentFrame Frame for showing dialogs
     * @param logFileHandler Log file handler for reading/writing
     * @param onSuccess Callback to run after successful formatting
     */
    public static void performFormatting(Frame parentFrame, LogFileHandler logFileHandler, Runnable onSuccess) {
        SwingUtilities.invokeLater(() -> {
            LoadingProgressDialog progress = new LoadingProgressDialog(parentFrame, "Formatting Log File");
            
            new Thread(() -> {
                try {
                    progress.setStatus("Reading log file...");
                    progress.show();
                    
                    Path logPath = Path.of(System.getProperty("user.home"), "log.txt");
                    if (!Files.exists(logPath)) {
                        SwingUtilities.invokeLater(() -> {
                            progress.close();
                            DialogHelper.showFileNotFound(parentFrame);
                        });
                        return;
                    }
                    
                    // Read file from memory (already decrypted if encrypted)
                    List<String> lines;
                    boolean isEncrypted = logFileHandler.isEncrypted();
                    
                    // Security: Check file size before processing
                    if (!isEncrypted && Files.size(logPath) > MAX_FILE_SIZE) {
                        throw new IllegalStateException("File too large to format (max 50MB)");
                    }
                    
                    if (isEncrypted) {
                        progress.setStatus("Decrypting log file...");
                        lines = new ArrayList<>(logFileHandler.getLines());
                    } else {
                        lines = Files.readAllLines(logPath);
                    }
                    
                    // Security: Validate collection size
                    if (lines.size() > MAX_COLLECTION_SIZE) {
                        throw new IllegalStateException("Too many lines to format (max " + MAX_COLLECTION_SIZE + ")");
                    }
                    
                    // Remove secure clipboard markers
                    progress.setStatus("Processing entries...");
                    lines = lines.stream()
                        .map(LogFileHandler::removeSecureMarker)
                        .collect(Collectors.toList());
                    
                    // Sort entries - this creates compact format with no blanks between entries
                    progress.setStatus("Sorting and formatting entries...");
                    List<String> formatted = sortEntriesByTimestamp(lines);
                    
                    // Create backup before writing
                    progress.setStatus("Creating backup...");
                    if (logFileHandler.getBackupManager() != null) {
                        logFileHandler.getBackupManager().createNumberedBackup();
                    }
                    
                    // Write to disk securely and memory-efficiently
                    progress.setStatus("Writing formatted file...");
                    if (isEncrypted) {
                        // For encrypted files: encrypt the formatted content
                        String fullText = String.join(LogFileFormat.INTERNAL_LINE_SEPARATOR, formatted);
                        logFileHandler.getEncryptionManager().encryptFile(fullText);
                        // Update the cache with the formatted content
                        // This ensures the next getLines() call returns the formatted data
                        logFileHandler.updateCachedLines(formatted);
                    } else {
                        // Write directly to file
                        Files.write(logPath, formatted);
                    }
                    
                    // Clear any pending writes that might overwrite our formatted content
                    logFileHandler.clearPendingWrites();
                    
                    // For encrypted files, the cache is already updated above
                    // For non-encrypted files, invalidate to force reload from disk
                    // This ensures the view picks up the newly formatted content
                    progress.setStatus("Finalizing...");
                    if (!isEncrypted) {
                        logFileHandler.invalidateCaches();
                    }
                    
                    // Success - reload the view
                    SwingUtilities.invokeLater(() -> {
                        progress.close();
                        
                        // Reload the view BEFORE showing success message
                        // This ensures user sees the formatted content immediately
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                        
                        // Small delay to let the view refresh, then show success
                        SwingUtilities.invokeLater(() -> {
                            DialogHelper.showSuccess(parentFrame,
                                "Success",
                                "Formatting Complete",
                                "Log file has been reformatted successfully.<br>" +
                                "All entries now have consistent spacing.");
                        });
                    });
                    
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        progress.close();
                        // Security: Don't expose internal error details
                        DialogHelper.showError(parentFrame,
                            "Error",
                            "Formatting Failed",
                            "Unable to format the log file.<br>" +
                            "Please check file permissions and try again.");
                    });
                }
            }, "Format-Thread").start();
        });
    }
}
