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
import java.util.*;
import javax.swing.*;

public class LogTextEditor extends JFrame {


    private final JList<String> logList = new JList<>();

    private LogFileHandler logFileHandler = new LogFileHandler();
    LogFileHandler getLogFileHandler() {
        return logFileHandler;
    }

    public FullLogPanel getFullLogPanel() {
        return fullLogPanel;
    }

    public EntryPanel getEntryPanel() {
        return entryPanel;
    }

    public LogListPanel getLogListPanel() {
        return logListPanel;
    }

    public SettingsPanel getSettingsPanel() {
        return settingsPanel;
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

    private String passwordReminder = "";
    private boolean isLocked = false;

    public boolean isLocked() {
        return isLocked;
    }

    private UIInitializer uiInitializer;
    private ActionHandler actionHandler;
    private SystemInitializer systemInitializer;

    public LogTextEditor() {
        // Initialize action handler first (needed by components)
        actionHandler = new ActionHandler(this, logFileHandler, logList, listModel);

        // Initialize components (they may depend on actionHandler)
        initializeComponents();

        // Set panels on action handler
        actionHandler.setPanels(logListPanel, fullLogPanel);

        // Initialize UI using extracted class
        uiInitializer = new UIInitializer(this, tabPane, navItems);
        uiInitializer.initializeUI();

        // Setup key bindings and system components
        setupKeyBindings();
        loadSettings();

        systemInitializer = new SystemInitializer(this);
        systemInitializer.initializeSystemComponents();

        setVisible(true);
    }

    private void initializeComponents() {
        // Initialize panels
        entryPanel = new EntryPanel(this);
        logListPanel = new LogListPanel(this, logFileHandler, listModel, logList);
        fullLogPanel = new FullLogPanel(this, logFileHandler);
        settingsPanel = new SettingsPanel(this, settings, settingsPath, logFileHandler);
    }

    public ActionListener copyLogEntryTextToClipBoard() {
        return actionHandler.createCopyLogEntryAction();
    }

    public void quickEntry() {
        actionHandler.createNewQuickEntryAction().actionPerformed(null);
    }

    public AbstractAction createNewQuickEntry() {
        return (AbstractAction) actionHandler.createNewQuickEntryAction();
    }

    public void saveEditedLogEntry() {
        actionHandler.saveEditedLogEntry();
    }

    // Helper: choose and show first log if list has any entries
    public void selectFirstLogIfAny() {
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
        actionHandler.deleteSelectedEntry();
    }

    public void editDateTime() {
        actionHandler.editDateTime();
    }

    public void saveLogEntry() {
        actionHandler.saveLogEntry();
    }

    public void loadLogEntries() throws Exception {
        logFileHandler.loadLogEntries(listModel);
        updateLogListView();
    }

    public void updateLogListView() {
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
        var recentLogs = logFileHandler.getRecentLogEntries(10);
        checkIfWindowIsVisible();

        for (var logEntry : recentLogs) {
            var logItem = new MenuItem(logEntry);
            logItem.addActionListener(e -> {
                var logContent = logFileHandler.loadEntry(logEntry);
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
        var listenerThread = new Thread(() -> {
            try {
                while (true) {
                    var clientSocket = SingleInstanceManager.getServerSocket().accept();
                    var in = new java.io.BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));
                    var message = in.readLine();
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
            try (var fis = new java.io.FileInputStream(settingsPath.toFile())) {
                settings.load(fis);
                settingsPanel.loadCurrentSettings();
                var backupDir = settings.getProperty("backupDirectory", "");
                logFileHandler.setBackupDirectory(backupDir);
                var enc = settings.getProperty("encrypted");
                passwordReminder = settings.getProperty("passwordReminder", "");
                var dataLoaded = false;
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
                PasswordDialog.PasswordResult result = PasswordDialog.showPasswordDialog(this, "🔒 Unlock you secret .LOG!", passwordReminder);
                char[] pwd = result.password;
                if (pwd == null) {
                    System.exit(0);
                }
                logFileHandler.setEncryption(pwd, salt);
                try {
                    loadLogEntries();
                    success = true;
                } catch (Exception e) {
                    String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (errorMsg.contains("tag mismatch") || 
                        errorMsg.contains("bad tag") ||
                        errorMsg.contains("badpadding") || 
                        errorMsg.contains("illegal block size") ||
                        errorMsg.contains("aeadbadtag") ||
                        errorMsg.contains("integrity check failed") ||
                        errorMsg.contains("mac check failed") ||
                        errorMsg.contains("decryption failed")) {
                        JOptionPane.showMessageDialog(this, "Incorrect password. Please try again.", "Password Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        logFileHandler.showErrorDialog("Error loading log entries: " + e.getMessage());
                        System.exit(0);
                    }
                }
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

    private void reloadEncryptedLog() {
        boolean success = false;
        while (!success) {
            PasswordDialog.PasswordResult result = PasswordDialog.showPasswordDialog(this, "Reload Encrypted Log", settings.getProperty("passwordReminder", ""));
            char[] pwd = result.password;
            if (pwd == null) {
                // User cancelled, exit
                System.exit(0);
                return;
            }
            byte[] salt = Base64.getDecoder().decode(settings.getProperty("salt"));
            logFileHandler.setEncryption(pwd, salt);
            try {
                loadLogEntries();
                success = true;
                isLocked = false;
                updateUILockState();
                fullLogPanel.loadFullLog(); // Refresh full log view after successful decryption
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (errorMsg.contains("tag mismatch") || 
                    errorMsg.contains("bad tag") ||
                    errorMsg.contains("badpadding") || 
                    errorMsg.contains("illegal block size") ||
                    errorMsg.contains("aeadbadtag") ||
                    errorMsg.contains("integrity check failed") ||
                    errorMsg.contains("mac check failed")) {
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

    public void updatePasswordReminder(String reminder) {
        this.passwordReminder = reminder;
    }

}
