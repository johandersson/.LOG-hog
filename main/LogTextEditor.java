/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package main;

import filehandling.LogFileHandler;
import gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

public class LogTextEditor extends JFrame {


    private final JList<String> logList = new JList<>();

    private LogFileHandler logFileHandler = new LogFileHandler();
    LogFileHandler getLogFileHandler() {
        return logFileHandler;
    }

    public FullLogPanel getFullLogPanel() {
        return fullLogPanel;
    }

    @Override
    public JRootPane getRootPane() {
        return super.getRootPane();
    }
    private final DefaultListModel<String> listModel = new DefaultListModel<>();

    private final java.util.List<NavItem> navItems = new ArrayList<>();
    AccentButton refreshButton = new AccentButton("Refresh");
    private final JTabbedPane tabPane = new JTabbedPane();

    private FullLogPanel fullLogPanel;

    private EntryPanel entryPanel;
    private LogListPanel logListPanel;
    private SettingsPanel settingsPanel;

    private static LogTextEditor instance;

    private final java.util.Properties settings = new java.util.Properties();
    private final java.nio.file.Path settingsPath = java.nio.file.Paths.get(System.getProperty("user.home"), "loghog_settings.properties");

    private Timer inactivityTimer;
    private int autoClearMinutes;
    private String passwordReminder = "";
    private boolean alwaysShowPassword = false;
    private boolean isLocked = false;

    public boolean isLocked() {
        return isLocked;
    }

    public LogTextEditor() {
        initUI();
        setupKeyBindings();
        loadSettings();
        initSystemTray();
        setupActivityListeners();
        setVisible(true);
    }

