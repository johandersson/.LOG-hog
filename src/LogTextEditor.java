import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

public class LogTextEditor extends JFrame {

    private JTextArea textArea;
    private JList<String> logList;
    private DefaultListModel<String> listModel;
    private JTextArea entryArea;
    private static final String FILE_PATH = System.getProperty("user.home") + File.separator + "log.txt";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm yyyy-MM-dd");

    public LogTextEditor() {
        setTitle("Log Text Editor");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Text entry tab
        JPanel entryPanel = new JPanel(new BorderLayout());
        textArea = new JTextArea();
        textArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(textArea);
        entryPanel.add(scrollPane, BorderLayout.CENTER);
        tabbedPane.add("Entry", entryPanel);

        // Log entries tab
        JPanel logPanel = new JPanel(new BorderLayout());
        listModel = new DefaultListModel<>();
        logList = new JList<>(listModel);
        JScrollPane listScrollPane = new JScrollPane(logList);
        entryArea = new JTextArea();
        entryArea.setFont(new Font("Arial", Font.PLAIN, 14));
        entryArea.setEditable(false);
        JScrollPane entryScrollPane = new JScrollPane(entryArea);
        logPanel.add(listScrollPane, BorderLayout.NORTH);
        logPanel.add(entryScrollPane, BorderLayout.CENTER);
        tabbedPane.add("Log Entries", logPanel);

        add(tabbedPane);

        // Set focus on the text area when the window opens
        SwingUtilities.invokeLater(() -> textArea.requestFocus());

        // Add a key listener to save the text on CTRL-S and reload on CTRL-R
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_S) && ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
                    saveText();
                } else if ((e.getKeyCode() == KeyEvent.VK_R) && ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
                    loadLogEntries();
                }
            }
        });

        // Load log entries into the list box
        loadLogEntries();

        // Add a list selection listener to display selected entry
        logList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedEntry = logList.getSelectedValue();
                    if (selectedEntry != null) {
                        displayLogEntry(selectedEntry);
                    }
                }
            }
        });

        // Add a change listener to handle tab switches
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (tabbedPane.getSelectedIndex() == 1) { // Assuming "Log Entries" is the second tab
                    if (!textArea.getText().trim().isEmpty()) {
                        saveText();
                        textArea.setText("");
                    }
                    loadLogEntries();
                }
            }
        });
    }

    private void saveText() {
        String text = textArea.getText().trim();
        if (!text.isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
                File file = new File(FILE_PATH);
                if (file.length() == 0) {
                    writer.write(".LOG\n");
                }
                String timeStamp = DATE_FORMAT.format(new Date());
                writer.write(timeStamp + "\n" + text + "\n\n");
                textArea.setText("");
                loadLogEntries(); // Reload log entries after saving
                sortLogEntries(); // Sort the list after saving
            } catch (IOException ex) {
                showErrorDialog("Error saving text: " + ex.getMessage());
            }
        }
    }

    private void loadLogEntries() {
        listModel.clear();
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }
        java.util.List<String> logEntries = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}")) {
                    logEntries.add(line);
                }
            }
        } catch (IOException ex) {
            showErrorDialog("Error loading log entries: " + ex.getMessage());
        }
        // Sort log entries in reverse order (most recent first)
        Collections.sort(logEntries, (entry1, entry2) -> {
            try {
                Date date1 = DATE_FORMAT.parse(entry1);
                Date date2 = DATE_FORMAT.parse(entry2);
                return date2.compareTo(date1);
            } catch (ParseException e) {
                return 0;
            }
        });
        for (String entry : logEntries) {
            listModel.addElement(entry);
        }
    }

    private void sortLogEntries() {
        java.util.List<String> logEntries = Collections.list(listModel.elements());
        Collections.sort(logEntries, (entry1, entry2) -> {
            try {
                Date date1 = DATE_FORMAT.parse(entry1);
                Date date2 = DATE_FORMAT.parse(entry2);
                return date2.compareTo(date1);
            } catch (ParseException e) {
                return 0;
            }
        });
        listModel.clear();
        for (String entry : logEntries) {
            listModel.addElement(entry);
        }
    }

    private void displayLogEntry(String timeStamp) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            StringBuilder entryText = new StringBuilder();
            boolean entryFound = false;
            while ((line = reader.readLine()) != null) {
                if (line.equals(timeStamp)) {
                    entryFound = true;
                    entryText.append(timeStamp).append("\n");
                } else if (entryFound) {
                    if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}")) {
                        break;
                    }
                    entryText.append(line).append("\n");
                }
            }
            entryArea.setText(entryText.toString());
        } catch (IOException ex) {
            showErrorDialog("Error displaying log entry: " + ex.getMessage());
        }
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LogTextEditor editor = new LogTextEditor();
            editor.setVisible(true);
        });
    }
}
