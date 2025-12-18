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
import java.io.BufferedReader;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import encryption.EncryptionManager;
import encryption.EncryptionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntryLoader {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);
    private final LogFileHandler logFileHandler;

    public EntryLoader(LogFileHandler logFileHandler) {
        this.logFileHandler = logFileHandler;
    }

    public void loadLogEntries(DefaultListModel<String> listModel) throws Exception {
        listModel.clear();
        if (!Files.exists(LogFileHandler.FILE_PATH)) return;

        List<String> lines;
        if (logFileHandler.isEncrypted()) {
            try {
                var data = Files.readAllBytes(LogFileHandler.FILE_PATH);
                var decrypted = EncryptionManager.getInstance().decryptWithFallback(data, logFileHandler.getPassword(), logFileHandler.getSalt());
                lines = Arrays.asList(decrypted.split("\n", -1));
            } catch (EncryptionException e) {
                throw new RuntimeException(e);
            }
        } else {
            lines = Files.readAllLines(LogFileHandler.FILE_PATH);
        }

        // Remove secure clipboard markers from lines
        lines = lines.stream().map(LogFileHandler::removeSecureMarker).collect(Collectors.toList());

        // Strip timestamp suffixes that may be in the file
        lines = lines.stream().map(line -> line.replaceAll("^(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}) \\([0-9]+\\)(.*)$", "$1$2")).collect(Collectors.toList());

        // Clean malformed timestamps with Unix timestamp prefixes
        lines = lines.stream().map(line -> line.replaceAll("^\\d+\\|(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2})(.*)$", "$1$2")).collect(Collectors.toList());

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
            Map<String, Integer> countMap = new HashMap<>();
            for (List<String> entry : sortedEntries) {
                // For the list view, show only the timestamp line (or first line for non-timestamp entries)
                if (!entry.isEmpty()) {
                    String rawTs = entry.get(0).trim();
                    if (tsPattern.matcher(rawTs).matches()) {
                        // Generate display timestamp without suffixes
                        String baseTs = rawTs.replaceAll(" \\([0-9]+\\)$", "");
                        listModel.addElement(baseTs);
                    } else {
                        listModel.addElement(rawTs);
                    }
                }
            }
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
        if (!Files.exists(LogFileHandler.FILE_PATH)) return;

        try {
            List<String> lines;
            if (logFileHandler.isEncrypted()) {
                byte[] data = Files.readAllBytes(LogFileHandler.FILE_PATH);
                String decrypted = EncryptionManager.getInstance().decryptWithFallback(data, logFileHandler.getPassword(), logFileHandler.getSalt());
                lines = Arrays.asList(decrypted.split("\n", -1));
            } else {
                lines = Files.readAllLines(LogFileHandler.FILE_PATH);
            }
            // Strip timestamp suffixes that may be in the file
            lines = lines.stream().map(line -> line.replaceAll("^(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}) \\([0-9]+\\)(.*)$", "$1$2")).collect(Collectors.toList());
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
            
            // Generate display timestamps without suffixes
            Map<String, Integer> countMap = new HashMap<>();
            for (List<String> entry : filteredEntries) {
                if (!entry.isEmpty()) {
                    String rawTs = entry.get(0).trim();
                    if (tsPattern.matcher(rawTs).matches()) {
                        // Generate display timestamp without suffixes
                        String baseTs = rawTs.replaceAll(" \\([0-9]+\\)$", "");
                        listModel.addElement(baseTs);
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
        if (!Files.exists(LogFileHandler.FILE_PATH)) return "";

        try {
            List<String> lines;
            if (logFileHandler.isEncrypted()) {
                byte[] data = Files.readAllBytes(LogFileHandler.FILE_PATH);
                String decrypted = EncryptionManager.getInstance().decryptWithFallback(data, logFileHandler.getPassword(), logFileHandler.getSalt());
                lines = Arrays.asList(decrypted.split("\n", -1));
            } else {
                lines = Files.readAllLines(LogFileHandler.FILE_PATH);
            }

            // Remove secure clipboard markers from lines
            lines = lines.stream().map(LogFileHandler::removeSecureMarker).collect(Collectors.toList());

            // Strip timestamp suffixes that may be in the file
            lines = lines.stream().map(line -> line.replaceAll("^(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}) \\([0-9]+\\)(.*)$", "$1$2")).collect(Collectors.toList());

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

            // Find the first entry with matching timestamp
            for (List<String> entry : timestampEntries) {
                String entryTs = entry.get(0).trim().replaceAll(" \\([0-9]+\\)$", "");
                if (entryTs.equals(timeStamp)) {
                    // Found the entry - return content without timestamp
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < entry.size(); i++) {
                        sb.append(entry.get(i)).append("\n");
                    }
                    return sb.toString().trim();
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
        if (!Files.exists(LogFileHandler.FILE_PATH)) return recentEntries;

        try {
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
            for (int j = 0; j < Math.min(i, timestamps.size()); j++) {
                recentEntries.add(timestamps.get(j));
            }
        } catch (Exception e) {
            logFileHandler.showErrorDialog("<html><b>🕒 Recent Entries Failed</b><br><br>Unable to load recent log entries.<br>" + e.getMessage() + "<br><br><i>Tip: Check the log file and ensure it contains valid timestamps.</i></html>");
        }
        return recentEntries;
    }
}
