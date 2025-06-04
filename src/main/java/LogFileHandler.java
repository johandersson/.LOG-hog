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

        // Append counter if duplicates exist
        String uniqueTimeStamp = count > 0 ? timeStamp + " (" + count + ")" : timeStamp;

        try {
            Files.writeString(FILE_PATH, uniqueTimeStamp + "\n" + text + "\n\n",
                    Files.exists(FILE_PATH) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);

            listModel.addElement(uniqueTimeStamp);
            sortListModel(listModel);
        } catch (IOException e) {
            showErrorDialog("Error saving text: " + e.getMessage());
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
                    .filter(line -> line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?"))
                    .sorted((a, b) -> parseDate(b).compareTo(parseDate(a))) // Sort descending (newest first)
                    .toList();

            logs.forEach(listModel::addElement);
        } catch (IOException e) {
            showErrorDialog("Error loading log entries: " + e.getMessage());
        }
    }

    private LocalDateTime parseDate(String timestamp) {
        return LocalDateTime.parse(timestamp.replaceAll(" \\(\\d+\\)", ""), DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd"));
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
