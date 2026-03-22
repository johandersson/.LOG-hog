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

package clipboard;

import javax.swing.*;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Enhanced clipboard security warnings with educational content about clipboard risks.
 * Provides comprehensive warnings and user education about clipboard security.
 */
public class ClipboardSecurityWarner {

    // Educational content about clipboard risks
    private static String getClipboardRisksEducation() {
        return "<html><body style='width: 400px; font-family: Segoe UI, sans-serif; font-size: 9px;'>" +
        "<h3 style='color: #d32f2f; margin-top: 0;'>⚠️ Clipboard Security Risks</h3>" +
        "<p><b>Clipboard contents remain accessible until overwritten or system restart.</b></p>" +
        "<ul style='margin-left: 20px;'>" +
        "<li><b>Malware Access:</b> Other applications can read clipboard contents</li>" +
        "<li><b>Cloud Sync:</b> Services like clipboard history may store data remotely</li>" +
        "<li><b>Shared Systems:</b> Other users can access clipboard on shared computers</li>" +
        "<li><b>Application Monitoring:</b> Keyloggers and spyware can capture clipboard data</li>" +
        "</ul>" +
        "<p style='color: #1976d2;'><b>.LOG-hog Security:</b> Content will auto-clear in " + SecureClipboardManager.getTimeoutSeconds() + " seconds for your protection.</p>" +
        "</body></html>";
    }

    private static final String ENCRYPTED_FILE_WARNING =
        "<html><body style='width: 400px; font-family: Segoe UI, sans-serif; font-size: 9px;'>" +
        "<h3 style='color: #f57c00; margin-top: 0;'>🔓 Encrypted File Warning</h3>" +
        "<p>This log file is encrypted. The copied text will be <b>unencrypted</b> in the clipboard.</p>" +
        "<p><b>This means sensitive information will be temporarily accessible in plain text!</b></p>" +
        getClipboardRisksEducation().replace("<h3", "<h4").replace("</h3>", "</h4>") +
        "</body></html>";

    private static final String FULL_LOG_WARNING =
        "<html><body style='width: 400px; font-family: Segoe UI, sans-serif; font-size: 9px;'>" +
        "<h3 style='color: #d32f2f; margin-top: 0;'>🚨 Full Log Export Warning</h3>" +
        "<p>You are about to copy your <b>entire log file</b> to the clipboard.</p>" +
        "<p><b>This may contain sensitive information across all your entries!</b></p>" +
        getClipboardRisksEducation().replace("<h3", "<h4").replace("</h3>", "</h4>") +
        "</body></html>";

    /**
     * Show enhanced warning for copying from encrypted files.
     * @return true if user confirms, false if cancelled
     */
    public static boolean showEncryptedFileWarning(Component parent) {
        return showSecurityWarning(parent, ENCRYPTED_FILE_WARNING,
            "Copy to Clipboard - Encrypted File Security Warning",
            "Copy anyway", "Don't copy");
    }

    /**
     * Show enhanced warning for copying full log to clipboard.
     * @return true if user confirms, false if cancelled
     */
    public static boolean showFullLogWarning(Component parent) {
        return showSecurityWarning(parent, FULL_LOG_WARNING,
            "Copy Full Log to Clipboard - Security Warning",
            "Copy full log", "Don't copy");
    }

