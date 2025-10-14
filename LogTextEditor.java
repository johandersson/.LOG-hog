import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private final JTabbedPane tabPane = new JTabbedPane();
    public LogTextEditor() {
        // Ensure the frame is decorated by the OS (native chrome)
        setUndecorated(false);

        setTitle(".LOG hog");
        setSize(980, 660);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Apply LAF tweaks (do not change the LAF here; set LAF from your launcher)
        applyLookAndFeelTweaks();

        // Root panel with subtle border to emulate card area (do NOT add a custom title bar)
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(new Color(0xD6DCE0)));
        root.setBackground(new Color(0xF3F6F9));
        setContentPane(root);

        // Main content area with left rail + center cards
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(new Color(0xF7FAFC));
        center.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel leftRail = createLeftRail();
        center.add(leftRail, BorderLayout.WEST);

        // content area (tabs wrapped in a card-like panel)
        JPanel contentCard = new JPanel(new BorderLayout());
        contentCard.setBackground(Color.WHITE);
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE7EBEF)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Use the field tabPane so NavItems can control it
        tabPane.addTab("Entry", createEntryPanel());
        tabPane.addTab("Log Entries", createLogPanel());
        tabPane.addTab("Full Log", createFullLogPanel());
        contentCard.add(tabPane, BorderLayout.CENTER);

        center.add(contentCard, BorderLayout.CENTER);

        // small status/footer area
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(8, 12, 8, 12));
        statusBar.setBackground(new Color(0xFFFFFF));
        JLabel footer = new JLabel("Press Ctrl+S to save and Ctrl+R to reload");
        footer.setFont(footer.getFont().deriveFont(Font.PLAIN, 12f));
        footer.setForeground(new Color(0x394B54));
        statusBar.add(footer, BorderLayout.WEST);

        root.add(center, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);

        // wire behavior and load data
        setupKeyBindings();
        loadLogEntries();
        loadFullLog(); // populate the full log tab at startup

        SwingUtilities.invokeLater(() -> textArea.requestFocusInWindow());
        setVisible(true);
    }


    // only tweak UI defaults; do not call UIManager.setLookAndFeel here
    private void applyLookAndFeelTweaks() {
        // UI tweaks that are safe to call after the LAF is already set
        UIManager.put("control", new Color(0xF3F6F9));
        UIManager.put("nimbusBase", new Color(0x2E3A3F));
        UIManager.put("text", new Color(0x22282B));
        Font uiFont = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("Label.font", uiFont);
        UIManager.put("Button.font", uiFont);
        UIManager.put("ComboBox.font", uiFont);
        UIManager.put("TabbedPane.font", uiFont);
    }

    // Do not create a custom title bar when you want native OS chrome.
