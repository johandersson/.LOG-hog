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

import encryption.EncryptionManager;
import gui.HighlightableTextPane;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import markdown.LinkHandler;
import markdown.MarkdownRenderer;

/**
 * Handles loading and processing log files for display in the Full Log view.
 * Manages decryption, parsing, and rendering of log entries.
 */
public class FullLogFileLoader {
    
    private static final int MAX_ENTRIES_TO_RENDER = 5000; // Limit for performance
    
    private final LogFileHandler logFileHandler;
    private final HighlightableTextPane textPane;
    
    public FullLogFileLoader(LogFileHandler logFileHandler, HighlightableTextPane textPane) {
        this.logFileHandler = logFileHandler;
        this.textPane = textPane;
    }
    
    /**
     * Loads and processes the log file for display.
     * @param logPath Path to the log file
     * @throws Exception if loading fails
     */
    public void loadAndProcessLogFile(Path logPath) throws Exception {
        // Use getLines() which returns cached lines for encrypted files
        // This ensures we get the most recent data including any formatting changes
        List<String> lines = logFileHandler.getLines();
        
        // Remove secure clipboard markers from lines
        lines = lines.stream()
            .map(LogFileHandler::removeSecureMarker)
            .collect(Collectors.toList());
        
        // Lazy loading: Only render recent N entries for performance
        List<List<String>> allEntries = LogParser.parseEntriesForFullLog(lines);
        List<List<String>> entriesToRender;
        
        if (allEntries.size() > MAX_ENTRIES_TO_RENDER) {
            // Take the most recent N entries (already sorted newest first)
            entriesToRender = allEntries.subList(0, MAX_ENTRIES_TO_RENDER);
            // Add info message at top
            List<String> infoEntry = new ArrayList<>();
            infoEntry.add("Showing " + MAX_ENTRIES_TO_RENDER + " most recent entries (out of " + allEntries.size() + " total)");
            infoEntry.add("Use the Log List view with filters to browse older entries.");
            entriesToRender = new ArrayList<>(entriesToRender);
            entriesToRender.add(0, infoEntry);
        } else {
            entriesToRender = allEntries;
        }
        
        MarkdownRenderer.renderMarkdownFromEntries(textPane, entriesToRender);
        LinkHandler.addLinkListeners(textPane);
    }
    
    /**
     * Attempts to read the file as raw text when normal loading fails.
     * @param chosen Path to the log file
     */
    public void fallbackReadRaw(Path chosen) {
        try {
            var bytes = Files.readAllBytes(chosen);
            var content = new String(bytes);
            textPane.setText(content);
            textPane.clearHighlights();
            textPane.setCaretPosition(0);
        } catch (Exception e) {
            // Security: Don't expose file paths or internal error details
            textPane.setText("Error reading log file. Please check file permissions and format.");
            textPane.clearHighlights();
        }
    }
}
