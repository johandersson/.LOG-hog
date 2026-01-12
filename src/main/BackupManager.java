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

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import gui.DialogHelper;
import gui.LoadingProgressDialog;

/**
 * Manages automatic and manual backups of log files.
 * Provides secure backup operations with configurable settings.
 */
public class BackupManager {
    private final Properties settings;
    private final SecureRandom random = new SecureRandom();
    private static final int MAX_NUMBERED_BACKUPS = 5;
    private static final int MAX_AUTO_BACKUPS = 10;
    private static final long SHOW_PROGRESS_THRESHOLD = 1024 * 1024; // Show progress for files > 1MB
    private long lastBackupTime = 0;
    private long lastFileModified = 0;
    private Frame parentFrame;

    public BackupManager(Properties settings) {
        this.settings = settings;
    }
    
    /**
     * Sets the parent frame for showing progress dialogs.
     */
    public void setParentFrame(Frame parent) {
        this.parentFrame = parent;
    }
    
    /**
     * Checks if backup directory is configured, and prompts user to set it up if not.
     * Should be called once on first use of auto-backup feature.
     */
    public void ensureBackupDirectoryConfigured() {
        String backupDir = settings.getProperty("backupDirectory", "");
        String configuredFlag = settings.getProperty("backupDirectoryConfigured", "false");
        
        // If already configured (either explicitly set or user made a choice), skip
        if (!backupDir.isEmpty() || "true".equals(configuredFlag)) {
            return;
        }
        
        // Show one-time setup dialog
        if (parentFrame != null) {
            SwingUtilities.invokeLater(() -> showBackupDirectorySetupDialog());
        }
    }
    
    /**
     * Shows a dialog prompting user to configure backup directory.
     */
    private void showBackupDirectorySetupDialog() {
        String homeDir = System.getProperty("user.home");
        String message = String.format(
            "<html><b>Backup Directory Not Configured</b><br><br>" +
            "You haven't set up a backup directory yet.<br><br>" +
            "<b>Current default:</b> %s<br><br>" +
            "Would you like to:<br>" +
            "• <b>Use your home directory</b> (default, easy)<br>" +
            "• <b>Configure a custom directory</b> (recommended)<br><br>" +
            "<i>This dialog will only appear once.</i></html>",
            homeDir
        );
        
        Object[] options = {"Configure Directory", "Use Home Directory"};
        int choice = DialogHelper.showOptions(
            parentFrame,
            "Backup Directory Setup",
            "Backup Directory Setup",
            message,
            JOptionPane.QUESTION_MESSAGE,
            options,
            options[0]
        );
        
        if (choice == 0) {
            // Configure custom directory
            configureCustomBackupDirectory();
        } else if (choice == 1) {
            // Use home directory
            settings.setProperty("backupDirectory", homeDir);
            settings.setProperty("backupDirectoryConfigured", "true");
            saveSettings();
            
            DialogHelper.showInfo(
                parentFrame,
                "Backup Directory Set",
                "Backup Directory Set",
                "Backups will be saved to:<br><b>" + homeDir + "</b><br><br>" +
                "You can change this later in Settings."
            );
        } else {
            // User closed dialog - use home directory as default
            settings.setProperty("backupDirectory", homeDir);
            settings.setProperty("backupDirectoryConfigured", "true");
            saveSettings();
        }
    }
    
    /**
     * Opens file chooser for user to select custom backup directory.
     */
    private void configureCustomBackupDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Backup Directory");
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        
        int result = fileChooser.showDialog(parentFrame, "Select");
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            String dirPath = selectedDir.getAbsolutePath();
            
            // Verify directory is writable
            if (!selectedDir.canWrite()) {
                DialogHelper.showError(
                    parentFrame,
                    "Error",
                    "Permission Denied",
                    "Cannot write to selected directory.<br>" +
                    "Please choose a different location."
                );
                // Try again
                configureCustomBackupDirectory();
                return;
            }
            
            settings.setProperty("backupDirectory", dirPath);
            settings.setProperty("backupDirectoryConfigured", "true");
            saveSettings();
            
