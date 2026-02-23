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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import gui.HighlightableTextPane;
import markdown.LinkHandler;
import markdown.MarkdownRenderer;

/**
 * Handles loading and processing log files for display in the Full Log view.
 * Manages decryption, parsing, and rendering of log entries.
 */
public class FullLogFileLoader {
    
    // Use centralized UI render cap
    
    private final LogFileHandler logFileHandler;
    private final HighlightableTextPane textPane;
    // Cached parsed data to avoid reparsing when the file hasn't changed
    private ParsedLogData cachedParsedData = null;
    private long cachedLastModified = 0L;
    private final Object cacheLock = new Object();
    
    public FullLogFileLoader(LogFileHandler logFileHandler, HighlightableTextPane textPane) {
        this.logFileHandler = logFileHandler;
        this.textPane = textPane;
    }
    
    public void fallbackReadRaw(Path chosen) {
        try {
            if (Files.exists(chosen) && Files.size(chosen) > ResourceLimits.MAX_FILE_SIZE) {
                String shortTitle = "File Too Large to Display";
                String longMessage = "The selected file is larger than the allowed display limit (" + (ResourceLimits.MAX_FILE_SIZE / (1024 * 1024)) + " MB).";
                filehandling.DialogHandler.showLimitExceeded(shortTitle, longMessage);
                textPane.setText("File too large to display raw. Please use filters or open a subset.");
                textPane.clearHighlights();
                return;
            }

            byte[] bytes = Files.readAllBytes(chosen);
            String content = new String(bytes);
            textPane.setText(content);
            textPane.clearHighlights();
            textPane.setCaretPosition(0);
        } catch (IOException e) {
            // Security: Don't expose file paths or internal error details
            textPane.setText("Error reading log file. Please check file permissions and format.");
            textPane.clearHighlights();
        }
    }
    
    /**
     * Loads and processes the log file for display without scrolling to bottom.
     * @param logPath Path to the log file
     * @throws Exception if loading fails
     */
    public void loadAndProcessLogFile(Path logPath, boolean scrollToBottom) throws Exception {
        ParsedLogData data = loadAndProcessLogFileInternal(logPath, scrollToBottom);
        MarkdownRenderer.renderMarkdownFromEntries(textPane, data.entriesToRender, scrollToBottom);
        LinkHandler.addLinkListeners(textPane);
    }

    /**
     * Loads and processes the log file, returning the parsed data for reuse.
     * This optimized version allows callers to reuse the parsed entries for statistics.
     * @param logPath Path to the log file
     * @param scrollToBottom whether to scroll to bottom after loading
     * @return ParsedLogData containing all entries and entries to render
     * @throws Exception if loading fails
     */
    public ParsedLogData loadAndProcessLogFileWithData(Path logPath, boolean scrollToBottom) throws Exception {
        // For compatibility: parse then render on current thread
        ParsedLogData data = loadAndProcessLogFileInternal(logPath, scrollToBottom);
        MarkdownRenderer.renderMarkdownFromEntries(textPane, data.entriesToRender, scrollToBottom);
        LinkHandler.addLinkListeners(textPane);
        return data;
    }

    /**
     * Parse the log file and return parsed data without performing any rendering.
     * Callers can invoke rendering on the EDT after parsing completes.
     */
    public ParsedLogData parseLogFile(Path logPath, boolean scrollToBottom) throws Exception {
        return loadAndProcessLogFileInternal(logPath, scrollToBottom);
    }

    /**
     * Invalidate any cached parsed data. Call when the underlying file is modified externally
     * or when encryption/decryption state has changed.
     */
    public void invalidateCache() {
        synchronized (cacheLock) {
            cachedParsedData = null;
            cachedLastModified = 0L;
        }
    }

    /**
     * Internal method that handles the parsing logic without rendering.
     */
    private ParsedLogData loadAndProcessLogFileInternal(Path logPath, boolean scrollToBottom) throws Exception {
        // Try to reuse cached parsed data when file unchanged and no pending writes
        try {
            long lastModified = 0L;
            if (Files.exists(logPath)) {
                lastModified = Files.getLastModifiedTime(logPath).toMillis();
            }
            synchronized (cacheLock) {
                if (cachedParsedData != null && cachedLastModified == lastModified && !logFileHandler.hasPendingWrites()) {
                    return cachedParsedData;
                }
            }
        } catch (Exception e) {
            // Fall through to normal parse on any error
        }

        // Use getLines() which returns cached lines for encrypted files
        // This ensures we get the most recent data including any formatting changes
        List<String> lines = logFileHandler.getLines();

        // Remove secure clipboard markers from lines
        lines = lines.stream()
            .map(LogFileHandler::removeSecureMarker)
            .collect(Collectors.toList());

        // Parse all entries (needed for statistics)
        List<List<String>> allEntries = LogParser.parseEntriesForFullLog(lines);
        List<List<String>> entriesToRender;

        if (allEntries.size() > ResourceLimits.MAX_ENTRIES_TO_RENDER) {
            // `allEntries` is sorted oldest first; take the last N (most recent) entries
            int total = allEntries.size();
            int start = Math.max(0, total - ResourceLimits.MAX_ENTRIES_TO_RENDER);
            entriesToRender = new ArrayList<>(allEntries.subList(start, total));
        } else {
            entriesToRender = allEntries;
        }

        ParsedLogData result = new ParsedLogData(allEntries, entriesToRender);
        // Update cache
        try {
            long lastModified = 0L;
            if (Files.exists(logPath)) {
                lastModified = Files.getLastModifiedTime(logPath).toMillis();
            }
            synchronized (cacheLock) {
                cachedParsedData = result;
                cachedLastModified = lastModified;
            }
        } catch (Exception e) {
            // Ignore cache update failures
        }

        return result;
    }
    
