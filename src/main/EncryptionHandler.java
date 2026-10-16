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
import security.AuthenticationFailureClassifier;

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
        beginAuthentication();
        // If the log file does not exist, offer create/browse/restore first
        java.nio.file.Path path = logFileHandler.getFilePath();
        if (!java.nio.file.Files.exists(path)) {
            boolean handled = DialogHandler.handleMissingLogFile(path, () -> {});
            if (!handled) {
                // User chose to exit or did not provide a file; do not prompt for password
                return false;
            }
            DialogHandler.MissingFileAction action = DialogHandler.getLastMissingFileAction();
            if (action == DialogHandler.MissingFileAction.CREATED) {
                settings.setProperty("encrypted", "true");
                try {
                    if (saveSettingsCallback != null) saveSettingsCallback.run();
                } catch (Exception ex) {
                    logFileHandler.showErrorDialog("<html><b>💾 Settings Save Failed</b><br><br>Unable to persist secure settings after log file creation.</html>");
                }
                DialogHelper.showWarning(parentFrame,
                    "Encrypted-Only Policy",
                    "Encrypted File Required",
                    "A plain log file cannot be used in encrypted-only mode.<br><br>" +
                    "Please initialize or restore an encrypted log file to continue.");
                return false;
            } else if (action != DialogHandler.MissingFileAction.COPIED
                    && action != DialogHandler.MissingFileAction.RESTORED) {
                return false;
            }
        }
        if (!java.nio.file.Files.exists(path)) return false;
        if (!EncryptionDetector.isFileEncrypted(path)) {
            settings.setProperty("encrypted", "true");
            try {
                if (saveSettingsCallback != null) saveSettingsCallback.run();
            } catch (Exception ex) {
                logFileHandler.showErrorDialog("<html><b>💾 Settings Save Failed</b><br><br>Unable to persist secure settings after detecting a non-encrypted log file.</html>");
            }
            DialogHelper.showError(parentFrame,
                "Encrypted File Required",
                "Encrypted-Only Policy",
                "The selected log file is not encrypted.<br><br>" +
                "Please restore an encrypted log file backup.");
            return false;
        }
        byte[] salt;
        try {
            salt = resolveAuthenticationSalt();
        } catch (IllegalArgumentException e) {
            DialogHelper.showError(parentFrame, "Corrupt Encryption Metadata", "Invalid Settings",
                "The stored encryption salt is corrupt or invalid. Please restore from a settings backup.");
            return false;
        }
        return performPasswordAuthentication(salt, "\uD83D\uDD12 Enter Password", true);
    }

    /**
     * Reloads an encrypted log by prompting for password and handling authentication.
     *
     * @return true if unlock was successful, false if cancelled or failed
     */
    public boolean reloadEncryptedLog() {
        beginAuthentication();
        byte[] salt;
        try {
            salt = resolveAuthenticationSalt();
        } catch (IllegalArgumentException e) {
            DialogHelper.showError(null, "Corrupt Encryption Metadata", "Invalid Settings",
                "The stored encryption salt is missing or corrupt and could not be recovered from the file.\n"
                + "Please restore from a backup.");
            return false;
        }
        return performPasswordAuthentication(salt, "\uD83D\uDD13 Unlock File", false);
    }

    private byte[] resolveAuthenticationSalt() {
        byte[] headerSalt = EncryptionDetector.extractSaltFromHeader(logFileHandler.getFilePath());
        if (headerSalt.length > 0) return headerSalt;
        String encoded = settings.getProperty("salt");
        if (encoded == null) throw new IllegalArgumentException("Missing salt");
        byte[] storedSalt = Base64.getDecoder().decode(encoded);
        if (storedSalt.length != 16) throw new IllegalArgumentException("Invalid salt length");
        return storedSalt;
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
        clearFailedAuthentication();
        final long session = parentFrame instanceof LogTextEditor editor ? editor.getSessionGeneration() : 0L;
        boolean success = false;
        AuthenticationAttemptPolicy attempts = new AuthenticationAttemptPolicy(System::exit);

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
            if (parentFrame instanceof LogTextEditor editor && editor.getSessionGeneration() != session) {
                result.close();
                clearFailedAuthentication();
                return false;
            }
            if (pwd == null) {
                clearFailedAuthentication();
                // Startup/recovery flow should terminate when auth is cancelled.
                if (exitOnCancel) {
                    System.exit(0);
                }
                // Treat cancel as a non-fatal abort for manual unlock flow.
                return false;
            }
            try {
                logFileHandler.setEncryption(pwd, salt);
                // Show progress dialog for large file decryption
                LoadingProgressDialog progressDialog = new LoadingProgressDialog(parentFrame, "Loading");
                progressDialog.show();
                
                try {
                    loadLogEntriesCallback.run();
                    if (parentFrame instanceof LogTextEditor editor && editor.getSessionGeneration() != session) {
                        throw new java.util.concurrent.CancellationException();
                    }

                    // Derive the backup HMAC key from credentials so it is never stored in settings.
                    // The key is only computable with the correct password.
                    backupManager.deriveAndSetHmacKey(pwd, logFileHandler.getSalt());
                    if (parentFrame instanceof LogTextEditor editor && editor.getSessionGeneration() != session) {
                        throw new java.util.concurrent.CancellationException();
                    }
                    if (!exitOnCancel) {
                        // For reload, update UI state
                        updateUILockStateCallback.run();
                        loadFullLogCallback.run();
                    }
                    String authenticatedSalt = Base64.getEncoder().encodeToString(logFileHandler.getSalt());
                    if (!authenticatedSalt.equals(settings.getProperty("salt"))) {
                        settings.setProperty("salt", authenticatedSalt);
                        settings.setProperty("encrypted", "true");
                        try {
                            if (saveSettingsCallback != null) saveSettingsCallback.run();
                        } catch (Exception ignored) {}
                    }
                    success = true;
                } finally {
                    progressDialog.close();
                }
                
                // Only zero out password after successful use
                java.util.Arrays.fill(pwd, '\0');
            } catch (Exception e) {
                success = false;
                clearFailedAuthentication();
                // Zero out the failed password attempt before showing error
                java.util.Arrays.fill(pwd, '\0');
                if (e instanceof java.util.concurrent.CancellationException) return false;
                
                if (attempts.recordFailure(exitOnCancel, () -> DialogHelper.showError(parentFrame,
                        "Authentication Failed",
                        "Session Attempts Exhausted",
                        "You have used all 3 tries for this session.<br><br>Please restart the unlock flow and try again."))) {
                    return false;
                }
                
                boolean isAuthError = AuthenticationFailureClassifier.isLikelyAuthenticationFailure(e);
                
                if (isAuthError) {
                    int remaining = attempts.getRemainingAttempts();
                    DialogHelper.showError(parentFrame, "Authentication Failed", "🔒 Authentication Failed",
                        "The password you entered appears to be incorrect, or the encrypted file has an unexpected format.<br><br>" +
                        "You have <b>" + remaining + "</b> attempt" + (remaining == 1 ? "" : "s") + " remaining in this session.<br><br>Tip: Double-check your password or password manager.");
                    // WindowShakeAnimation.shake(parentFrame);
                    // Add progressive delay after failed attempts
                    long delay = switch (attempts.getAttempts()) {
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
                    return false; // Don't retry on non-authentication errors
                }
            } finally {
                java.util.Arrays.fill(pwd, '\0');
                result.close();
                if (!success) clearFailedAuthentication();
            }
        }
        return true; // Success
    }

    private void clearFailedAuthentication() {
        try {
            logFileHandler.clearSensitiveData();
        } finally {
            if (backupManager != null) backupManager.clearInMemoryHmacKey();
        }
    }

    private void beginAuthentication() {
        if (parentFrame instanceof LogTextEditor editor && !editor.isLocked()) editor.setLocked(true);
        clearFailedAuthentication();
    }
}