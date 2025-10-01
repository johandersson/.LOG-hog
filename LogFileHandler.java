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

        String entry = uniqueTimeStamp + "\n" + text + "\n"; // Each log entry is stored separately

        try {
            Files.writeString(FILE_PATH, entry, Files.exists(FILE_PATH)
                    ? java.nio.file.StandardOpenOption.APPEND
                    : java.nio.file.StandardOpenOption.CREATE);

            listModel.addElement(uniqueTimeStamp);
            sortListModel(listModel);
            System.out.println("Saved log entry: \n" + entry); // Debug print
        } catch (IOException e) {
            showErrorDialog("Error saving text: " + e.getMessage());
        }
    }

    //delete certain log entry
    private void deleteLogEntry(String timeStamp, DefaultListModel<String> listModel) {
        if (!Files.exists(FILE_PATH)) return;

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            List<String> updatedLines = new ArrayList<>();
            boolean found = false;

            for (String line : lines) {
                if (line.trim().equals(timeStamp.trim())) {
                    found = true; // Skip the timestamp line
                    continue;
                }
                if (found && line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}")) {
                    found = false; // Stop collecting lines after the next timestamp
                }
                if (!found) {
                    updatedLines.add(line);
                }
            }

            Files.write(FILE_PATH, updatedLines);
            listModel.removeElement(timeStamp);
            System.out.println("Deleted log entry: " + timeStamp); // Debug print
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
        System.out.println("Sorted log entries: " + sortedEntries); // Debug print
    }

    void loadLogEntries(DefaultListModel<String> listModel) {
        listModel.clear();
        if (!Files.exists(FILE_PATH)) return;

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            if (!lines.isEmpty() && lines.get(0).trim().equals(".LOG")) {
                lines.remove(0); // Remove .LOG header
            }

            List<String> timestamps = new ArrayList<>();

            for (String line : lines) {
                if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                    timestamps.add(line.trim()); // Store only timestamps
                }
            }

            listModel.clear();
            timestamps.sort(Comparator.comparing(this::parseDate).reversed());
            timestamps.forEach(listModel::addElement);

            System.out.println("Consistently sorted timestamps (newest first): \n" + timestamps); // Debug print

        } catch (IOException e) {
            showErrorDialog("Error loading log entries: " + e.getMessage());
        }
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

            System.out.println("Searching for entry: " + timeStamp.trim()); // Debug print

            for (String line : lines) {
                if (line.trim().equals(timeStamp.trim())) {
                    found = true;
                    continue; // Skip the timestamp itself
                }

                if (found) {
                    if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}")) break; // Stop at next timestamp
                    entry.append(line).append("\n");
                }
            }

            System.out.println("Final entry loaded:\n" + entry.toString().trim()); // Debug print
            return entry.toString().trim();
        } catch (IOException e) {
            showErrorDialog("Error displaying log entry: " + e.getMessage());
        }
        return "";
    }

    private void showErrorDialog(String message) {
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