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

import javax.swing.JOptionPane;

import gui.DialogHelper;

/**
 * Handles user dialogs for file operations and error recovery.
 * Extracted from LogFileHandler to separate UI concerns from file operations.
 */
public class DialogHandler {
    public enum MissingFileAction {
        NONE,
        CREATED,
        COPIED,
        RESTORED,
        EXIT
    }

    private static MissingFileAction lastMissingFileAction = MissingFileAction.NONE;

    public static MissingFileAction getLastMissingFileAction() {
        return lastMissingFileAction;
    }
    
    /**
     * Shows error dialog with formatted message.
     * Extracts title from HTML message if present.
     */
    public static void showErrorDialog(String message) {
        // Extract title from HTML message if present
        String title = "Error";
        String details = message;
        if (message.contains("<b>") && message.contains("</b>")) {
            int start = message.indexOf("<b>") + 3;
            int end = message.indexOf("</b>");
            title = message.substring(start, end).replace("💾 ", "").replace("🔄 ", "");
            details = message.substring(end + 4).replace("<html>", "").replace("</html>", "").replace("<br><br>", "<br>").trim();
        }
        DialogHelper.showError(null, "Error", title, details);
    }
    
    /**
     * Shows error dialog with recovery options including backup restore.
     */
    public static void showErrorDialogWithRecovery(String message, String title, Runnable onRestoreBackup) {
        Object[] options = {"OK", "Restore from Backup"};
        // Extract clean message from HTML
        String cleanMsg = message.replace("<html>", "").replace("</html>", "").replace("<b>", "").replace("</b>", "").replace("<i>", "").replace("</i>", "");
        int choice = DialogHelper.showOptions(
            null,
            title,
            title,
            cleanMsg,
            JOptionPane.ERROR_MESSAGE,
            new Object[]{"OK", "Restore from Backup"},
            "OK"
        );
        
        if (choice == 1 && onRestoreBackup != null) {
            // User chose to restore from backup
            onRestoreBackup.run();
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
            "• <b>Browse for log.txt</b> (set as default)<br>" +
            "• <b>Restore from backup</b> (if available)<br>" +
            "• <b>Exit and manually fix the issue </b><br></html>",
            filePath.getFileName()
        );

        Object[] options = {"Create New", "Browse...", "Restore from Backup", "Exit"};
        int choice = DialogHelper.showOptions(
            null,
            "Log File Missing",
            "Log File Missing",
            message,
            JOptionPane.WARNING_MESSAGE,
            options,
            "Create New"
        );

        // reset action
        lastMissingFileAction = MissingFileAction.NONE;

        if (choice == 0) {
            // Create new file
            try {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, ".LOG" + LogFileFormat.LINE_SEPARATOR + LogFileFormat.LINE_SEPARATOR);
                DialogHelper.showSuccess(
                    null,
                    "Success",
                    "File Created",
                    "New log file created successfully!<br><br>" +
                    "Location: <b>" + filePath + "</b>"
                );
                // Inform user that new file will NOT be encrypted until they enable encryption
                DialogHelper.showInfo(null, "New File Created", "Not Encrypted", "The new log file is currently unencrypted. You must enable encryption in Settings to encrypt it.");
                lastMissingFileAction = MissingFileAction.CREATED;
                if (onInvalidateCache != null) onInvalidateCache.run();
                return true;
            } catch (Exception e) {
                showErrorDialog("<html><b>Failed to create log file</b><br><br>Unable to create the log file. Please check permissions and try again.</html>");
                return false;
            }
        } else if (choice == 1) {
            // Browse for log.txt
            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
            fileChooser.setDialogTitle("Select log.txt file");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(java.io.File f) {
                    return f.isDirectory() || f.getName().equalsIgnoreCase("log.txt");
                }
                @Override
                public String getDescription() {
                    return "Log File (log.txt)";
                }
            });
            int result = fileChooser.showOpenDialog(null);
            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                java.io.File selectedFile = fileChooser.getSelectedFile();
                if (!selectedFile.getName().equalsIgnoreCase("log.txt")) {
                    showErrorDialog("<html><b>Invalid File</b><br><br>Please select a file named log.txt.</html>");
                    return false;
                }
                // Optionally validate file contents (e.g., check for .LOG header)
                try {
                    // Read only the prefix of the file (cheap) to detect the .LOG header
                    java.nio.file.Path selPath = selectedFile.toPath();
                    try (java.io.InputStream in = java.nio.file.Files.newInputStream(selPath)) {
                        byte[] buf = new byte[4];
                        int r = in.read(buf);
                        String start = r > 0 ? new String(buf, 0, Math.max(0, r), java.nio.charset.StandardCharsets.UTF_8) : "";
                        if (!start.startsWith(".LOG")) {
                            int confirm = DialogHelper.showOptions(
                                null,
                                "Confirm log.txt",
                                "Confirm log.txt",
                                "Selected file does not appear to be a valid log.txt. Set as default anyway?",
                                JOptionPane.QUESTION_MESSAGE,
                                new Object[]{"Yes", "No"},
                                "No"
                            );
                            if (confirm != 0) return false;
                        }
                    }
                } catch (Exception e) {
                    showErrorDialog("<html><b>File Read Error</b><br><br>Unable to read the selected file.</html>");
                    return false;
                }
                // Ask user if they want to use this file as the active log file (copy into place)
                int confirm = DialogHelper.showOptions(
                    null,
                    "Use Selected File",
                    "Use Selected File",
                    "Copy the selected file to the default log location and use it as the active log file?",
                    JOptionPane.QUESTION_MESSAGE,
                    new Object[]{"Yes", "No"},
                    "No"
                );
                if (confirm == 0) {
                    // Run copy on background thread and show modal progress dialog
                    final gui.LoadingProgressDialog progress = new gui.LoadingProgressDialog(null, "Copying File", true);
                    progress.setStatus("Copying selected log file...");
                    progress.setIndeterminate(true);

                    final java.util.concurrent.atomic.AtomicBoolean success = new java.util.concurrent.atomic.AtomicBoolean(false);
                    final java.util.concurrent.atomic.AtomicReference<Exception> err = new java.util.concurrent.atomic.AtomicReference<>();

                    Thread bg = new Thread(() -> {
                        try {
                                    Files.createDirectories(filePath.getParent());
                                    // Only copy if selected file path appears safe
                                    java.nio.file.Path sel = selectedFile.toPath();
                                    if (!security.PathValidator.isSafeFilePath(sel)) {
                                        // Record error for UI thread and exit runnable
                                        err.set(new IllegalArgumentException("Selected file path is not allowed"));
                                        javax.swing.SwingUtilities.invokeLater(() -> showErrorDialog("<html><b>Invalid File</b><br><br>Selected file path is not allowed.</html>"));
                                        return;
                                    }
                                    Files.copy(sel, filePath, StandardCopyOption.REPLACE_EXISTING);
                            if (onInvalidateCache != null) onInvalidateCache.run();
                            lastMissingFileAction = MissingFileAction.COPIED;
                            success.set(true);
                        } catch (Exception e) {
                            err.set(e);
                        } finally {
                            javax.swing.SwingUtilities.invokeLater(() -> progress.close());
                        }
                    }, "loghog-copy-selected-file");
                    bg.setDaemon(true);
                    bg.start();

                    // Block UI with modal dialog until copy completes
                    progress.showModal();

                    if (success.get()) {
                        DialogHelper.showSuccess(null, "Default Set", "Default log.txt Updated", "Copied and set active log file:<br><b>" + filePath.toString() + "</b>");
                        return true;
                    } else {
                        showErrorDialog("<html><b>Copy Failed</b><br><br>Unable to copy the selected file to the default location.</html>");
                        return false;
                    }
                }
                return false;
            }
            return false;
        } else if (choice == 2) {
            // Restore from backup
            lastMissingFileAction = MissingFileAction.RESTORED;
            return showBackupRestoreDialog(filePath, onInvalidateCache);
        } else {
            // Exit
            lastMissingFileAction = MissingFileAction.EXIT;
            return false;
        }
    }
    
    /**
     * Shows backup restore dialog and allows user to select a backup file.
     * @return true if restore was successful, false otherwise
     */
    public static boolean showBackupRestoreDialog(Path filePath, Runnable onInvalidateCache) {
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
            java.io.File backupFile = fileChooser.getSelectedFile();

            // Use a modal progress dialog and run the copy on a background thread.
            var progress = new gui.LoadingProgressDialog(null, "Restoring Backup", true);
            progress.setStatus("Restoring backup: " + backupFile.getName());
            progress.setIndeterminate(true);

            final java.util.concurrent.atomic.AtomicBoolean success = new java.util.concurrent.atomic.AtomicBoolean(false);
            final java.util.concurrent.atomic.AtomicReference<Exception> errorRef = new java.util.concurrent.atomic.AtomicReference<>();

                    Thread bg = new Thread(() -> {
                try {
                    // Validate backup path before copying
                    java.nio.file.Path b = backupFile.toPath();
                    if (!security.PathValidator.isSafeFilePath(b)) {
                        // Record error and close
                        errorRef.set(new IllegalArgumentException("Selected backup path is not allowed"));
                        javax.swing.SwingUtilities.invokeLater(() -> showErrorDialog("<html><b>Invalid Backup</b><br><br>Selected backup path is not allowed.</html>"));
                        return;
                    }
                    Files.copy(b, filePath, StandardCopyOption.REPLACE_EXISTING);
                    if (onInvalidateCache != null) onInvalidateCache.run();
                    success.set(true);
                } catch (Exception e) {
                    errorRef.set(e);
                } finally {
                    // Close the modal dialog from the EDT
                    javax.swing.SwingUtilities.invokeLater(() -> progress.close());
                }
            }, "loghog-restore-backup");
            bg.setDaemon(true);
            bg.start();

            // Show modal dialog (blocks until progress.close() is called)
            progress.showModal();

            if (success.get()) {
                DialogHelper.showSuccess(
                    null,
                    "Restore Complete",
                    "Backup Restored",
                    "Backup restored successfully!<br><br>" +
                    "From: <b>" + backupFile.getName() + "</b><br>" +
                    "To: <b>" + filePath.getFileName() + "</b>"
                );
                return true;
            } else {
                showErrorDialog("<html><b>Restore Failed</b><br><br>Unable to restore from backup. The backup file may be corrupted.</html>");
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

        DialogHelper.showError(null, "Limit Exceeded", shortTitle, message);
    }
}
