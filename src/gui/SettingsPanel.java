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

package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
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
import main.SecureSettings;
import utils.Toast;

public class SettingsPanel extends JPanel {
    private final LogTextEditor editor;
    private final Properties settings;
    private final Path settingsPath;
    private final LogFileHandler logFileHandler;
    private final SecureSettings secureSettings;
    private final BackupManager backupManager;

    private JCheckBox encryptionCheckBox;
    private JButton applyButton;
    private JTextField reminderField;
    private JLabel statusLabel;
    private JTextField backupDirField;
    private JButton browseBackupButton;
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
        this.secureSettings = new SecureSettings();
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

        // Decrypt section
        contentPanel.add(createDecryptPanel());

        // Backup section
        contentPanel.add(createBackupPanel());

        // Backup directory section
        contentPanel.add(createBackupDirPanel());

        // Auto-backup section
        contentPanel.add(createAutoBackupPanel());

        // Reminder section
        contentPanel.add(createReminderPanel());

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

        encryptionCheckBox = new JCheckBox("Enable encryption");
        encryptionCheckBox.setBackground(Color.WHITE);
        encryptionCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        encryptionCheckBox.setSelected("true".equals(settings.getProperty("encrypted")));

        panel.add(encryptionCheckBox);
        return panel;
    }

    private JPanel createDecryptPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Decrypt File"));

        var decryptButton = new StandardButton("Decrypt Log File", new Color(0xE0E0E0), new Color(0xB0B0B0));
        decryptButton.addActionListener(e -> decryptLogFile());

        var decryptWarning = new JLabel("<html><b>Warning:</b> This will permanently decrypt your log file and store it in plain text.</html>");
        decryptWarning.setForeground(Color.RED);
        decryptWarning.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panel.add(decryptButton);
        panel.add(decryptWarning);
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
        backupDirLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backupDirField = new JTextField(20);
        backupDirField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
        autoBackupCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panel.add(autoBackupCheckBox);
        return panel;
    }

    private JPanel createReminderPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Password Reminder"));

        var reminderLabel = new JLabel("Optional reminder (stored in plain text): ");
        reminderLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reminderField = new JTextField(20);
        reminderField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panel.add(reminderLabel);
        panel.add(reminderField);
        return panel;
    }

    private JPanel createSplashPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Splash Screen"));

        splashOnStartupCheckBox = new JCheckBox("Show splash screen on startup");
        splashOnStartupCheckBox.setBackground(Color.WHITE);
        splashOnStartupCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        splashOnStartupCheckBox.setSelected("true".equals(settings.getProperty("showSplashOnStartup", "true")));

        panel.add(splashOnStartupCheckBox);
        return panel;
    }

    private JPanel createClipboardSecurityPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Clipboard Security"));

        clipboardAutoClearCheckBox = new JCheckBox("Auto-clear clipboard after copying");
        clipboardAutoClearCheckBox.setBackground(Color.WHITE);
        clipboardAutoClearCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clipboardAutoClearCheckBox.setSelected("true".equals(settings.getProperty("clipboardAutoClear", "true")));

        var timeoutLabel = new JLabel("Timeout (seconds): ");
        timeoutLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clipboardTimeoutField = new JTextField(5);
        clipboardTimeoutField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clipboardTimeoutField.setText(settings.getProperty("clipboardTimeout", "30"));

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
        autoLockCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        autoLockCheckBox.setSelected("true".equals(settings.getProperty("autoLockEnabled", "false")));

        var timeoutLabel = new JLabel("Timeout (minutes): ");
        timeoutLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        // Get timeout in seconds and convert to minutes, default is 15 minutes (900 seconds)
        int timeoutSeconds = Integer.parseInt(settings.getProperty("autoLockTimeout", "900"));
        int timeoutMinutes = timeoutSeconds / 60;
        
        // Spinner: min=15, max=1440 (24 hours), step=5, initial=timeoutMinutes
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
            Math.max(15, Math.min(1440, timeoutMinutes)), // value (clamped)
            15,    // min: 15 minutes
            1440,  // max: 24 hours
            5      // step: 5 minutes
        );
        autoLockTimeoutSpinner = new JSpinner(spinnerModel);
        autoLockTimeoutSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panel.add(statusLabel);
        return panel;
    }

    public void loadCurrentSettings() {
        reminderField.setText(secureSettings.getDecryptedProperty(settings, "passwordReminder", ""));
        backupDirField.setText(settings.getProperty("backupDirectory", ""));
        autoBackupCheckBox.setSelected("true".equals(settings.getProperty("autoBackupEnabled", "false")));
        splashOnStartupCheckBox.setSelected("true".equals(settings.getProperty("showSplashOnStartup", "true")));
        clipboardAutoClearCheckBox.setSelected("true".equals(settings.getProperty("clipboardAutoClear", "true")));
        clipboardTimeoutField.setText(settings.getProperty("clipboardTimeout", "30"));
        autoLockCheckBox.setSelected("true".equals(settings.getProperty("autoLockEnabled", "false")));
        
        // Load auto-lock timeout in minutes
        int timeoutSeconds = Integer.parseInt(settings.getProperty("autoLockTimeout", "900"));
        int timeoutMinutes = timeoutSeconds / 60;
        
        // Check if timeout is outside valid range and alert user
        if (timeoutMinutes < 15 || timeoutMinutes > 1440) {
            int clampedMinutes = Math.max(15, Math.min(1440, timeoutMinutes));
            JOptionPane.showMessageDialog(editor, 
                "<html><b>Auto-Lock Timeout Adjusted</b><br><br>" +
                "The saved auto-lock timeout (" + timeoutMinutes + " minutes) is outside the valid range.<br>" +
                "Valid range: 15-1440 minutes (15 minutes to 24 hours).<br><br>" +
                "Timeout has been adjusted to: " + clampedMinutes + " minutes.</html>", 
                "Settings Validation", 
                JOptionPane.WARNING_MESSAGE);
            timeoutMinutes = clampedMinutes;
        }
        autoLockTimeoutSpinner.setValue(timeoutMinutes);
        
        var isEncrypted = "true".equals(settings.getProperty("encrypted"));
        encryptionCheckBox.setSelected(isEncrypted);
        encryptionCheckBox.setEnabled(!isEncrypted); // Disable if already encrypted
    }

    private void applySettings() {
        var enable = encryptionCheckBox.isSelected();
        var currentEnc = settings.getProperty("encrypted");

        // Check if encryption setting changed
        if (enable && !"true".equals(currentEnc)) {
            // Enabling encryption
            enableEncryption();
            return; // enableEncryption handles its own saving
        } else if (!enable && "true".equals(currentEnc)) {
            // User unchecked the box but file is still encrypted
            statusLabel.setText("Use the 'Decrypt Log File' button to decrypt the file.");
            statusLabel.setForeground(Color.ORANGE);
            encryptionCheckBox.setSelected(true); // Keep it checked until they decrypt
            return;
        }

        // Check if any settings actually changed
        var currentReminder = secureSettings.getDecryptedProperty(settings, "passwordReminder", "");
        var currentBackupDir = settings.getProperty("backupDirectory", "");
        var currentSplashOnStartup = "true".equals(settings.getProperty("showSplashOnStartup", "true"));
        var currentClipboardAutoClear = "true".equals(settings.getProperty("clipboardAutoClear", "true"));
        var currentClipboardTimeout = settings.getProperty("clipboardTimeout", "30");
        var currentAutoLockEnabled = "true".equals(settings.getProperty("autoLockEnabled", "false"));
        int currentAutoLockTimeoutSeconds = Integer.parseInt(settings.getProperty("autoLockTimeout", "900"));
        var newReminder = reminderField.getText();
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
            JOptionPane.showMessageDialog(editor, "Clipboard timeout must be a number between 5 and 30 seconds.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            loadCurrentSettings(); // Reset to current valid values
            return;
        }

        // Auto-lock timeout is already validated by spinner bounds (15-1440 minutes)

        // Validate reminder field
        if (!isValidReminder(newReminder)) {
            JOptionPane.showMessageDialog(editor, "Password reminder must be less than 200 characters and cannot contain control characters.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            loadCurrentSettings(); // Reset to current valid values
            return;
        }

        // Validate backup directory
        if (!isValidBackupDirectory(newBackupDir)) {
            JOptionPane.showMessageDialog(editor, "Backup directory path is invalid or contains unsafe characters.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            loadCurrentSettings(); // Reset to current valid values
            return;
        }

        var currentAutoBackupEnabled = "true".equals(settings.getProperty("autoBackupEnabled", "false"));

        var settingsChanged = !currentReminder.equals(newReminder) || !currentBackupDir.equals(newBackupDir) ||
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
        secureSettings.setEncryptedProperty(settings, "passwordReminder", newReminder);
        editor.updatePasswordReminder(newReminder);
        settings.setProperty("backupDirectory", newBackupDir);
        
        // If auto-backup is being enabled for the first time, ensure directory is configured
        if (newAutoBackupEnabled && !currentAutoBackupEnabled) {
            backupManager.ensureBackupDirectoryConfigured();
        }
        
        settings.setProperty("autoBackupEnabled", newAutoBackupEnabled ? "true" : "false");
        settings.setProperty("showSplashOnStartup", newSplashOnStartup ? "true" : "false");
        settings.setProperty("clipboardAutoClear", newClipboardAutoClear ? "true" : "false");
        settings.setProperty("clipboardTimeout", newClipboardTimeout);
        settings.setProperty("autoLockEnabled", newAutoLockEnabled ? "true" : "false");
        settings.setProperty("autoLockTimeout", String.valueOf(newAutoLockTimeoutSeconds));
        saveSettings();

        // Update secure clipboard settings immediately
        clipboard.SecureClipboardManager.setAutoClearEnabled(newClipboardAutoClear);
        try {
            int timeoutValue = Integer.parseInt(newClipboardTimeout);
            clipboard.SecureClipboardManager.setTimeoutSeconds(timeoutValue);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(editor, "Invalid clipboard timeout value. Using default.", "Settings Error", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(editor, "Clipboard timeout must be between 5 and 30 seconds.", "Settings Error", JOptionPane.WARNING_MESSAGE);
        }

        // Update auto-lock settings immediately (pass timeout in seconds as string)
        editor.updateAutoLockSettings(newAutoLockEnabled, String.valueOf(newAutoLockTimeoutSeconds));

        loadCurrentSettings(); // Refresh fields with saved values
        Toast.showToast(editor, "Settings saved!");
    }

    private void enableEncryption() {
        var pwdResult = PasswordDialog.showPasswordDialog(editor, "Create Password", reminderField.getText(), "<html>Create a strong password for your encrypted log.<br><br><b>Requirements:</b><br>• At least 20 characters<br>• At least one uppercase letter (A-Z)<br>• At least one special character (!@#$%^&* etc.) <i>unless password scores 'Strong'</i><br>• Must score at least 'Good' strength<br><br>Use the <b>Generate</b> button for a secure random password, or create your own.<br><br><b>⚠️ Remember to save your password in a password manager!</b></html>", true);
        var pwd = pwdResult.password;
        if (pwd == null) return;

        if (pwd.length < 20) {
            JOptionPane.showMessageDialog(editor, "Password must be at least 20 characters");
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
                JOptionPane.showMessageDialog(editor, requirements);
                return;
            }
        }

        if (score < 45) { // Require at least 'Good' (45+)
            JOptionPane.showMessageDialog(editor, "Password is too weak. Please create a stronger password (aim for 'Good' or 'Strong' in the indicator).");
            return;
        }

        var confirmResult = PasswordDialog.showPasswordDialog(editor, "Confirm new password", reminderField.getText(), "Confirm your new password.");
        var confirm = confirmResult.password;
        if (!java.util.Arrays.equals(pwd, confirm)) {
            JOptionPane.showMessageDialog(editor, "Passwords do not match");
            java.util.Arrays.fill(pwd, '\0');
            java.util.Arrays.fill(confirm, '\0');
            return;
        }

        // Encrypt current file
        try {
            logFileHandler.enableEncryption(pwd);

            // Backup settings file before modifying
            if (java.nio.file.Files.exists(settingsPath)) {
                var backupSettingsPath = settingsPath.resolveSibling(settingsPath.getFileName().toString() + ".bak");
                java.nio.file.Files.copy(settingsPath, backupSettingsPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            var saltBytes = logFileHandler.getSalt();
            var saltBase64 = Base64.getEncoder().encodeToString(saltBytes);
            settings.setProperty("encrypted", "true");
            settings.setProperty("salt", saltBase64);
            saveSettings();

            // Verify the settings were saved correctly
            var savedSalt = settings.getProperty("salt");
            if (savedSalt == null || savedSalt.isEmpty()) {
                throw new Exception("Failed to save encryption salt to settings");
            }
            if (!savedSalt.equals(saltBase64)) {
                throw new Exception("Salt mismatch after save! Expected: " + saltBase64 + ", Got: " + savedSalt);
            }

            statusLabel.setText("Encryption enabled successfully.");
            statusLabel.setForeground(Color.BLUE);
            editor.loadLogEntries();
            editor.getFullLogPanel().loadFullLog();

            // Perform automatic backup after successful encryption
            performAutomaticBackup();

            // Check for and offer to clean up old unencrypted backups
            cleanupOldUnencryptedBackups();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(editor, "Encryption failed. Please check your password and try again.");
            statusLabel.setText("Encryption failed. Please check your password and try again.");
            statusLabel.setForeground(Color.RED);
        } finally {
            java.util.Arrays.fill(pwd, '\0');
            java.util.Arrays.fill(confirm, '\0');
        }
    }

    private void cleanupOldUnencryptedBackups() {
        try {
            Path logFilePath = logFileHandler.getFilePath();
            String backupDirStr = backupManager.getAutoBackupDirectory();
            Path backupDir = java.nio.file.Paths.get(backupDirStr);

            if (!Files.exists(backupDir)) {
                return; // No backup directory
            }

            // Find .bak files that are unencrypted
            List<Path> unencryptedBackups = Files.list(backupDir)
                .filter(path -> path.getFileName().toString().endsWith(".bak"))
                .filter(path -> !encryption.EncryptionDetector.isFileEncrypted(path))
                .collect(Collectors.toList());

            if (unencryptedBackups.isEmpty()) {
                return; // No unencrypted backups to clean up
            }

            // Show dialog asking user what to do
            StringBuilder message = new StringBuilder();
            message.append("<html><b>Security Notice: Old Unencrypted Backups Found</b><br><br>");
            message.append("After encrypting your log file, we found ").append(unencryptedBackups.size());
            message.append(" backup file(s) that contain unencrypted data:<br><br>");

            for (Path backup : unencryptedBackups) {
                message.append("• ").append(backup.getFileName().toString()).append("<br>");
            }

            message.append("<br><b>Security Risk:</b> These backups contain your log data in plain text.<br>");
            message.append("Anyone with access to your backup location can read them.<br><br>");
            message.append("<b>Recommendation:</b> Delete these old backups to prevent data exposure.<br>");
            message.append("You can keep them if you need them for recovery purposes.<br><br>");
            message.append("What would you like to do?</html>");

            int choice = JOptionPane.showOptionDialog(
                editor,
                message.toString(),
                "Clean Up Old Backups",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"Delete old unencrypted backups", "Keep them"},
                "Delete old unencrypted backups"
            );

            if (choice == JOptionPane.YES_OPTION) {
                // Delete the unencrypted backups securely
                for (Path backup : unencryptedBackups) {
                    try {
                        encryption.EncryptionDetector.secureDelete(backup);
                    } catch (Exception e) {
                        // Log error but continue with others
                        System.err.println("Failed to securely delete backup: " + backup + " - " + e.getMessage());
                    }
                }

                JOptionPane.showMessageDialog(
                    editor,
                    "Old unencrypted backup files have been securely deleted.",
                    "Cleanup Complete",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (Exception e) {
            // Silently ignore cleanup errors to not disrupt the encryption success
            System.err.println("Error during backup cleanup: " + e.getMessage());
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
        var confirm = JOptionPane.showConfirmDialog(editor, "Backups are copies of your current log file.\nIf encrypted, the backup will remain encrypted for security.\nDo you want to proceed?", "Backup Info", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        var chooser = new JFileChooser();
        var backupDir = backupDirField.getText();
        if (!backupDir.isEmpty()) {
            chooser.setCurrentDirectory(new java.io.File(backupDir));
        }
        var date = LocalDate.now().toString();
        chooser.setSelectedFile(new java.io.File("loghog-backup-" + date + ".txt"));
        var filter = new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                if (f.isDirectory()) return true;
                var name = f.getName();
                return name.startsWith("loghog-backup-") && name.endsWith(".txt");
            }
            @Override
            public String getDescription() {
                return "LogHog backup files (*.txt)";
            }
        };
        chooser.setFileFilter(filter);
        var res = chooser.showSaveDialog(editor);
        if (res == JFileChooser.APPROVE_OPTION) {
            var selectedFile = chooser.getSelectedFile();
            var backupPath = selectedFile.toPath();
            try {
                // Securely delete existing backup file if it exists
                if (Files.exists(backupPath)) {
                    secureDelete(backupPath);
                }
                Files.copy(Paths.get(System.getProperty("user.home"), "log.txt"), backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                statusLabel.setText("Backup saved to: " + backupPath.toString());
            } catch (java.io.IOException | SecurityException ex) {
                JOptionPane.showMessageDialog(editor, "Backup failed. Please check file permissions and try again.");
            }
        }
    }

    private void saveSettings() {
        try (var fos = new FileOutputStream(settingsPath.toFile())) {
            settings.store(fos, "LogHog settings");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(editor, "Error saving settings. Please check file permissions and try again.");
        }
    }

    private void decryptLogFile() {
        if (!logFileHandler.isEncrypted()) {
            JOptionPane.showMessageDialog(editor, "The log file is not currently encrypted.", "Not Encrypted", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (!showDecryptionWarning()) {
            return;
        }

        try {
            // Decrypt the file
            logFileHandler.disableEncryption();

            // Backup settings file before modifying
            backupSettingsFile();

            // Update settings - but save BEFORE clearing in case something goes wrong
            var oldEncrypted = settings.getProperty("encrypted");
            var oldSalt = settings.getProperty("salt");

            updateSettingsForDecryption();

            // Verify decryption worked by trying to read the file
            verifyDecryptionSuccess(oldEncrypted, oldSalt);

            // Update UI and show success
            updateUIAfterSuccessfulDecryption();
            showDecryptionSuccessMessage();

        } catch (Exception ex) {
            handleDecryptionError(ex);
        }
    }

    private boolean showDecryptionWarning() {
        var confirm = JOptionPane.showConfirmDialog(editor,
            "<html><b>WARNING: Security Risk</b><br><br>" +
            "This will permanently decrypt your log file and save it as plain text.<br>" +
            "Anyone with access to your computer will be able to read the file.<br><br>" +
            "A backup of the encrypted file will be saved as log.txt.bak<br><br>" +
            "Are you sure you want to proceed?</html>",
            "Decrypt Log File - Security Warning",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return confirm == JOptionPane.YES_OPTION;
    }

    private void backupSettingsFile() {
        if (java.nio.file.Files.exists(settingsPath)) {
            var backupSettingsPath = settingsPath.resolveSibling(settingsPath.getFileName().toString() + ".bak");
            try {
                java.nio.file.Files.copy(settingsPath, backupSettingsPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                // Silently ignore backup failures
            }
        }
    }

    private void updateSettingsForDecryption() {
        settings.setProperty("encrypted", "false");
        settings.remove("salt");
        saveSettings();
    }

    private void verifyDecryptionSuccess(String oldEncrypted, String oldSalt) throws Exception {
        try {
            editor.loadLogEntries();
        } catch (Exception verifyEx) {
            // Rollback settings
            settings.setProperty("encrypted", oldEncrypted);
            if (oldSalt != null) {
                settings.setProperty("salt", oldSalt);
            }
            saveSettings();
            throw new Exception("Decryption verification failed: " + verifyEx.getMessage());
        }
    }

    private void updateUIAfterSuccessfulDecryption() {
        encryptionCheckBox.setSelected(false);
        statusLabel.setText("File decrypted successfully. Encryption disabled.");
        statusLabel.setForeground(new Color(0, 128, 0)); // Green
        editor.getFullLogPanel().loadFullLog();

        // Perform automatic backup after successful decryption
        performAutomaticBackup();
    }

    private void showDecryptionSuccessMessage() {
        JOptionPane.showMessageDialog(editor,
            "Log file has been decrypted successfully.\nA backup of the encrypted file was saved as log.txt.bak",
            "Decryption Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleDecryptionError(Exception ex) {
        JOptionPane.showMessageDialog(editor,
            "Decryption failed: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        statusLabel.setText("Decryption failed.");
        statusLabel.setForeground(Color.RED);
    }

    private boolean isValidClipboardTimeout(String timeoutStr) {
        if (timeoutStr == null || timeoutStr.trim().isEmpty()) {
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

    private boolean isValidAutoLockTimeout(String timeoutStr) {
        if (timeoutStr == null || timeoutStr.trim().isEmpty()) {
            return false;
        }

        try {
            int timeout = Integer.parseInt(timeoutStr.trim());
            // Timeout must be between 10 seconds and 24 hours (86400 seconds)
            return timeout >= 10 && timeout <= 86400;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidReminder(String reminder) {
        if (reminder == null) {
            return true; // Empty reminder is allowed
        }
        if (reminder.length() > 200) {
            return false;
        }
        // Check for control characters
        for (char c : reminder.toCharArray()) {
            if (Character.isISOControl(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidBackupDirectory(String path) {
        if (path == null || path.trim().isEmpty()) {
            return true; // Empty path is allowed (will use default)
        }
        try {
            // Check if it's a valid path
            java.nio.file.Paths.get(path);
            // Check for dangerous characters that could be used for path traversal
            if (path.contains("..") || path.contains("\\") && !System.getProperty("os.name").toLowerCase().contains("windows")) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void secureDelete(java.nio.file.Path filePath) throws java.io.IOException {
        if (!Files.exists(filePath)) {
            return;
        }

        long fileSize = Files.size(filePath);
        SecureRandom random = new SecureRandom();

        // Overwrite file multiple times with random data
        for (int pass = 0; pass < 3; pass++) {
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(filePath.toFile(), "rw")) {
                byte[] buffer = new byte[8192];
                long remaining = fileSize;
                while (remaining > 0) {
                    int toWrite = (int) Math.min(buffer.length, remaining);
                    random.nextBytes(buffer);
                    raf.write(buffer, 0, toWrite);
                    remaining -= toWrite;
                }
                raf.getFD().sync(); // Force write to disk
            }
        }

        // Finally delete the file
        Files.delete(filePath);
    }
}
