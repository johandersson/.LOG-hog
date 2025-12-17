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
import java.time.LocalDate;
import java.util.Base64;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import filehandling.LogFileHandler;
import main.LogTextEditor;
import utils.Toast;

public class SettingsPanel extends JPanel {
    private final LogTextEditor editor;
    private final Properties settings;
    private final Path settingsPath;
    private final LogFileHandler logFileHandler;

    private JCheckBox encryptionCheckBox;
    private JButton applyButton;
    private JTextField reminderField;
    private JLabel statusLabel;
    private JTextField backupDirField;
    private JButton browseBackupButton;
    private JCheckBox splashOnStartupCheckBox;
    private JCheckBox clipboardAutoClearCheckBox;
    private JTextField clipboardTimeoutField;

    public SettingsPanel(LogTextEditor editor, Properties settings, Path settingsPath, LogFileHandler logFileHandler) {
        this.editor = editor;
        this.settings = settings;
        this.settingsPath = settingsPath;
        this.logFileHandler = logFileHandler;

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

        // Reminder section
        contentPanel.add(createReminderPanel());

        // Splash screen section
        contentPanel.add(createSplashPanel());

        // Clipboard security section
        contentPanel.add(createClipboardSecurityPanel());

        // Button section
        contentPanel.add(createButtonPanel());

        // Status section
        contentPanel.add(createStatusPanel());

        add(contentPanel, BorderLayout.NORTH);

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

    private JPanel createButtonPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);

        applyButton = new AccentButton("Apply Changes");
        applyButton.addActionListener(e -> applySettings());

        panel.add(applyButton);
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
        reminderField.setText(settings.getProperty("passwordReminder", ""));
        backupDirField.setText(settings.getProperty("backupDirectory", ""));
        splashOnStartupCheckBox.setSelected("true".equals(settings.getProperty("showSplashOnStartup", "true")));
        clipboardAutoClearCheckBox.setSelected("true".equals(settings.getProperty("clipboardAutoClear", "true")));
        clipboardTimeoutField.setText(settings.getProperty("clipboardTimeout", "30"));
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
        var currentReminder = settings.getProperty("passwordReminder", "");
        var currentBackupDir = settings.getProperty("backupDirectory", "");
        var currentSplashOnStartup = "true".equals(settings.getProperty("showSplashOnStartup", "true"));
        var currentClipboardAutoClear = "true".equals(settings.getProperty("clipboardAutoClear", "true"));
        var currentClipboardTimeout = settings.getProperty("clipboardTimeout", "30");
        var newReminder = reminderField.getText();
        var newBackupDir = backupDirField.getText();
        var newSplashOnStartup = splashOnStartupCheckBox.isSelected();
        var newClipboardAutoClear = clipboardAutoClearCheckBox.isSelected();
        var newClipboardTimeout = clipboardTimeoutField.getText();

        // Validate clipboard timeout
        if (!isValidClipboardTimeout(newClipboardTimeout)) {
            JOptionPane.showMessageDialog(editor, "Clipboard timeout must be a number between 1 and 3600 seconds (1 hour).", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            loadCurrentSettings(); // Reset to current valid values
            return;
        }

        var settingsChanged = !currentReminder.equals(newReminder) || !currentBackupDir.equals(newBackupDir) ||
                            currentSplashOnStartup != newSplashOnStartup ||
                            currentClipboardAutoClear != newClipboardAutoClear ||
                            !currentClipboardTimeout.equals(newClipboardTimeout);

        if (!settingsChanged) {
            statusLabel.setText("No changes to apply.");
            statusLabel.setForeground(Color.BLUE);
            return;
        }

        // Save settings
        settings.setProperty("passwordReminder", newReminder);
        editor.updatePasswordReminder(newReminder);
        settings.setProperty("backupDirectory", newBackupDir);
        settings.setProperty("showSplashOnStartup", newSplashOnStartup ? "true" : "false");
        settings.setProperty("clipboardAutoClear", newClipboardAutoClear ? "true" : "false");
        settings.setProperty("clipboardTimeout", newClipboardTimeout);
        saveSettings();

        // Update secure clipboard settings immediately
        clipboard.SecureClipboardManager.setAutoClearEnabled(newClipboardAutoClear);
        try {
            int timeoutValue = Integer.parseInt(newClipboardTimeout);
            clipboard.SecureClipboardManager.setTimeoutSeconds(timeoutValue);
        } catch (NumberFormatException e) {
            // If invalid, keep current setting
        }

        loadCurrentSettings(); // Refresh fields with saved values
        Toast.showToast(editor, "Settings saved!");
    }

    private void enableEncryption() {
        var pwdResult = PasswordDialog.showPasswordDialog(editor, "Create Password", reminderField.getText(), "<html>Create a strong password for your encrypted log.<br><br><b>Requirements:</b><br>• At least 20 characters<br>• At least one uppercase letter (A-Z)<br>• At least one special character (!@#$%^&* etc.)<br>• Must score at least 'Good' strength<br><br>Use a passphrase for maximum security.</html>", true);
        var pwd = pwdResult.password;
        if (pwd == null) return;

        if (pwd.length < 20) {
            JOptionPane.showMessageDialog(editor, "Password must be at least 20 characters");
            return;
        }

        var hasUpper = false;
        var hasSpecial = false;
        for (char c : pwd) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        if (!hasUpper || !hasSpecial) {
            JOptionPane.showMessageDialog(editor, "Password must contain at least one uppercase letter and one special character (e.g., !@#$%^&*()_+-=[]{}|;':\",./<>?)");
            return;
        }

        // Check strength score
        int score = gui.PasswordStrengthIndicator.calculateStrength(pwd);
        if (score < 50) { // Require at least 'Good' (50+)
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

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(editor, "Encryption failed: " + ex.getMessage());
            statusLabel.setText("Encryption failed: " + ex.getMessage());
            statusLabel.setForeground(Color.RED);
        } finally {
            java.util.Arrays.fill(pwd, '\0');
            java.util.Arrays.fill(confirm, '\0');
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
                Files.copy(Paths.get(System.getProperty("user.home"), "log.txt"), backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                statusLabel.setText("Backup saved to: " + backupPath.toString());
            } catch (java.io.IOException | SecurityException ex) {
                JOptionPane.showMessageDialog(editor, "Backup failed: " + ex.getMessage());
            }
        }
    }

    private void saveSettings() {
        try (var fos = new FileOutputStream(settingsPath.toFile())) {
            settings.store(fos, "LogHog settings");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(editor, "Error saving settings: " + e.getMessage());
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
            // Timeout must be between 1 second and 1 hour (3600 seconds)
            return timeout >= 1 && timeout <= 3600;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
