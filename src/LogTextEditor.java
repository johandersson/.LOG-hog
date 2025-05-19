import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LogTextEditor extends JFrame {

    private final JTextArea textArea = new JTextArea();
    private final JList<String> logList = new JList<>(new DefaultListModel<>());
    private final JTextArea entryArea = new JTextArea();
    private final LogFileHandler logFileHandler = new LogFileHandler();

    public LogTextEditor() {
        setupUI();
        setupEventListeners();
        logFileHandler.loadLogEntries((DefaultListModel<String>) logList.getModel());
    }

    private void setupUI() {
        setTitle("Log Text Editor");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Entry", createEntryPanel());
        tabbedPane.add("Log Entries", createLogPanel());

        add(tabbedPane);
        SwingUtilities.invokeLater(textArea::requestFocus);
    }

    private JPanel createEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        entryArea.setFont(new Font("Arial", Font.PLAIN, 14));
        entryArea.setEditable(false);
        panel.add(new JScrollPane(logList), BorderLayout.NORTH);
        panel.add(new JScrollPane(entryArea), BorderLayout.CENTER);
        return panel;
    }

    private void setupEventListeners() {
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println("Key pressed: " + e.getKeyCode());
                if (e.isControlDown()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_S -> {
                            System.out.println("Ctrl+S triggered!");
                            logFileHandler.saveText(textArea.getText(), (DefaultListModel<String>) logList.getModel());
                            //clear text area after saving
                            textArea.setText("");
                        }
                        case KeyEvent.VK_R -> logFileHandler.loadLogEntries((DefaultListModel<String>) logList.getModel());
                    }
                }
            }
        });


        logList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedEntry = logList.getSelectedValue();
                if (selectedEntry != null) {
                    entryArea.setText(logFileHandler.loadEntry(selectedEntry));
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LogTextEditor().setVisible(true));
    }
}

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
                    .collect(Collectors.toList());

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
