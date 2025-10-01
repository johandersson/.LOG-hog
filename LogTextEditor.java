import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;

public class LogTextEditor extends JFrame {

    private final JTextArea textArea = new JTextArea();
    private final JList<String> logList = new JList<>();
    private final JTextArea entryArea = new JTextArea();
    private final LogFileHandler logFileHandler = new LogFileHandler();
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    // New components for full log tab
    private final JTextArea fullLogArea = new JTextArea();
    private final JButton copyFullLogButton = new JButton("Copy Full Log to Clipboard");
    private final JLabel fullLogPathLabel = new JLabel("Log file: (not loaded)");

    public LogTextEditor() {
        setTitle(".LOG hog");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabPane = new JTabbedPane();
        tabPane.addTab("Entry", createEntryPanel());
        tabPane.addTab("Log Entries", createLogPanel());
        tabPane.addTab("Full Log", createFullLogPanel());

        JLabel footer = new JLabel("Press Ctrl+S to save and Ctrl+R to load", SwingConstants.CENTER);

        add(tabPane, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        setupKeyBindings();
        loadLogEntries();
        loadFullLog(); // populate the full log tab at startup
        textArea.requestFocusInWindow();
    }

    private JPanel createEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        logList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        logList.setModel(listModel);

        logList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String selectedItem = logList.getSelectedValue();
                System.out.println("Selected Timestamp Clicked: " + selectedItem);
                if (selectedItem != null) {
                    String logContent = logFileHandler.loadEntry(selectedItem);
                    System.out.println("Setting UI Text:\n" + logContent);
                    entryArea.setText(logContent);
                }
            }
        });

        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Entry");
        deleteItem.addActionListener(e -> deleteSelectedEntry());
        contextMenu.add(deleteItem);
        logList.setComponentPopupMenu(contextMenu);

        entryArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        entryArea.setRows(10);
        JScrollPane entryScroll = new JScrollPane(entryArea);
        entryScroll.setPreferredSize(new Dimension(600, 200));

        panel.add(new JScrollPane(logList), BorderLayout.CENTER);
        panel.add(entryScroll, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createFullLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));

        fullLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        fullLogArea.setEditable(false);
        fullLogArea.setLineWrap(true);
        fullLogArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(fullLogArea);
        panel.add(scroll, BorderLayout.CENTER);

        // Path display above text area
        JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.add(fullLogPathLabel, BorderLayout.WEST);
        panel.add(pathPanel, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadFullLog());
        copyFullLogButton.addActionListener(e -> copyFullLogToClipboard());
        bottom.add(refreshButton);
        bottom.add(copyFullLogButton);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void deleteSelectedEntry() {
        String selectedItem = logList.getSelectedValue();
        if (selectedItem != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete this entry?",
                    "Delete Entry",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                logFileHandler.deleteEntry(selectedItem, listModel);
                updateLogListView();
                entryArea.setText("");
                loadFullLog(); // update full log view after deletion
            }
        }
    }

    private void saveLogEntry() {
        logFileHandler.saveText(textArea.getText(), listModel);
        textArea.setText("");
        updateLogListView();
        loadFullLog(); // update full log view after save
    }

    private void loadLogEntries() {
        logFileHandler.loadLogEntries(listModel);
        updateLogListView();
    }

    private void updateLogListView() {
        logList.setModel(listModel);
        System.out.println("ListView contents: " + Collections.list(listModel.elements()));
    }

    private void setupKeyBindings() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "load");

        actionMap.put("save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveLogEntry();
            }
        });
        actionMap.put("load", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                loadLogEntries();
                loadFullLog();
            }
        });
    }

    // Reads the whole log.txt file and places its contents into fullLogArea.
    // Preference order: user's home dir, then current working directory.
    private void loadFullLog() {
        SwingUtilities.invokeLater(() -> {
            String userHome = System.getProperty("user.home");
            Path homePath = Paths.get(userHome, "log.txt");
            Path cwdPath = Paths.get(System.getProperty("user.dir"), "log.txt");

            Path chosen = null;
            if (Files.exists(homePath)) {
                chosen = homePath;
            } else if (Files.exists(cwdPath)) {
                chosen = cwdPath;
            }

            if (chosen == null) {
                fullLogArea.setText("log.txt not found in user home or current working directory.\n"
                        + "Checked paths:\n"
                        + userHome + File.separator + "log.txt\n"
                        + System.getProperty("user.dir") + File.separator + "log.txt");
                fullLogPathLabel.setText("Log file: not found");
                System.out.println("log.txt not found. Checked: " + homePath + " and " + cwdPath);
                return;
            }

            try {
                byte[] bytes = Files.readAllBytes(chosen);
                String content = new String(bytes, StandardCharsets.UTF_8);
                fullLogArea.setText(content);
                fullLogArea.setCaretPosition(0);
                fullLogPathLabel.setText("Log file: " + chosen.toAbsolutePath().toString());
                System.out.println("Loaded log file from: " + chosen.toAbsolutePath().toString());
            } catch (IOException ex) {
                fullLogArea.setText("Error reading " + chosen.toAbsolutePath().toString() + " : " + ex.getMessage());
                fullLogPathLabel.setText("Log file: error reading file");
                System.err.println("Error reading log file: " + ex.getMessage());
            }
        });
    }

    // Copies entire contents of fullLogArea to system clipboard
    private void copyFullLogToClipboard() {
        String text = fullLogArea.getText();
        if (text == null || text.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Log is empty or not loaded.", "Copy Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            clipboard.setContents(selection, selection);
            JOptionPane.showMessageDialog(this, "Full log copied to clipboard.", "Copied", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException ise) {
            JOptionPane.showMessageDialog(this, "Unable to access clipboard right now. Try again.", "Clipboard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LogTextEditor().setVisible(true));
    }
}
