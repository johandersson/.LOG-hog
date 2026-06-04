/*
 * Copyright (C) 2026 Johan Andersson
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

import java.util.Base64;
import java.util.Properties;

import javax.swing.JFrame;
// Unused import removed for PMD compliance
import javax.swing.SwingUtilities;

import filehandling.LogFileHandler;
import filehandling.DialogHandler;
import encryption.EncryptionDetector;
import gui.DialogHelper;
import gui.LoadingProgressDialog;
import gui.PasswordDialog;
import gui.SecurityDelayDialog;

/**
 * Handles encryption setup, password authentication, and related security operations.
 * This class manages the encryption workflow including password prompts, retry logic,
 * progressive delays for failed attempts, and secure password handling.
 */
public class EncryptionHandler {

    private final JFrame parentFrame;
    private final LogFileHandler logFileHandler;
    private final Properties settings;
    private final Runnable loadLogEntriesCallback;
    private final Runnable updateUILockStateCallback;
    private final Runnable loadFullLogCallback;
    private final Runnable saveSettingsCallback;
    private final BackupManager backupManager;

    /**
     * Constructs an EncryptionHandler with the necessary dependencies.
     *
     * @param parentFrame the parent JFrame for dialogs
     * @param logFileHandler the LogFileHandler for encryption operations
     * @param settings the application settings
     * @param loadLogEntriesCallback callback to load log entries
     * @param updateUILockStateCallback callback to update UI lock state
     * @param loadFullLogCallback callback to load full log
     * @param backupManager the BackupManager for deriving the session HMAC key
     */
    public EncryptionHandler(JFrame parentFrame, LogFileHandler logFileHandler, Properties settings,
                           Runnable loadLogEntriesCallback, Runnable updateUILockStateCallback,
                           Runnable loadFullLogCallback, Runnable saveSettingsCallback,
                           BackupManager backupManager) {
        this.parentFrame = parentFrame;
        this.logFileHandler = logFileHandler;
        this.settings = settings;
        this.loadLogEntriesCallback = loadLogEntriesCallback;
        this.updateUILockStateCallback = updateUILockStateCallback;
        this.loadFullLogCallback = loadFullLogCallback;
        this.saveSettingsCallback = saveSettingsCallback;
        this.backupManager = backupManager;
    }

    /**
     * Handles the initial encryption setup process, including password authentication
     * and retry logic with progressive delays.
     */
    public boolean handleEncryptionSetup() {
        String saltStr = settings.getProperty("salt");
        if (saltStr != null) {
            // If the log file does not exist, offer create/browse/restore first
            java.nio.file.Path path = logFileHandler.getFilePath();
            if (!java.nio.file.Files.exists(path)) {
                boolean handled = DialogHandler.handleMissingLogFile(path, () -> {});
                if (!handled) {
                    // User chose to exit or did not provide a file; do not prompt for password
                    return false;
                }

                // Inspect what the user did and act accordingly
                DialogHandler.MissingFileAction action = DialogHandler.getLastMissingFileAction();
                if (action == DialogHandler.MissingFileAction.CREATED) {
                    // New file created -> clear encryption state (in-memory)
                    settings.setProperty("encrypted", "false");
                    settings.remove("salt");
                    // Persist the cleared settings immediately
                    try {
                        if (saveSettingsCallback != null) saveSettingsCallback.run();
                    } catch (Exception ex) {
                        logFileHandler.showErrorDialog("<html><b>💾 Settings Save Failed</b><br><br>Unable to persist settings after creating new log file.</html>");
                    }
                    // Note: DialogHandler already showed the "Not Encrypted" info dialog during file creation
                    return false;
                } else if (action == DialogHandler.MissingFileAction.COPIED || action == DialogHandler.MissingFileAction.RESTORED) {
                    // If user copied/restored a file into place, fall through and let detector decide
                } else {
                    return false;
                }
            }
            // Now, if file exists, use detector to decide whether to prompt for password
            java.nio.file.Path pathNow = logFileHandler.getFilePath();
            if (!java.nio.file.Files.exists(pathNow)) {
                return false;
            }

            boolean looksEncrypted = EncryptionDetector.isFileEncrypted(pathNow);
            if (!looksEncrypted) {
                // File is plain text -> clear encryption state
                settings.setProperty("encrypted", "false");
                settings.remove("salt");
                // Persist the cleared settings
                try {
                    if (saveSettingsCallback != null) saveSettingsCallback.run();
                } catch (Exception ex) {
                    logFileHandler.showErrorDialog("<html><b>💾 Settings Save Failed</b><br><br>Unable to persist settings after detecting an unencrypted log file.</html>");
                }
                return false;
            }

            // File looks encrypted -> decode the stored salt and authenticate
            saltStr = settings.getProperty("salt");
            if (saltStr == null) {
                DialogHelper.showError(parentFrame, "Missing Encryption Metadata", "Encrypted File Found",
                    "The selected log file is encrypted but the application has no encryption metadata (salt).\nPlease restore from a settings backup.");
                return false;
            }

            byte[] salt;
            try {
                salt = Base64.getDecoder().decode(saltStr);
            } catch (IllegalArgumentException e) {
                DialogHelper.showError(parentFrame, "Corrupt Encryption Metadata", "Invalid Settings",
                    "The stored encryption salt is corrupt or invalid. Please restore from a settings backup.");
                return false;
            }
            return performPasswordAuthentication(salt, "\uD83D\uDD12 Enter Password", true);
        }
        return false;
    }

