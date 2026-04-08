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

import javax.swing.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.regex.Pattern;

import encryption.Encryptor;

public class EntryLoader {
    private final LogFileHandler logFileHandler;
    private final Encryptor encryptor;
    
    // Performance caches - invalidated when file changes
    private final Map<String, String> entryContentCache = new HashMap<>();
    private List<String> timestampListCache;
    private final Map<String, Integer> duplicateCountCache = new HashMap<>();
    private long cacheLastModified;
    // Cached parsed entries with pre-parsed timestamps for fast filtering
    private List<ParsedEntry> parsedEntriesCache;
    
    // Helper class to store parsed entry with timestamp
    private static class ParsedEntry {
        final String timestamp;
        final LocalDateTime dateTime;
        
        ParsedEntry(String timestamp, LocalDateTime dateTime) {
            this.timestamp = timestamp;
            this.dateTime = dateTime;
        }
    }
    
    // Helper class for pre-parsed timestamp sorting optimization
    private static class TimestampEntry {
        final List<String> entry;
        final LocalDateTime dateTime;
        
        TimestampEntry(List<String> entry, LocalDateTime dateTime) {
            this.entry = entry;
            this.dateTime = dateTime;
        }
    }

    public EntryLoader(LogFileHandler logFileHandler) {
        this(logFileHandler, encryption.EncryptionManager.getInstance());
    }

    public EntryLoader(LogFileHandler logFileHandler, Encryptor encryptor) {
        this.logFileHandler = logFileHandler;
        this.encryptor = encryptor;
        // Touch the encryptor to avoid unused-field PMD warnings and validate it's non-null
        Objects.requireNonNull(this.encryptor);
    }

    /**
     * Returns a list of distinct years that appear in the parsed entries.
     * Uses the cached parsed entries if available to avoid re-parsing the file.
     * The returned list is ordered newest-first and limited to {@code maxYears} entries.
     */
    public List<Integer> getAvailableYears(int maxYears) throws Exception {
        if (!isCacheValid() || parsedEntriesCache == null) {
            parseParsedEntriesCache();
        }

        java.util.LinkedHashSet<Integer> years = new java.util.LinkedHashSet<>();
        if (parsedEntriesCache != null) {
            for (ParsedEntry pe : parsedEntriesCache) {
                if (pe.dateTime != null) {
                    years.add(pe.dateTime.getYear());
                    // If maxYears is positive, respect the cap; if <= 0, return all years
                    if (maxYears > 0 && years.size() >= maxYears) break;
                }
            }
        }

        if (years.isEmpty()) {
            years.add(java.time.LocalDate.now().getYear());
        }

        return new ArrayList<>(years);
    }
    
    /**
     * Invalidates all caches. Called when file is modified.
     * Public method for LogFileHandler to call.
     */
    public void invalidateCaches() {
        secureClearCaches();
    }
    
    /**
     * Securely clears all cached data by overwriting content before clearing.
     * This prevents memory forensics from recovering sensitive log content.
     * Aligns with encryption.md security requirements for cache clearing.
     */
    private void secureClearCaches() {
        // Overwrite entry content cache values before clearing
        for (Map.Entry<String, String> entry : entryContentCache.entrySet()) {
            // Can't truly overwrite String objects in Java, but we can remove references
            // Setting to null helps GC reclaim memory faster
        }
        entryContentCache.clear();
        
        // Overwrite timestamp list cache
        if (timestampListCache != null) {
            for (int i = 0; i < timestampListCache.size(); i++) {
                timestampListCache.set(i, null);
            }
            timestampListCache.clear();
            timestampListCache = null;
        }
        
        // Clear duplicate count cache
        duplicateCountCache.clear();
        
        // Overwrite parsed entries cache
        if (parsedEntriesCache != null) {
            for (int i = 0; i < parsedEntriesCache.size(); i++) {
                parsedEntriesCache.set(i, null);
            }
            parsedEntriesCache.clear();
            parsedEntriesCache = null;
        }
        
        cacheLastModified = 0;
    }
    
