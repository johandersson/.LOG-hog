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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final Highlighter.HighlightPainter searchPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

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
        SwingUtilities.invokeLater(() -> textArea.requestFocusInWindow());
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
        if (selectedItem == null) return;

        // Build preview: timestamp, blank line, then up to 200 chars of the entry followed by "..."
        String entryText = logFileHandler.loadEntry(selectedItem);
        String previewBody;
        if (entryText == null || entryText.isBlank()) {
            previewBody = "(no content)";
        } else {
            String trimmed = entryText.length() > 200 ? entryText.substring(0, 200) + "..." : entryText;
            previewBody = trimmed;
        }
        String previewFull = selectedItem + "\n\n" + previewBody;

        JTextArea previewArea = createPreviewArea(previewFull);

        // Compose dialog content: question label above preview
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JLabel question = new JLabel("Are you sure you want to delete this entry?");
        panel.add(question, BorderLayout.NORTH);
        panel.add(new JScrollPane(previewArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

        int confirm = JOptionPane.showConfirmDialog(this,
                panel,
                "Delete Entry",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            logFileHandler.deleteEntry(selectedItem, listModel);
            updateLogListView();
            entryArea.setText("");
            loadFullLog(); // update full log view after deletion
        }
    }

    private static JTextArea createPreviewArea(String previewFull) {
        // Create a small preview component
        JTextArea previewArea = new JTextArea(previewFull);
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        previewArea.setBackground(UIManager.getColor("Panel.background"));
        previewArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        previewArea.setRows(6);
        previewArea.setColumns(40);
        return previewArea;
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

    // updated setupKeyBindings method
    private void setupKeyBindings() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "load");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "find"); // Ctrl+F

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
        actionMap.put("find", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                showFindDialogAndSearch();
            }
        });
    }

    // new method: shows input dialog and triggers search
    private void showFindDialogAndSearch() {
        String query = JOptionPane.showInputDialog(this, "Search text:", "Find", JOptionPane.QUESTION_MESSAGE);
        if (query == null) return;
        performSearchInFullLog(query);
    }

    // new method: perform search, highlight occurrences, scroll to first hit or show "Text not found"
    private void performSearchInFullLog(String query) {
        Highlighter highlighter = fullLogPane.getHighlighter();
        highlighter.removeAllHighlights();

        Document doc = fullLogPane.getDocument();
        int len = doc.getLength();
        String text;
        try {
            text = doc.getText(0, len);
        } catch (BadLocationException e) {
            JOptionPane.showMessageDialog(this, "Error accessing document", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        String qLower = query.toLowerCase(Locale.ROOT);

        int index = lower.indexOf(qLower);
        if (index == -1) {
            JOptionPane.showMessageDialog(this, "Text not found", "Find", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int start = 0;
        try {
            while ((start = lower.indexOf(qLower, start)) >= 0) {
                int end = start + qLower.length();
                highlighter.addHighlight(start, end, searchPainter);
                start = end;
            }
        } catch (BadLocationException ex) {
            // ignore individual highlight failures
        }

        try {
            Rectangle rect = fullLogPane.modelToView(index);
            if (rect != null) {
                rect.height = Math.max(rect.height, 20);
                fullLogPane.scrollRectToVisible(rect);
                fullLogPane.setCaretPosition(index);
            }
        } catch (BadLocationException ex) {
            // ignore scrolling failure
        }
    }




    // Entry point (public or package-private depending on your class)
    private void loadFullLog() {
        SwingUtilities.invokeLater(() -> {
            Path chosen = findLogPath();
            if (chosen == null) {
                showLogNotFound();
                return;
            }

            ensureLinkListenersInstalled();

            clearEditorForNewLoad(chosen);

            try {
                List<String> lines = Files.readAllLines(chosen, StandardCharsets.UTF_8);
                StyledDocument doc = fullLogPane.getStyledDocument();
                Map<String, Style> styles = createStyles(doc);

                renderLines(lines, doc, styles);
                fullLogPane.setCaretPosition(0);
            } catch (IOException | BadLocationException ex) {
                fallbackReadRaw(chosen);
            }
        });
    }

    /* -------------------------
       Helpers: file discovery
       ------------------------- */
    private Path findLogPath() {
        String userHome = System.getProperty("user.home");
        Path homePath = Paths.get(userHome, "log.txt");
        Path cwdPath = Paths.get(System.getProperty("user.dir"), "log.txt");
        if (Files.exists(homePath)) return homePath;
        if (Files.exists(cwdPath)) return cwdPath;
        return null;
    }

    private void showLogNotFound() {
        String userHome = System.getProperty("user.home");
        fullLogPane.setText("log.txt not found in user home or current working directory.\n"
                + "Checked paths:\n"
                + userHome + File.separator + "log.txt\n"
                + System.getProperty("user.dir") + File.separator + "log.txt");
        fullLogPathLabel.setText("Log file: not found");
        fullLogPane.getHighlighter().removeAllHighlights();
    }

    /* -------------------------
       Editor initialization
       ------------------------- */
    private void clearEditorForNewLoad(Path chosen) {
        fullLogPane.setText("");
        fullLogPane.getHighlighter().removeAllHighlights();
        fullLogPathLabel.setText("Log file: " + chosen.toAbsolutePath().toString());
    }

    /* -------------------------
       Styles factory
       ------------------------- */
    private Map<String, Style> createStyles(StyledDocument doc) {
        Map<String, Style> styles = new HashMap<>();

        Style defaultStyle = doc.addStyle("default", null);
        StyleConstants.setFontFamily(defaultStyle, "Georgia");
        StyleConstants.setFontSize(defaultStyle, 14);
        StyleConstants.setForeground(defaultStyle, Color.DARK_GRAY);
        styles.put("default", defaultStyle);

        Style tsStyle = doc.addStyle("timestamp", defaultStyle);
        StyleConstants.setFontSize(tsStyle, 16);
        StyleConstants.setBold(tsStyle, true);
        StyleConstants.setForeground(tsStyle, Color.BLACK);
        styles.put("timestamp", tsStyle);

        Style sepStyle = doc.addStyle("sep", defaultStyle);
        StyleConstants.setFontSize(sepStyle, 10);
        styles.put("sep", sepStyle);

        return styles;
    }

    /* -------------------------
       Rendering: lines + links
       ------------------------- */
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]|]+)(?:\\|([^\\]]*))?\\]");


    private static final String TS_REGEX = "^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?$";

    private void renderLines(List<String> lines, StyledDocument doc, Map<String, Style> styles)
            throws BadLocationException {
        Style defaultStyle = styles.get("default");
        Style tsStyle = styles.get("timestamp");
        Style sepStyle = styles.get("sep");

        for (String line : lines) {
            if (line.matches(TS_REGEX)) {
                doc.insertString(doc.getLength(), line + "\n", tsStyle);
            } else if (line.trim().isEmpty()) {
                doc.insertString(doc.getLength(), "\n", sepStyle);
            } else {
                appendLineWithInlineLinks(doc, line, defaultStyle);
                doc.insertString(doc.getLength(), "\n", defaultStyle);
            }
        }
    }

    private void appendLineWithInlineLinks(StyledDocument doc, String line, Style defaultStyle)
            throws BadLocationException {
        Matcher m = LINK_PATTERN.matcher(line);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                String before = line.substring(last, m.start());
                doc.insertString(doc.getLength(), before, defaultStyle);
            }

            String target = m.group(1);                     // required: the URL/target
            String display = m.group(2);                    // optional: link text (may be null or empty)
            String textToShow = (display != null && !display.isEmpty()) ? display : target;

            SimpleAttributeSet linkAttr = new SimpleAttributeSet();
            StyleConstants.setForeground(linkAttr, Color.BLUE);
            StyleConstants.setUnderline(linkAttr, true);
            linkAttr.addAttribute("href", target);

            doc.insertString(doc.getLength(), textToShow, linkAttr);
            last = m.end();
        }
        if (last < line.length()) {
            String after = line.substring(last);
            doc.insertString(doc.getLength(), after, defaultStyle);
        }
    }


    /* -------------------------
       Mouse listeners for links
       ------------------------- */
    private void ensureLinkListenersInstalled() {
        // Avoid adding multiple duplicate listeners
        boolean hasLinkListener = Arrays.stream(fullLogPane.getMouseListeners())
                .anyMatch(l -> l.getClass().getName().contains("MouseAdapter") || l.getClass().getName().contains("MouseListener"));
        if (hasLinkListener) return;

        fullLogPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleLinkClick(e);
            }
        });

        fullLogPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleLinkHover(e);
            }
        });
    }

    private void handleLinkClick(MouseEvent e) {
        try {
            int pos = fullLogPane.viewToModel2D(e.getPoint());
            if (pos < 0) return;
            StyledDocument doc = fullLogPane.getStyledDocument();
            AttributeSet attrs = doc.getCharacterElement(pos).getAttributes();
            Object hrefObj = attrs.getAttribute("href");
            if (hrefObj instanceof String) {
                String href = (String) hrefObj;
                if (!href.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
                    href = "http://" + href;
                }
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new java.net.URI(href));
                }
            }
        } catch (Exception ex) {
            // swallow or log depending on your logging conventions
        }
    }

    private void handleLinkHover(MouseEvent e) {
        try {
            int pos = fullLogPane.viewToModel2D(e.getPoint());
            if (pos < 0) {
                fullLogPane.setCursor(Cursor.getDefaultCursor());
                return;
            }
            AttributeSet attrs = fullLogPane.getStyledDocument().getCharacterElement(pos).getAttributes();
            Object hrefObj = attrs.getAttribute("href");
            fullLogPane.setCursor(hrefObj instanceof String ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        } catch (Exception ex) {
            fullLogPane.setCursor(Cursor.getDefaultCursor());
        }
    }

    /* -------------------------
       Fallback raw read
       ------------------------- */
    private void fallbackReadRaw(Path chosen) {
        try {
            byte[] bytes = Files.readAllBytes(chosen);
            String content = new String(bytes, StandardCharsets.UTF_8);
            fullLogPane.setText(content);
            fullLogPane.getHighlighter().removeAllHighlights();
            fullLogPane.setCaretPosition(0);
        } catch (IOException e) {
            fullLogPane.setText("Error reading " + chosen.toAbsolutePath().toString() + " : " + e.getMessage());
            fullLogPathLabel.setText("Log file: error reading file");
            fullLogPane.getHighlighter().removeAllHighlights();
        }
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
