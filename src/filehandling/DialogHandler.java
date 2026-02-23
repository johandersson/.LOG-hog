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

package filehandling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import gui.DialogHelper;

/**
 * Handles user dialogs for file operations and error recovery.
 * Extracted from LogFileHandler to separate UI concerns from file operations.
 */
public class DialogHandler {
    
    /**
     * Shows error dialog with formatted message.
     * Extracts title from HTML message if present.
     */
    public static void showErrorDialog(String message) {
        // Extract title from HTML message if present
        final String computedTitle;
        final String computedDetails;
        if (message != null && message.contains("<b>") && message.contains("</b>")) {
            int start = message.indexOf("<b>") + 3;
            int end = message.indexOf("</b>");
            computedTitle = message.substring(start, end).replace("💾 ", "").replace("🔄 ", "");
            computedDetails = message.substring(end + 4).replace("<html>", "").replace("</html>", "").replace("<br><br>", "<br>").trim();
        } else {
            computedTitle = "Error";
            computedDetails = message;
        }

        runOnEDT(() -> {
            DialogHelper.showError(null, "Error", computedTitle, computedDetails);
            return null;
        });
    }
    
    /**
     * Shows error dialog with recovery options including backup restore.
     */
    public static void showErrorDialogWithRecovery(String message, String title, Runnable onRestoreBackup) {
        Object[] options = {"OK", "Restore from Backup"};
        // Extract clean message from HTML
        String cleanMsg = message.replace("<html>", "").replace("</html>", "").replace("<b>", "").replace("</b>", "").replace("<i>", "").replace("</i>", "");
        int choice = runOnEDT(() -> DialogHelper.showOptions(
            null,
            title,
            title,
            cleanMsg,
            JOptionPane.ERROR_MESSAGE,
            new Object[]{"OK", "Restore from Backup"},
            "OK"
        ));

        if (choice == 1 && onRestoreBackup != null) {
            // Run restore on background thread to avoid blocking EDT during file IO
            new Thread(() -> onRestoreBackup.run(), "BackupRestore").start();
        }
    }
    
    /**
     * Shows dialog when log file is missing, offering to create new or restore from backup.
     * @return true if file now exists, false otherwise
     */
    public static boolean handleMissingLogFile(Path filePath, Runnable onInvalidateCache) {
        if (Files.exists(filePath)) {
            return true; // File exists, no action needed
        }
        
        String message = String.format(
            "<html><b>⚠️ Log File Not Found</b><br><br>" +
            "The log file <b>%s</b> does not exist.<br><br>" +
            "Would you like to:<br>" +
            "• <b>Create a new log file</b> (starts fresh)<br>" +
            "• <b>Restore from backup</b> (if available)<br>" +
            "• <b>Exit</b> and manually fix the issue</html>",
            filePath.getFileName()
        );
        
        Object[] options = {"Create New", "Restore from Backup", "Exit"};
        int choice = runOnEDT(() -> DialogHelper.showOptions(
            null,
            "Log File Missing",
            "Log File Missing",
            message,
            JOptionPane.WARNING_MESSAGE,
            new Object[]{"Create New", "Restore from Backup", "Exit"},
            "Create New"
        ));
        
        if (choice == 0) {
            // Create new file
            try {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, ".LOG" + LogFileFormat.LINE_SEPARATOR + LogFileFormat.LINE_SEPARATOR);
                runOnEDT(() -> {
                    DialogHelper.showSuccess(
                        null,
                        "Success",
                        "File Created",
                        "New log file created successfully!<br><br>" +
                        "Location: <b>" + filePath + "</b>"
                    );
                    return null;
                });
                return true;
            } catch (Exception e) {
                // Security: Don't expose internal error details
                showErrorDialog("<html><b>Failed to create log file</b><br><br>" +
                    "Unable to create the log file. Please check permissions and try again.</html>");
                return false;
            }
        } else if (choice == 1) {
            // Restore from backup
            return showBackupRestoreDialog(filePath, onInvalidateCache);
        } else {
            // Exit
            return false;
        }
    }
    
    /**
     * Shows backup restore dialog and allows user to select a backup file.
     * @return true if restore was successful, false otherwise
     */
    public static boolean showBackupRestoreDialog(Path filePath, Runnable onInvalidateCache) {
        final java.io.File[] selected = new java.io.File[1];

        // Show file chooser on EDT and capture selection
        runOnEDT(() -> {
            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
            fileChooser.setDialogTitle("Select Backup File to Restore");
            fileChooser.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(java.io.File f) {
                    return f.isDirectory() || f.getName().endsWith(".txt") || f.getName().endsWith(".bak");
                }

                @Override
                public String getDescription() {
                    return "Backup Files (*.txt, *.bak)";
                }
            });

            int result = fileChooser.showOpenDialog(null);
            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                selected[0] = fileChooser.getSelectedFile();
            }
            return null;
        });

        if (selected[0] != null) {
            java.io.File backupFile = selected[0];
            try {
                // Perform file copy off the EDT to avoid blocking UI
                Files.copy(backupFile.toPath(), filePath, StandardCopyOption.REPLACE_EXISTING);
                if (onInvalidateCache != null) {
                    SwingUtilities.invokeLater(onInvalidateCache);
                }
                runOnEDT(() -> {
                    DialogHelper.showSuccess(
                        null,
                        "Restore Complete",
                        "Backup Restored",
                        "Backup restored successfully!<br><br>" +
                        "From: <b>" + backupFile.getName() + "</b><br>" +
                        "To: <b>" + filePath.getFileName() + "</b>"
                    );
                    return null;
                });
                return true;
            } catch (Exception e) {
                // Security: Don't expose internal error details
                showErrorDialog("<html><b>Restore Failed</b><br><br>" +
                    "Unable to restore from backup. The backup file may be corrupted.</html>");
                return false;
            }
        }
        return false;
    }

    /**
     * Shows a friendly dialog when a resource limit is exceeded (file too large or too many entries).
     * This provides an explanation and suggested actions for the user.
     */
    public static void showLimitExceeded(String shortTitle, String longMessage) {
        String message = String.format(
            "<html><b>⚠️ %s</b><br><br>%s<br><br>Suggested actions:<br>• Use the Log List view with filters to find older entries<br>• Archive or rollover large log files (monthly/yearly)<br>• Increase limits in advanced settings if you know what you're doing</html>",
            shortTitle,
            longMessage
        );

        runOnEDT(() -> {
            DialogHelper.showError(null, "Limit Exceeded", shortTitle, message);
            return null;
        });
    }

    private static <T> T runOnEDT(Callable<T> callable) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> exRef = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result.set(callable.call());
                } catch (Exception e) {
                    exRef.set(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (exRef.get() != null) {
            throw new RuntimeException(exRef.get());
        }
        return result.get();
    }
}
