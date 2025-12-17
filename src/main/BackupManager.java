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
import java.util.Properties;

/**
 * Manages automatic and manual backups of log files.
 * Provides secure backup operations with configurable settings.
 */
public class BackupManager {
    private final Properties settings;
    private final SecureRandom random = new SecureRandom();

    public BackupManager(Properties settings) {
        this.settings = settings;
    }

    /**
     * Performs automatic backup if enabled in settings.
     * Called after encryption/decryption operations.
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

            System.out.println("Automatic backup created: " + backupPath.toString());

        } catch (Exception e) {
            System.err.println("Automatic backup failed: " + e.getMessage());
            // Don't show UI errors for automatic backups
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
     * Gets the configured auto-backup directory.
     */
    public String getAutoBackupDirectory() {
        String autoDir = settings.getProperty("autoBackupDirectory", "");
        if (autoDir.isEmpty()) {
            // Fallback to manual backup directory
            autoDir = settings.getProperty("backupDirectory", "");
        }
        if (autoDir.isEmpty()) {
            // Final fallback to user home
            autoDir = System.getProperty("user.home");
        }
        return autoDir;
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