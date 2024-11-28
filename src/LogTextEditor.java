import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogTextEditor extends JFrame {

    private JTextArea textArea;
    private JList<String> logList;
    private DefaultListModel<String> listModel;
    private JTextArea entryArea;
    private static final String FILE_PATH = System.getProperty("user.home") + File.separator + "log.txt";

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
    }

    private void saveText() {
        String text = textArea.getText().trim();
        if (!text.isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
                File file = new File(FILE_PATH);
                if (file.length() == 0) {
                    writer.write(".LOG\n");
                }
                String timeStamp = new SimpleDateFormat("HH:mm yyyy-MM-dd").format(new Date());
                writer.write(timeStamp + "\n" + text + "\n\n");
                textArea.setText("");
                loadLogEntries(); // Reload log entries after saving
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
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}")) {
                    listModel.addElement(line);
                }
            }
        } catch (IOException ex) {
            showErrorDialog("Error loading log entries: " + ex.getMessage());
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