    /**
     * Show general clipboard security education dialog.
     */
    public static void showClipboardEducation(Component parent) {
        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setText(getClipboardRisksEducation());
        htmlPane.setEditable(false);
        htmlPane.setOpaque(false);
        htmlPane.setBorder(null);
        htmlPane.setPreferredSize(new Dimension(450, 350));

        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, htmlPane,
            "Clipboard Security Education", JOptionPane.INFORMATION_MESSAGE));
    }

    /**
     * Show warning about clipboard containing secure content.
     */
    public static void showSecureContentWarning(Component parent) {
        String message =
            "<html><body style='width: 350px; font-family: Segoe UI, sans-serif; font-size: 9px;'>" +
            "<h3 style='color: #1976d2; margin-top: 0;'>🔒 Secure Clipboard Active</h3>" +
            "<p>Clipboard contains .LOG-hog content that will auto-clear for security.</p>" +
            "<p><b>Content will be automatically cleared in " +
            SecureClipboardManager.getTimeoutSeconds() + " seconds.</b></p>" +
            "<p>You can manually clear it now if needed.</p>" +
            "</body></html>";

        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setText(message);
        htmlPane.setEditable(false);
        htmlPane.setOpaque(false);
        htmlPane.setBorder(null);
        htmlPane.setPreferredSize(new Dimension(400, 200));

        Object[] options = {"Clear Now", "Keep Content"};

        final int[] result = new int[1];
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = JOptionPane.showOptionDialog(parent, htmlPane,
                "Secure Clipboard Status", JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[1]));
        } catch (Exception e) {
            result[0] = JOptionPane.CLOSED_OPTION;
        }

        switch (result[0]) {
            case JOptionPane.YES_OPTION: // Clear Now
                SecureClipboardManager.clearSecureClipboard();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Clipboard cleared for security.",
                    "Clipboard Cleared", JOptionPane.INFORMATION_MESSAGE));
                break;
            case JOptionPane.NO_OPTION: // Keep Content - do nothing
            default:
                break;
        }
    }

    /**
     * Show a security tip notification.
     */
    public static void showSecurityTip(Component parent) {
        if (!SecureClipboardManager.isAutoClearEnabled()) {
            return; // Don't show tips if auto-clear is disabled
        }

        String tip =
            "<html><body style='width: 300px; font-family: Segoe UI, sans-serif; font-size: 9px;'>" +
            "<h4 style='color: #1976d2; margin-top: 0;'>💡 Security Tip</h4>" +
            "<p>Copied content will auto-clear from clipboard in " +
            SecureClipboardManager.getTimeoutSeconds() + " seconds for your security.</p>" +
            "</body></html>";

        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setText(tip);
        htmlPane.setEditable(false);
        htmlPane.setOpaque(false);
        htmlPane.setBorder(null);
        htmlPane.setPreferredSize(new Dimension(350, 140));

        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, htmlPane,
            "Security Tip", JOptionPane.INFORMATION_MESSAGE));
    }

    /**
     * Generic security warning dialog with custom content.
     */
    private static boolean showSecurityWarning(Component parent, String message,
            String title, String confirmText, String cancelText) {

        // Create a proper HTML-capable dialog
        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setText(message);
        htmlPane.setEditable(false);
        htmlPane.setOpaque(false);
        htmlPane.setBorder(null);
        htmlPane.setPreferredSize(new Dimension(450, 350));

        // Make links clickable if needed
        htmlPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    String url = e.getURL().toString();
                    // Only allow http/https links from clickable HTML panels
                    if (security.PathValidator.isSafeHttpUrl(url)) {
                        java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                    } else {
                        gui.DialogHelper.showWarning(null, "Blocked Link", "Blocked Link",
                            "This link uses an unsupported scheme and was blocked for your safety.");
                    }
                } catch (Exception ex) {
                    // Ignore silently to avoid exposing internals
                }
            }
        });

        Object[] options = {confirmText, cancelText};

        final int[] result = new int[1];
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = JOptionPane.showOptionDialog(parent, htmlPane, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]));
        } catch (Exception e) {
            return false;
        }

        switch (result[0]) {
            case JOptionPane.YES_OPTION: // Confirm action
                return true;
            case JOptionPane.NO_OPTION: // Cancel
            default:
                return false;
        }
    }

    /**
     * Create a menu item for clipboard security actions.
     */
    public static JMenuItem createSecurityMenuItem() {
        JMenuItem securityItem = new JMenuItem("Clipboard Security...");
        securityItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component parent = (Component) e.getSource();
                Window window = SwingUtilities.getWindowAncestor(parent);
                if (window != null) {
                    parent = window;
                }

                if (SecureClipboardManager.hasSecureContent()) {
                    showSecureContentWarning(parent);
                } else {
                    showClipboardEducation(parent);
                }
            }
        });
        return securityItem;
    }
}