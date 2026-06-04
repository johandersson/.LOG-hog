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

package gui;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Utility class for showing consistent, formatted dialogs throughout the application.
 * Provides HTML-formatted messages with icons and standard styling.
 */
public class DialogHelper {
    // Builds a safe HTML message. If `message` already contains an <html> block
    // we avoid double-wrapping and try to inject `details` before the closing
    // </html> tag if present. This prevents orphan closing tags from appearing
    // when callers already pass fully-formed HTML.
    private static String buildHtml(String message, String details) {
        if (message == null) message = "";
        String trimmed = message.trim();
        if (trimmed.toLowerCase().startsWith("<html")) {
            if (details == null || details.isEmpty()) return message;
            int closeIdx = trimmed.toLowerCase().lastIndexOf("</html>");
            if (closeIdx != -1) {
                // insert details before the closing tag
                String beforeClose = message.substring(0, message.length() - (trimmed.length() - closeIdx));
                return beforeClose + "<br><br>" + details + "</html>";
            }
            return message + "<br><br>" + details;
        }
        // Strip any <html>…</html> wrapper from details before embedding it,
        // so callers that pass fully-formed HTML don't create nested <html> tags.
        String safeDetails = details;
        if (safeDetails != null) {
            String dtLow = safeDetails.trim().toLowerCase();
            if (dtLow.startsWith("<html")) {
                safeDetails = safeDetails.trim();
                int gt = safeDetails.indexOf('>');
                if (gt != -1) safeDetails = safeDetails.substring(gt + 1);
                int closeIdx = safeDetails.toLowerCase().lastIndexOf("</html>");
                if (closeIdx != -1) safeDetails = safeDetails.substring(0, closeIdx);
            }
        }
        StringBuilder htmlBuilder = new StringBuilder("<html><b>").append(message).append("</b>");
        if (safeDetails != null && !safeDetails.isEmpty()) {
            htmlBuilder.append("<br><br>").append(safeDetails);
        }
        htmlBuilder.append("</html>");
        return htmlBuilder.toString();
    }
    
    /**
     * Shows an error dialog with formatted HTML message.
     * @param parent Parent component
     * @param title Dialog title
     * @param message Main message (will be bold)
     * @param details Additional details (optional, can be null)
     */
    public static void showError(Component parent, String title, String message, String details) {
        final String htmlFinal = buildHtml(message, details);
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(parent, htmlFinal, title, JOptionPane.ERROR_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, htmlFinal, title, JOptionPane.ERROR_MESSAGE));
        }
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
        final String htmlFinal = buildHtml(message, details);
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(parent, htmlFinal, title, JOptionPane.WARNING_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, htmlFinal, title, JOptionPane.WARNING_MESSAGE));
        }
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
        final String htmlFinal = buildHtml(message, details);
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(parent, htmlFinal, title, JOptionPane.INFORMATION_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, htmlFinal, title, JOptionPane.INFORMATION_MESSAGE));
        }
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
        final String htmlFinal = buildHtml("✓ " + message, details);
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(parent, htmlFinal, title, JOptionPane.INFORMATION_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, htmlFinal, title, JOptionPane.INFORMATION_MESSAGE));
        }
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
        final String htmlFinal = buildHtml(message, details);
        if (SwingUtilities.isEventDispatchThread()) {
            int result = JOptionPane.showConfirmDialog(parent, htmlFinal, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            return result == JOptionPane.YES_OPTION;
        } else {
            final int[] result = new int[1];
            try {
                SwingUtilities.invokeAndWait(() -> result[0] = JOptionPane.showConfirmDialog(parent, htmlFinal, title,
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE));
            } catch (Exception e) {
                return false;
            }
            return result[0] == JOptionPane.YES_OPTION;
        }
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
        final String htmlFinal = buildHtml(message, details);
        if (SwingUtilities.isEventDispatchThread()) {
            return JOptionPane.showOptionDialog(parent, htmlFinal, title,
                JOptionPane.YES_NO_OPTION, messageType, null, options, defaultOption);
        } else {
            final int[] result = new int[1];
            try {
                SwingUtilities.invokeAndWait(() -> result[0] = JOptionPane.showOptionDialog(parent, htmlFinal, title,
                    JOptionPane.YES_NO_OPTION, messageType, null, options, defaultOption));
            } catch (Exception e) {
                return -1;
            }
            return result[0];
        }
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