    private void initUI() {
        // Ensure the frame is decorated by the OS (native chrome)
        setUndecorated(false);

        setTitle(".LOG hog");
        setSize(1200, 660);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logFileHandler.clearSensitiveData();
            }
        });
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

        SwingUtilities.invokeLater(() -> entryPanel.getTextArea().requestFocusInWindow());
    }

    private void initSystemTray() {
        var systemTrayMenu = new SystemTrayMenu(this);
        SystemTrayMenu.initSystemTray();
    }

    private void setupActivityListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                resetInactivityTimer();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                resetInactivityTimer();
            }
        });
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
        tabPane.addTab("Entry", entryPanel = new EntryPanel(this));
        tabPane.addTab("Log Entries", logListPanel = new LogListPanel(this, logFileHandler, listModel, logList));
        tabPane.addTab("Full Log", fullLogPanel = new FullLogPanel(this, logFileHandler));
        tabPane.addTab("Settings", settingsPanel = new SettingsPanel(this, settings, settingsPath, logFileHandler));
        tabPane.addTab("Help", new InformationPanel(tabPane, "help.md", "Help"));
        tabPane.addTab("About", new InformationPanel(tabPane, "license.md", "About"));
        tabPane.addChangeListener(e -> {
            if (tabPane.getSelectedIndex() == 2) {
                fullLogPanel.loadFullLog();
            }
        });
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
            new gui.SplashScreen(); // modal, shows splash
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




    public ActionListener copyLogEntryTextToClipBoard() {
        return e -> {
            String selectedItem = logList.getSelectedValue();
            if (selectedItem != null) {
                //Copy both timestamp and entry text
                String logContent = logFileHandler.loadEntry(selectedItem);
                clipboard.ClipboardManager.copyLogEntryToClipboard(selectedItem, logContent, LogTextEditor.this);
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

    public AbstractAction createNewQuickEntry() {
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
                    try {
                        loadLogEntries();
                        fullLogPanel.loadFullLog();
                        SystemTrayMenu.updateRecentLogsMenu();
                    } catch (Exception ex) {
                        logFileHandler.showErrorDialog("Error refreshing data: " + ex.getMessage());
                    }
                }
            }
        };
    }

    public void saveEditedLogEntry() {
        if (isLocked) {
            JOptionPane.showMessageDialog(this, "File is locked. Press Unlock file in Full log view to unlock it again.", "Locked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String selectedItem = logList.getSelectedValue();
        if (selectedItem == null) return;

        logFileHandler.updateEntry(selectedItem, logListPanel.getEntryArea().getText());
        updateLogListView();
        logList.setSelectedValue(selectedItem, true);
        fullLogPanel.loadFullLog();
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
                logListPanel.getEntryArea().setText(content);
            }
        } else {
            // nothing to show
            logList.clearSelection();
            logListPanel.getEntryArea().setText("");
        }
    }









    public void deleteSelectedEntry() {
        if (isLocked) {
            JOptionPane.showMessageDialog(this, "File is locked. Press Unlock file in Full log view to unlock it again.", "Locked", JOptionPane.WARNING_MESSAGE);
            return;
        }
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
            fullLogPanel.loadFullLog(); // update full log view after deletion
            SystemTrayMenu.updateRecentLogsMenu();
        }
    }

    public void editDateTime() {
        if (isLocked) {
            JOptionPane.showMessageDialog(this, "File is locked. Press Unlock file in Full log view to unlock it again.", "Locked", JOptionPane.WARNING_MESSAGE);
            return;
        }
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
        logFileHandler.changeTimestamp(selectedItem, newDateTime.trim(), listModel);
        // reload full log and update menu
        fullLogPanel.loadFullLog();
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


    public void saveLogEntry() {
        if (isLocked) {
            JOptionPane.showMessageDialog(this, "File is locked. Press Unlock file in Full log view to unlock it again.", "Locked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        logFileHandler.saveText(entryPanel.getTextArea().getText(), listModel);
        entryPanel.getTextArea().setText("");
        updateLogListView();
        fullLogPanel.loadFullLog(); // update full log view after save
        SystemTrayMenu.updateRecentLogsMenu();
    }

    public void loadLogEntries() throws Exception {
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
                if (isLocked) {
                    JOptionPane.showMessageDialog(LogTextEditor.this, "File is locked. Press Unlock file in Full log view to unlock it again.", "Locked", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    loadLogEntries();
                    fullLogPanel.loadFullLog();
                    SystemTrayMenu.updateRecentLogsMenu();
                } catch (Exception ex) {
                    logFileHandler.showErrorDialog("Error loading data: " + ex.getMessage());
                }
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
        fullLogPanel.performSearchInFullLog(query);
    }








    public static void main(String[] args) {
        if (SingleInstanceManager.isAnotherInstanceRunning()) {
            SingleInstanceManager.notifyExistingInstance();
            System.exit(0);
        }

        SwingUtilities.invokeLater(() -> {
            LogTextEditor editor = new LogTextEditor();
            instance = editor;
            editor.setVisible(true);
            editor.startSingleInstanceListener();
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
                logListPanel.getEntryArea().setText(logContent);
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

    private void startSingleInstanceListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (true) {
                    java.net.Socket clientSocket = SingleInstanceManager.getServerSocket().accept();
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));
                    String message = in.readLine();
                    if ("BRING_TO_FRONT".equals(message)) {
                        SwingUtilities.invokeLater(() -> {
                            checkIfWindowIsVisible();
                            this.toFront();
                            this.requestFocus();
                        });
                    }
                    in.close();
                    clientSocket.close();
                }
            } catch (java.io.IOException e) {
                // Socket closed, which is expected when the application exits
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void loadSettings() {
        if (java.nio.file.Files.exists(settingsPath)) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(settingsPath.toFile())) {
                settings.load(fis);
                String backupDir = settings.getProperty("backupDirectory", "");
                logFileHandler.setBackupDirectory(backupDir);
                String enc = settings.getProperty("encrypted");
                String autoClearStr = settings.getProperty("autoClearMinutes", "30");
                autoClearMinutes = Integer.parseInt(autoClearStr);
                passwordReminder = settings.getProperty("passwordReminder", "");
                alwaysShowPassword = "true".equals(settings.getProperty("alwaysShowPassword", "false"));
                boolean dataLoaded = false;
                if ("true".equals(enc)) {
                    handleEncryptionSetup();
                    dataLoaded = true;
                }
                if (!dataLoaded) {
                    try {
                        loadLogEntries();
                        fullLogPanel.loadFullLog();
                    } catch (Exception e) {
                        logFileHandler.showErrorDialog("Error loading data: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                logFileHandler.showErrorDialog("Error loading settings: " + e.getMessage());
            }
        } else {
            try {
                loadLogEntries();
                fullLogPanel.loadFullLog();
            } catch (Exception e) {
                logFileHandler.showErrorDialog("Error loading data: " + e.getMessage());
            }
        }
    }

    private void handleEncryptionSetup() {
        String saltStr = settings.getProperty("salt");
        if (saltStr != null) {
            byte[] salt = java.util.Base64.getDecoder().decode(saltStr);
            boolean success = false;
            int attempts = 0;
            while (!success) {
                attempts++;
                if (attempts > 3) {
                    JOptionPane.showMessageDialog(this, "Too many failed attempts. Exiting for security.", "Security Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
                PasswordDialog.PasswordResult result = PasswordDialog.showPasswordDialog(this, "🔒 Unlock you secret .LOG!", passwordReminder, alwaysShowPassword);
                char[] pwd = result.password;
                if (pwd == null) {
                    System.exit(0);
                }
                if (result.alwaysShow) {
                    settings.setProperty("alwaysShowPassword", "true");
                    saveSettings();
                }
                logFileHandler.setEncryption(pwd, salt);
                try {
                    loadLogEntries();
                    success = true;
                } catch (Exception e) {
                    System.out.println("Decryption failed: " + e.getMessage());
                    if (e.getMessage().contains("Tag mismatch")) {
                        JOptionPane.showMessageDialog(this, "Incorrect password. Please try again.", "Password Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        logFileHandler.showErrorDialog("Error loading log entries: " + e.getMessage());
                        System.exit(0);
                    }
                }
            }
            if (autoClearMinutes > 0) {
                startInactivityTimer();
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

    private void startInactivityTimer() {
        inactivityTimer = new Timer(autoClearMinutes * 60 * 1000, e -> {
            performAutoLock("Auto-clear activated due to " + autoClearMinutes + " minutes of inactivity.\n" +
                "The log file has been unloaded for security.\n\n" +
                "Do you want to stay and reload the encrypted log, or exit the program?");
        });
        inactivityTimer.start();
    }

    public void manualLock() {
        logFileHandler.clearSensitiveData();
        // Clear UI
        listModel.clear();
        fullLogPanel.loadFullLog(); // This will show empty since locked
        isLocked = true;
        updateUILockState();
    }

    public void manualUnlock() {
        reloadEncryptedLog();
    }

    private void performAutoLock(String message) {
        logFileHandler.clearSensitiveData();
        // Clear UI
        listModel.clear();
        fullLogPanel.loadFullLog(); // This will show encrypted or empty
        isLocked = true;
        updateUILockState();

        int choice = JOptionPane.showOptionDialog(this,
            message,
            "Security Notice",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new String[]{"Stay and reload file", "Exit program"},
            "Stay and reload file");

        if (choice == 0) { // Stay and reload
            reloadEncryptedLog();
        } else { // Exit or closed
            System.exit(0);
        }
    }

    private void reloadEncryptedLog() {
        boolean success = false;
        while (!success) {
            PasswordDialog.PasswordResult result = PasswordDialog.showPasswordDialog(this, "Reload Encrypted Log", settings.getProperty("passwordReminder", ""), settings.getProperty("alwaysShowPassword", "false").equals("true"));
            char[] pwd = result.password;
            if (pwd == null) {
                // User cancelled, exit
                System.exit(0);
                return;
            }
            byte[] salt = Base64.getDecoder().decode(settings.getProperty("salt"));
            if (result.alwaysShow) {
                settings.setProperty("alwaysShowPassword", "true");
                saveSettings();
            }
            logFileHandler.setEncryption(pwd, salt);
            try {
                loadLogEntries();
                success = true;
                isLocked = false;
                updateUILockState();
                if (autoClearMinutes > 0) {
                    startInactivityTimer();
                }
            } catch (Exception e) {
                System.out.println("Decryption failed: " + e.getMessage());
                if (e.getMessage().contains("Tag mismatch")) {
                    JOptionPane.showMessageDialog(this, "Incorrect password. Please try again.", "Password Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    logFileHandler.showErrorDialog("Error loading log entries: " + e.getMessage());
                    System.exit(0);
                }
            }
        }
    }

    private void updateUILockState() {
        entryPanel.setLocked(isLocked);
        logListPanel.setLocked(isLocked);
        fullLogPanel.updateLockButton();
        // Also disable logListPanel if needed, but since listModel is cleared, maybe not necessary
    }

    private void resetInactivityTimer() {
        if (inactivityTimer != null) {
            inactivityTimer.restart();
        }
    }

}
