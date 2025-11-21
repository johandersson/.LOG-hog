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
        // Already loaded in init
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
    }

    private void enableEncryption() {
        char[] pwd = PasswordDialog.showPasswordDialog(editor, "Enter password");
        if (pwd == null) return;

        if (pwd.length < 16) {
            JOptionPane.showMessageDialog(editor, "Password must be at least 16 characters");
            return;
        }

        char[] confirm = PasswordDialog.showPasswordDialog(editor, "Confirm password");
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
        int confirm = JOptionPane.showConfirmDialog(editor, "Backups are not encrypted! They will be stored as plain text.\nDo you want to proceed?", "Backup Warning", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int res = chooser.showSaveDialog(editor);
        if (res == JFileChooser.APPROVE_OPTION) {
            Path dir = chooser.getSelectedFile().toPath();
            String date = LocalDate.now().toString();
            Path backupPath = dir.resolve("loghog-backup-" + date + ".txt");
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