            DialogHelper.showInfo(
                parentFrame,
                "Backup Directory Set",
                "Backup Directory Set",
                "Backups will be saved to:<br><b>" + dirPath + "</b><br><br>" +
                "You can change this later in Settings."
            );
        } else {
            // User cancelled - ask if they want to use home directory instead
            String homeDir = System.getProperty("user.home");
            int fallback = JOptionPane.showConfirmDialog(
                parentFrame,
                "<html>No directory selected.<br><br>" +
                "Use home directory (<b>" + homeDir + "</b>) instead?</html>",
                "Use Default?",
                JOptionPane.YES_NO_OPTION
            );
            
            if (fallback == JOptionPane.YES_OPTION) {
                settings.setProperty("backupDirectory", homeDir);
                settings.setProperty("backupDirectoryConfigured", "true");
                saveSettings();
            }
        }
    }
    
    /**
     * Saves settings to file.
     */
    private void saveSettings() {
        try {
            Path settingsPath = Paths.get(System.getProperty("user.home"), ".loghog", "settings.properties");
            Files.createDirectories(settingsPath.getParent());
            try (var out = new java.io.FileOutputStream(settingsPath.toFile())) {
                settings.store(out, "LogHog Settings");
            }
        } catch (Exception e) {
            System.err.println("Failed to save backup settings: " + e.getMessage());
        }
    }

    /**
     * Creates a backup on application startup if file exists.
     * This protects against data loss from crashes or bugs.
     * Also ensures backup directory is configured if auto-backup is enabled.
     */
    public void performStartupBackup() {
        if (!isAutoBackupEnabled()) {
            return;
        }
        
        // Ensure backup directory is configured (only if auto-backup is already enabled)
        // Note: On first startup, auto-backup is disabled, so this won't trigger.
        // The main trigger is when user enables auto-backup in Settings.
        ensureBackupDirectoryConfigured();
        
        try {
            Path logPath = getLogFilePath();
            if (Files.exists(logPath) && Files.size(logPath) > 0) {
                performAutomaticBackup();
            }
        } catch (Exception e) {
            // Silently fail - don't block startup
        }
    }

    /**
     * Checks if periodic backup is needed based on time and file changes.
     * Call this periodically (e.g., every minute) for minimal overhead.
     */
    public void checkPeriodicBackup() {
        if (!isAutoBackupEnabled()) {
            return;
        }
        
        try {
            Path logPath = getLogFilePath();
            if (!Files.exists(logPath)) {
                return;
            }
            
            long currentModified = Files.getLastModifiedTime(logPath).toMillis();
            long currentTime = System.currentTimeMillis();
            int intervalMinutes = getBackupIntervalMinutes();
            
            // Only backup if: file changed AND enough time passed
            if (currentModified > lastFileModified && 
                (currentTime - lastBackupTime) > (intervalMinutes * 60000L)) {
                performAutomaticBackup();
                lastBackupTime = currentTime;
                lastFileModified = currentModified;
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    /**
     * Performs automatic backup if enabled in settings.
     * Called after encryption/decryption operations or periodically.
     */
    public void performAutomaticBackup() {
        if (!isAutoBackupEnabled()) {
            return;
        }

        LoadingProgressDialog progressDialog = null;
        
        try {
            Path backupPath = createBackupPath();
            Path logPath = getLogFilePath();
            
            // Show progress dialog for larger files
            long fileSize = Files.exists(logPath) ? Files.size(logPath) : 0;
            if (fileSize > SHOW_PROGRESS_THRESHOLD && parentFrame != null) {
                progressDialog = new LoadingProgressDialog(parentFrame, "Automatic Backup");
                LoadingProgressDialog finalDialog = progressDialog;
                SwingUtilities.invokeLater(() -> {
                    finalDialog.setStatus("Saving backup...");
                    finalDialog.setProgress(0);
                    finalDialog.show();
                });
            }

            // Ensure backup directory exists
            Files.createDirectories(backupPath.getParent());
            
            if (progressDialog != null) {
                LoadingProgressDialog finalDialog = progressDialog;
                SwingUtilities.invokeLater(() -> finalDialog.setProgress(25));
            }

            // Securely delete existing file if it exists
            if (Files.exists(backupPath)) {
                secureDelete(backupPath);
            }
            
            if (progressDialog != null) {
                LoadingProgressDialog finalDialog = progressDialog;
                SwingUtilities.invokeLater(() -> finalDialog.setProgress(50));
            }

            // Copy log file
            Files.copy(logPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            
            if (progressDialog != null) {
                LoadingProgressDialog finalDialog = progressDialog;
                SwingUtilities.invokeLater(() -> finalDialog.setProgress(75));
            }
            
            // Verify backup was created successfully
            if (!verifyBackup(logPath, backupPath)) {
                System.err.println("Backup verification failed");
                return;
            }
            
            if (progressDialog != null) {
                LoadingProgressDialog finalDialog = progressDialog;
                SwingUtilities.invokeLater(() -> finalDialog.setProgress(100));
            }
            
            // Rotate old backups asynchronously to avoid blocking
            new Thread(() -> rotateAutoBackups()).start();

        } catch (Exception e) {
            System.err.println("Automatic backup failed: " + e.getMessage());
            // Don't show UI errors for automatic backups
        } finally {
            // Close progress dialog after a brief delay to show completion
            if (progressDialog != null) {
                LoadingProgressDialog finalDialog = progressDialog;
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Show completion briefly
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    finalDialog.close();
                }).start();
            }
        }
    }

    /**
     * Creates a numbered backup (.bak, .bak.1, .bak.2, etc.) before file modifications.
     * This is fast and keeps recent history without accumulating files.
     */
    public void createNumberedBackup() {
        try {
            Path logPath = getLogFilePath();
            if (!Files.exists(logPath)) {
                return;
            }
            
            Path bakPath = Paths.get(logPath.toString() + ".bak");
            
            // Rotate existing numbered backups (bak.4 -> delete, bak.3 -> bak.4, etc.)
            for (int i = MAX_NUMBERED_BACKUPS - 1; i > 0; i--) {
                Path oldBackup = Paths.get(bakPath.toString() + "." + i);
                if (i == MAX_NUMBERED_BACKUPS - 1) {
                    // Delete oldest
                    Files.deleteIfExists(oldBackup);
                } else {
                    Path newBackup = Paths.get(bakPath.toString() + "." + (i + 1));
                    if (Files.exists(oldBackup)) {
                        Files.move(oldBackup, newBackup, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            
            // Move .bak to .bak.1
            if (Files.exists(bakPath)) {
                Files.move(bakPath, Paths.get(bakPath.toString() + ".1"), StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Create new .bak
            Files.copy(logPath, bakPath, StandardCopyOption.REPLACE_EXISTING);
            
        } catch (Exception e) {
            // Don't fail the save operation if backup fails
            System.err.println("Numbered backup failed: " + e.getMessage());
        }
    }

    /**
     * Creates a manual backup with user interaction.
     * Returns the backup path if successful, null if cancelled or failed.
     */
    public Path createManualBackup() {
        // This would be implemented in the UI layer
        // For now, return null to indicate manual backup should be handled by SettingsPanel
        return null;
    }

    /**
     * Checks if automatic backup is enabled.
     */
    public boolean isAutoBackupEnabled() {
        return "true".equals(settings.getProperty("autoBackupEnabled", "false"));
    }

    /**
     * Rotates auto backups, keeping only the most recent MAX_AUTO_BACKUPS files.
     */
    private void rotateAutoBackups() {
        try {
            String backupDir = getAutoBackupDirectory();
            Path backupDirPath = Paths.get(backupDir);
            
            if (!Files.exists(backupDirPath)) {
                return;
            }
            
            // Find all auto backup files
            List<Path> backups = Files.list(backupDirPath)
                .filter(p -> p.getFileName().toString().startsWith("loghog-auto-backup-"))
                .sorted(Comparator.comparing(p -> {
                    try {
                        return Files.getLastModifiedTime(p);
                    } catch (IOException e) {
                        return null;
                    }
                }))
                .collect(Collectors.toList());
            
            // Delete oldest backups if we exceed the limit
            while (backups.size() > MAX_AUTO_BACKUPS) {
                Path oldest = backups.remove(0);
                Files.deleteIfExists(oldest);
            }
            
        } catch (Exception e) {
            // Silently fail rotation
        }
    }
    
    /**
     * Verifies a backup was created successfully by checking size.
     * Fast check without reading entire file content.
     */
    private boolean verifyBackup(Path original, Path backup) {
        try {
            return Files.exists(backup) && 
                   Files.size(backup) == Files.size(original);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the backup interval in minutes from settings.
     */
    private int getBackupIntervalMinutes() {
        try {
            return Integer.parseInt(settings.getProperty("autoBackupInterval", "30"));
        } catch (NumberFormatException e) {
            return 30; // Default 30 minutes
        }
    }

    /**
     * Gets the configured auto-backup directory.
     */
    public String getAutoBackupDirectory() {
        String backupDir = settings.getProperty("backupDirectory", "");
        if (backupDir.isEmpty()) {
            // Fallback to user home
            backupDir = System.getProperty("user.home");
        }
        return backupDir;
    }

    /**
     * Creates a unique backup file path with timestamp.
     */
    private Path createBackupPath() {
        String backupDir = getAutoBackupDirectory();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "loghog-auto-backup-" + timestamp + ".txt";
        return Paths.get(backupDir, filename);
    }

    /**
     * Gets the path to the log file.
     */
    private Path getLogFilePath() {
        return Paths.get(System.getProperty("user.home"), "log.txt");
    }

    /**
     * Securely deletes a file by overwriting it multiple times.
     */
    private void secureDelete(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            return;
        }

        long fileSize = Files.size(filePath);

        // Overwrite file multiple times with random data
        for (int pass = 0; pass < 3; pass++) {
            try (var raf = new java.io.RandomAccessFile(filePath.toFile(), "rw")) {
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