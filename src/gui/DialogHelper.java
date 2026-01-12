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

package gui;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * Utility class for showing consistent, formatted dialogs throughout the application.
 * Provides HTML-formatted messages with icons and standard styling.
 */
public class DialogHelper {
    
    /**
     * Shows an error dialog with formatted HTML message.
     * @param parent Parent component
     * @param title Dialog title
     * @param message Main message (will be bold)
     * @param details Additional details (optional, can be null)
     */
    public static void showError(Component parent, String title, String message, String details) {
        String html = "<html><b>" + message + "</b>";
        if (details != null && !details.isEmpty()) {
            html += "<br><br>" + details;
        }
        html += "</html>";
        JOptionPane.showMessageDialog(parent, html, title, JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Shows an error dialog with just a message.
     */
    public static void showError(Component parent, String title, String message) {
        showError(parent, title, message, null);
    }
    
    /**
     * Shows a warning dialog with formatted HTML message.
     */
    public static void showWarning(Component parent, String title, String message, String details) {
        String html = "<html><b>" + message + "</b>";
        if (details != null && !details.isEmpty()) {
            html += "<br><br>" + details;
        }
        html += "</html>";
        JOptionPane.showMessageDialog(parent, html, title, JOptionPane.WARNING_MESSAGE);
    }
    
    /**
     * Shows a warning dialog with just a message.
     */
    public static void showWarning(Component parent, String title, String message) {
        showWarning(parent, title, message, null);
    }
    
    /**
     * Shows an information dialog with formatted HTML message.
     */
    public static void showInfo(Component parent, String title, String message, String details) {
        String html = "<html><b>" + message + "</b>";
        if (details != null && !details.isEmpty()) {
            html += "<br><br>" + details;
        }
        html += "</html>";
        JOptionPane.showMessageDialog(parent, html, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Shows an information dialog with just a message.
     */
    public static void showInfo(Component parent, String title, String message) {
        showInfo(parent, title, message, null);
    }
    
    /**
     * Shows a success dialog (using information icon with checkmark).
     */
    public static void showSuccess(Component parent, String title, String message, String details) {
        String html = "<html><b>✓ " + message + "</b>";
        if (details != null && !details.isEmpty()) {
            html += "<br><br>" + details;
        }
        html += "</html>";
        JOptionPane.showMessageDialog(parent, html, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Shows a success dialog with just a message.
     */
    public static void showSuccess(Component parent, String title, String message) {
        showSuccess(parent, title, message, null);
    }
    
    /**
     * Shows a confirmation dialog (Yes/No).
     * @return true if user clicked Yes, false otherwise
     */
    public static boolean confirm(Component parent, String title, String message, String details) {
        String html = "<html><b>" + message + "</b>";
        if (details != null && !details.isEmpty()) {
            html += "<br><br>" + details;
        }
        html += "</html>";
        int result = JOptionPane.showConfirmDialog(parent, html, title, 
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
    
    /**
     * Shows a confirmation dialog with just a message.
     */
    public static boolean confirm(Component parent, String title, String message) {
        return confirm(parent, title, message, null);
    }
    
    /**
     * Shows a custom option dialog with custom buttons.
     * @param options Array of button labels
     * @param defaultOption Default selected option
     * @return Index of selected option, or -1 if closed
     */
    public static int showOptions(Component parent, String title, String message, String details,
                                   int messageType, Object[] options, Object defaultOption) {
        String html = "<html><b>" + message + "</b>";
        if (details != null && !details.isEmpty()) {
            html += "<br><br>" + details;
        }
        html += "</html>";
        return JOptionPane.showOptionDialog(parent, html, title,
            JOptionPane.YES_NO_OPTION, messageType, null, options, defaultOption);
    }
    
    /**
     * Shows a file locked warning with consistent formatting.
     */
    public static void showFileLocked(Component parent) {
        showWarning(parent, "Cannot Format", "File is Locked", 
            "Please unlock the file before formatting.");
    }
    
    /**
     * Shows a file not found error with consistent formatting.
     */
    public static void showFileNotFound(Component parent) {
        showError(parent, "Error", "File Not Found", "Log file does not exist.");
    }
    
    /**
     * Shows entry not found information with helpful details.
     */
    public static void showEntryNotFound(Component parent) {
        showInfo(parent, "Entry Not Found", "Entry Not Found",
            "This entry is not visible in the current Log List view.<br>" +
            "You may need to adjust the year/month filter to see it.");
    }
    
    /**
     * Shows a decryption failed error.
     */
    public static void showDecryptionFailed(Component parent) {
        showError(parent, "Security Error", "Decryption failed.", 
            "The file has been locked for security. Please use the Unlock button to try again.");
    }
}
