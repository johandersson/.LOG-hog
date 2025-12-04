package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Properties;
import main.LogTextEditor;
import filehandling.LogFileHandler;

public class SettingsPanel extends JPanel {
    private LogTextEditor editor;
    private Properties settings;
    private Path settingsPath;
    private LogFileHandler logFileHandler;

    private JCheckBox encryptionCheckBox;
    private JButton applyButton;
    private JLabel statusLabel;
    private JSpinner autoClearSpinner;
    private JTextField reminderField;

    public SettingsPanel(LogTextEditor editor, Properties settings, Path settingsPath, LogFileHandler logFileHandler) {
        this.editor = editor;
        this.settings = settings;
        this.settingsPath = settingsPath;
        this.logFileHandler = logFileHandler;

        initComponents();
        loadCurrentSettings();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);

        // Encryption section
        JPanel encryptionPanel = new JPanel(new BorderLayout());
        encryptionPanel.setBorder(BorderFactory.createTitledBorder("Encryption"));
        encryptionPanel.setBackground(Color.WHITE);

        encryptionCheckBox = new JCheckBox("Enable encryption for log file");
        String enc = settings.getProperty("encrypted");
        encryptionCheckBox.setSelected("true".equals(enc));

        JLabel warningLabel = new JLabel("<html><b>Warning:</b> If you lose the password, data is lost forever.</html>");
        warningLabel.setForeground(Color.RED);

        JLabel performanceLabel = new JLabel("<html><i>Note: Enabling encryption may slow down program loading, especially in the settings tab and full log view.</i></html>");
        performanceLabel.setForeground(Color.GRAY);

        JLabel backupLabel = new JLabel("<html><b>Backup:</b> Make a backup of your log file before enabling encryption for safety.</html>");
        backupLabel.setForeground(Color.BLUE);

        encryptionPanel.add(encryptionCheckBox, BorderLayout.NORTH);
        JPanel warningsPanel = new JPanel();
        warningsPanel.setLayout(new BoxLayout(warningsPanel, BoxLayout.Y_AXIS));
        warningsPanel.setBackground(Color.WHITE);
        warningsPanel.add(warningLabel);
        warningsPanel.add(performanceLabel);
        warningsPanel.add(backupLabel);
        encryptionPanel.add(warningsPanel, BorderLayout.CENTER);