// Keep this method empty or remove it entirely and do not add its result to the frame.
    private JPanel createTitleBar() {
        // Return an empty non-opaque panel with zero preferred height to avoid altering layout
        JPanel bar = new JPanel();
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 0));
        return bar;
    }

    // Small styled components reused in this class



    private static class WindowBtn extends JButton {
        WindowBtn(String text, ActionListener act, Color bg) {
            super(text);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            setFocusPainted(false);
            setBackground(bg);
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(getFont().deriveFont(Font.BOLD, 12f));
            addActionListener(act);
        }
    }



    private final java.util.List<NavItem> navItems = new ArrayList<>();

    private JPanel createLeftRail() {
        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(170, 0));
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(new Color(0xF7FAFC));
        left.setBorder(new EmptyBorder(12, 10, 12, 10));

        JLabel logo = new JLabel("⟡");
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 26f));
        logo.setForeground(new Color(0x2B3A42));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.setOpaque(false);
        logo.setBorder(new EmptyBorder(0, 0, 8, 0));
        left.add(logo);
        left.add(Box.createVerticalStrut(12));

        // create NavItems bound to tab indices using the extracted NavItem class (title, tabIndex, tabPane)
        NavItem n0 = new NavItem("Entry", 0, tabPane);
        NavItem n1 = new NavItem("Log Entries", 1, tabPane);
        NavItem n2 = new NavItem("Full Log", 2, tabPane);

        navItems.clear();
        navItems.add(n0); navItems.add(n1); navItems.add(n2);

        left.add(n0);
        left.add(Box.createVerticalStrut(6));
        left.add(n1);
        left.add(Box.createVerticalStrut(6));
        left.add(n2);
        left.add(Box.createVerticalGlue());

        JLabel ver = new JLabel("v1.0");
        ver.setFont(ver.getFont().deriveFont(11f));
        ver.setForeground(new Color(0x6B7A80));
        ver.setAlignmentX(Component.LEFT_ALIGNMENT);
        ver.setOpaque(false);
        left.add(ver);

        // ensure nav visuals update when user changes tabs programmatically
        tabPane.addChangeListener(e -> updateNavActiveStates());
        updateNavActiveStates(); // sync visuals initially

        return left;
    }


    private void updateNavActiveStates() {
        // NavItem instances listen to tabPane changes themselves; just repaint to force visual refresh
        int sel = tabPane.getSelectedIndex();
        for (NavItem ni : navItems) {
            ni.repaint();
            // ensure accessibility/visual sync for the selected item
            if (navItems.indexOf(ni) == sel) {
                ni.getAccessibleContext().firePropertyChange(
                        javax.accessibility.AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        null, null);
            }
        }
    }



    private JPanel createEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(true);
        JScrollPane sp = new JScrollPane(textArea);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xE6E9EB)));
        panel.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.setBackground(Color.WHITE);
        JButton saveBtn = new AccentButton("Save");
        saveBtn.addActionListener(e -> saveLogEntry());
        bottom.add(saveBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);

        // Top: filter controls
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        filterPanel.setOpaque(false);
        JLabel filterLabel = new JLabel("Filter on date");
        filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));
        filterPanel.add(filterLabel);

        int currentYear = Year.now().getValue();
        Integer[] years = IntStream.rangeClosed(2000, currentYear).boxed().toArray(Integer[]::new);
        JComboBox<Integer> yearCombo = new JComboBox<>(years);
        yearCombo.setSelectedItem(currentYear);
        filterPanel.add(yearCombo);

        String[] months = new String[] {
                "01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr",
                "05 - May", "06 - Jun", "07 - Jul", "08 - Aug",
                "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec"
        };
        JComboBox<String> monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        filterPanel.add(monthCombo);

        JButton clearFilterBtn = new JButton("Clear");
        filterPanel.add(clearFilterBtn);

        panel.add(filterPanel, BorderLayout.NORTH);

        // Center: list and editor pane in a split for a polished layout
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.33);
        split.setBorder(null);

        logList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logList.setModel(listModel);

        JScrollPane listScroll = new JScrollPane(logList);
        listScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE6E9EB)),
                new EmptyBorder(6,6,6,6)
        ));

        JPanel entryContainer = new JPanel(new BorderLayout());
        entryContainer.setBorder(new EmptyBorder(6,6,6,6));
        entryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        entryArea.setLineWrap(true);
        entryArea.setWrapStyleWord(true);
        JScrollPane entryScroll = new JScrollPane(entryArea);
        entryScroll.setPreferredSize(new Dimension(600, 220));
        entryContainer.add(entryScroll, BorderLayout.CENTER);

        split.setLeftComponent(listScroll);
        split.setRightComponent(entryContainer);

        panel.add(split, BorderLayout.CENTER);

        // Popup and listeners
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Entry");
        deleteItem.addActionListener(e -> deleteSelectedEntry());
        contextMenu.add(deleteItem);
        logList.setComponentPopupMenu(contextMenu);

        logList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String selectedItem = logList.getSelectedValue();
                if (selectedItem != null) {
                    String logContent = logFileHandler.loadEntry(selectedItem);
                    entryArea.setText(logContent);
                }
            }
        });

        // Filter actions
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
        panel.setBackground(Color.WHITE);

        fullLogPane.setEditable(false);
        fullLogPane.setBackground(Color.WHITE);
        fullLogPane.setFont(new Font("Georgia", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(fullLogPane);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE6E9EB)));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.setOpaque(false);
        fullLogPathLabel.setForeground(new Color(0x3A4A52));
        pathPanel.add(fullLogPathLabel, BorderLayout.WEST);
        panel.add(pathPanel, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
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
