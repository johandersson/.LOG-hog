import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LogFileHandler {
    private static final Path FILE_PATH = Path.of(System.getProperty("user.home"), "log.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");

    void saveText(String text, DefaultListModel<String> listModel) {
        if (text.isBlank()) return;

        String timeStamp = FORMATTER.format(LocalDateTime.now());
        int count = getDuplicateCount(timeStamp);
        String uniqueTimeStamp = count > 0 ? timeStamp + " (" + count + ")" : timeStamp;

        // Entry ends with one blank line for nicer formatting
        String entry = uniqueTimeStamp + "\n" + text + "\n\n";

        try {
            if (Files.exists(FILE_PATH)) {
                // Inspect last line to avoid creating multiple blank lines between entries.
                List<String> existing = Files.readAllLines(FILE_PATH);
                boolean lastLineIsBlank = !existing.isEmpty() && existing.get(existing.size() - 1).trim().isEmpty();
                String toWrite = lastLineIsBlank ? entry : System.lineSeparator() + entry;
                Files.writeString(FILE_PATH, toWrite, java.nio.file.StandardOpenOption.APPEND);
            } else {
                Files.writeString(FILE_PATH, entry, java.nio.file.StandardOpenOption.CREATE);
            }

            listModel.addElement(uniqueTimeStamp);
            sortListModel(listModel);
            System.out.println("Saved log entry: \n" + entry);
        } catch (IOException e) {
            showErrorDialog("Error saving text: " + e.getMessage());
        }
    }

    // delete certain log entry
    private void deleteLogEntry(String timeStamp, DefaultListModel<String> listModel) {
        if (!Files.exists(FILE_PATH)) return;

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            List<String> updatedLines = new ArrayList<>();
            boolean skipping = false;

            for (String line : lines) {
                // timestamp lines are exact matches (whitespace trimmed)
                if (!skipping && line.trim().equals(timeStamp.trim())) {
                    skipping = true; // start skipping this timestamp and its body
                    continue;
                }

                if (skipping) {
                    // stop skipping when we hit the next timestamp line
                    if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                        skipping = false;
                        // This line is the next timestamp; it should be kept
                        updatedLines.add(line);
                    } else {
                        // while skipping, simply continue (this drops blank lines and body lines)
                        continue;
                    }
                } else {
                    updatedLines.add(line);
                }
            }

            // Normalize spacing: ensure at most one blank line between entries
            List<String> normalized = new ArrayList<>();
            boolean prevBlank = false;
            for (String l : updatedLines) {
                boolean isBlank = l.trim().isEmpty();
                if (isBlank) {
                    if (!prevBlank) {
                        normalized.add(""); // keep single blank line
                        prevBlank = true;
                    } // else skip additional blank lines
                } else {
                    normalized.add(l);
                    prevBlank = false;
                }
            }

            Files.write(FILE_PATH, normalized);
            listModel.removeElement(timeStamp);
            System.out.println("Deleted log entry: " + timeStamp);
        } catch (IOException e) {
            showErrorDialog("Error deleting log entry: " + e.getMessage());
        }
    }

    private int getDuplicateCount(String timeStamp) {
        if (!Files.exists(FILE_PATH)) return 0;

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            return (int) lines.stream()
                    .filter(line -> line.startsWith(timeStamp))
                    .count();
        } catch (IOException e) {
            showErrorDialog("Error checking duplicates: " + e.getMessage());
            return 0;
        }
    }

    private void sortListModel(DefaultListModel<String> listModel) {
        List<String> sortedEntries = Collections.list(listModel.elements()).stream()
                .sorted((a, b) -> parseDate(b).compareTo(parseDate(a)))
                .toList();

        listModel.clear();
        sortedEntries.forEach(listModel::addElement);
        System.out.println("Sorted log entries: " + sortedEntries);
    }

    void loadLogEntries(DefaultListModel<String> listModel) {
        listModel.clear();
        if (!Files.exists(FILE_PATH)) return;

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            if (!lines.isEmpty() && lines.get(0).trim().equals(".LOG")) {
                lines.remove(0);
            }

            List<String> timestamps = new ArrayList<>();

            for (String line : lines) {
                if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                    timestamps.add(line.trim());
                }
            }

            timestamps.sort(Comparator.comparing(this::parseDate).reversed());
            timestamps.forEach(listModel::addElement);

            System.out.println("Consistently sorted timestamps (newest first): \n" + timestamps);
        } catch (IOException e) {
            showErrorDialog("Error loading log entries: " + e.getMessage());
        }
    }

    // load only entries matching year and month (1..12)
    public void loadFilteredEntries(DefaultListModel<String> listModel, int year, int month) {
        listModel.clear();
        if (!Files.exists(FILE_PATH)) return;

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            if (!lines.isEmpty() && lines.get(0).trim().equals(".LOG")) {
                lines.remove(0);
            }

            List<String> timestamps = new ArrayList<>();
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

            timestamps.sort(Comparator.comparing(this::parseDate).reversed());
            timestamps.forEach(listModel::addElement);
            System.out.println("Loaded filtered timestamps for " + year + "-" + String.format("%02d", month) + ": " + timestamps);
        } catch (IOException e) {
            showErrorDialog("Error loading filtered log entries: " + e.getMessage());
        }
    }

    // produce a filtered DefaultListModel from an existing model
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

    private LocalDateTime parseDate(String entry) {
        return LocalDateTime.parse(entry.split("\n")[0].replaceAll(" \\(\\d+\\)", ""), FORMATTER);
    }

    String loadEntry(String timeStamp) {
        if (!Files.exists(FILE_PATH)) return "";

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            StringBuilder entry = new StringBuilder();
            boolean found = false;

            System.out.println("Searching for entry: " + timeStamp.trim());

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

            System.out.println("Final entry loaded:\n" + entry.toString().trim());
            return entry.toString().trim();
        } catch (IOException e) {
            showErrorDialog("Error displaying log entry: " + e.getMessage());
        }
        return "";
    }

    void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void deleteEntry(String selectedItem, DefaultListModel<String> listModel) {
        if (selectedItem != null && !selectedItem.isBlank()) {
            deleteLogEntry(selectedItem, listModel);
        } else {
            showErrorDialog("No entry selected for deletion.");
        }
    }
}
