import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

public class LogTextEditor extends JFrame {


    private final JTextArea textArea = new UndoRedoTextArea();
    private final JList<String> logList = new JList<>();
    private final JTextArea entryArea = new UndoRedoTextArea();
    private final LogFileHandler logFileHandler = new LogFileHandler(); // external class
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    private final JTextPane fullLogPane = new JTextPane();
    private final java.util.List<NavItem> navItems = new ArrayList<>();
    JButton refreshButton = new AccentButton("Refresh");
    private final JLabel fullLogPathLabel = new JLabel("Log file: (not loaded)");
    JButton copyFullLogButton = new AccentButton("Copy Full Log to Clipboard");
    JButton openInNotepadButton = new AccentButton("Open in Notepad");

    private final Highlighter.HighlightPainter searchPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
    private final JTabbedPane tabPane = new JTabbedPane();

    private static LogTextEditor instance;

    private final java.util.Properties settings = new java.util.Properties();
    private final java.nio.file.Path settingsPath = java.nio.file.Paths.get(System.getProperty("user.home"), "loghog_settings.properties");

    public LogTextEditor() {
        // Ensure the frame is decorated by the OS (native chrome)
        setUndecorated(false);

        setTitle(".LOG hog");
        setSize(1200, 660);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        addIcon();
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
        createContentCardWithTabs(center);

        // small status/footer area
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(8, 12, 8, 12));
        statusBar.setBackground(new Color(0xFFFFFF));
        JLabel footer = new JLabel("Write something and hit Ctrl+S! Search with Ctrl+F. For a quick short entry, use Ctrl+N anywhere.");
        footer.setFont(footer.getFont().deriveFont(Font.PLAIN, 12f));
        footer.setForeground(new Color(0x394B54));
        statusBar.add(footer, BorderLayout.WEST);

        root.add(center, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);

        setupKeyBindings();
        loadSettings();
        loadLogEntries();
        loadFullLog();
        //init systemtray menu
       var systemTrayMenu = new SystemTrayMenu(this);
       SystemTrayMenu.initSystemTray();

