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

import filehandling.LogFileHandler;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Properties;
import javax.swing.*;
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

    private void logToFile(String message) {
        try (FileWriter fw = new FileWriter("debug.log", true)) {
            fw.write(message + "\n");
        } catch (IOException e) {
            // Optionally log to a logger or ignore
        }
    }

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
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
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
        encryptionCheckBox.setSelected("true".equals(settings.getProperty("encrypted")));

        panel.add(encryptionCheckBox);
        return panel;
    }

    private JPanel createDecryptPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Decrypt File"));

        var decryptButton = new JButton("Decrypt Log File");
        decryptButton.addActionListener(e -> decryptLogFile());

        var decryptWarning = new JLabel("<html><b>Warning:</b> This will permanently decrypt your log file and store it in plain text.</html>");
        decryptWarning.setForeground(Color.RED);

        panel.add(decryptButton);
        panel.add(decryptWarning);
        return panel;
    }

    private JPanel createBackupPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);

        var backupButton = new JButton("Backup Log File");
        backupButton.addActionListener(e -> backupLogFile());

        panel.add(backupButton);
        return panel;
    }

    private JPanel createBackupDirPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Backup Directory"));

        var backupDirLabel = new JLabel("Default backup directory: ");
        backupDirField = new JTextField(20);
        var browseBackupButton = new JButton("Browse...");
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
        reminderField = new JTextField(20);

        panel.add(reminderLabel);
        panel.add(reminderField);
        return panel;
    }

    private JPanel createButtonPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);

        applyButton = new JButton("Apply Changes");
        applyButton.addActionListener(e -> applySettings());

        panel.add(applyButton);
        return panel;
    }

    private JPanel createStatusPanel() {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(Color.WHITE);

        statusLabel = new JLabel("");
        statusLabel.setForeground(Color.BLUE);

        panel.add(statusLabel);
        return panel;
    }

    public void loadCurrentSettings() {
        reminderField.setText(settings.getProperty("passwordReminder", ""));
        backupDirField.setText(settings.getProperty("backupDirectory", ""));
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
        var newReminder = reminderField.getText();
        var newBackupDir = backupDirField.getText();

        var settingsChanged = !currentReminder.equals(newReminder) || !currentBackupDir.equals(newBackupDir);

        if (!settingsChanged) {
            statusLabel.setText("No changes to apply.");
            statusLabel.setForeground(Color.BLUE);
            return;
        }

        // Save settings
        settings.setProperty("passwordReminder", newReminder);
        editor.updatePasswordReminder(newReminder);
        settings.setProperty("backupDirectory", newBackupDir);
        saveSettings();
        loadCurrentSettings(); // Refresh fields with saved values
        statusLabel.setText("Settings saved.");
        statusLabel.setForeground(Color.BLUE);
        Toast.showToast(editor, "Settings saved!");
    }

    private void enableEncryption() {
        var pwdResult = PasswordDialog.showPasswordDialog(editor, "Enter new password (min 16 chars, 1 uppercase, 1 special char)", reminderField.getText(), "Create a new password for your log.");
        var pwd = pwdResult.password;
        if (pwd == null) return;

        if (pwd.length < 16) {
            JOptionPane.showMessageDialog(editor, "Password must be at least 16 characters");
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

        var confirmResult = PasswordDialog.showPasswordDialog(editor, "Confirm new password", reminderField.getText(), "Confirm your new password.");
        var confirm = confirmResult.password;
        if (!java.util.Arrays.equals(pwd, confirm)) {
            JOptionPane.showMessageDialog(editor, "Passwords do not match");
            return;
        }

        // Encrypt current file
        try {
            logFileHandler.enableEncryption(pwd);

            // Backup settings file before modifying
            if (java.nio.file.Files.exists(settingsPath)) {
                var backupSettingsPath = settingsPath.resolveSibling(settingsPath.getFileName().toString() + ".bak");
                java.nio.file.Files.copy(settingsPath, backupSettingsPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logToFile("Settings file backed up to: " + backupSettingsPath.toString());
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
            logToFile("Settings saved successfully.");
        } catch (Exception e) {
            logToFile("Error saving settings: " + e.getMessage());
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
                logToFile("Settings file backed up to: " + backupSettingsPath.toString());
            } catch (Exception e) {
                logToFile("Failed to backup settings file: " + e.getMessage());
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
}