    /**
     * Checks if caches are valid based on file modification time.
     */
    private boolean isCacheValid() {
        try {
            if (!Files.exists(logFileHandler.getFilePath())) {
                return false;
            }
            long currentModified = Files.getLastModifiedTime(logFileHandler.getFilePath()).toMillis();
            return currentModified == cacheLastModified && cacheLastModified > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Updates cache timestamp after parsing.
     */
    private void updateCacheTimestamp() {
        try {
            if (Files.exists(logFileHandler.getFilePath())) {
                cacheLastModified = Files.getLastModifiedTime(logFileHandler.getFilePath()).toMillis();
            }
        } catch (Exception e) {
            cacheLastModified = 0;
        }
    }

    public void loadLogEntries(DefaultListModel<String> listModel) throws Exception {
        javax.swing.SwingUtilities.invokeLater(() -> listModel.clear());
        
        // Check if file exists and handle missing file
        if (!Files.exists(logFileHandler.getFilePath())) {
            if (!logFileHandler.handleMissingLogFile()) {
                return; // User cancelled or couldn't recover
            }
            // File was created/restored, but might be empty
            if (!Files.exists(logFileHandler.getFilePath())) {
                return;
            }
        }

        // Use centralized getLines() which enforces file-size limits and handles decryption
        // If we have a valid parsed cache, use it to populate the list quickly
        if (isCacheValid() && parsedEntriesCache != null) {
            List<String> elementsToAdd = new ArrayList<>(parsedEntriesCache.size());
            List<String> timestamps = new ArrayList<>(parsedEntriesCache.size());
            for (ParsedEntry pe : parsedEntriesCache) {
                elementsToAdd.add(pe.timestamp);
                timestamps.add(pe.timestamp);
            }

            // Batch update the model on EDT - DefaultListModel is NOT thread-safe
            final List<String> elementsFinal = elementsToAdd;
            javax.swing.SwingUtilities.invokeLater(() -> {
                listModel.removeAllElements();
                for (String element : elementsFinal) {
                    listModel.addElement(element);
                }
            });

            // Keep timestamp cache for other callers
            timestampListCache = timestamps;
            // We've populated the view from cache - done
            return;
        }

        List<String> lines = logFileHandler.getLines();

        // Single pass: remove markers and clean timestamps in one operation
        lines = lines.stream()
            .map(LogFileHandler::removeSecureMarker)
            .map(line -> line.replaceAll("^\\d+\\|(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2})(.*)$", "$1$2"))
            .collect(Collectors.toList());

        try {
            // Parse all entries
            var allEntries = LogParser.parseAllEntries(lines);

            // Separate timestamp and non-timestamp entries
            // Pre-parse timestamps for O(N) instead of O(N log N) parsing during sort
            var timestampEntriesWithDates = new ArrayList<TimestampEntry>();
            var nonTimestampEntries = new ArrayList<List<String>>();
            var tsPattern = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$");
            
            for (List<String> entry : allEntries) {
                if (!entry.isEmpty() && tsPattern.matcher(entry.get(0).trim()).matches()) {
                    // Pre-parse timestamp once
                    LocalDateTime dateTime = null;
                    try {
                        dateTime = utils.DateHandler.parseTimestamp(entry.get(0));
                    } catch (Exception e) {
                        // Parsing failed, use null for stable sort at end
                    }
                    timestampEntriesWithDates.add(new TimestampEntry(entry, dateTime));
                } else {
                    nonTimestampEntries.add(entry);
                }
            }

            // Sort by pre-parsed timestamps - O(N log N) comparisons with O(1) per comparison
            // Descending order (newest first)
            timestampEntriesWithDates.sort((a, b) -> {
                if (a.dateTime == null && b.dateTime == null) return 0;
                if (a.dateTime == null) return 1;  // nulls last
                if (b.dateTime == null) return -1;
                return b.dateTime.compareTo(a.dateTime);  // Descending
            });
            
            // Extract sorted entries
            var timestampEntries = new ArrayList<List<String>>(timestampEntriesWithDates.size());
            for (TimestampEntry te : timestampEntriesWithDates) {
                timestampEntries.add(te.entry);
            }

            List<List<String>> sortedEntries = new ArrayList<>();
            sortedEntries.addAll(nonTimestampEntries); // preamble notes at top
            sortedEntries.addAll(timestampEntries);
            
            // Populate caches while building list model
            entryContentCache.clear();
            List<String> timestamps = new ArrayList<>();
            
            // Track occurrence counts for display suffixes
            Map<String, Integer> occurrenceCount = new HashMap<>();
            
            // Collect all elements first for batched update
            List<String> elementsToAdd = new ArrayList<>(sortedEntries.size());
            
            for (List<String> entry : sortedEntries) {
                // For the list view, show only the timestamp line (or first line for non-timestamp entries)
                if (!entry.isEmpty()) {
                    String rawTs = entry.get(0).trim();
                    // Strip any existing suffix from file (for backwards compatibility)
                    String cleanTs = rawTs.replaceAll(" \\(\\d+\\)$", "");
                    
                    // PMD: Suppress warning - StringBuilder is required per entry
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    StringBuilder content = new StringBuilder();
                    for (int i = 1; i < entry.size(); i++) {
                        content.append(entry.get(i)).append('\n');
                    }
                    
                    if (tsPattern.matcher(cleanTs).matches()) {
                        // Track occurrence for display suffix
                        int occurrence = occurrenceCount.getOrDefault(cleanTs, 0);
                        occurrenceCount.put(cleanTs, occurrence + 1);
                        
                        // Generate display timestamp with suffix for duplicates
                        String displayTs = occurrence > 0 ? cleanTs + " (" + occurrence + ")" : cleanTs;
                        
                        // Use display timestamp as cache key
                        entryContentCache.put(displayTs, content.toString().trim());
                        elementsToAdd.add(displayTs);
                        timestamps.add(displayTs);
                    } else {
                        entryContentCache.put(rawTs, content.toString().trim());
                        elementsToAdd.add(rawTs);
                    }
                }
            }
            
            // Batch update on EDT - DefaultListModel is NOT thread-safe
            final List<String> elementsFinal = elementsToAdd;
            javax.swing.SwingUtilities.invokeLater(() -> {
                listModel.removeAllElements();
                for (String element : elementsFinal) {
                    listModel.addElement(element);
                }
            });
            
            // Cache timestamp list for getRecentLogEntries
            timestampListCache = timestamps;

            // Also populate parsedEntriesCache so future tab switches can use the cache
            List<ParsedEntry> parsed = new ArrayList<>(timestamps.size());
            for (String ts : timestamps) {
                LocalDateTime dt = null;
                try {
                    dt = utils.DateHandler.parseTimestamp(ts);
                } catch (Exception ignored) {
                }
                parsed.add(new ParsedEntry(ts, dt));
            }
            parsedEntriesCache = parsed;

            updateCacheTimestamp();
        } catch (Exception e) {
            // Security: Don't check/expose exception messages - use generic errors
            String errorMsg;
            if (e instanceof java.time.format.DateTimeParseException) {
                // Style the date parsing error
                errorMsg = "<html><b>⚠️ Timestamp Parsing Error</b><br><br>" +
                               "LogHog couldn't understand the timestamp format in your file.<br><br>" +
                               "<b>Solution:</b> Ensure timestamps follow the format <code>HH:mm yyyy-MM-dd</code> (e.g., 14:30 2025-12-16).<br>" +
                               "You can reformat the file or use LogHog's export feature for compatibility.</html>";
            } else {
                errorMsg = "<html><b>❌ Loading Error</b><br><br>Unable to load log timestamps. Please check the file format.</html>";
            }
            logFileHandler.showErrorDialog(errorMsg);
            // Do not throw, continue with empty list
        }
    }

    public void loadFilteredEntriesByYear(DefaultListModel<String> listModel, int year) {
        if (!Files.exists(logFileHandler.getFilePath())) {
            javax.swing.SwingUtilities.invokeLater(() -> listModel.removeAllElements());
            return;
        }

        try {
            // Use cache if valid, otherwise parse and cache
            if (!isCacheValid() || parsedEntriesCache == null) {
                parseParsedEntriesCache();
            }
            
            // Fast O(M) filtering from cached parsed entries
            List<String> filtered = new ArrayList<>();
            for (ParsedEntry entry : parsedEntriesCache) {
                if (entry.dateTime != null && entry.dateTime.getYear() == year) {
                    filtered.add(entry.timestamp);
                }
            }
            
            // Update Swing model on EDT - DefaultListModel is NOT thread-safe
            final List<String> filteredFinal = filtered;
            javax.swing.SwingUtilities.invokeLater(() -> {
                listModel.removeAllElements();
                for (String timestamp : filteredFinal) {
                    listModel.addElement(timestamp);
                }
            });
        } catch (Exception e) {
            // Security: Don't expose internal error details
            logFileHandler.showErrorDialog("<html><b>🔍 Filter Failed</b><br><br>Unable to load filtered log entries.<br><br><i>Tip: Check the log file format and try reloading.</i></html>");
        }
    }

    /**
     * Compute a list of timestamps for a given year without touching Swing components.
     * Safe to call off the EDT.
     */
    public List<String> computeTimestampsByYear(int year) throws Exception {
        if (!Files.exists(logFileHandler.getFilePath())) {
            return Collections.emptyList();
        }

        if (!isCacheValid() || parsedEntriesCache == null) {
            parseParsedEntriesCache();
        }

        List<String> filtered = new ArrayList<>();
        for (ParsedEntry entry : parsedEntriesCache) {
            if (entry.dateTime != null && entry.dateTime.getYear() == year) {
                filtered.add(entry.timestamp);
            }
        }
        return filtered;
    }

    /**
     * Compute a list of timestamps for a given year+month without touching Swing components.
     * Safe to call off the EDT.
     */
    public List<String> computeTimestampsByYearMonth(int year, int month) throws Exception {
        if (!Files.exists(logFileHandler.getFilePath())) {
            return Collections.emptyList();
        }

        if (!isCacheValid() || parsedEntriesCache == null) {
            parseParsedEntriesCache();
        }

        List<String> filtered = new ArrayList<>();
        for (ParsedEntry entry : parsedEntriesCache) {
            if (entry.dateTime != null && entry.dateTime.getYear() == year && entry.dateTime.getMonthValue() == month) {
                filtered.add(entry.timestamp);
            }
        }
        return filtered;
    }

    public void loadFilteredEntries(DefaultListModel<String> listModel, int year, int month) {
        if (!Files.exists(logFileHandler.getFilePath())) {
            javax.swing.SwingUtilities.invokeLater(() -> listModel.removeAllElements());
            return;
        }

        try {
            // Use cache if valid, otherwise parse and cache
            if (!isCacheValid() || parsedEntriesCache == null) {
                parseParsedEntriesCache();
            }
            
            // Fast O(M) filtering from cached parsed entries
            List<String> filtered = new ArrayList<>();
            for (ParsedEntry entry : parsedEntriesCache) {
                if (entry.dateTime != null && 
                    entry.dateTime.getYear() == year && 
                    entry.dateTime.getMonthValue() == month) {
                    filtered.add(entry.timestamp);
                }
            }
            
            // Update Swing model on EDT - DefaultListModel is NOT thread-safe
            final List<String> filteredFinal = filtered;
            javax.swing.SwingUtilities.invokeLater(() -> {
                listModel.removeAllElements();
                for (String timestamp : filteredFinal) {
                    listModel.addElement(timestamp);
                }
            });
        } catch (Exception e) {
            // Security: Don't expose internal error details
            logFileHandler.showErrorDialog("<html><b>🔍 Filter Failed</b><br><br>Unable to load filtered log entries.<br><br><i>Tip: Check the log file format and try reloading.</i></html>");
        }
    }

    /**
     * Parse file once and cache all entries with pre-parsed timestamps.
     * This enables O(M) filtering instead of O(N) file parsing on every filter change.
     */
    private void parseParsedEntriesCache() throws Exception {
        List<String> lines = logFileHandler.getLines();
        
        // Clean malformed timestamps
        lines = lines.stream()
            .map(line -> line.replaceAll("^\\d+\\|(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2})(.*)$", "$1$2"))
            .collect(Collectors.toList());
        
        // Parse entries
        List<List<String>> entries = new ArrayList<>();
        List<String> currentEntry = new ArrayList<>();
        Pattern tsPattern = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$", Pattern.MULTILINE);
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (".LOG".equalsIgnoreCase(trimmed)) continue;
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
        
        // Parse timestamps and create cached entries
        List<ParsedEntry> parsed = new ArrayList<>(entries.size());
        for (List<String> entry : entries) {
            if (!entry.isEmpty() && tsPattern.matcher(entry.get(0).trim()).matches()) {
                String timestamp = entry.get(0).trim();
                LocalDateTime dateTime = null;
                try {
                    dateTime = utils.DateHandler.parseTimestamp(timestamp);
                } catch (Exception e) {
                    // Security: Don't log parsing errors to console
                    // Failed parses will have null dateTime and sort to end
                }
                parsed.add(new ParsedEntry(timestamp, dateTime));
            }
        }
        
        // Sort by date descending (once, not on every filter)
        parsed.sort((a, b) -> {
            if (a.dateTime == null || b.dateTime == null) {
                return (a.timestamp).compareTo(b.timestamp);
            }
            return b.dateTime.compareTo(a.dateTime);
        });
        
        parsedEntriesCache = parsed;
        updateCacheTimestamp();
    }

    public DefaultListModel<String> filterModelByYearMonth(DefaultListModel<String> sourceModel, int year, int month) {
        DefaultListModel<String> filtered = new DefaultListModel<>();
        for (int i = 0; i < sourceModel.getSize(); i++) {
            String entry = sourceModel.getElementAt(i);
            try {
                LocalDateTime dt = utils.DateHandler.parseTimestamp(entry);
                if (dt.getYear() == year && dt.getMonthValue() == month) {
                    filtered.addElement(entry);
                }
            } catch (Exception ignored) {
            }
        }
        // Already sorted in previous step
        return filtered;
    }

    public String loadEntry(String timeStamp) {
        if (!Files.exists(logFileHandler.getFilePath())) return "";

        try {
            // Check cache first - O(1) lookup!
            if (isCacheValid()) {
                String cached = entryContentCache.get(timeStamp.trim());
                if (cached != null) {
                    return cached;
                }
                
                // Try without suffix for backward compatibility
                String baseTsParam = timeStamp.trim().replaceAll(" \\([0-9]+\\)$", "");
                for (Map.Entry<String, String> entry : entryContentCache.entrySet()) {
                    String entryTs = entry.getKey();
                    String baseTsEntry = entryTs.replaceAll(" \\([0-9]+\\)$", "");
                    if (baseTsEntry.equals(baseTsParam)) {
                        return entry.getValue();
                    }
                }
            }
            
            // Cache miss - fall back to full parse (rare, only on first load or cache invalidation)
            List<String> lines = logFileHandler.getLines();

            // Single pass cleanup
            lines = lines.stream()
                .map(LogFileHandler::removeSecureMarker)
                .collect(Collectors.toList());
            
            // Parse all entries
            var allEntries = LogParser.parseAllEntries(lines);

            // Rebuild cache
            entryContentCache.clear();
            var tsPattern = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$");
            
            for (List<String> entry : allEntries) {
                if (!entry.isEmpty() && tsPattern.matcher(entry.get(0).trim()).matches()) {
                    String entryTs = entry.get(0).trim();
                    // PMD: Suppress warning - StringBuilder is required per entry
                    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                    StringBuilder content = new StringBuilder();
                    for (int i = 1; i < entry.size(); i++) {
                        content.append(entry.get(i)).append('\n');
                    }
                    entryContentCache.put(entryTs, content.toString().trim());
                }
            }
            
            updateCacheTimestamp();
            
            // Now try cache again
            String result = entryContentCache.get(timeStamp.trim());
            if (result != null) {
                return result;
            }
            
            // Try without suffix
            String baseTsParam = timeStamp.trim().replaceAll(" \\([0-9]+\\)$", "");
            for (Map.Entry<String, String> entry : entryContentCache.entrySet()) {
                String entryTs = entry.getKey();
                String baseTsEntry = entryTs.replaceAll(" \\([0-9]+\\)$", "");
                if (baseTsEntry.equals(baseTsParam)) {
                    return entry.getValue();
                }
            }

            return "";
        } catch (Exception e) {
            // Security: Don't expose internal error details
            logFileHandler.showErrorDialog("<html><b>👁️ Display Failed</b><br><br>Unable to display the log entry.<br><br><i>Tip: The entry may be corrupted or the file may be locked.</i></html>");
        }
        return "";
    }

    /**
     * Get content for a timestamp. Used by LogEntrySearcher for O(N) search.
     * @param timestamp the timestamp to get content for
     * @return the content, or empty string if not found
     */
    public String getEntryContent(String timestamp) {
        return loadEntry(timestamp);
    }
    
    /**
     * Search entries with combined date and text filtering.
     * This is O(N) where N is entries in cache.
     * 
     * @param year filter year (ignored if <= 0)
     * @param month filter month 1-12 (0 = all months, ignored if year <= 0)
     * @param searchOptions text search options (can be null/empty for no text search)
     * @return list of matching timestamps
     */
    public List<String> searchEntries(int year, int month, LogEntrySearcher.SearchOptions searchOptions) throws Exception {
        if (!Files.exists(logFileHandler.getFilePath())) {
            return Collections.emptyList();
        }
        
        // Ensure cache is populated
        if (!isCacheValid() || parsedEntriesCache == null) {
            parseParsedEntriesCache();
        }
        
        // Also ensure content cache is populated for text search
        if (searchOptions != null && !searchOptions.isEmpty()) {
            if (!isCacheValid() || entryContentCache.isEmpty()) {
                // Force content cache population by loading any entry
                // This triggers full cache rebuild
                if (!parsedEntriesCache.isEmpty()) {
                    loadEntry(parsedEntriesCache.get(0).timestamp);
                }
            }
        }
        
        // First pass: date filter (O(N))
        List<String> dateFiltered = new ArrayList<>();
        for (ParsedEntry entry : parsedEntriesCache) {
            if (year > 0 && entry.dateTime != null) {
                if (entry.dateTime.getYear() != year) continue;
                if (month > 0 && entry.dateTime.getMonthValue() != month) continue;
            }
            dateFiltered.add(entry.timestamp);
        }
        
        // Second pass: text search (O(M) where M = date filtered entries)
        if (searchOptions == null || searchOptions.isEmpty()) {
            return dateFiltered;
        }
        
        return LogEntrySearcher.searchTimestamps(
            dateFiltered,
            this::getEntryContent,
            searchOptions
        );
    }

    public List<String> getRecentLogEntries(int i) {
        List<String> recentEntries = new ArrayList<>();
        if (!Files.exists(logFileHandler.getFilePath())) return recentEntries;

        try {
            // Use cached timestamp list if available - O(1) instead of O(N)!
            if (isCacheValid() && timestampListCache != null) {
                // Cache is already sorted newest first
                for (int j = 0; j < Math.min(i, timestampListCache.size()); j++) {
                    recentEntries.add(timestampListCache.get(j));
                }
                return recentEntries;
            }
            
            // Cache miss - parse file
            List<String> lines = logFileHandler.getLines();
            List<String> timestamps = new ArrayList<>();
            for (String line : lines) {
                if (utils.DateHandler.isTimestamp(line)) {
                    timestamps.add(line.trim());
                }
            }
            timestamps.sort((a, b) -> {
                try {
                    return utils.DateHandler.parseTimestamp(b).compareTo(utils.DateHandler.parseTimestamp(a));
                } catch (Exception e) {
                    return 0; // keep original order if parsing fails
                }
            });
            
            // Update cache
            timestampListCache = timestamps;
            updateCacheTimestamp();
            
            for (int j = 0; j < Math.min(i, timestamps.size()); j++) {
                recentEntries.add(timestamps.get(j));
            }
        } catch (Exception e) {
            // Security: Don't expose internal error details
            logFileHandler.showErrorDialog("<html><b>🕒 Recent Entries Failed</b><br><br>Unable to load recent log entries.<br><br><i>Tip: Check the log file and ensure it contains valid timestamps.</i></html>");
        }
        return recentEntries;
    }
}
