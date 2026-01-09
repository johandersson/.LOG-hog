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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import encryption.EncryptionManager;
import encryption.EncryptionException;
import java.util.regex.Pattern;

import encryption.Encryptor;

public class EntryLoader {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);
    private final LogFileHandler logFileHandler;
    private final Encryptor encryptor;
    
    // Performance caches - invalidated when file changes
    private final Map<String, String> entryContentCache = new HashMap<>();
    private List<String> timestampListCache = null;
    private final Map<String, Integer> duplicateCountCache = new HashMap<>();
    private long cacheLastModified = 0;

    public EntryLoader(LogFileHandler logFileHandler) {
        this(logFileHandler, EncryptionManager.getInstance());
    }

    public EntryLoader(LogFileHandler logFileHandler, Encryptor encryptor) {
        this.logFileHandler = logFileHandler;
        this.encryptor = encryptor;
    }
    
    /**
     * Invalidates all caches. Called when file is modified.
     * Public method for LogFileHandler to call.
     */
    public void invalidateCaches() {
        entryContentCache.clear();
        timestampListCache = null;
        duplicateCountCache.clear();
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
        listModel.clear();
        
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

        List<String> lines;
        if (logFileHandler.isEncrypted()) {
            try {
                var data = Files.readAllBytes(logFileHandler.getFilePath());
                var decrypted = encryptor.decryptWithFallback(data, logFileHandler.getPassword(), logFileHandler.getSalt());
                lines = Arrays.asList(decrypted.split("\r?\n", -1));
            } catch (EncryptionException e) {
                throw new RuntimeException(e);
            }
        } else {
            lines = Files.readAllLines(logFileHandler.getFilePath());
        }

        // Single pass: remove markers and clean timestamps in one operation
        lines = lines.stream()
            .map(LogFileHandler::removeSecureMarker)
            .map(line -> line.replaceAll("^\\d+\\|(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2})(.*)$", "$1$2"))
            .collect(Collectors.toList());

        try {
            // Parse all entries
            var allEntries = LogParser.parseAllEntries(lines);

            // Separate timestamp and non-timestamp entries
            var timestampEntries = new ArrayList<List<String>>();
            var nonTimestampEntries = new ArrayList<List<String>>();
            var tsPattern = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$");
            for (List<String> entry : allEntries) {
                if (!entry.isEmpty() && tsPattern.matcher(entry.get(0).trim()).matches()) {
                    timestampEntries.add(entry);
                } else {
                    nonTimestampEntries.add(entry);
                }
            }

            // Sort timestamp entries by date descending
            timestampEntries.sort((a, b) -> {
                try {
                    LocalDateTime dateA = utils.DateHandler.parseTimestamp(a.get(0));
                    LocalDateTime dateB = utils.DateHandler.parseTimestamp(b.get(0));
                    return dateB.compareTo(dateA);
                } catch (Exception e) {
                    return b.get(0).compareTo(a.get(0));
                }
            });

            List<List<String>> sortedEntries = new ArrayList<>();
            sortedEntries.addAll(nonTimestampEntries); // preamble notes at top
            sortedEntries.addAll(timestampEntries);
            
            // Populate caches while building list model
            entryContentCache.clear();
            List<String> timestamps = new ArrayList<>();
            
            for (List<String> entry : sortedEntries) {
                // For the list view, show only the timestamp line (or first line for non-timestamp entries)
                if (!entry.isEmpty()) {
                    String rawTs = entry.get(0).trim();
                    
                    // Cache entry content for fast retrieval
                    StringBuilder content = new StringBuilder();
                    for (int i = 1; i < entry.size(); i++) {
                        content.append(entry.get(i)).append("\n");
                    }
                    entryContentCache.put(rawTs, content.toString().trim());
                    
                    if (tsPattern.matcher(rawTs).matches()) {
                        // Keep the suffix to distinguish duplicate entries
                        listModel.addElement(rawTs);
                        timestamps.add(rawTs);
                    } else {
                        listModel.addElement(rawTs);
                    }
                }
            }
            
            // Cache timestamp list for getRecentLogEntries
            timestampListCache = timestamps;
            updateCacheTimestamp();
        } catch (Exception e) {
            if (!e.getMessage().contains("Tag mismatch")) {
                String errorMsg = e.getMessage();
                if (errorMsg.contains("Unrecognized date format")) {
                    // Style the date parsing error
                    errorMsg = "<html><b>⚠️ Timestamp Parsing Error</b><br><br>" +
                               "LogHog couldn't understand the timestamp format in your file.<br><br>" +
                               "<b>Details:</b> " + errorMsg + "<br><br>" +
                               "<b>Solution:</b> Ensure timestamps follow the format <code>HH:mm yyyy-MM-dd</code> (e.g., 14:30 2025-12-16).<br>" +
                               "You can reformat the file or use LogHog's export feature for compatibility.</html>";
                } else {
                    errorMsg = "<html><b>❌ Loading Error</b><br><br>" + errorMsg + "</html>";
                }
                logFileHandler.showErrorDialog(errorMsg);
            }
            // Do not throw, continue with empty list
        }
    }

    public void loadFilteredEntries(DefaultListModel<String> listModel, int year, int month) {
        listModel.clear();
        if (!Files.exists(logFileHandler.getFilePath())) return;

        try {
            List<String> lines;
            if (logFileHandler.isEncrypted()) {
                byte[] data = Files.readAllBytes(logFileHandler.getFilePath());
                String decrypted = encryptor.decryptWithFallback(data, logFileHandler.getPassword(), logFileHandler.getSalt());
                lines = Arrays.asList(decrypted.split("\r?\n", -1));
            } else {
                lines = Files.readAllLines(logFileHandler.getFilePath());
            }
            // Don't strip timestamp suffixes - we need them to distinguish duplicate entries
            
            // Clean malformed timestamps with Unix timestamp prefixes
            lines = lines.stream().map(line -> line.replaceAll("^\\d+\\|(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2})(.*)$", "$1$2")).collect(Collectors.toList());
            List<List<String>> entries = new ArrayList<>();
            List<String> currentEntry = new ArrayList<>();
            Pattern tsPattern = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$", Pattern.MULTILINE);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.equalsIgnoreCase(".LOG")) continue;
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
            List<List<String>> filteredEntries = new ArrayList<>();
            for (List<String> entry : entries) {
                if (!entry.isEmpty() && tsPattern.matcher(entry.get(0).trim()).matches()) {
                    try {
                        LocalDateTime dt = utils.DateHandler.parseTimestamp(entry.get(0).trim());
                        if (dt.getYear() == year && dt.getMonthValue() == month) {
                            filteredEntries.add(entry);
                        }
                    } catch (Exception ignored) {}
                }
            }
            filteredEntries.sort((a, b) -> {
                try {
                    LocalDateTime dateA = utils.DateHandler.parseTimestamp(a.get(0));
                    LocalDateTime dateB = utils.DateHandler.parseTimestamp(b.get(0));
                    return dateB.compareTo(dateA);
                } catch (Exception e) {
                    return b.get(0).compareTo(a.get(0));
                }
            });
            
            // Keep suffixes to distinguish duplicate entries
            for (List<String> entry : filteredEntries) {
                if (!entry.isEmpty()) {
                    String rawTs = entry.get(0).trim();
                    if (tsPattern.matcher(rawTs).matches()) {
                        // Keep the suffix to distinguish duplicate entries
                        listModel.addElement(rawTs);
                    } else {
                        listModel.addElement(rawTs);
                    }
                }
            }
        } catch (Exception e) {
            logFileHandler.showErrorDialog("<html><b>🔍 Filter Failed</b><br><br>Unable to load filtered log entries.<br>" + e.getMessage() + "<br><br><i>Tip: Check the log file format and try reloading.</i></html>");
        }
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
                    if (baseTsEntry.equals(baseTsParam) && !timeStamp.contains("(")) {
                        return entry.getValue();
                    }
                }
            }
            
            // Cache miss - fall back to full parse (rare, only on first load or cache invalidation)
            List<String> lines;
            if (logFileHandler.isEncrypted()) {
                byte[] data = Files.readAllBytes(logFileHandler.getFilePath());
                String decrypted = EncryptionManager.getInstance().decryptWithFallback(data, logFileHandler.getPassword(), logFileHandler.getSalt());
                lines = Arrays.asList(decrypted.split("\r?\n", -1));
            } else {
                lines = Files.readAllLines(logFileHandler.getFilePath());
            }

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
                    StringBuilder content = new StringBuilder();
                    for (int i = 1; i < entry.size(); i++) {
                        content.append(entry.get(i)).append("\n");
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
                if (baseTsEntry.equals(baseTsParam) && !timeStamp.contains("(")) {
                    return entry.getValue();
                }
            }

            return "";
        } catch (Exception e) {
            logFileHandler.showErrorDialog("<html><b>👁️ Display Failed</b><br><br>Unable to display the log entry.<br>" + e.getMessage() + "<br><br><i>Tip: The entry may be corrupted or the file may be locked.</i></html>");
        }
        return "";
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
                if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?")) {
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
            logFileHandler.showErrorDialog("<html><b>🕒 Recent Entries Failed</b><br><br>Unable to load recent log entries.<br>" + e.getMessage() + "<br><br><i>Tip: Check the log file and ensure it contains valid timestamps.</i></html>");
        }
        return recentEntries;
    }
}
