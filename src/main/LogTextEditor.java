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

import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import filehandling.LogFileHandler;
import gui.AccentButton;
import gui.EntryPanel;
import gui.FullLogPanel;
import gui.LogListPanel;
import gui.NavItem;
import gui.SettingsPanel;
import gui.SystemTrayMenu;

public class LogTextEditor extends JFrame {

    private final Application application;
    private final JList<String> logList = new JList<>();

    // For backward compatibility - delegate to application
    private LogFileHandler logFileHandler;
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

    public JTabbedPane getTabPane() {
        return tabPane;
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

    // Secure settings for sensitive data
    private final SecureSettings secureSettings;

    private String passwordReminder = "";
    private boolean isLocked = false;
    private final Object lockObject = new Object();
    private BackupManager backupManager;
    private javax.swing.Timer periodicBackupTimer;

    public boolean isLocked() {
        synchronized (lockObject) {
            return isLocked;
        }
    }

    public void setLocked(boolean locked) {
        synchronized (lockObject) {
            this.isLocked = locked;
            updateUILockState();
        }
    }

    private UIInitializer uiInitializer;
    private ActionHandler actionHandler;
    private SystemInitializer systemInitializer;
    private EncryptionHandler encryptionHandler;

    public LogTextEditor() {
        // Initialize application with services
        java.nio.file.Path logFilePath = java.nio.file.Paths.get(System.getProperty("user.home"), "log.txt");
        application = new Application(logFilePath);

        // For backward compatibility
        logFileHandler = (LogFileHandler) application.getLogFileOperations();

        // Initialize secure settings
        secureSettings = new SecureSettings();
        
        // Initialize backup manager
        backupManager = new BackupManager(settings);
        logFileHandler.setBackupManager(backupManager);
        
        try {
            // Initialize action handler first (needed by components)
            actionHandler = new ActionHandler(this, logFileHandler, logList, listModel);

            // Initialize components (they may depend on actionHandler)
            initializeComponents();

            // Set panels on action handler
            actionHandler.setPanels(logListPanel, fullLogPanel);

            // Initialize UI using extracted class
            uiInitializer = new UIInitializer(this, tabPane, navItems, settings);
            uiInitializer.initializeUI();

            // Initialize system components (system tray, etc.)
            systemInitializer = new SystemInitializer(this);
            systemInitializer.initializeSystemComponents();

            // Initialize encryption handler before loading settings
            encryptionHandler = new EncryptionHandler(this, logFileHandler, settings, secureSettings,
                () -> {
                    try {
                        loadLogEntries();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                this::updateUILockState,
                () -> {
                    try {
                        fullLogPanel.loadFullLog();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

            // Setup key bindings and system components
            setupKeyBindings();
            loadSettings();

            // Update system tray recent logs menu now that settings are loaded
            SystemTrayMenu.updateRecentLogsMenu();
            
            // Perform startup backup to protect against crashes
            backupManager.performStartupBackup();
            
            // Start periodic backup timer (checks every minute for changes)
            startPeriodicBackupTimer();

            setVisible(true);

            // Focus the entry text area on startup
            SwingUtilities.invokeLater(() -> entryPanel.getTextArea().requestFocusInWindow());

        } catch (Exception e) {
            System.err.println("Error in LogTextEditor constructor: " + e.getMessage());
            throw e;
        }
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
    private void loadAndDisplayEntry(String timestamp) {
        if (timestamp != null && !timestamp.trim().isEmpty()) {
            String content = logFileHandler.loadEntry(timestamp);
            logListPanel.getEntryArea().setText(content);
        } else {
            logListPanel.getEntryArea().setText("");
        }
    }

    public void selectFirstLogIfAny() {
        if (listModel.getSize() > 0) {
            // choose index 0 (first in model). If your model is sorted newest-first,
            // index 0 will be the newest; adjust if your model ordering differs.
            logList.setSelectedIndex(0);
            logList.ensureIndexIsVisible(0);
            String item = listModel.getElementAt(0);
            loadAndDisplayEntry(item);
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

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "load");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "find"); // Ctrl+F

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
                    logFileHandler.showErrorDialog("<html><b>🔄 Reload Failed</b><br><br>Unable to reload log data.<br>" + ex.getMessage() + "<br><br><i>Tip: Check the log file and try again.</i></html>");
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
        fullLogPanel.openSearchDialog();
    }








    public static void main(String[] args) {
        
        // Check if running in headless environment
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.err.println("LogHog is a GUI application and cannot run in a headless environment.");
            System.err.println("Please run LogHog in a desktop environment with display capabilities.");
            System.exit(1);
        }
        
        // Set up look and feel (same as LogHog.main)
        try {
            for (var info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        // Let the OS draw the title bar and buttons (native chrome)
        JFrame.setDefaultLookAndFeelDecorated(false);
        
        if (SingleInstanceManager.isAnotherInstanceRunning()) {
            SingleInstanceManager.showAlreadyRunningDialog();
            SingleInstanceManager.notifyExistingInstance();
            System.exit(0);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                LogTextEditor editor = new LogTextEditor();
                editor.setVisible(true);
                editor.startSingleInstanceListener();
            } catch (Exception e) {
                System.err.println("Error during GUI initialization: " + e.getClass().getSimpleName());
                System.exit(1);
            }
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
                loadAndDisplayEntry(logEntry);
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
                    } else if ("LOGHOG_PING".equals(message)) {
                        var out = new java.io.PrintWriter(clientSocket.getOutputStream(), true);
                        out.println("LOGHOG_PONG");
                        out.close();
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
                // Show splash screen on startup if enabled
                if ("true".equals(settings.getProperty("showSplashOnStartup", "true"))) {
                    new gui.SplashScreen().setVisible(true);
                }
                var backupDir = settings.getProperty("backupDirectory", "");
                logFileHandler.setBackupDirectory(backupDir);
                var enc = settings.getProperty("encrypted");
                passwordReminder = secureSettings.getDecryptedProperty(settings, "passwordReminder", "");
                var dataLoaded = false;
                if ("true".equals(enc)) {
                    encryptionHandler.handleEncryptionSetup();
                    dataLoaded = true;
                }
                if (!dataLoaded) {
                    try {
                        loadLogEntries();
                        fullLogPanel.loadFullLog();
                    } catch (Exception e) {
                        logFileHandler.showErrorDialog("<html><b>📂 Load Failed</b><br><br>Unable to load log data.<br>" + e.getMessage() + "<br><br><i>Tip: The file may be missing or corrupted.</i></html>");
                    }
                }
            } catch (Exception e) {
                logFileHandler.showErrorDialog("<html><b>⚙️ Settings Load Failed</b><br><br>Unable to load application settings.<br>" + e.getMessage() + "<br><br><i>Tip: Settings will use defaults.</i></html>");
            }
        } else {
            try {
                loadLogEntries();
                fullLogPanel.loadFullLog();
            } catch (Exception e) {
                logFileHandler.showErrorDialog("<html><b>📂 Load Failed</b><br><br>Unable to load log data.<br>" + e.getMessage() + "<br><br><i>Tip: The file may be missing or corrupted.</i></html>");
            }
        }
    }

    private void saveSettings() {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(settingsPath.toFile())) {
            settings.store(fos, "LogHog settings");
        } catch (Exception e) {
            logFileHandler.showErrorDialog("<html><b>💾 Settings Save Failed</b><br><br>Unable to save application settings.<br>" + e.getMessage() + "<br><br><i>Tip: Settings may not persist between sessions.</i></html>");
        }
    }

    public void manualLock() {
        synchronized (lockObject) {
            logFileHandler.clearSensitiveData();
            // Clear UI
            listModel.clear();
            fullLogPanel.loadFullLog(); // This will show empty since locked
            isLocked = true;
            updateUILockState();
        }
    }

    public void manualUnlock() {
        boolean success = encryptionHandler.reloadEncryptedLog();
        if (success) {
            synchronized (lockObject) {
                isLocked = false;
                updateUILockState();
            }
        } else {
            // Show feedback that file is still locked
            JOptionPane.showMessageDialog(this, 
                "<html><b>🔒 File Still Locked</b><br><br>" +
                "The file remains locked because the unlock operation was cancelled or failed.<br><br>" +
                "<i>You can try again by clicking the Unlock button.</i></html>", 
                "File Locked", 
                JOptionPane.WARNING_MESSAGE);
        }
    }    
    /**
     * Starts a timer that periodically checks if backup is needed.
     * Runs every 60 seconds with minimal overhead.
     */
    private void startPeriodicBackupTimer() {
        periodicBackupTimer = new javax.swing.Timer(60000, e -> {
            // Run backup check in background to avoid blocking UI
            new Thread(() -> backupManager.checkPeriodicBackup()).start();
        });
        periodicBackupTimer.start();
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

    private void initializeSecureClipboard() {
        // Initialize secure clipboard settings from saved preferences
        boolean autoClear = "true".equals(settings.getProperty("clipboardAutoClear", "true"));
        int timeout = Integer.parseInt(settings.getProperty("clipboardTimeout", "30"));

        clipboard.SecureClipboardManager.setAutoClearEnabled(autoClear);
        try {
            clipboard.SecureClipboardManager.setTimeoutSeconds(timeout);
        } catch (IllegalArgumentException e) {
            // If saved timeout is invalid, use default
            try {
                clipboard.SecureClipboardManager.setTimeoutSeconds(30);
            } catch (IllegalArgumentException e2) {
                // This should never happen with hardcoded 30
            }
        }

        // Cleanup is now handled directly in UIInitializer before System.exit(0)
        // No shutdown hook needed
    }



}
