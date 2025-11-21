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
            // Disabling encryption - not implemented yet
            statusLabel.setText("Disabling encryption is not yet implemented.");
        } else {
            statusLabel.setText("No changes to apply.");
        }

        // Save auto-clear setting
        int autoClear = (Integer) autoClearSpinner.getValue();
        settings.setProperty("autoClearMinutes", String.valueOf(autoClear));
        settings.setProperty("passwordReminder", reminderField.getText());
        saveSettings();
        statusLabel.setText("Settings saved.");
    }

    private void enableEncryption() {
        char[] pwd = PasswordDialog.showPasswordDialog(editor, "Enter new password (min 16 chars, 1 uppercase, 1 special char)", reminderField.getText());
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

        char[] confirm = PasswordDialog.showPasswordDialog(editor, "Confirm new password", reminderField.getText());
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

            statusLabel.setText("Encryption enabled successfully.");
            editor.loadLogEntries();
            editor.loadFullLog();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(editor, "Encryption failed: " + ex.getMessage());
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
}