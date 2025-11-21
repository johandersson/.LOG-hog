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

public class EntryLoader {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.getDefault());
    private final LogFileHandler logFileHandler;

    public EntryLoader(LogFileHandler logFileHandler) {
        this.logFileHandler = logFileHandler;
    }

    public void loadLogEntries(DefaultListModel<String> listModel) throws Exception {
        listModel.clear();
        if (!Files.exists(LogFileHandler.FILE_PATH)) return;

        try {
            if (logFileHandler.isEncrypted()) {
                byte[] data = Files.readAllBytes(LogFileHandler.FILE_PATH);
                SecretKey key = EncryptionManager.deriveKey(logFileHandler.getPassword(), logFileHandler.getSalt());
                String decrypted = EncryptionManager.decrypt(data, key);
                logFileHandler.cachedLines = new ArrayList<>(Arrays.asList(decrypted.split("\n")));
                List<String> timestamps = new ArrayList<>();
                for (String line : logFileHandler.cachedLines) {
                    if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                        timestamps.add(line.trim());
                    }
                }
                timestamps.sort(Comparator.comparing(this::parseDate).reversed());
                timestamps.forEach(listModel::addElement);
            } else {
                List<String> timestamps = new ArrayList<>();
                try (BufferedReader reader = Files.newBufferedReader(LogFileHandler.FILE_PATH)) {
                    String line;
                    boolean first = true;
                    while ((line = reader.readLine()) != null) {
                        if (first) {
                            first = false;
                            if (line.trim().equals(".LOG")) {
                                continue;
                            }
                        }
                        if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                            timestamps.add(line.trim());
                        }
                    }
                }
                timestamps.sort(Comparator.comparing(this::parseDate).reversed());
                timestamps.forEach(listModel::addElement);
            }
        } catch (Exception e) {
            if (!e.getMessage().contains("Tag mismatch")) {
                logFileHandler.showErrorDialog("Error loading log entries: " + e.getMessage());
            }
            throw e;
        }
    }

    public void loadFilteredEntries(DefaultListModel<String> listModel, int year, int month) {
        listModel.clear();
        if (!Files.exists(LogFileHandler.FILE_PATH)) return;

        try {
            List<String> timestamps = new ArrayList<>();
            if (logFileHandler.isEncrypted()) {
                List<String> lines = logFileHandler.getLines();
                for (String line : lines) {
                    if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                        String trimmed = line.trim();
                        try {
                            LocalDateTime dt = parseDate(trimmed);
                            if (dt.getYear() == year && dt.getMonthValue() == month) {
                                timestamps.add(trimmed);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } else {
                try (BufferedReader reader = Files.newBufferedReader(LogFileHandler.FILE_PATH)) {
                    String line;
                    boolean first = true;
                    while ((line = reader.readLine()) != null) {
                        if (first) {
                            first = false;
                            if (line.trim().equals(".LOG")) {
                                continue;
                            }
                        }
                        if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                            String trimmed = line.trim();
                            try {
                                LocalDateTime dt = parseDate(trimmed);
                                if (dt.getYear() == year && dt.getMonthValue() == month) {
                                    timestamps.add(trimmed);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
            timestamps.sort(Comparator.comparing(this::parseDate).reversed());
            timestamps.forEach(listModel::addElement);
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
        List<String> sorted = Collections.list(filtered.elements()).stream()
                .sorted((a, b) -> parseDate(b).compareTo(parseDate(a)))
                .toList();
        filtered.clear();
        sorted.forEach(filtered::addElement);
        return filtered;
    }

    public String loadEntry(String timeStamp) {
        if (!Files.exists(LogFileHandler.FILE_PATH)) return "";

        try {
            List<String> lines = logFileHandler.getLines();
            StringBuilder entry = new StringBuilder();
            boolean found = false;

            for (String line : lines) {
                if (!found && line.trim().equals(timeStamp.trim())) {
                    found = true;
                    continue;
                }

                if (found) {
                    // stop at next timestamp (accounts for entries with or without blank lines)
                    if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
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
            List<String> timestamps = new ArrayList<>();
            if (logFileHandler.isEncrypted()) {
                List<String> lines = logFileHandler.getLines();
                for (String line : lines) {
                    if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                        timestamps.add(line.trim());
                    }
                }
            } else {
                try (BufferedReader reader = Files.newBufferedReader(LogFileHandler.FILE_PATH)) {
                    String line;
                    boolean first = true;
                    while ((line = reader.readLine()) != null) {
                        if (first) {
                            first = false;
                            if (line.trim().equals(".LOG")) {
                                continue;
                            }
                        }
                        if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                            timestamps.add(line.trim());
                        }
                    }
                }
            }
            timestamps.sort(Comparator.comparing(this::parseDate).reversed());
            for (int j = 0; j < Math.min(i, timestamps.size()); j++) {
                recentEntries.add(timestamps.get(j));
            }
        } catch (Exception e) {
            logFileHandler.showErrorDialog("Error loading recent log entries: " + e.getMessage());
        }
        return recentEntries;
    }

    public LocalDateTime parseDate(String entry) {
        String dateStr = entry.split("\n")[0].replaceAll(" \\(\\d+\\)", "");
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