        SwingUtilities.invokeLater(() -> textArea.requestFocusInWindow());
        setVisible(true);
    }

    private void addIcon() {
        //add same L icon as in system tray
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 16, 16);
        g2.setColor(Color.WHITE);
        g2.drawString("L", 4, 12);
        g2.dispose();
        setIconImage(image);
    }

    private void createContentCardWithTabs(JPanel center) {
        JPanel contentCard = new JPanel(new BorderLayout());
        contentCard.setBackground(Color.WHITE);
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE7EBEF)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        tabPane.setUI(new HiddenTabUI());
        tabPane.addTab("Entry", createEntryPanel());
        tabPane.addTab("Log Entries", createLogPanel());
        tabPane.addTab("Full Log", createFullLogPanel());
        tabPane.addTab("Settings", new SettingsPanel(this, settings, settingsPath, logFileHandler));
        tabPane.addTab("Help", new InformationPanel(tabPane, "help.md", "Help"));
        tabPane.addTab("About", new InformationPanel(tabPane, "license.md", "About"));
        contentCard.add(tabPane, BorderLayout.CENTER);

        center.add(contentCard, BorderLayout.CENTER);
    }

    private void applyLookAndFeelTweaks() {
        UIManager.put("control", new Color(0xF3F6F9));
        UIManager.put("nimbusBase", new Color(0x2E3A3F));
        UIManager.put("text", new Color(0x22282B));
        Font uiFont = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("Label.font", uiFont);
        UIManager.put("Button.font", uiFont);
        UIManager.put("ComboBox.font", uiFont);
        UIManager.put("TabbedPane.font", uiFont);
    }


    private JPanel createLeftRail() {
        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(170, 0));
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(true);
        left.setBackground(new Color(0xF7FAFC));
        left.setBorder(new EmptyBorder(12, 10, 12, 10));


        // create NavItems bound to tab indices using the extracted NavItem class (title, tabIndex, tabPane)
        NavItem n0 = new NavItem("Entry", 0, tabPane, null);
        NavItem n1 = new NavItem("Log Entries", 1, tabPane, null);
        NavItem n2 = new NavItem("Full Log", 2, tabPane, null);
        NavItem n3 = new NavItem("Settings", 3, tabPane, null);
        NavItem n4 = new NavItem("Help", 4, tabPane, null);
        Runnable aboutOnClick = () -> {
            new SplashScreen(); // modal, shows splash
            tabPane.setSelectedIndex(5);
        };
        NavItem n5 = new NavItem("About", 5, tabPane, aboutOnClick);

        navItems.clear();
        navItems.add(n0);
        navItems.add(n1);
        navItems.add(n2);
        navItems.add(n3);
        navItems.add(n4);
        navItems.add(n5);

        left.add(n0);
        left.add(Box.createVerticalStrut(6));
        left.add(n1);
        left.add(Box.createVerticalStrut(6));
        left.add(n2);
        left.add(Box.createVerticalStrut(6));
        left.add(n3);
        left.add(Box.createVerticalStrut(6));
        left.add(n4);
        left.add(Box.createVerticalStrut(6));
        left.add(n5);
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

        String[] months = new String[]{
                "01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr",
                "05 - May", "06 - Jun", "07 - Jul", "08 - Aug",
                "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec"
        };
        JComboBox<String> monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        filterPanel.add(monthCombo);


        panel.add(filterPanel, BorderLayout.NORTH);

        // Center: list and editor pane in a split for a polished layout
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.33);
        split.setBorder(null);

        logList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logList.setModel(listModel);

        // Ensure single-selection model
        logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane listScroll = new JScrollPane(logList);
        listScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE6E9EB)),
                new EmptyBorder(6, 6, 6, 6)
        ));

        JPanel entryContainer = new JPanel(new BorderLayout());
        entryContainer.setBorder(new EmptyBorder(6, 6, 6, 6));
        entryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        entryArea.setLineWrap(true);
        entryArea.setWrapStyleWord(true);
        JScrollPane entryScroll = new JScrollPane(entryArea);
        entryScroll.setPreferredSize(new Dimension(600, 220));
        entryContainer.add(entryScroll, BorderLayout.CENTER);

        split.setLeftComponent(listScroll);
        split.setRightComponent(entryContainer);

        panel.add(split, BorderLayout.CENTER);

        //Add ctrl+s binding to update entry text
        entryArea.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "saveEntry");
        entryArea.getActionMap().put("saveEntry", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveEditedLogEntry();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newEntryGlobal");
        getRootPane().getActionMap().put("newEntryGlobal", createNewQuickEntry());


        // Add save button under the entry area
        JPanel entryBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        entryBottom.setOpaque(false);
        JButton saveEntryBtn = new AccentButton("Save Entry");
        saveEntryBtn.addActionListener(e -> saveEditedLogEntry());
        entryBottom.add(saveEntryBtn);
        entryContainer.add(entryBottom, BorderLayout.SOUTH);

        // Popup and listeners
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy Entry to Clipboard");
        copyItem.addActionListener(copyLogEntryTextToClipBoard());
        contextMenu.add(copyItem);
        logList.setComponentPopupMenu(contextMenu);
        JMenuItem deleteItem = new JMenuItem("Delete Entry");
        deleteItem.addActionListener(e -> deleteSelectedEntry());
        contextMenu.add(deleteItem);
        JMenuItem editDateTimeItem = new JMenuItem("Edit Date/Time");
        editDateTimeItem.addActionListener(e -> editDateTime());
        contextMenu.add(editDateTimeItem);
        // Selection handling: clicking or programmatic selection loads entry text
        logList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String selectedItem = logList.getSelectedValue();
                if (selectedItem != null) {
                    String logContent = logFileHandler.loadEntry(selectedItem);
                    entryArea.setText(logContent);
                }
            }
        });

        // Also respond to keyboard or programmatic selection changes
        logList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedItem = logList.getSelectedValue();
                if (selectedItem != null) {
                    String logContent = logFileHandler.loadEntry(selectedItem);
                    entryArea.setText(logContent);
                } else {
                    entryArea.setText("");
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
                    }
                } catch (Exception ex) {
                    logFileHandler.showErrorDialog("Error applying date filter: " + ex.getMessage());
                }
            }
        };
        yearCombo.addActionListener(applyFilterAction);
        monthCombo.addActionListener(applyFilterAction);

        // Ensure initial population selects first item if any
        SwingUtilities.invokeLater(() -> selectFirstLogIfAny());

        return panel;
    }

    private ActionListener copyLogEntryTextToClipBoard() {
        return e -> {
            String selectedItem = logList.getSelectedValue();
            if (selectedItem != null) {
                //Copy both timestamp and entry text
                String logContent = logFileHandler.loadEntry(selectedItem);
                String toCopy = selectedItem + "\n\n" + logContent;
                StringSelection stringSelection = new StringSelection(toCopy);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                //show a small popup message "Copied to clipboard"
                JOptionPane.showMessageDialog(
                        LogTextEditor.this,
                        "Log entry copied to clipboard.",
                        "Copied",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
    }

    public void quickEntry() {
        createNewQuickEntry().actionPerformed(null);
    }

    private AbstractAction createNewQuickEntry() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newEntry = JOptionPane.showInputDialog(
                        LogTextEditor.this,
                        "Enter new log entry:",
                        "New Log Entry",
                        JOptionPane.PLAIN_MESSAGE);
                if (newEntry != null && !newEntry.isBlank()) {
                    logFileHandler.saveText(newEntry, listModel);
                    updateLogListView();
                    loadFullLog(); // update full log view after save
                    SystemTrayMenu.updateRecentLogsMenu();
                }
            }
        };
    }

    private void saveEditedLogEntry() {
        String selectedItem = logList.getSelectedValue();
        if (selectedItem == null) return;

        logFileHandler.updateEntry(selectedItem, entryArea.getText());
        updateLogListView();
        logList.setSelectedValue(selectedItem, true);
        loadFullLog();
        SystemTrayMenu.updateRecentLogsMenu();
    }

    // Helper: choose and show first log if list has any entries
    private void selectFirstLogIfAny() {
        if (listModel.getSize() > 0) {
            // choose index 0 (first in model). If your model is sorted newest-first,
            // index 0 will be the newest; adjust if your model ordering differs.
            logList.setSelectedIndex(0);
            logList.ensureIndexIsVisible(0);
            String item = listModel.getElementAt(0);
            if (item != null) {
                String content = logFileHandler.loadEntry(item);
                entryArea.setText(content);
            }
        } else {
            // nothing to show
            logList.clearSelection();
            entryArea.setText("");
        }
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
        copyFullLogButton.addActionListener(e -> copyFullLogToClipboard());
        bottom.add(copyFullLogButton);
        panel.add(bottom, BorderLayout.SOUTH);

        openInNotepadButton.addActionListener(e -> openLogInNotepad());
        bottom.add(openInNotepadButton);

        //add getRightBottomPanel to bottom right, under the copy and refresh buttons
        JPanel rightBottomPanel = getRightBottomPanel();
        bottom.add(rightBottomPanel, 0); // add at index 0 to place it
        return panel;

    }

    private JPanel getRightBottomPanel() {
        JPanel rightBottomSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rightBottomSearchPanel.setOpaque(false);
        //Add label above search field, and then break
        JLabel searchLabel = new JLabel("Search:");
        rightBottomSearchPanel.add(searchLabel);
        //Add search form
        var searchField = new JTextField(15);
        //Enter in search field triggers search
        searchField.addActionListener(getActionListenerForSearchField(searchField));


        rightBottomSearchPanel.add(searchField);
        var searchBtn = new AccentButton("Find");
        rightBottomSearchPanel.add(searchBtn);

        searchBtn.addActionListener(getActionListenerForSearchField(searchField));

        JButton prevHighlightBtn = new AccentButton("<-");
        JButton nextHighlightBtn = new AccentButton("->");

        prevHighlightBtn.addActionListener(e -> navigateToHighlight(false));
        nextHighlightBtn.addActionListener(e -> navigateToHighlight(true));
        rightBottomSearchPanel.add(prevHighlightBtn);
        rightBottomSearchPanel.add(nextHighlightBtn);
        return rightBottomSearchPanel;
    }

    //open log in notepad
    private void openLogInNotepad() {
        Path logPath = findLogPath();
        if (logPath == null) {
            showLogNotFound();
            return;
        }
        try {
            new ProcessBuilder("notepad.exe", logPath.toAbsolutePath().toString()).start();
        } catch (IOException e) {
            logFileHandler.showErrorDialog("Error opening log in Notepad, will try to open in vim: " + e.getMessage());
            //try to open in vim on linux or mac
            var guessOs = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if (guessOs.contains("linux") || guessOs.contains("mac")) {
                try {
                    new ProcessBuilder("vim", logPath.toAbsolutePath().toString()).start();
                } catch (IOException ex) {
                    logFileHandler.showErrorDialog("Error opening log in vim: " + ex.getMessage());
                }
            }
        }
    }

    private ActionListener getActionListenerForSearchField(JTextField searchField) {
        return e -> {
            String query = searchField.getText();
            if (query != null && !query.isBlank()) {
                performSearchInFullLog(query);
            }
        };
    }

    private void navigateToHighlight(boolean b) {
        Highlighter highlighter = fullLogPane.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        if (highlights.length == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        int targetIndex = getTargetIndex(b, highlights);

        if (targetIndex != -1) {
            Highlighter.Highlight h = highlights[targetIndex];
            int start = h.getStartOffset();
            try {
                Rectangle rect = fullLogPane.modelToView2D(start).getBounds();
                rect.height = Math.max(rect.height, 20);
                fullLogPane.scrollRectToVisible(rect);
                fullLogPane.setCaretPosition(start);
            } catch (BadLocationException ex) {
                // ignore scrolling failure
            }
        }
    }

    private int getTargetIndex(boolean b, Highlighter.Highlight[] highlights) {
        int caretPos = fullLogPane.getCaretPosition();
        int targetIndex = -1;

        if (b) { // next
            for (int i = 0; i < highlights.length; i++) {
                if (highlights[i].getStartOffset() > caretPos) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1) targetIndex = 0; // wrap around
        } else { // previous
            for (int i = highlights.length - 1; i >= 0; i--) {
                if (highlights[i].getEndOffset() < caretPos) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1) targetIndex = highlights.length - 1; // wrap around
        }
        return targetIndex;
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
            //select top if any
            selectFirstLogIfAny();
            loadFullLog(); // update full log view after deletion
            SystemTrayMenu.updateRecentLogsMenu();
        }
    }

    private void editDateTime() {
        String selectedItem = logList.getSelectedValue();
        if (selectedItem == null) return;

        String newDateTime = JOptionPane.showInputDialog(this, "Enter new date and time (format: HH:mm yyyy-MM-dd):", selectedItem);
        if (newDateTime == null) return;
        if (newDateTime.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Date and time cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // validate
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");
            java.time.LocalDateTime.parse(newDateTime.trim(), formatter);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid format. Use HH:mm yyyy-MM-dd", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // update
        logFileHandler.changeTimestamp(selectedItem, newDateTime.trim());
        // reload list
        loadLogEntries();
        loadFullLog();
        SystemTrayMenu.updateRecentLogsMenu();
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
        SystemTrayMenu.updateRecentLogsMenu();
    }

    public void loadLogEntries() {
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
                SystemTrayMenu.updateRecentLogsMenu();
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
        //if in some other tab, first jump to Full Log tab, first check if not already there
        if (tabPane.getSelectedIndex() != 2) {
            tabPane.setSelectedIndex(2);
        }
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
    public void loadFullLog() {
        SwingUtilities.invokeLater(() -> {
            Path logPath = Path.of(System.getProperty("user.home"), "log.txt");
            if (!Files.exists(logPath)) {
                showLogNotFound();
                return;
            }

            clearEditorForNewLoad(logPath);

            try {
                List<String> lines = logFileHandler.getLines();
                MarkdownRenderer.renderMarkdown(fullLogPane, lines);
                MarkdownRenderer.addLinkListeners(fullLogPane);
            } catch (Exception ex) {
                fallbackReadRaw(logPath);
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
        if (SingleInstanceManager.isAnotherInstanceRunning()) {
            SingleInstanceManager.showAlreadyRunningDialog();
            System.exit(0);
        }

        SwingUtilities.invokeLater(() -> {
            LogTextEditor editor = new LogTextEditor();
            instance = editor;
            editor.setVisible(true);
        });
    }

    public void updateRecentLogsMenu(Menu recentLogsMenu) {
        //return 5 most recent log entries as menu items
        recentLogsMenu.removeAll();
        List<String> recentLogs = logFileHandler.getRecentLogEntries(10);
        checkIfWindowIsVisible();

        for (String logEntry : recentLogs) {
            MenuItem logItem = new MenuItem(logEntry);
            logItem.addActionListener(e -> {
                String logContent = logFileHandler.loadEntry(logEntry);
                entryArea.setText(logContent);
                tabPane.setSelectedIndex(1); // switch to Log Entries tab
                logList.setSelectedValue(logEntry, true); // select the log entry in the list
            });
            recentLogsMenu.add(logItem);
        }
    }

    private void checkIfWindowIsVisible() {
        //if LogTextEditor is not visible, make it visible when a recent log is clicked
        var isMinimized = (this.getExtendedState() & JFrame.ICONIFIED) == JFrame.ICONIFIED;
        if (!this.isVisible() || isMinimized) {
            this.setVisible(true);
            this.setExtendedState(JFrame.NORMAL);
            this.toFront();
        }
    }

    private void loadSettings() {
        if (java.nio.file.Files.exists(settingsPath)) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(settingsPath.toFile())) {
                settings.load(fis);
                String enc = settings.getProperty("encrypted");
                if ("true".equals(enc)) {
                    String saltStr = settings.getProperty("salt");
                    if (saltStr != null) {
                        byte[] salt = java.util.Base64.getDecoder().decode(saltStr);
                        char[] pwd = promptPassword();
                        if (pwd != null) {
                            logFileHandler.setEncryption(pwd, salt);
                            loadLogEntries();
                        } else {
                            System.exit(0);
                        }
                    }
                }
            } catch (Exception e) {
                logFileHandler.showErrorDialog("Error loading settings: " + e.getMessage());
            }
        }
    }

    private void saveSettings() {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(settingsPath.toFile())) {
            settings.store(fos, "LogHog settings");
        } catch (Exception e) {
            logFileHandler.showErrorDialog("Error saving settings: " + e.getMessage());
        }
    }

    private char[] promptPassword() {
        return PasswordDialog.showPasswordDialog(this, "Enter password");
    }


}
