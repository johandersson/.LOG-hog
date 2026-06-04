/*
 * Copyright (C) 2026 Johan Andersson
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private ParsedLogData cachedParsedData;
    private long cachedLastModified;
    private final Object cacheLock = new Object();
    
    public FullLogFileLoader(LogFileHandler logFileHandler, HighlightableTextPane textPane) {
        this.logFileHandler = logFileHandler;
        this.textPane = textPane;
    }

    /**
     * Adds display suffixes (1), (2), etc. to duplicate timestamps in entries.
     * Processes entries in chronological order (oldest first) for consistent numbering.
     * @param entries List of entries where each entry is a List of Strings with timestamp as first element
     * @return New list with display suffixes added to timestamps
     */
    private List<List<String>> addDisplaySuffixes(List<List<String>> entries) {
        if (entries == null || entries.isEmpty()) return entries;
        
        Map<String, Integer> occurrenceCount = new HashMap<>();
        List<List<String>> result = new ArrayList<>(entries.size());
        
        for (List<String> entry : entries) {
            if (entry.isEmpty()) {
                result.add(entry);
                continue;
            }
            
            String rawTs = entry.get(0).trim();
            // Strip any existing suffix from old files
            String cleanTs = rawTs.replaceAll(" \\(\\d+\\)$", "");
            
            // Check if this is a timestamp line
            if (utils.DateHandler.isTimestamp(cleanTs)) {
                int occurrence = occurrenceCount.getOrDefault(cleanTs, 0);
                occurrenceCount.put(cleanTs, occurrence + 1);
                
                // Generate display timestamp with suffix for duplicates
                String displayTs = occurrence > 0 ? cleanTs + " (" + occurrence + ")" : cleanTs;
                
                // Create new entry with display timestamp
                List<String> newEntry = new ArrayList<>(entry.size());
                newEntry.add(displayTs);
                for (int i = 1; i < entry.size(); i++) {
                    newEntry.add(entry.get(i));
                }
                result.add(newEntry);
            } else {
                result.add(entry);
            }
        }
        
        return result;
    }

    /** Compatibility: invalidate internal caches used by this loader. */
    public void invalidateCache() {
        synchronized (cacheLock) {
            this.cachedParsedData = null;
            this.cachedLastModified = 0L;
        }
    }

    /** Compatibility overload: parse log file and return parsed data while honoring scroll flag. */
    public ParsedLogData parseLogFile(Path logPath, boolean scrollToBottom) throws Exception {
        return loadAndProcessLogFileInternal(logPath, scrollToBottom);
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
        // Respect a practical UI render cap to avoid blocking the EDT when many entries
        List<List<String>> toRender = data.entriesToRender;
        if (toRender != null && toRender.size() > ResourceLimits.MAX_ENTRIES_TO_RENDER_UI) {
            // Create an informational top entry and render only the newest UI-limited subset
            List<String> infoEntry = new ArrayList<>();
            infoEntry.add("Showing " + ResourceLimits.MAX_ENTRIES_TO_RENDER_UI + " most recent entries (UI-limited) out of " + data.getTotalEntryCount() + " total");
            infoEntry.add("Use the Log List view with filters to browse older entries.");

            List<List<String>> uiSubset = new ArrayList<>();
            int start = Math.max(0, toRender.size() - ResourceLimits.MAX_ENTRIES_TO_RENDER_UI);
            uiSubset.addAll(toRender.subList(start, toRender.size()));
            // Show informational note after the rendered entries so it appears at the bottom
            uiSubset.add(infoEntry);
            // Try to assemble document from per-entry cache when available to avoid
            // repeated entry-level rendering. Falls back to full renderer when cache
            // is unavailable.
            if (!assembleDocumentFromPerEntryCache(data, uiSubset, scrollToBottom)) {
                MarkdownRenderer.renderMarkdownFromEntries(textPane, uiSubset, scrollToBottom);
            }
        } else {
            if (!assembleDocumentFromPerEntryCache(data, toRender, scrollToBottom)) {
                MarkdownRenderer.renderMarkdownFromEntries(textPane, toRender, scrollToBottom);
            }
        }
        LinkHandler.addLinkListeners(textPane);
    }

    /**
     * Attempt to build the displayed document by assembling per-entry cached
     * StyledDocuments stored in ParsedLogData.perEntryDocCache. Returns true
     * when successful; false if any entry is missing from the cache and the
     * caller should fall back to MarkdownRenderer.
     */
    private boolean assembleDocumentFromPerEntryCache(ParsedLogData data, List<List<String>> entriesToRender, boolean scrollToBottom) {
        if (data == null || entriesToRender == null) return false;
        // Quick-path: require a non-empty per-entry cache
        synchronized (data.perEntryDocCache) {
            if (data.perEntryDocCache.isEmpty()) return false;
            try {
                javax.swing.text.DefaultStyledDocument target = new javax.swing.text.DefaultStyledDocument();
                boolean first = true;
                for (List<String> entry : entriesToRender) {
                    if (!first) {
                        String separator = filehandling.LogFileFormat.INTERNAL_LINE_SEPARATOR.repeat(filehandling.LogFileFormat.DISPLAY_ENTRY_SEPARATOR_BLANKS);
                        javax.swing.text.SimpleAttributeSet sepAttrs = new javax.swing.text.SimpleAttributeSet();
                        target.insertString(target.getLength(), separator, sepAttrs);
                    }
                    first = false;

                    String key = entry.isEmpty() ? "" : entry.get(0).trim();
                    java.lang.ref.SoftReference<javax.swing.text.StyledDocument> ref = data.perEntryDocCache.get(key);
                    if (ref == null) return false;
                    javax.swing.text.StyledDocument src = ref.get();
                    if (src == null) return false;

                    // Copy content from src into target preserving attributes
                    int len = src.getLength();
                    int pos = 0;
                    while (pos < len) {
                        javax.swing.text.Element elem = src.getCharacterElement(pos);
                        int start = elem.getStartOffset();
                        int end = elem.getEndOffset();
                        if (end > len) end = len;
                        String text = src.getText(start, end - start);
                        javax.swing.text.AttributeSet attrs = elem.getAttributes();
                        javax.swing.text.SimpleAttributeSet copy = new javax.swing.text.SimpleAttributeSet(attrs);
                        target.insertString(target.getLength(), text, copy);
                        pos = end;
                    }
                }

                textPane.setDocument(target);
                textPane.setCaretPosition(scrollToBottom ? target.getLength() : 0);
                return true;
            } catch (Exception e) {
                // On any failure, bail out and let caller fall back to MarkdownRenderer
                return false;
            }
        }
    }

    /**
     * Internal method that handles the parsing logic without rendering.
     */
    private ParsedLogData loadAndProcessLogFileInternal(Path logPath, boolean scrollToBottom) throws Exception {
        // Reference parameters for diagnostics and to avoid unused-parameter warnings
        utils.Log.debug(() -> "loadAndProcessLogFileInternal: " + (logPath != null ? logPath.toString() : "(null)") + " scroll=" + scrollToBottom);
        // Fast path: if we have cached parsed data and the file hasn't changed
        // since last parse, and there are no pending writes, reuse the cache
        // to avoid expensive re-parsing and markdown rendering.
        try {
            long lm = 0L;
            if (logPath != null && Files.exists(logPath)) {
                try {
                    lm = Files.getLastModifiedTime(logPath).toMillis();
                } catch (Exception ignored) {
                    lm = 0L;
                }
            }
            synchronized (cacheLock) {
                if (this.cachedParsedData != null && this.cachedLastModified == lm && !logFileHandler.hasPendingWrites()) {
                    return this.cachedParsedData;
                }
            }
        } catch (Exception ignored) {
            // If anything goes wrong determining the cache validity, continue with full parse
        }
        // Use streaming parsing when possible to avoid allocating large intermediate lists
        List<List<String>> entriesToRender;
        if (logFileHandler.isEncrypted()) {
            // Encrypted files use cached lines already
            List<String> lines = logFileHandler.getLines();
            lines = lines.stream().map(LogFileHandler::removeSecureMarker).collect(Collectors.toList());
            List<List<String>> allEntries = LogParser.parseEntriesForFullLog(lines);
            if (allEntries.size() > ResourceLimits.MAX_ENTRIES_TO_RENDER) {
                // Select the most recent entries (tail) and keep chronological order (oldest->newest)
                int start = Math.max(0, allEntries.size() - ResourceLimits.MAX_ENTRIES_TO_RENDER);
                entriesToRender = new ArrayList<>(allEntries.subList(start, allEntries.size()));
                List<String> infoEntry = new ArrayList<>();
                infoEntry.add("Showing " + ResourceLimits.MAX_ENTRIES_TO_RENDER + " most recent entries (out of " + allEntries.size() + " total)");
                infoEntry.add("Use the Log List view with filters to browse older entries.");
                // Append informational entry so it displays after the log entries
                entriesToRender.add(infoEntry);
            } else {
                entriesToRender = allEntries;
            }
            // Add display suffixes for duplicate timestamps
            entriesToRender = addDisplaySuffixes(entriesToRender);
            // Cache parsed data for later lookups (e.g., getEntryContent)
            long lm = 0L;
            try {
                Path p = logPath;
                if (p != null && Files.exists(p)) lm = Files.getLastModifiedTime(p).toMillis();
            } catch (Exception ignored) {}
            synchronized (cacheLock) {
                ParsedLogData pd = new ParsedLogData(allEntries, entriesToRender);
                // Build per-entry rendered documents for UI-sized sets to speed up
                // subsequent re-renders. Limit to avoid excess memory use.
                try {
                    if (entriesToRender != null && entriesToRender.size() <= ResourceLimits.MAX_ENTRIES_TO_RENDER_UI) {
                        synchronized (pd.perEntryDocCache) {
                            for (List<String> entry : entriesToRender) {
                                if (entry == null || entry.isEmpty()) continue;
                                String key = entry.get(0).trim();
                                // Avoid rebuilding if already present
                                if (pd.perEntryDocCache.containsKey(key)) continue;
                                try {
                                    javax.swing.text.StyledDocument doc = MarkdownRenderer.buildDocumentFromEntries(java.util.List.of(entry), null);
                                    pd.perEntryDocCache.put(key, new java.lang.ref.SoftReference<>(doc));
                                } catch (Exception ignored) {
                                    // If building an entry doc fails, skip caching it
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                this.cachedParsedData = pd;
                this.cachedLastModified = lm;
            }
            return this.cachedParsedData;
        } else {
            try (var stream = logFileHandler.getLinesStreamed()) {
                java.util.stream.Stream<String> cleaned = stream.map(LogFileHandler::removeSecureMarker);
                StreamProcessor.ParseResult res = StreamProcessor.parseEntriesForFullLogStreamWithStats(cleaned);
                long total = res.totalEntries;
                List<List<String>> limited = res.entriesNewestFirst;
                // limited is newest-first; convert to chronological order (oldest->newest)
                List<List<String>> chrono = new ArrayList<>();
                if (limited != null) {
                    chrono.addAll(limited);
                    java.util.Collections.reverse(chrono);
                }

                if (total > ResourceLimits.MAX_ENTRIES_TO_RENDER) {
                    int start = Math.max(0, chrono.size() - ResourceLimits.MAX_ENTRIES_TO_RENDER);
                    List<List<String>> tail = new ArrayList<>(chrono.subList(start, chrono.size()));
                    List<String> infoEntry = new ArrayList<>();
                    infoEntry.add("Showing " + ResourceLimits.MAX_ENTRIES_TO_RENDER + " most recent entries (out of " + total + " total)");
                    infoEntry.add("Use the Log List view with filters to browse older entries.");
                    // Place informational entry after the tail so it appears at the bottom of the view
                    tail.add(infoEntry);
                    entriesToRender = tail;
                } else {
                    entriesToRender = chrono;
                }
                // Add display suffixes for duplicate timestamps
                entriesToRender = addDisplaySuffixes(entriesToRender);
                // Cache parsed data for later lookups
                long lm = 0L;
                try {
                    Path p = logPath;
                    if (p != null && Files.exists(p)) lm = Files.getLastModifiedTime(p).toMillis();
                } catch (Exception ignored) {}
                ParsedLogData pd = new ParsedLogData((int)Math.min(total, Integer.MAX_VALUE), entriesToRender);
                try {
                    if (entriesToRender != null && entriesToRender.size() <= ResourceLimits.MAX_ENTRIES_TO_RENDER_UI) {
                        synchronized (pd.perEntryDocCache) {
                            for (List<String> entry : entriesToRender) {
                                if (entry == null || entry.isEmpty()) continue;
                                String key = entry.get(0).trim();
                                if (pd.perEntryDocCache.containsKey(key)) continue;
                                try {
                                    javax.swing.text.StyledDocument doc = MarkdownRenderer.buildDocumentFromEntries(java.util.List.of(entry), null);
                                    pd.perEntryDocCache.put(key, new java.lang.ref.SoftReference<>(doc));
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                } catch (Exception ignored) {}
                synchronized (cacheLock) {
                    this.cachedParsedData = pd;
                    this.cachedLastModified = lm;
                }
                return pd;
            }
        }
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

        // Apply lazy loading if too many entries, but do NOT add info panel
        List<List<String>> entriesToRender;
        if (filteredEntries.size() > ResourceLimits.MAX_ENTRIES_TO_RENDER) {
            int start = Math.max(0, filteredEntries.size() - ResourceLimits.MAX_ENTRIES_TO_RENDER);
            entriesToRender = new ArrayList<>(filteredEntries.subList(start, filteredEntries.size()));
        } else {
            entriesToRender = filteredEntries;
        }

        // Add display suffixes for duplicate timestamps
        entriesToRender = addDisplaySuffixes(entriesToRender);
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

        // Add display suffixes for duplicate timestamps
        entriesToRender = addDisplaySuffixes(entriesToRender);
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

    /**
     * Gets the content of an entry by its display timestamp.
     * Used for content-based matching when navigating from Full Log to Log List
     * for entries with duplicate timestamps.
     * 
     * @param displayTimestamp The timestamp as shown in Full Log (may include suffix like " (1)")
     * @return The entry content (lines after timestamp joined with newline), or null if not found
     */
    public String getEntryContent(String displayTimestamp) {
        synchronized (cacheLock) {
            if (cachedParsedData == null || cachedParsedData.entriesToRender == null) {
                return null;
            }
            
            for (List<String> entry : cachedParsedData.entriesToRender) {
                if (!entry.isEmpty() && entry.get(0).trim().equals(displayTimestamp.trim())) {
                    // Found the entry - join content lines (skip timestamp at index 0)
                    StringBuilder content = new StringBuilder();
                    for (int i = 1; i < entry.size(); i++) {
                        if (i > 1) content.append('\n');
                        content.append(entry.get(i));
                    }
                    return content.toString().trim();
                }
            }
            return null;
        }
    }
}