        contentPanel.add(encryptionPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Decrypt button
        JPanel decryptPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        decryptPanel.setBackground(Color.WHITE);
        decryptPanel.setBorder(BorderFactory.createTitledBorder("Decrypt File"));
        
        JButton decryptButton = new JButton("Decrypt Log File");
        decryptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                decryptLogFile();
            }
        });
        
        JLabel decryptWarning = new JLabel("<html><b>Warning:</b> This will permanently decrypt your log file and store it in plain text.</html>");
        decryptWarning.setForeground(Color.RED);
        
        decryptPanel.add(decryptButton);
        decryptPanel.add(decryptWarning);
        
        contentPanel.add(decryptPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Backup button
        JPanel backupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backupPanel.setBackground(Color.WHITE);
        JButton backupButton = new JButton("Backup Log File");
        backupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                backupLogFile();
            }
        });
        backupPanel.add(backupButton);

        contentPanel.add(backupPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Auto-clear setting
        JPanel autoClearPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        autoClearPanel.setBackground(Color.WHITE);
        autoClearPanel.setBorder(BorderFactory.createTitledBorder("Security"));
        JLabel autoClearLabel = new JLabel("Auto-clear after inactivity (minutes, 0 to disable): ");
        autoClearSpinner = new JSpinner(new SpinnerNumberModel(30, 0, 120, 5));
        autoClearPanel.add(autoClearLabel);
        autoClearPanel.add(autoClearSpinner);

        contentPanel.add(autoClearPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Password reminder
        JPanel reminderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reminderPanel.setBackground(Color.WHITE);
        reminderPanel.setBorder(BorderFactory.createTitledBorder("Password Reminder"));
        JLabel reminderLabel = new JLabel("Optional reminder (stored in plain text): ");
        reminderField = new JTextField(20);
        reminderPanel.add(reminderLabel);
        reminderPanel.add(reminderField);

        contentPanel.add(reminderPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Apply button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(Color.WHITE);
        applyButton = new JButton("Apply Changes");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applySettings();
            }
        });
        buttonPanel.add(applyButton);

        contentPanel.add(buttonPanel);

        // Status label
        statusLabel = new JLabel("");
        statusLabel.setForeground(Color.BLUE);
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(Color.WHITE);
        statusPanel.add(statusLabel);
        contentPanel.add(statusPanel);

        add(contentPanel, BorderLayout.NORTH);
    }

    private void loadCurrentSettings() {
        String autoClear = settings.getProperty("autoClearMinutes", "30");
        try {
            autoClearSpinner.setValue(Integer.parseInt(autoClear));
        } catch (NumberFormatException e) {
            autoClearSpinner.setValue(30);
        }
        reminderField.setText(settings.getProperty("passwordReminder", ""));
    }

    private void applySettings() {
        boolean enable = encryptionCheckBox.isSelected();
        String currentEnc = settings.getProperty("encrypted");

        if (enable && !"true".equals(currentEnc)) {
            // Enabling encryption
            enableEncryption();
        } else if (!enable && "true".equals(currentEnc)) {
            // User unchecked the box but file is still encrypted
            statusLabel.setText("Use the 'Decrypt Log File' button to decrypt the file.");
            statusLabel.setForeground(Color.ORANGE);
            encryptionCheckBox.setSelected(true); // Keep it checked until they decrypt
            return;
        } else {
            statusLabel.setText("No encryption changes to apply.");
        }

        // Save auto-clear setting
        int autoClear = (Integer) autoClearSpinner.getValue();
        settings.setProperty("autoClearMinutes", String.valueOf(autoClear));
        settings.setProperty("passwordReminder", reminderField.getText());
        saveSettings();
        statusLabel.setText("Settings saved.");
        statusLabel.setForeground(Color.BLUE);
    }

    private void enableEncryption() {
        PasswordDialog.PasswordResult pwdResult = PasswordDialog.showPasswordDialog(editor, "Enter new password (min 16 chars, 1 uppercase, 1 special char)", reminderField.getText(), false);
        char[] pwd = pwdResult.password;
        if (pwd == null) return;

        if (pwd.length < 16) {
            JOptionPane.showMessageDialog(editor, "Password must be at least 16 characters");
            return;
        }

        boolean hasUpper = false;
        boolean hasSpecial = false;
        for (char c : pwd) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        if (!hasUpper || !hasSpecial) {
            JOptionPane.showMessageDialog(editor, "Password must contain at least one uppercase letter and one special character (e.g., !@#$%^&*()_+-=[]{}|;':\",./<>?)");
            return;
        }

        PasswordDialog.PasswordResult confirmResult = PasswordDialog.showPasswordDialog(editor, "Confirm new password", reminderField.getText(), false);
        char[] confirm = confirmResult.password;
        if (!java.util.Arrays.equals(pwd, confirm)) {
            JOptionPane.showMessageDialog(editor, "Passwords do not match");
            return;
        }

        // Encrypt current file
        try {
            logFileHandler.enableEncryption(pwd);

            settings.setProperty("encrypted", "true");
            settings.setProperty("salt", Base64.getEncoder().encodeToString(logFileHandler.getSalt()));
            saveSettings();
            
            // Verify the settings were saved correctly
            String savedSalt = settings.getProperty("salt");
            if (savedSalt == null || savedSalt.isEmpty()) {
                throw new Exception("Failed to save encryption salt to settings");
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

    private void backupLogFile() {
        int confirm = JOptionPane.showConfirmDialog(editor, "Backups are copies of your current log file.\nIf encrypted, the backup will remain encrypted for security.\nDo you want to proceed?", "Backup Info", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        JFileChooser chooser = new JFileChooser();
        String date = LocalDate.now().toString();
        chooser.setSelectedFile(new java.io.File("loghog-backup-" + date + ".txt"));
        javax.swing.filechooser.FileFilter filter = new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                if (f.isDirectory()) return true;
                String name = f.getName();
                return name.startsWith("loghog-backup-") && name.endsWith(".txt");
            }
            @Override
            public String getDescription() {
                return "LogHog backup files (*.txt)";
            }
        };
        chooser.setFileFilter(filter);
        int res = chooser.showSaveDialog(editor);
        if (res == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = chooser.getSelectedFile();
            Path backupPath = selectedFile.toPath();
            try {
                Files.copy(Paths.get(System.getProperty("user.home"), "log.txt"), backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                statusLabel.setText("Backup saved to: " + backupPath.toString());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editor, "Backup failed: " + ex.getMessage());
            }
        }
    }

    private void saveSettings() {
        try (FileOutputStream fos = new FileOutputStream(settingsPath.toFile())) {
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

        // Show security warning
        int confirm = JOptionPane.showConfirmDialog(editor,
            "<html><b>WARNING: Security Risk</b><br><br>" +
            "This will permanently decrypt your log file and save it as plain text.<br>" +
            "Anyone with access to your computer will be able to read the file.<br><br>" +
            "A backup of the encrypted file will be saved as log.txt.bak<br><br>" +
            "Are you sure you want to proceed?</html>",
            "Decrypt Log File - Security Warning",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // Decrypt the file
            logFileHandler.disableEncryption();
            
            // Update settings - but save BEFORE clearing in case something goes wrong
            String oldEncrypted = settings.getProperty("encrypted");
            String oldSalt = settings.getProperty("salt");
            
            settings.setProperty("encrypted", "false");
            settings.remove("salt");
            saveSettings();
            
            // Verify decryption worked by trying to read the file
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
            
            // Update UI
            encryptionCheckBox.setSelected(false);
            statusLabel.setText("File decrypted successfully. Encryption disabled.");
            statusLabel.setForeground(new Color(0, 128, 0)); // Green
            
            // Reload data
            editor.getFullLogPanel().loadFullLog();
            
            JOptionPane.showMessageDialog(editor,
                "Log file has been decrypted successfully.\nA backup of the encrypted file was saved as log.txt.bak",
                "Decryption Complete",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(editor,
                "Decryption failed: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Decryption failed.");
            statusLabel.setForeground(Color.RED);
        }
    }
}