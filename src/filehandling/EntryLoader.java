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
import javax.crypto.SecretKey;
import encryption.EncryptionManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntryLoader {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.getDefault());
    private final LogFileHandler logFileHandler;

    public EntryLoader(LogFileHandler logFileHandler) {
        this.logFileHandler = logFileHandler;
    }

    public void loadLogEntries(DefaultListModel<String> listModel) throws Exception {
        listModel.clear();
        if (!Files.exists(LogFileHandler.FILE_PATH)) return;

        List<String> lines;
        if (logFileHandler.isEncrypted()) {
            byte[] data = Files.readAllBytes(LogFileHandler.FILE_PATH);
            SecretKey key = EncryptionManager.deriveKey(logFileHandler.getPassword(), logFileHandler.getSalt());
            String decrypted = EncryptionManager.decrypt(data, key);
            lines = Arrays.asList(decrypted.split("\n", -1));
        } else {
            lines = Files.readAllLines(LogFileHandler.FILE_PATH);
        }

        try {
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
            // Sort only entries that start with a timestamp
            List<List<String>> timestampEntries = new ArrayList<>();
            List<List<String>> nonTimestampEntries = new ArrayList<>();
            for (List<String> entry : entries) {
                if (!entry.isEmpty() && tsPattern.matcher(entry.get(0).trim()).matches()) {
                    timestampEntries.add(entry);
                } else {
                    nonTimestampEntries.add(entry);
                }
            }
            // Filter to current month
            LocalDateTime now = LocalDateTime.now();
            int currentYear = now.getYear();
            int currentMonth = now.getMonthValue();
            List<List<String>> filteredTimestampEntries = new ArrayList<>();
            for (List<String> entry : timestampEntries) {
                try {
                    LocalDateTime dt = parseDate(entry.get(0));
                    if (dt.getYear() == currentYear && dt.getMonthValue() == currentMonth) {
                        filteredTimestampEntries.add(entry);
                    }
                } catch (Exception ignored) {}
            }
            filteredTimestampEntries.sort((a, b) -> {
                try {
                    LocalDateTime dateA = parseDate(a.get(0));
                    LocalDateTime dateB = parseDate(b.get(0));
                    return dateB.compareTo(dateA);
                } catch (Exception e) {
                    return b.get(0).compareTo(a.get(0));
                }
            });
            List<List<String>> sortedEntries = new ArrayList<>();
            sortedEntries.addAll(nonTimestampEntries); // preamble notes at top
            sortedEntries.addAll(filteredTimestampEntries);
            for (List<String> entry : sortedEntries) {
                // For the list view, show only the timestamp line (or first line for non-timestamp entries)
                if (!entry.isEmpty()) {
                    listModel.addElement(entry.get(0).trim());
                }
            }
        } catch (Exception e) {
            if (!e.getMessage().contains("Tag mismatch")) {
                logFileHandler.showErrorDialog("Error loading log entries: " + e.getMessage());
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
                SecretKey key = EncryptionManager.deriveKey(logFileHandler.getPassword(), logFileHandler.getSalt());
                String decrypted = EncryptionManager.decrypt(data, key);
                lines = Arrays.asList(decrypted.split("\n", -1));
            } else {
                lines = Files.readAllLines(LogFileHandler.FILE_PATH);
            }
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
                        LocalDateTime dt = parseDate(entry.get(0).trim());
                        if (dt.getYear() == year && dt.getMonthValue() == month) {
                            filteredEntries.add(entry);
                        }
                    } catch (Exception ignored) {}
                }
            }
            filteredEntries.sort((a, b) -> {
                try {
                    LocalDateTime dateA = parseDate(a.get(0));
                    LocalDateTime dateB = parseDate(b.get(0));
                    return dateB.compareTo(dateA);
                } catch (Exception e) {
                    return b.get(0).compareTo(a.get(0));
                }
            });
            for (List<String> entry : filteredEntries) {
                if (!entry.isEmpty()) {
                    listModel.addElement(entry.get(0).trim());
                }
            }
        } catch (Exception e) {
            logFileHandler.showErrorDialog("Error loading filtered log entries: " + e.getMessage());
        }
    }

    public DefaultListModel<String> filterModelByYearMonth(DefaultListModel<String> sourceModel, int year, int month) {
        DefaultListModel<String> filtered = new DefaultListModel<>();
        for (int i = 0; i < sourceModel.getSize(); i++) {
            String entry = sourceModel.getElementAt(i);
            try {
                LocalDateTime dt = parseDate(entry);
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
                SecretKey key = EncryptionManager.deriveKey(logFileHandler.getPassword(), logFileHandler.getSalt());
                String decrypted = EncryptionManager.decrypt(data, key);
                lines = Arrays.asList(decrypted.split("\n", -1));
            } else {
                lines = Files.readAllLines(LogFileHandler.FILE_PATH);
            }
            StringBuilder entry = new StringBuilder();
            boolean found = false;

            for (String line : lines) {
                if (!found && line.trim().equals(timeStamp.trim())) {
                    found = true;
                    continue;
                }

                if (found) {
                    // stop at next timestamp (accounts for entries with or without blank lines)
                    if (line.trim().matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}.*")) {
                        break;
                    }
                    entry.append(line).append("\n");
                }
            }

            return entry.toString().trim();
        } catch (Exception e) {
            logFileHandler.showErrorDialog("Error displaying log entry: " + e.getMessage());
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
                    return parseDate(b).compareTo(parseDate(a));
                } catch (Exception e) {
                    return 0; // keep original order if parsing fails
                }
            });
            for (int j = 0; j < Math.min(i, timestamps.size()); j++) {
                recentEntries.add(timestamps.get(j));
            }
        } catch (Exception e) {
            logFileHandler.showErrorDialog("Error loading recent log entries: " + e.getMessage());
        }
        return recentEntries;
    }

    public LocalDateTime parseDate(String entry) {
        // Extract the timestamp from the beginning of the line
        Pattern tsPattern = Pattern.compile("^(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2})( \\([0-9]+\\))?");
        Matcher matcher = tsPattern.matcher(entry.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("No timestamp found in: " + entry);
        }
        String dateStr = matcher.group(1);
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.getDefault()),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault()),
                DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm", Locale.getDefault()),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.getDefault()),
                DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.ENGLISH)
        );
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(dateStr, fmt);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("Unrecognized date format: " + dateStr);
    }
}
