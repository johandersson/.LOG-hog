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
        ParsedLogData data = loadAndProcessLogFileInternal(logPath, scrollToBottom);
        MarkdownRenderer.renderMarkdownFromEntries(textPane, data.entriesToRender, scrollToBottom);
        LinkHandler.addLinkListeners(textPane);
        return data;
    }

    /**
     * Parses the log file without rendering - for use with background workers.
     * Rendering should be done on the EDT after this returns.
     * @param logPath Path to the log file
     * @return ParsedLogData containing all entries and entries to render
     * @throws Exception if loading fails
     */
    public ParsedLogData parseLogFile(Path logPath) throws Exception {
        return loadAndProcessLogFileInternal(logPath, false);
    }

    /**
     * Renders pre-parsed entries to the text pane.
     * Must be called on the EDT.
     * @param data The parsed log data
     * @param scrollToBottom whether to scroll to bottom after rendering
     */
    public void renderParsedData(ParsedLogData data, boolean scrollToBottom) {
        MarkdownRenderer.renderMarkdownFromEntries(textPane, data.entriesToRender, scrollToBottom);
        LinkHandler.addLinkListeners(textPane);
    }

    /**
     * Internal method that handles the parsing logic without rendering.
     */
    private ParsedLogData loadAndProcessLogFileInternal(Path logPath, boolean scrollToBottom) throws Exception {
        // Use streaming parsing when possible to avoid allocating large intermediate lists
        List<List<String>> allEntries;
        if (logFileHandler.isEncrypted()) {
            // Encrypted files use cached lines already
            List<String> lines = logFileHandler.getLines();
            lines = lines.stream().map(LogFileHandler::removeSecureMarker).collect(Collectors.toList());
            allEntries = LogParser.parseEntriesForFullLog(lines);
        } else {
            try (var stream = logFileHandler.getLinesStreamed()) {
                java.util.stream.Stream<String> cleaned = stream.map(LogFileHandler::removeSecureMarker);
                allEntries = StreamProcessor.parseEntriesForFullLogStream(cleaned);
            }
        }
        List<List<String>> entriesToRender;

        if (allEntries.size() > ResourceLimits.MAX_ENTRIES_TO_RENDER) {
            // Take the most recent N entries (already sorted newest first)
            entriesToRender = allEntries.subList(0, ResourceLimits.MAX_ENTRIES_TO_RENDER);
            // Add info message at top
            List<String> infoEntry = new ArrayList<>();
            infoEntry.add("Showing " + ResourceLimits.MAX_ENTRIES_TO_RENDER + " most recent entries (out of " + allEntries.size() + " total)");
            infoEntry.add("Use the Log List view with filters to browse older entries.");
            entriesToRender = new ArrayList<>(entriesToRender);
            entriesToRender.add(0, infoEntry);
        } else {
            entriesToRender = allEntries;
        }

        return new ParsedLogData(allEntries, entriesToRender);
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
        // Use getLines() which returns cached lines
        List<String> lines = logFileHandler.getLines();
        
        // Remove secure clipboard markers from lines
        lines = lines.stream()
            .map(LogFileHandler::removeSecureMarker)
            .collect(Collectors.toList());
        
        // Parse all entries
        List<List<String>> allEntries = LogParser.parseEntriesForFullLog(lines);
        
        // Filter entries by year and month
        List<List<String>> filteredEntries = filterEntriesByDate(allEntries, year, month);
        
        // Apply lazy loading if too many entries
        List<List<String>> entriesToRender;
        if (filteredEntries.size() > ResourceLimits.MAX_ENTRIES_TO_RENDER) {
            entriesToRender = filteredEntries.subList(0, ResourceLimits.MAX_ENTRIES_TO_RENDER);
            // Add info message at top
            List<String> infoEntry = new ArrayList<>();
            infoEntry.add("Showing " + ResourceLimits.MAX_ENTRIES_TO_RENDER + " most recent entries (out of " + filteredEntries.size() + " total for " + year + "-" + String.format("%02d", month) + ")");
            infoEntry.add("Use the Log List view with filters to browse older entries.");
            entriesToRender = new ArrayList<>(entriesToRender);
            entriesToRender.add(0, infoEntry);
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
    /**
     * Loads and processes the log file for display without scrolling to bottom.
     * @param logPath Path to the log file
        // Use getLines() which returns cached lines
        List<String> lines = logFileHandler.getLines();
        
        // Remove secure clipboard markers from lines
        lines = lines.stream()
            .map(LogFileHandler::removeSecureMarker)
            .collect(Collectors.toList());
        
        // Parse all entries
        List<List<String>> allEntries = LogParser.parseEntriesForFullLog(lines);
        
        // Filter entries by year
        List<List<String>> filteredEntries = filterEntriesByYear(allEntries, year);
        
        // Apply lazy loading if too many entries
        List<List<String>> entriesToRender;
        if (filteredEntries.size() > MAX_ENTRIES_TO_RENDER) {
            entriesToRender = filteredEntries.subList(0, MAX_ENTRIES_TO_RENDER);
            // Add info message at top
            List<String> infoEntry = new ArrayList<>();
            infoEntry.add("Showing " + MAX_ENTRIES_TO_RENDER + " most recent entries (out of " + filteredEntries.size() + " total for " + year + ")");
            infoEntry.add("Use the Log List view with filters to browse older entries.");
            entriesToRender = new ArrayList<>(entriesToRender);
            entriesToRender.add(0, infoEntry);
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


