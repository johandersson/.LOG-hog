import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

class LogFileHandler {
    private static final Path FILE_PATH = Path.of(System.getProperty("user.home"), "log.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");

    void saveText(String text, DefaultListModel<String> listModel) {
        if (text.isBlank()) return;

        String timeStamp = FORMATTER.format(LocalDateTime.now());
        try {
            Files.writeString(FILE_PATH, timeStamp + "\n" + text + "\n\n", Files.exists(FILE_PATH)
                    ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);

            // Add to list model and re-sort
            listModel.addElement(timeStamp);
            sortListModel(listModel);
        } catch (IOException e) {
            showErrorDialog("Error saving text: " + e.getMessage());
        }
    }

    // Ensure sorting happens right after saving
    private void sortListModel(DefaultListModel<String> listModel) {
        List<String> sortedEntries = Collections.list(listModel.elements()).stream()
                .sorted((a, b) -> parseDate(b).compareTo(parseDate(a))) // Sort descending
                .toList();

        listModel.clear();
        sortedEntries.forEach(listModel::addElement);
    }


    void loadLogEntries(DefaultListModel<String> listModel) {
        listModel.clear();
        if (!Files.exists(FILE_PATH)) return;

        try {
            List<String> logs = Files.lines(FILE_PATH)
                    .filter(line -> line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}"))
                    .sorted((a, b) -> {
                        LocalDateTime dateA = parseDate(a);
                        LocalDateTime dateB = parseDate(b);
                        return dateB.compareTo(dateA); // Sort descending (newest first)
                    })
                    .toList();

            logs.forEach(listModel::addElement);
        } catch (IOException e) {
            showErrorDialog("Error loading log entries: " + e.getMessage());
        }
    }

    private LocalDateTime parseDate(String timestamp) {
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd"));
    }


    String loadEntry(String timeStamp) {
        if (!Files.exists(FILE_PATH)) return "";

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            int index = lines.indexOf(timeStamp);
            return index >= 0 ? String.join("\n", lines.subList(index, Math.min(index + 3, lines.size()))) : "";
        } catch (IOException e) {
            showErrorDialog("Error displaying log entry: " + e.getMessage());
            return "";
        }
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}