    /**
     * Reloads an encrypted log by prompting for password and handling authentication.
     *
     * @return true if unlock was successful, false if cancelled or failed
     */
    public boolean reloadEncryptedLog() {
        byte[] salt;
        try {
            salt = Base64.getDecoder().decode(settings.getProperty("salt"));
        } catch (IllegalArgumentException | NullPointerException e) {
            DialogHelper.showError(null, "Corrupt Encryption Metadata", "Invalid Settings",
                "The stored encryption salt is missing or corrupt. Please restore from a settings backup.");
            return false;
        }
        return performPasswordAuthentication(salt, "\uD83D\uDD13 Unlock File", false);
    }

    /**
     * Performs password authentication with retry logic and progressive delays.
     *
     * @param salt the salt for encryption
     * @param dialogTitle the title for the password dialog
     * @param exitOnCancel whether to exit the application if password entry is cancelled
     * @return true if authentication was successful, false if cancelled or failed
     */
    private boolean performPasswordAuthentication(byte[] salt, String dialogTitle, boolean exitOnCancel) {
        boolean success = false;
        int attempts = 0;
        while (!success) {
            PasswordDialog.PasswordResult result;
            // Ensure the password dialog is shown on the EDT regardless of caller thread
            if (SwingUtilities.isEventDispatchThread()) {
                result = PasswordDialog.showPasswordDialog(parentFrame, dialogTitle);
            } else {
                final PasswordDialog.PasswordResult[] holder = new PasswordDialog.PasswordResult[1];
                try {
                    SwingUtilities.invokeAndWait(() -> holder[0] = PasswordDialog.showPasswordDialog(parentFrame, dialogTitle));
                } catch (Exception e) {
                    // If dialog invocation fails, treat as cancel
                    return false;
                }
                result = holder[0];
            }
            char[] pwd = result.password;
            if (pwd == null) {
                if (exitOnCancel) {
                    System.exit(0);
                }
                return false; // User cancelled
            }
            try {
                logFileHandler.setEncryption(pwd, salt);
            } catch (Exception e) {
                // If setEncryption fails, continue with authentication flow
                // The method will try to load entries which may trigger proper encryption setup
            }
            try {
                // Show progress dialog for large file decryption
                LoadingProgressDialog progressDialog = new LoadingProgressDialog(parentFrame, "Loading");
                progressDialog.show();
                
                try {
                    loadLogEntriesCallback.run();
                    success = true;

                    // Derive the backup HMAC key from credentials so it is never stored in settings.
                    // The key is only computable with the correct password.
                    backupManager.deriveAndSetHmacKey(pwd, salt);

                    if (!exitOnCancel) {
                        // For reload, update UI state
                        updateUILockStateCallback.run();
                        loadFullLogCallback.run();
                    }
                } finally {
                    progressDialog.close();
                }
                
                // Only zero out password after successful use
                java.util.Arrays.fill(pwd, '\0');
            } catch (Exception e) {
                // Zero out the failed password attempt before showing error
                java.util.Arrays.fill(pwd, '\0');
                
                attempts++;
                if (attempts >= 4) {
                    DialogHelper.showError(parentFrame, "Security Error", "🚫 Security Lock", "Too many failed password attempts.<br>The application is now locked for security.<br><br>Please restart the application to try again.");
                    System.exit(0);
                }
                
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                String exceptionType = e.getClass().getSimpleName().toLowerCase();
                
                // Check if this is a decryption/authentication error
                boolean isAuthError = errorMsg.contains("tag mismatch") ||
                    errorMsg.contains("bad tag") ||
                    errorMsg.contains("badpadding") ||
                    errorMsg.contains("illegal block size") ||
                    errorMsg.contains("aeadbadtag") ||
                    errorMsg.contains("integrity check failed") ||
                    errorMsg.contains("mac check failed") ||
                    errorMsg.contains("decryption failed") ||
                    errorMsg.contains("unable to open your file") ||
                    errorMsg.contains("your password might be incorrect") ||
                    errorMsg.contains("malformedinput") ||
                    errorMsg.contains("input length") ||
                    exceptionType.contains("indexoutofbounds") ||
                    exceptionType.contains("nullpointer") ||
                    errorMsg.contains("malformed") ||
                    errorMsg.contains("index") ||
                    errorMsg.contains("split");
                
                if (isAuthError) {
                    int remaining = 4 - attempts;
                    DialogHelper.showError(parentFrame, "Authentication Failed", "🔒 Authentication Failed",
                        "The password you entered appears to be incorrect, or the encrypted file has an unexpected format.<br><br>" +
                        "You have <b>" + remaining + "</b> attempt" + (remaining == 1 ? "" : "s") + " remaining before the application locks for security.<br><br>Tip: Double-check your password or password manager.");
                    // WindowShakeAnimation.shake(parentFrame);
                    // Add progressive delay after failed attempts
                    long delay = switch (attempts) {
                        case 1 -> 3000; // 3 seconds
                        case 2 -> 15000; // 15 seconds
                        case 3 -> 30000; // 30 seconds
                        default -> 0;
                    };
                    // Show the security delay dialog and wait for it to finish
                    // so the next password prompt only appears after the delay.
                    if (SwingUtilities.isEventDispatchThread()) {
                        SecurityDelayDialog.showDialog(delay, parentFrame);
                    } else {
                        try {
                            SwingUtilities.invokeAndWait(() -> SecurityDelayDialog.showDialog(delay, parentFrame));
                        } catch (Exception ex) {
                            // If invoking the dialog fails, fall back to scheduling it asynchronously
                            SwingUtilities.invokeLater(() -> SecurityDelayDialog.showDialog(delay, parentFrame));
                        }
                    }
                } else {
                    // For non-authentication errors, show error and exit/return
                    logFileHandler.showErrorDialog("<html><b>📁 Load Failed</b><br><br>Unable to load log entries due to a file error.<br><br><i>Technical details: " + e.getClass().getSimpleName() + "</i><br><br><i>Tip: The file may be corrupted. Try restoring from a backup.</i></html>");
                    if (exitOnCancel) {
                        System.exit(0);
                    }
                    return false; // Don't retry on non-authentication errors
                }
            }
        }
        return true; // Success
    }
}