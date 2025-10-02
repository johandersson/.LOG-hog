import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;

public class LogTextEditor extends JFrame {

    private final JTextArea textArea = new JTextArea();
    private final JList<String> logList = new JList<>();
    private final JTextArea entryArea = new JTextArea();
    private final LogFileHandler logFileHandler = new LogFileHandler(); // external class
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    // Full log view uses JTextPane so we can style timestamps
    private final JTextPane fullLogPane = new JTextPane();
    private final JButton copyFullLogButton = new JButton("Copy Full Log to Clipboard");
    private final JLabel fullLogPathLabel = new JLabel("Log file: (not loaded)");

    public LogTextEditor() {
        setTitle(".LOG hog");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabPane = new JTabbedPane();
        tabPane.addTab("Entry", createEntryPanel());
        tabPane.addTab("Log Entries", createLogPanel());
        tabPane.addTab("Full Log", createFullLogPanel());

        JLabel footer = new JLabel("Press Ctrl+S to save and Ctrl+R to reload", SwingConstants.CENTER);

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

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> saveLogEntry());
        bottom.add(saveBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top: simple filter controls (label + year combobox + month combobox + clear)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        JLabel filterLabel = new JLabel("Filter on date");
        filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));

        int currentYear = Year.now().getValue();
        Integer[] years = IntStream.rangeClosed(2000, currentYear).boxed().toArray(Integer[]::new);
        JComboBox<Integer> yearCombo = new JComboBox<>(years);
        yearCombo.setSelectedItem(currentYear);

        String[] months = new String[] {
                "01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr",
                "05 - May", "06 - Jun", "07 - Jul", "08 - Aug",
                "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec"
        };
        JComboBox<String> monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);

        JButton clearFilterBtn = new JButton("Clear");

        filterPanel.add(filterLabel);
        filterPanel.add(yearCombo);
        filterPanel.add(monthCombo);
        filterPanel.add(clearFilterBtn);

        // Center: list and entry area
        logList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        logList.setModel(listModel);

        logList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String selectedItem = logList.getSelectedValue();
                if (selectedItem != null) {
                    String logContent = logFileHandler.loadEntry(selectedItem);
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
        entryScroll.setPreferredSize(new Dimension(600, 220));

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(logList), BorderLayout.CENTER);
        panel.add(entryScroll, BorderLayout.SOUTH);

        // Filter actions: when year or month changes, apply filter
        Action applyFilterAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Integer year = (Integer) yearCombo.getSelectedItem();
                    int month = monthCombo.getSelectedIndex() + 1;
                    if (year != null) {
                        logFileHandler.loadFilteredEntries(listModel, year, month);
                        updateLogListView();
                    }
                } catch (Exception ex) {
                    logFileHandler.showErrorDialog("Error applying date filter: " + ex.getMessage());
                }
            }
        };
        yearCombo.addActionListener(applyFilterAction);
        monthCombo.addActionListener(applyFilterAction);

        clearFilterBtn.addActionListener(e -> {
            logFileHandler.loadLogEntries(listModel);
            updateLogListView();
        });

        return panel;
    }

    private JPanel createFullLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));

        fullLogPane.setEditable(false);
        fullLogPane.setBackground(Color.WHITE);

        // Base font is a nicer serif for reading; timestamps will be larger/bold
        fullLogPane.setFont(new Font("Georgia", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(fullLogPane);
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

    // Reads the whole log.txt file and places its contents into fullLogPane with styling
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
                fullLogPane.setText("log.txt not found in user home or current working directory.\n"
                        + "Checked paths:\n"
                        + userHome + File.separator + "log.txt\n"
                        + System.getProperty("user.dir") + File.separator + "log.txt");
                fullLogPathLabel.setText("Log file: not found");
                return;
            }

            fullLogPane.setText("");
            fullLogPathLabel.setText("Log file: " + chosen.toAbsolutePath().toString());

            try {
                List<String> lines = Files.readAllLines(chosen, StandardCharsets.UTF_8);
                StyledDocument doc = fullLogPane.getStyledDocument();

                // default/body style
                Style defaultStyle = doc.addStyle("default", null);
                StyleConstants.setFontFamily(defaultStyle, "Georgia");
                StyleConstants.setFontSize(defaultStyle, 14);
                StyleConstants.setForeground(defaultStyle, Color.DARK_GRAY);

                // timestamp style: larger, bold
                Style tsStyle = doc.addStyle("timestamp", defaultStyle);
                StyleConstants.setFontSize(tsStyle, 16);
                StyleConstants.setBold(tsStyle, true);
                StyleConstants.setForeground(tsStyle, Color.BLACK);

                // small separator for blank lines
                Style sepStyle = doc.addStyle("sep", defaultStyle);
                StyleConstants.setFontSize(sepStyle, 10);

                String tsRegex = "^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?$";

                for (String line : lines) {
                    if (line.matches(tsRegex)) {
                        doc.insertString(doc.getLength(), line + "\n", tsStyle);
                    } else {
                        if (line.trim().isEmpty()) {
                            doc.insertString(doc.getLength(), "\n", sepStyle);
                        } else {
                            doc.insertString(doc.getLength(), line + "\n", defaultStyle);
                        }
                    }
                }

                fullLogPane.setCaretPosition(0);
            } catch (IOException | BadLocationException ex) {
                try {
                    byte[] bytes = Files.readAllBytes(chosen);
                    String content = new String(bytes, StandardCharsets.UTF_8);
                    fullLogPane.setText(content);
                    fullLogPane.setCaretPosition(0);
                } catch (IOException e) {
                    fullLogPane.setText("Error reading " + chosen.toAbsolutePath().toString() + " : " + e.getMessage());
                    fullLogPane.setText("Error reading " + chosen.toAbsolutePath().toString() + " : " + e.getMessage());
                    fullLogPathLabel.setText("Log file: error reading file");
                }
            }
        });
    }

    // Copies entire contents of fullLogPane to system clipboard
    private void copyFullLogToClipboard() {
        String text = fullLogPane.getText();
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
        SwingUtilities.invokeLater(() -> {
            LogTextEditor editor = new LogTextEditor();
            editor.setVisible(true);
        });
    }
}
