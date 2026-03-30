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
import java.nio.file.StandardOpenOption;
import main.HmacUtils;
import main.SecurityEventLogger;
import main.TamperDetector;
import main.SecureDeletionUtils;
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
            "Please configure your backup directory.",
            message,
            JOptionPane.QUESTION_MESSAGE,
            options,
            options[0]
        );

        if (choice == 0) { // Configure Directory
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
                    showBackupDirectorySetupDialog();
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
            }
        } else if (choice == 1) { // Use Home Directory
            settings.setProperty("backupDirectory", homeDir);
            settings.setProperty("backupDirectoryConfigured", "true");
            saveSettings();
        }
    }
    
    /**
     * Saves settings to file.
     */
    private void saveSettings() {
        try {
            Path settingsPath = Paths.get(System.getProperty("user.home"), ".loghog", "settings.properties");
            Files.createDirectories(settingsPath.getParent());
            try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(settingsPath)) {
                settings.store(out, "LogHog Settings");
            }
            // Attempt to restrict settings file permissions to the current user only
            setOwnerOnlyPermissions(settingsPath);
        } catch (Exception e) {
            // Security: Don't log exception details to console
        }
    }

    private void setOwnerOnlyPermissions(Path path) {
        try {
            try {
                java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = java.nio.file.attribute.PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(path, perms);
                return;
            } catch (UnsupportedOperationException | SecurityException ignored) {
                // Not POSIX or not permitted
            }

            java.io.File f = path.toFile();
            f.setReadable(true, true);
            f.setWritable(true, true);
            f.setExecutable(false, true);
        } catch (Exception ignored) {
            // Best-effort only
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

        // If called on the EDT, run the backup in a background thread to avoid blocking UI.
        if (SwingUtilities.isEventDispatchThread()) {
            Thread bg = new Thread(() -> performAutomaticBackupImpl(), "loghog-automatic-backup");
            bg.setDaemon(true);
            bg.start();
            return;
        }

        // Otherwise run synchronously (useful for tests and non-UI callers)
        performAutomaticBackupImpl();
    }

    private void performAutomaticBackupImpl() {
        LoadingProgressDialog progressDialog = null;
        try {
            Path backupPath = createBackupPath();
            Path logPath = getLogFilePath();

            // Show progress dialog for larger files (only if parentFrame is set)
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
                SecureDeletionUtils.wipeFile(backupPath);
            }

            if (progressDialog != null) {
                LoadingProgressDialog finalDialog = progressDialog;
                SwingUtilities.invokeLater(() -> finalDialog.setProgress(50));
            }



            // Tamper detection: check if log file has been externally modified
            TamperDetector tamperDetector = new TamperDetector();
            try {
                tamperDetector.recordBaseline(logPath);
            } catch (Exception e) {
                // Silent fail, continue
            }

            Files.copy(logPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

            // After copy, check for tampering
            try {
                if (tamperDetector.isTampered(logPath)) {
                    SecurityEventLogger.log("TamperDetected", "Log file was modified during backup: " + logPath);
                }
            } catch (Exception e) {
                // Silent fail
            }

            // Compute and append HMAC for integrity verification
            byte[] key = deriveSimpleHmacKey();
            byte[] data = Files.readAllBytes(backupPath);
            byte[] hmac = HmacUtils.computeHmacSha256(key, data);
            Files.write(backupPath, hmac, StandardOpenOption.APPEND);
            SecurityEventLogger.log("BackupCreated", "Backup file created and HMAC appended: " + backupPath);

            if (progressDialog != null) {
                LoadingProgressDialog finalDialog = progressDialog;
                SwingUtilities.invokeLater(() -> finalDialog.setProgress(75));
            }


            // Verify backup was created successfully (size and HMAC)
            if (!verifyBackupWithHmac(logPath, backupPath)) {
                SecurityEventLogger.log("BackupVerificationFailed", "Backup verification failed for: " + backupPath);
                return;
            }

            if (progressDialog != null) {
                LoadingProgressDialog finalDialog = progressDialog;
                SwingUtilities.invokeLater(() -> finalDialog.setProgress(100));
            }

            // Rotate old backups asynchronously to avoid blocking
            Thread t = new Thread(() -> rotateAutoBackups());
            t.setDaemon(true);
            t.start();

        } catch (Exception e) {
            // Security: Don't log exception details to console
            // Don't show UI errors for automatic backups
        } finally {
            // Close progress dialog after a brief delay to show completion
            if (progressDialog != null) {
                LoadingProgressDialog finalDialog = progressDialog;
                Thread t2 = new Thread(() -> {
                    try {
                        Thread.sleep(500); // Show completion briefly
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    finalDialog.close();
                });
                t2.setDaemon(true);
                t2.start();
            }
        }
    }

    /**
     * Derives a simple HMAC key for backup integrity (not persisted, ephemeral for demo).
     * In production, use a securely stored key.
     */
    private byte[] deriveSimpleHmacKey() {
        // For demo: use a fixed string and user home as salt (not secure for real use)
        String base = System.getProperty("user.home") + "-loghog-hmac-key";
        try {
            return java.util.Arrays.copyOf(base.getBytes("UTF-8"), 32);
        } catch (Exception e) {
            return new byte[32];
        }
    }

    /**
     * Verifies backup by size and HMAC.
     */
    private boolean verifyBackupWithHmac(Path original, Path backup) {
        try {
            byte[] orig = Files.readAllBytes(original);
            byte[] backupAll = Files.readAllBytes(backup);
            if (backupAll.length < 32) return false;
            byte[] backupData = java.util.Arrays.copyOf(backupAll, backupAll.length - 32);
            byte[] backupHmac = java.util.Arrays.copyOfRange(backupAll, backupAll.length - 32, backupAll.length);
            byte[] key = deriveSimpleHmacKey();
            boolean sizeMatch = orig.length == backupData.length;
            boolean hmacMatch = HmacUtils.verifyHmacSha256(key, backupData, backupHmac);
            return sizeMatch && hmacMatch;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a numbered backup (.bak, .bak.1, .bak.2, etc.) before file modifications.
     * This is fast and keeps recent history without accumulating files.
     * Backups are stored in the configured backup directory.
     */
    public void createNumberedBackup() {
        try {
            Path logPath = getLogFilePath();
            if (!Files.exists(logPath)) {
                return;
            }
            // Determine if the log file is encrypted by inspecting header magic
            boolean isEncrypted = false;
            try (java.io.InputStream in = Files.newInputStream(logPath)) {
                byte[] hdr = new byte[4];
                int r = in.read(hdr);
                if (r == 4) {
                    isEncrypted = hdr[0] == 'L' && hdr[1] == 'O' && hdr[2] == 'G' && hdr[3] == 'H';
                }
            } catch (Exception ignored) {}

            // Get backup directory
            String backupDir = getAutoBackupDirectory();
            Path backupDirPath = Paths.get(backupDir);
            Files.createDirectories(backupDirPath);

            // Create backup file path in the backup directory
            String bakFilename = logPath.getFileName().toString() + (isEncrypted ? ".bak.enc" : ".bak");
            Path bakPath = backupDirPath.resolve(bakFilename);
            
            // Rotate existing numbered backups (bak.4 -> delete, bak.3 -> bak.4, etc.)
            for (int i = MAX_NUMBERED_BACKUPS - 1; i > 0; i--) {
                Path oldBackup = Paths.get(bakPath.toString() + "." + i);
                if (i == MAX_NUMBERED_BACKUPS - 1) {
                    // Securely delete oldest backup to prevent recovery
                    if (Files.exists(oldBackup)) {
                        SecureDeletionUtils.wipeFile(oldBackup);
                    }
                } else {
                    Path newBackup = Paths.get(bakPath.toString() + "." + (i + 1));
                    if (Files.exists(oldBackup)) {
                        Files.move(oldBackup, newBackup, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            
            // Move previous backup to numbered slot (.bak -> .bak.1 or .bak.enc -> .bak.enc.1)
            if (Files.exists(bakPath)) {
                Files.move(bakPath, Paths.get(bakPath.toString() + ".1"), StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Create new backup (encrypted files are copied as .bak.enc)
            Files.copy(logPath, bakPath, StandardCopyOption.REPLACE_EXISTING);

            // If the file is encrypted, attempt to securely delete any legacy plaintext backups
            if (isEncrypted) {
                try {
                    // Look for legacy .bak files and remove them securely
                    for (int i = 0; i < MAX_NUMBERED_BACKUPS; i++) {
                        Path legacy = Paths.get(backupDirPath.toString(), logPath.getFileName().toString() + ".bak" + (i == 0 ? "" : "." + i));
                        if (Files.exists(legacy)) {
                            SecureDeletionUtils.wipeFile(legacy);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
        } catch (Exception e) {
            // Don't fail the save operation if backup fails
            // Security: Don't log exception details to console
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
            
            // Securely delete oldest backups if we exceed the limit
            while (backups.size() > MAX_AUTO_BACKUPS) {
                Path oldest = backups.remove(0);
                try {
                    SecureDeletionUtils.wipeFile(oldest);
                } catch (Exception e) {
                    // If secure delete fails, log the error but don't crash
                    // Security: Don't expose details to console
                }
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
     * This method is public static so it can be used throughout the application
     * for consistent secure deletion of sensitive backup files.
     */
    // Secure deletion now uses SecureDeletionUtils.wipeFile
}