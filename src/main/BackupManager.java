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

/**
 * Manages automatic and manual backups of log files.
 * Provides secure backup operations with configurable settings.
 */
public class BackupManager {
    private final Properties settings;
    private final SecureRandom random = new SecureRandom();
    private static final int MAX_NUMBERED_BACKUPS = 5;
    private static final int MAX_AUTO_BACKUPS = 10;
    private long lastBackupTime = 0;
    private long lastFileModified = 0;

    public BackupManager(Properties settings) {
        this.settings = settings;
    }

    /**
     * Creates a backup on application startup if file exists.
     * This protects against data loss from crashes or bugs.
     */
    public void performStartupBackup() {
        if (!isAutoBackupEnabled()) {
            return;
        }
        
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

        try {
            Path backupPath = createBackupPath();
            Path logPath = getLogFilePath();

            // Ensure backup directory exists
            Files.createDirectories(backupPath.getParent());

            // Securely delete existing file if it exists
            if (Files.exists(backupPath)) {
                secureDelete(backupPath);
            }

            // Copy log file
            Files.copy(logPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Verify backup was created successfully
            if (!verifyBackup(logPath, backupPath)) {
                System.err.println("Backup verification failed");
                return;
            }
            
            // Rotate old backups asynchronously to avoid blocking
            new Thread(() -> rotateAutoBackups()).start();

        } catch (Exception e) {
            System.err.println("Automatic backup failed: " + e.getMessage());
            // Don't show UI errors for automatic backups
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