    /**
     * Loads and processes filtered log entries for display.
     * @param year The year to filter by
     * @param month The month to filter by (1-12)
     * @throws Exception if loading fails
     */
    /**
     * Loads and processes the log file for display without scrolling to bottom.
     * @param logPath Path to the log file
     * @throws Exception if loading fails
     */
    public void loadAndProcessLogFileNoScroll(Path logPath) throws Exception {
        loadAndProcessLogFile(logPath, false);
    }

    public void loadFilteredEntries(int year, int month) throws Exception {
        // Prefer using cached parsed data when valid to avoid reparsing
        List<List<String>> allEntries = null;
        try {
            long lastModified = 0L;
            Path logPath = logFileHandler.getFilePath();
            if (Files.exists(logPath)) {
                lastModified = Files.getLastModifiedTime(logPath).toMillis();
            }
            synchronized (cacheLock) {
                if (cachedParsedData != null && cachedLastModified == lastModified && !logFileHandler.hasPendingWrites()) {
                    allEntries = cachedParsedData.allEntries;
                }
            }
        } catch (Exception e) {
            allEntries = null;
        }

        if (allEntries == null) {
            // Use getLines() which returns cached lines
            List<String> lines = logFileHandler.getLines();
            // Remove secure clipboard markers from lines
            lines = lines.stream()
                .map(LogFileHandler::removeSecureMarker)
                .collect(Collectors.toList());
            // Parse all entries
            allEntries = LogParser.parseEntriesForFullLog(lines);
        }

        // Filter entries by year and month
        List<List<String>> filteredEntries = filterEntriesByDate(allEntries, year, month);

        // Apply lazy loading if too many entries
        List<List<String>> entriesToRender;
        if (filteredEntries.size() > ResourceLimits.MAX_ENTRIES_TO_RENDER) {
            int total = filteredEntries.size();
            int start = Math.max(0, total - ResourceLimits.MAX_ENTRIES_TO_RENDER);
            entriesToRender = new ArrayList<>(filteredEntries.subList(start, total));
        } else {
            entriesToRender = filteredEntries;
        }

        MarkdownRenderer.renderMarkdownFromEntries(textPane, entriesToRender, true);
        LinkHandler.addLinkListeners(textPane);
    }
    
    /**
     * Loads and processes filtered log entries for display by year only.
     * @param year The year to filter by
     * @throws Exception if loading fails
     */
    public void loadFilteredEntriesByYear(int year) throws Exception {
        // Prefer using cached parsed data when valid to avoid reparsing
        List<List<String>> allEntries = null;
        try {
            long lastModified = 0L;
            Path logPath = logFileHandler.getFilePath();
            if (Files.exists(logPath)) {
                lastModified = Files.getLastModifiedTime(logPath).toMillis();
            }
            synchronized (cacheLock) {
                if (cachedParsedData != null && cachedLastModified == lastModified && !logFileHandler.hasPendingWrites()) {
                    allEntries = cachedParsedData.allEntries;
                }
            }
        } catch (Exception e) {
            allEntries = null;
        }

        if (allEntries == null) {
            // Use getLines() which returns cached lines
            List<String> lines = logFileHandler.getLines();
            // Remove secure clipboard markers from lines
            lines = lines.stream()
                .map(LogFileHandler::removeSecureMarker)
                .collect(Collectors.toList());
            // Parse all entries
            allEntries = LogParser.parseEntriesForFullLog(lines);
        }

        // Filter entries by year
        List<List<String>> filteredEntries = filterEntriesByYear(allEntries, year);

        // Apply lazy loading if too many entries
        List<List<String>> entriesToRender;
        if (filteredEntries.size() > ResourceLimits.MAX_ENTRIES_TO_RENDER) {
            int total = filteredEntries.size();
            int start = Math.max(0, total - ResourceLimits.MAX_ENTRIES_TO_RENDER);
            entriesToRender = new ArrayList<>(filteredEntries.subList(start, total));
        } else {
            entriesToRender = filteredEntries;
        }

        MarkdownRenderer.renderMarkdownFromEntries(textPane, entriesToRender, true);
        LinkHandler.addLinkListeners(textPane);
    }
    
    /**
     * Filters entries by year and month.
     */
    private List<List<String>> filterEntriesByDate(List<List<String>> entries, int year, int month) {
        return entries.stream()
            .filter(entry -> {
                if (entry.isEmpty()) return false;
                String timestamp = entry.get(0);
                try {
                    // Parse timestamp like "HH:mm yyyy-MM-dd"
                    String[] parts = timestamp.split(" ");
                    if (parts.length >= 2) {
                        String dateStr = parts[1];
                        String[] dateParts = dateStr.split("-");
                        if (dateParts.length >= 2) {
                            int entryYear = Integer.parseInt(dateParts[0]);
                            int entryMonth = Integer.parseInt(dateParts[1]);
                            return entryYear == year && entryMonth == month;
                        }
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Skip malformed entries
                }
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Filters entries by year only.
     */
    private List<List<String>> filterEntriesByYear(List<List<String>> entries, int year) {
        return entries.stream()
            .filter(entry -> {
                if (entry.isEmpty()) return false;
                String timestamp = entry.get(0);
                try {
                    // Parse timestamp like "HH:mm yyyy-MM-dd"
                    String[] parts = timestamp.split(" ");
                    if (parts.length >= 2) {
                        String dateStr = parts[1];
                        String[] dateParts = dateStr.split("-");
                        if (dateParts.length >= 1) {
                            int entryYear = Integer.parseInt(dateParts[0]);
                            return entryYear == year;
                        }
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Skip malformed entries
                }
                return false;
            })
            .collect(Collectors.toList());
    }
}


