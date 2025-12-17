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

import java.util.Base64;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import filehandling.LogFileHandler;
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
    private final SecureSettings secureSettings;

    /**
     * Constructs an EncryptionHandler with the necessary dependencies.
     *
     * @param parentFrame the parent JFrame for dialogs
     * @param logFileHandler the LogFileHandler for encryption operations
     * @param settings the application settings
     * @param secureSettings the SecureSettings for encryption
     * @param loadLogEntriesCallback callback to load log entries
     * @param updateUILockStateCallback callback to update UI lock state
     * @param loadFullLogCallback callback to load full log
     */
    public EncryptionHandler(JFrame parentFrame, LogFileHandler logFileHandler, Properties settings, SecureSettings secureSettings,
                           Runnable loadLogEntriesCallback, Runnable updateUILockStateCallback,
                           Runnable loadFullLogCallback) {
        this.parentFrame = parentFrame;
        this.logFileHandler = logFileHandler;
        this.settings = settings;
        this.secureSettings = secureSettings;
        this.loadLogEntriesCallback = loadLogEntriesCallback;
        this.updateUILockStateCallback = updateUILockStateCallback;
        this.loadFullLogCallback = loadFullLogCallback;
    }

    /**
     * Handles the initial encryption setup process, including password authentication
     * and retry logic with progressive delays.
     */
    public void handleEncryptionSetup() {
        String saltStr = settings.getProperty("salt");
        if (saltStr != null) {
            byte[] salt = Base64.getDecoder().decode(saltStr);
            performPasswordAuthentication(salt, "🔒 Enter Password", true);
        }
    }

    /**
     * Reloads an encrypted log by prompting for password and handling authentication.
     */
    public void reloadEncryptedLog() {
        byte[] salt = Base64.getDecoder().decode(settings.getProperty("salt"));
        performPasswordAuthentication(salt, "Reload Encrypted Log", false);
    }

    /**
     * Performs password authentication with retry logic and progressive delays.
     *
     * @param salt the salt for encryption
     * @param dialogTitle the title for the password dialog
     * @param exitOnCancel whether to exit the application if password entry is cancelled
     */
    private void performPasswordAuthentication(byte[] salt, String dialogTitle, boolean exitOnCancel) {
        String passwordReminder = secureSettings.getDecryptedProperty(settings, "passwordReminder", "");
        boolean success = false;
        int attempts = 0;
        while (!success) {
            PasswordDialog.PasswordResult result = PasswordDialog.showPasswordDialog(parentFrame, dialogTitle, passwordReminder);
            char[] pwd = result.password;
            if (pwd == null) {
                if (exitOnCancel) {
                    System.exit(0);
                }
                return;
            }
            logFileHandler.setEncryption(pwd, salt);
            java.util.Arrays.fill(pwd, '\0'); // Zero out password for security
            try {
                loadLogEntriesCallback.run();
                success = true;
                if (!exitOnCancel) {
                    // For reload, update UI state
                    updateUILockStateCallback.run();
                    loadFullLogCallback.run();
                }
            } catch (Exception e) {
                attempts++;
                if (attempts >= 4) {
                    JOptionPane.showMessageDialog(parentFrame, "<html><b>🚫 Security Lock</b><br><br>Too many failed password attempts.<br>The application is now locked for security.<br><br>Please restart the application to try again.</html>", "Security Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (errorMsg.contains("tag mismatch") ||
                    errorMsg.contains("bad tag") ||
                    errorMsg.contains("badpadding") ||
                    errorMsg.contains("illegal block size") ||
                    errorMsg.contains("aeadbadtag") ||
                    errorMsg.contains("integrity check failed") ||
                    errorMsg.contains("mac check failed") ||
                    errorMsg.contains("decryption failed")) {
                    int remaining = 4 - attempts;
                    JOptionPane.showMessageDialog(parentFrame, "<html><b>🔒 Authentication Failed</b><br><br>The password you entered is incorrect.<br>You have <b>" + remaining + "</b> attempts remaining before the application locks for security.<br><br><i>Tip: Use your password manager or reminder if needed.</i></html>", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                    // WindowShakeAnimation.shake(parentFrame);
                    // Add progressive delay after failed attempts
                    long delay = switch (attempts) {
                        case 1 -> 3000; // 3 seconds
                        case 2 -> 15000; // 15 seconds
                        case 3 -> 30000; // 30 seconds
                        default -> 0;
                    };
                    SecurityDelayDialog.showDialog(delay, parentFrame);
                } else {
                    logFileHandler.showErrorDialog("<html><b>📁 Load Failed</b><br><br>Unable to load log entries.<br>" + e.getMessage() + "<br><br><i>Tip: The file may be corrupted or inaccessible.</i></html>");
                    if (exitOnCancel) {
                        System.exit(0);
                    }
                }
            }
        }
    }
}