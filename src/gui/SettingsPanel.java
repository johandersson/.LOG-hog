/*
 * Copyright (C) 2026 Johan Andersson
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

package gui;

import utils.Log;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import filehandling.LogFileHandler;
import main.BackupManager;
import main.LogTextEditor;
import utils.Toast;
import main.SecureDeletionUtils;

public final class SettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String VALUE_TRUE = "true";
    private static final String VALUE_FALSE = "false";
    private static final String KEY_ENCRYPTED = "encrypted";
    private static final String KEY_SALT = "salt";
    private static final String KEY_BACKUP_DIRECTORY = "backupDirectory";
    private static final String KEY_AUTO_BACKUP_ENABLED = "autoBackupEnabled";
    private static final String KEY_SHOW_SPLASH = "showSplashOnStartup";
    private static final String KEY_CLIPBOARD_AUTO_CLEAR = "clipboardAutoClear";
    private static final String KEY_CLIPBOARD_TIMEOUT = "clipboardTimeout";
    private static final String KEY_AUTO_LOCK_ENABLED = "autoLockEnabled";
    private static final String KEY_AUTO_LOCK_TIMEOUT = "autoLockTimeout";
    private static final String BACKUP_EXTENSION = ".bak";
    private static final String OS_WINDOWS = "windows";
    private static final String FONT_UI = "Segoe UI";
    // Removed unused private method rotateEncryptionKey (PMD)
    private final LogTextEditor editor;
    private final Properties settings;
    private final Path settingsPath;
    private final LogFileHandler logFileHandler;
    private final BackupManager backupManager;

    private JCheckBox encryptionCheckBox;
    private JButton applyButton;
    private JLabel statusLabel;
    private JTextField backupDirField;
    // Removed unused private field browseBackupButton (PMD)
    private JCheckBox splashOnStartupCheckBox;
    private JCheckBox clipboardAutoClearCheckBox;
    private JTextField clipboardTimeoutField;
    private JCheckBox autoBackupCheckBox;
    private JCheckBox autoLockCheckBox;
    private JSpinner autoLockTimeoutSpinner;

    public SettingsPanel(LogTextEditor editor, Properties settings, Path settingsPath, LogFileHandler logFileHandler) {
        this.editor = editor;
        this.settings = settings;
        this.settingsPath = settingsPath;
        this.logFileHandler = logFileHandler;
        this.backupManager = new BackupManager(settings);
        this.backupManager.setParentFrame(editor); // Set parent for progress dialogs

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        var contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contentPanel.setBackground(Color.WHITE);

        // Encryption section
        contentPanel.add(createEncryptionPanel());

        // Encrypted-only policy: decryption UI is intentionally removed.

        // Backup section
        contentPanel.add(createBackupPanel());

        // Backup directory section
        contentPanel.add(createBackupDirPanel());

        // Auto-backup section
        contentPanel.add(createAutoBackupPanel());

        // Splash screen section
        contentPanel.add(createSplashPanel());

        // Clipboard security section
        contentPanel.add(createClipboardSecurityPanel());

        // Auto-lock section
        contentPanel.add(createAutoLockPanel());

        // Button section
        contentPanel.add(createButtonPanel());

        // Status section
        contentPanel.add(createStatusPanel());

        // Wrap contentPanel in a JScrollPane for scrollable content
        var scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        loadCurrentSettings();
    }

    private JPanel createEncryptionPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Encryption"));

        encryptionCheckBox = new JCheckBox("Encryption is required (always enabled)");
        encryptionCheckBox.setBackground(Color.WHITE);
        encryptionCheckBox.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        encryptionCheckBox.setSelected(true);
        encryptionCheckBox.setEnabled(false);

        panel.add(encryptionCheckBox);
        return panel;
    }

    private JPanel createBackupPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);

        var backupButton = new StandardButton("Backup Log File", new Color(0xE0E0E0), new Color(0xB0B0B0));
        backupButton.addActionListener(e -> backupLogFile());

        panel.add(backupButton);
        return panel;
    }

    private JPanel createBackupDirPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Backup Directory"));

        var backupDirLabel = new JLabel("Default backup directory: ");
        backupDirLabel.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        backupDirField = new JTextField(20);
        backupDirField.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        var browseBackupButton = new StandardButton("Browse...", new Color(0xE0E0E0), new Color(0xB0B0B0));
        browseBackupButton.addActionListener(e -> browseBackupDirectory());

        panel.add(backupDirLabel);
        panel.add(backupDirField);
        panel.add(browseBackupButton);
        return panel;
    }

    private JPanel createAutoBackupPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Automatic Backup"));

        autoBackupCheckBox = new JCheckBox("Enable automatic periodic backup (every 30 minutes when file changes)");
        autoBackupCheckBox.setBackground(Color.WHITE);
        autoBackupCheckBox.setFont(new Font(FONT_UI, Font.PLAIN, 13));

        panel.add(autoBackupCheckBox);
        return panel;
    }

    private JPanel createSplashPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Splash Screen"));

        splashOnStartupCheckBox = new JCheckBox("Show splash screen on startup");
        splashOnStartupCheckBox.setBackground(Color.WHITE);
        splashOnStartupCheckBox.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        splashOnStartupCheckBox.setSelected(VALUE_TRUE.equals(settings.getProperty(KEY_SHOW_SPLASH, VALUE_TRUE)));

        panel.add(splashOnStartupCheckBox);
        return panel;
    }

    private JPanel createClipboardSecurityPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Clipboard Security"));

        clipboardAutoClearCheckBox = new JCheckBox("Auto-clear clipboard after copying");
        clipboardAutoClearCheckBox.setBackground(Color.WHITE);
        clipboardAutoClearCheckBox.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        clipboardAutoClearCheckBox.setSelected(VALUE_TRUE.equals(settings.getProperty(KEY_CLIPBOARD_AUTO_CLEAR, VALUE_TRUE)));

        var timeoutLabel = new JLabel("Timeout (seconds): ");
        timeoutLabel.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        clipboardTimeoutField = new JTextField(5);
        clipboardTimeoutField.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        clipboardTimeoutField.setText(settings.getProperty(KEY_CLIPBOARD_TIMEOUT, "30"));

        panel.add(clipboardAutoClearCheckBox);
        panel.add(timeoutLabel);
        panel.add(clipboardTimeoutField);
        return panel;
    }

    private JPanel createAutoLockPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Auto-Lock File"));

        autoLockCheckBox = new JCheckBox("Lock file after inactivity");
        autoLockCheckBox.setBackground(Color.WHITE);
        autoLockCheckBox.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        autoLockCheckBox.setSelected(VALUE_TRUE.equals(settings.getProperty(KEY_AUTO_LOCK_ENABLED, VALUE_FALSE)));

        var timeoutLabel = new JLabel("Timeout (minutes): ");
        timeoutLabel.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        
        // Get timeout in seconds and convert to minutes, default is 15 minutes (900 seconds)
        int timeoutSeconds = Integer.parseInt(settings.getProperty(KEY_AUTO_LOCK_TIMEOUT, "900"));
        int timeoutMinutes = timeoutSeconds / 60;
        
        // Spinner: min=15, max=1440 (24 hours), step=5, initial=timeoutMinutes
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
            Math.max(15, Math.min(1440, timeoutMinutes)), // value (clamped)
            15,    // min: 15 minutes
            1440,  // max: 24 hours
            5      // step: 5 minutes
        );
        autoLockTimeoutSpinner = new JSpinner(spinnerModel);
        autoLockTimeoutSpinner.setFont(new Font(FONT_UI, Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) autoLockTimeoutSpinner.getEditor()).getTextField().setColumns(5);

        panel.add(autoLockCheckBox);
        panel.add(timeoutLabel);
        panel.add(autoLockTimeoutSpinner);
        return panel;
    }

    private JPanel createButtonPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);

        applyButton = new AccentButton("Apply Changes");
        applyButton.addActionListener(e -> applySettings());

        var generatorButton = new StandardButton("Password Generator", new Color(0xE0E0E0), new Color(0xB0B0B0));
        generatorButton.addActionListener(e -> PasswordGeneratorDialog.showDialog(editor));

        panel.add(applyButton);
        panel.add(generatorButton);
        return panel;
    }

    private JPanel createStatusPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);

        statusLabel = new JLabel("");
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setFont(new Font(FONT_UI, Font.PLAIN, 13));

        panel.add(statusLabel);
        return panel;
    }

    public void loadCurrentSettings() {
        backupDirField.setText(settings.getProperty(KEY_BACKUP_DIRECTORY, ""));
        autoBackupCheckBox.setSelected(VALUE_TRUE.equals(settings.getProperty(KEY_AUTO_BACKUP_ENABLED, VALUE_FALSE)));
        splashOnStartupCheckBox.setSelected(VALUE_TRUE.equals(settings.getProperty(KEY_SHOW_SPLASH, VALUE_TRUE)));
        clipboardAutoClearCheckBox.setSelected(VALUE_TRUE.equals(settings.getProperty(KEY_CLIPBOARD_AUTO_CLEAR, VALUE_TRUE)));
        clipboardTimeoutField.setText(settings.getProperty(KEY_CLIPBOARD_TIMEOUT, "30"));
        autoLockCheckBox.setSelected(VALUE_TRUE.equals(settings.getProperty(KEY_AUTO_LOCK_ENABLED, VALUE_FALSE)));
        
        // Load auto-lock timeout in minutes
        int timeoutSeconds = Integer.parseInt(settings.getProperty(KEY_AUTO_LOCK_TIMEOUT, "900"));
        int timeoutMinutes = timeoutSeconds / 60;
        
        // Check if timeout is outside valid range and alert user
        if (timeoutMinutes < 15 || timeoutMinutes > 1440) {
            int clampedMinutes = Math.max(15, Math.min(1440, timeoutMinutes));
            gui.DialogHelper.showWarning(editor, "Settings Validation", "Auto-Lock Timeout Adjusted",
                "The saved auto-lock timeout (" + timeoutMinutes + " minutes) is outside the valid range.<br>" +
                "Valid range: 15-1440 minutes (15 minutes to 24 hours).<br><br>" +
                "Timeout has been adjusted to: " + clampedMinutes + " minutes.");
            timeoutMinutes = clampedMinutes;
        }
        autoLockTimeoutSpinner.setValue(timeoutMinutes);
        
        encryptionCheckBox.setSelected(true);
        encryptionCheckBox.setEnabled(false);
    }

    private void applySettings() {
        var currentEnc = settings.getProperty(KEY_ENCRYPTED);

        // Encrypted-only policy: always require encryption and bootstrap if not enabled yet.
        if (!VALUE_TRUE.equals(currentEnc)) {
            enableEncryption();
            return;
        }

        // Check if any settings actually changed
        var currentBackupDir = settings.getProperty(KEY_BACKUP_DIRECTORY, "");
        var currentSplashOnStartup = VALUE_TRUE.equals(settings.getProperty(KEY_SHOW_SPLASH, VALUE_TRUE));
        var currentClipboardAutoClear = VALUE_TRUE.equals(settings.getProperty(KEY_CLIPBOARD_AUTO_CLEAR, VALUE_TRUE));
        var currentClipboardTimeout = settings.getProperty(KEY_CLIPBOARD_TIMEOUT, "30");
        var currentAutoLockEnabled = VALUE_TRUE.equals(settings.getProperty(KEY_AUTO_LOCK_ENABLED, VALUE_FALSE));
        int currentAutoLockTimeoutSeconds = Integer.parseInt(settings.getProperty(KEY_AUTO_LOCK_TIMEOUT, "900"));
        var newBackupDir = backupDirField.getText();
        var newAutoBackupEnabled = autoBackupCheckBox.isSelected();
        var newSplashOnStartup = splashOnStartupCheckBox.isSelected();
        var newClipboardAutoClear = clipboardAutoClearCheckBox.isSelected();
        var newClipboardTimeout = clipboardTimeoutField.getText();
        var newAutoLockEnabled = autoLockCheckBox.isSelected();
        
        // Get spinner value (in minutes) and convert to seconds, with validation
        int newAutoLockMinutes = (Integer) autoLockTimeoutSpinner.getValue();
        // Clamp to valid range (15-1440 minutes) and convert to seconds
        newAutoLockMinutes = Math.max(15, Math.min(1440, newAutoLockMinutes));
        int newAutoLockTimeoutSeconds = newAutoLockMinutes * 60;

        // Validate clipboard timeout
        if (!isValidClipboardTimeout(newClipboardTimeout)) {
            gui.DialogHelper.showError(editor, "Invalid Input", "Clipboard timeout must be a number between 5 and 30 seconds.");
            loadCurrentSettings(); // Reset to current valid values
            return;
        }

        // Auto-lock timeout is already validated by spinner bounds (15-1440 minutes)

        // Validate backup directory
        if (!isValidBackupDirectory(newBackupDir)) {
            gui.DialogHelper.showError(editor, "Invalid Input", "Backup directory path is invalid or contains unsafe characters.");
            loadCurrentSettings(); // Reset to current valid values
            return;
        }

        var currentAutoBackupEnabled = VALUE_TRUE.equals(settings.getProperty(KEY_AUTO_BACKUP_ENABLED, VALUE_FALSE));

        var settingsChanged = !currentBackupDir.equals(newBackupDir) ||
                            currentAutoBackupEnabled != newAutoBackupEnabled ||
                            currentSplashOnStartup != newSplashOnStartup ||
                            currentClipboardAutoClear != newClipboardAutoClear ||
                            !currentClipboardTimeout.equals(newClipboardTimeout) ||
                            currentAutoLockEnabled != newAutoLockEnabled ||
                            currentAutoLockTimeoutSeconds != newAutoLockTimeoutSeconds;

        if (!settingsChanged) {
            statusLabel.setText("No changes to apply.");
            statusLabel.setForeground(Color.BLUE);
            return;
        }

        // Save settings
        settings.setProperty(KEY_BACKUP_DIRECTORY, newBackupDir);
        
        // If auto-backup is being enabled for the first time, ensure directory is configured
        if (newAutoBackupEnabled && !currentAutoBackupEnabled) {
            backupManager.ensureBackupDirectoryConfigured();
        }
        
        settings.setProperty(KEY_AUTO_BACKUP_ENABLED, newAutoBackupEnabled ? VALUE_TRUE : VALUE_FALSE);
        settings.setProperty(KEY_SHOW_SPLASH, newSplashOnStartup ? VALUE_TRUE : VALUE_FALSE);
        settings.setProperty(KEY_CLIPBOARD_AUTO_CLEAR, newClipboardAutoClear ? VALUE_TRUE : VALUE_FALSE);
        settings.setProperty(KEY_CLIPBOARD_TIMEOUT, newClipboardTimeout);
        settings.setProperty(KEY_AUTO_LOCK_ENABLED, newAutoLockEnabled ? VALUE_TRUE : VALUE_FALSE);
        settings.setProperty(KEY_AUTO_LOCK_TIMEOUT, String.valueOf(newAutoLockTimeoutSeconds));
        saveSettings();

        // Update secure clipboard settings immediately
        clipboard.SecureClipboardManager.setAutoClearEnabled(newClipboardAutoClear);
        try {
            int timeoutValue = Integer.parseInt(newClipboardTimeout);
            clipboard.SecureClipboardManager.setTimeoutSeconds(timeoutValue);
        } catch (NumberFormatException e) {
            gui.DialogHelper.showWarning(editor, "Settings Error", "Invalid clipboard timeout value. Using default.", null);
        } catch (IllegalArgumentException e) {
            gui.DialogHelper.showWarning(editor, "Settings Error", "Clipboard timeout must be between 5 and 30 seconds.", null);
        }

        // Update auto-lock settings immediately (pass timeout in seconds as string)
        editor.updateAutoLockSettings(newAutoLockEnabled, String.valueOf(newAutoLockTimeoutSeconds));

        loadCurrentSettings(); // Refresh fields with saved values
        Toast.showToast(editor, "Settings saved!");
    }

    private void enableEncryption() {
        var pwdResult = PasswordDialog.showPasswordDialog(editor, "Create Password", "<html>Create a strong password for your encrypted log.<br><br><b>Requirements:</b><br>• At least 20 characters<br>• At least one uppercase letter (A-Z)<br>• At least one special character (!@#$%^&* etc.) <i>unless password scores 'Strong'</i><br>• Must score at least 'Good' strength<br><br>Use the <b>Generate</b> button for a secure random password, or create your own.<br><br><b>⚠️ Remember to save your password in a password manager!</b></html>", true);
        var pwd = pwdResult.password;
        if (pwd == null) return;

        if (pwd.length < 20) {
            gui.DialogHelper.showError(editor, "Invalid Password", "Password must be at least 20 characters");
            return;
        }

        // Check strength score first
        int score = gui.PasswordStrengthIndicator.calculateStrength(pwd);
        
        // If password scores "Strong" or better (65+), accept it regardless of character requirements
        // This allows strong passphrases without uppercase/special chars
        if (score < 65) {
            var hasUpper = false;
            var hasSpecial = false;
            for (char c : pwd) {
                if (Character.isUpperCase(c)) hasUpper = true;
                if (!Character.isLetterOrDigit(c) && c != ' ') hasSpecial = true; // spaces don't count as special
            }
            
            if (!hasUpper || !hasSpecial) {
                String requirements = "Password must contain at least one uppercase letter and one special character (e.g., !@#$%^&*()_+-=[]{}|;':\",./<>?), OR score 'Strong' or higher in the strength indicator.";
                gui.DialogHelper.showWarning(editor, "Password Requirements", requirements, null);
                return;
            }
        }

        if (score < 45) { // Require at least 'Good' (45+)
            gui.DialogHelper.showWarning(editor, "Weak Password", "Password is too weak. Please create a stronger password (aim for 'Good' or 'Strong' in the indicator).", null);
            return;
        }

        // Loop confirm dialog until passwords match or user cancels
        char[] confirm;
        while (true) {
            var confirmResult = PasswordDialog.showPasswordDialog(editor, "Confirm new password", "Confirm your new password.");
            if (confirmResult.password == null) {
                java.util.Arrays.fill(pwd, '\0');
                return;
            }
            confirm = confirmResult.password;
            if (java.util.Arrays.equals(pwd, confirm)) {
                break;
            }
            gui.DialogHelper.showError(editor, "Mismatch", "Passwords do not match. Please try again.");
            java.util.Arrays.fill(confirm, '\0');
        }
        final char[] confirmedPwd = confirm;

        // Snapshot the settings values needed by the background thread.
        // Properties is not thread-safe; reading it off the EDT is a data race.
        final String backupDirSnapshot = settings.getProperty(KEY_BACKUP_DIRECTORY, "");

        // Run encryption off the EDT to keep the UI responsive and show progress.
        statusLabel.setText("Encrypting...");
        statusLabel.setForeground(Color.BLUE);
        encryptionCheckBox.setEnabled(false);

        // Show a specialized progress dialog that stays at 100% and provides an OK button.
        EncryptionProgressDialog progressDialog = new EncryptionProgressDialog(editor, "Encrypting");
        progressDialog.setStatus("Encrypting file...");
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        // Run encryption in background and then refresh parsed entries in a separate worker
        new javax.swing.SwingWorker<List<String>, Void>() {
            private Exception error;
            private byte[] saltBytesResult;

            @Override
            @SuppressWarnings("PMD.AvoidCatchingGenericException")
            protected List<String> doInBackground() {
                try {
                    logFileHandler.enableEncryption(pwd);

                    // Save settings backup
                    if (java.nio.file.Files.exists(settingsPath)) {
                        java.nio.file.Path backupSettingsPath;
                        if (backupDirSnapshot != null && !backupDirSnapshot.isEmpty()) {
                            java.nio.file.Path backupDirPath = java.nio.file.Paths.get(backupDirSnapshot);
                            java.nio.file.Files.createDirectories(backupDirPath);
                            backupSettingsPath = backupDirPath.resolve(settingsPath.getFileName().toString() + BACKUP_EXTENSION);
                        } else {
                            backupSettingsPath = settingsPath.resolveSibling(settingsPath.getFileName().toString() + BACKUP_EXTENSION);
                        }
                        java.nio.file.Files.copy(settingsPath, backupSettingsPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Store salt — settings update happens in done() on the EDT after success
                    saltBytesResult = logFileHandler.getSalt();

                    // Compute parsed entries off-EDT to avoid blocking UI when updating list model
                    List<List<String>> parsed = logFileHandler.getParsedEntries();
                    List<String> elements = new java.util.ArrayList<>();
                    for (List<String> entry : parsed) {
                        if (entry != null && !entry.isEmpty()) {
                            elements.add(entry.get(0).trim());
                        }
                    }
                    return elements;
                } catch (Exception ex) {
                    this.error = ex;
                    return java.util.Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        progressDialog.close();
                        JOptionPane.showMessageDialog(editor, "Encryption failed. Please check your password and try again.");
                        statusLabel.setText("Encryption failed. Please check your password and try again.");
                        statusLabel.setForeground(Color.RED);
                        encryptionCheckBox.setSelected(false);
                    } else {
                        try {
                            List<String> elements = get();

                            // Encryption succeeded — NOW persist settings on the EDT
                            var saltBase64 = Base64.getEncoder().encodeToString(saltBytesResult);
                            settings.setProperty(KEY_ENCRYPTED, VALUE_TRUE);
                            settings.setProperty(KEY_SALT, saltBase64);
                            saveSettings();

                            // Update list model on EDT in a single batch
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                try {
                                    var logListPanel = editor.getLogListPanel();
                                    var listModel = logListPanel.getListModel();
                                    listModel.removeAllElements();
                                    for (String el : elements) listModel.addElement(el);
                                    editor.updateLogListView();
                                } catch (RuntimeException ignore) {
                                    Log.warn(() -> "Unable to refresh log list model after encryption.");
                                }
                            });

                            // Ask FullLogPanel to reload (it uses its own SwingWorker internally)
                            editor.getFullLogPanel().loadFullLog();

                            statusLabel.setText("Encryption enabled successfully.");
                            statusLabel.setForeground(Color.BLUE);

                            // Arrange to show completion state and silently clean up legacy .bak files.
                            progressDialog.setOnOkCallback(SettingsPanel.this::cleanupLegacyBackupsSilently);
                            progressDialog.showCompletion();

                            // Perform automatic backup after successful encryption
                            performAutomaticBackup();
                        } catch (InterruptedException ex2) {
                            Thread.currentThread().interrupt();
                            progressDialog.close();
                            JOptionPane.showMessageDialog(editor, "Encryption succeeded but UI refresh was interrupted: " + ex2.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
                            statusLabel.setText("Encryption completed but settings update was interrupted.");
                            statusLabel.setForeground(Color.ORANGE);
                        } catch (ExecutionException ex2) {
                            progressDialog.close();
                            JOptionPane.showMessageDialog(editor, "Encryption succeeded but saving settings failed: " + ex2.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
                            statusLabel.setText("Encryption completed but settings update failed.");
                            statusLabel.setForeground(Color.ORANGE);
                        }
                    }
                } finally {
                    // Ensure sensitive data cleared
                    java.util.Arrays.fill(pwd, '\0');
                    java.util.Arrays.fill(confirmedPwd, '\0');
                    // Keep the checkbox disabled when encrypted
                    encryptionCheckBox.setEnabled(false);
                }
            }
        }.execute();
    }

    private void cleanupLegacyBackupsSilently() {
        try {
            String backupDirStr = backupManager.getAutoBackupDirectory();
            Path backupDir = java.nio.file.Paths.get(backupDirStr);

            if (!Files.exists(backupDir)) {
                return; // No backup directory
            }

            // Find legacy .bak files that are not encrypted artifacts and securely remove them.
            List<Path> legacyBackups = Files.list(backupDir)
                .filter(path -> path.getFileName().toString().endsWith(".bak"))
                .filter(path -> !encryption.EncryptionDetector.isFileEncrypted(path))
                .collect(Collectors.toList());

            if (legacyBackups.isEmpty()) {
                return;
            }

            for (Path backup : legacyBackups) {
                try {
                    SecureDeletionUtils.wipeFile(backup);
                } catch (java.io.IOException e) {
                    Log.error(() -> "Failed to securely delete legacy backup: " + backup, e);
                }
            }

        } catch (java.io.IOException e) {
            // Best effort cleanup; do not block normal flow.
            Log.error("Error during backup cleanup", e);
        }
    }

    private void browseBackupDirectory() {
        var chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        var current = backupDirField.getText();
        if (!current.isEmpty()) {
            chooser.setCurrentDirectory(new java.io.File(current));
        }
        var res = chooser.showOpenDialog(editor);
        if (res == JFileChooser.APPROVE_OPTION) {
            backupDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void performAutomaticBackup() {
        backupManager.performAutomaticBackup();
    }

    private void backupLogFile() {
        var confirm = gui.DialogHelper.confirm(editor, "Backup Info",
            "Backups are copies of your current log file.",
            "If encrypted, the backup will remain encrypted for security.<br>Do you want to proceed?");
        if (!confirm) return;

        var chooser = new JFileChooser();
        var backupDir = backupDirField.getText();
        if (!backupDir.isEmpty()) {
            chooser.setCurrentDirectory(new java.io.File(backupDir));
        }
        var date = LocalDate.now().toString();
        chooser.setSelectedFile(new java.io.File("loghog-backup-" + date + ".enc"));
        var filter = new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                if (f.isDirectory()) return true;
                var name = f.getName();
                return name.startsWith("loghog-backup-") && name.endsWith(".enc");
            }
            @Override
            public String getDescription() {
                return "LogHog encrypted backup files (*.enc)";
            }
        };
        chooser.setFileFilter(filter);
        var res = chooser.showSaveDialog(editor);
        if (res == JFileChooser.APPROVE_OPTION) {
            var selectedFile = chooser.getSelectedFile();
            var backupPath = selectedFile.toPath();
            var selectedDir = backupPath.getParent();

            UiTaskRunner.runModalBackgroundTask(editor, "Manual Backup", "Saving backup...", () -> {
                try {
                    if (Files.exists(backupPath)) {
                        SecureDeletionUtils.wipeFile(backupPath);
                    }
                    Path encryptedLogPath = logFileHandler.getFilePath();
                    Files.copy(encryptedLogPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    var settingsSource = Paths.get(System.getProperty("user.home"), "settings.ini");
                    if (Files.exists(settingsSource)) {
                        var settingsBackupPath = selectedDir.resolve("settings.ini");
                        if (Files.exists(settingsBackupPath)) {
                            SecureDeletionUtils.wipeFile(settingsBackupPath);
                        }
                        Files.copy(settingsSource, settingsBackupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        javax.swing.SwingUtilities.invokeLater(() -> statusLabel.setText("Backup saved to: " + selectedDir.toString()));
                    } else {
                        javax.swing.SwingUtilities.invokeLater(() -> statusLabel.setText("Backup saved to: " + backupPath.toString()));
                    }
                } catch (java.io.IOException ex) {
                    javax.swing.SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(editor, "Backup failed. Please check file permissions and try again."));
                }
            });
        }
    }

    private void saveSettings() {
        try (var fos = java.nio.file.Files.newOutputStream(settingsPath)) {
            settings.store(fos, "LogHog settings");
            security.SecurityFilePolicy.ensureOwnerOnlyPermissions(settingsPath);
            if (!security.SecurityFilePolicy.isOwnerOnlyAccessEnforced(settingsPath)
                && !VALUE_TRUE.equals(settings.getProperty("permissionsWarningShown", VALUE_FALSE))) {
                settings.setProperty("permissionsWarningShown", VALUE_TRUE);
                gui.DialogHelper.showWarning(editor,
                    "Security Notice",
                    "Platform Permission Limits",
                    "Strict owner-only permission verification is unavailable on this platform.<br><br>" +
                    "For best protection, use a dedicated user account and full-disk encryption.");
            }
        } catch (java.io.IOException e) {
            gui.DialogHelper.showError(editor, "Error", "Error saving settings. Please check file permissions and try again.");
        }
    }


    private boolean isValidClipboardTimeout(String timeoutStr) {
        if (timeoutStr == null || timeoutStr.isBlank()) {
            return false;
        }

        try {
            int timeout = Integer.parseInt(timeoutStr.trim());
            // Timeout must be between 5 and 30 seconds (matches SecureClipboardManager enforcement)
            return timeout >= 5 && timeout <= 30;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Removed unused isValidAutoLockTimeout method (PMD UnusedPrivateMethod)

    private boolean isValidBackupDirectory(String path) {
        if (path == null || path.isBlank()) {
            return true; // Empty path is allowed (will use default)
        }
        try {
            // Check if it's a valid path
            java.nio.file.Paths.get(path);
            // Check for dangerous characters that could be used for path traversal
            if (path.contains("..") || path.contains("\\") && !System.getProperty("os.name").toLowerCase(Locale.ROOT).contains(OS_WINDOWS)) {
                return false;
            }
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }

}
