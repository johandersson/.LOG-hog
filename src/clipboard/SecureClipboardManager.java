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

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.concurrent.*;
import javax.swing.*;
import utils.Toast;

/**
 * Secure clipboard manager that automatically clears clipboard contents after a timeout
 * to prevent sensitive data from remaining accessible to other applications.
 */
public class SecureClipboardManager {
    private static final String LOGHOG_CLIPBOARD_MARKER = "[LOGHOG_SECURE_CONTENT]";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static ScheduledFuture<?> clearTask;
    private static int timeoutSeconds = 30; // Default 30 seconds
    private static boolean autoClearEnabled = true;

    /**
     * Set the automatic clipboard clearing timeout in seconds.
     */
    public static void setTimeoutSeconds(int seconds) {
        timeoutSeconds = Math.max(5, seconds); // Minimum 5 seconds
    }

    /**
     * Enable or disable automatic clipboard clearing.
     */
    public static void setAutoClearEnabled(boolean enabled) {
        autoClearEnabled = enabled;
        if (!enabled && clearTask != null) {
            clearTask.cancel(false);
            clearTask = null;
        }
    }

    /**
     * Check if automatic clearing is enabled.
     */
    public static boolean isAutoClearEnabled() {
        return autoClearEnabled;
    }

    /**
     * Securely copy text to clipboard with automatic clearing.
     * Marks content as coming from .LOG-hog for security tracking.
     */
    public static void copySecureTextToClipboard(String text, Component parent) {
        copySecureTextToClipboard(text, parent, "Text copied to clipboard securely.");
    }

    /**
     * Securely copy text to clipboard with automatic clearing and custom message.
     */
    public static void copySecureTextToClipboard(String text, Component parent, String successMessage) {
        if (text == null || text.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(parent, "Text is empty.", "Copy Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Mark content as secure and add timestamp
        String secureContent = LOGHOG_CLIPBOARD_MARKER + System.currentTimeMillis() + "|" + text;

        StringSelection selection = new StringSelection(secureContent);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        try {
            clipboard.setContents(selection, selection);

            // Show success message
            Component toastParent = parent;
            Window window = SwingUtilities.getWindowAncestor(parent);
            if (window != null) {
                toastParent = window;
            }
            Toast.showToast(toastParent, successMessage + " (Auto-clear in " + timeoutSeconds + "s)");

            // Schedule automatic clearing if enabled
            if (autoClearEnabled) {
                scheduleClipboardClearing();
            }

        } catch (IllegalStateException ise) {
            JOptionPane.showMessageDialog(parent, "Unable to access clipboard right now. Try again.", "Clipboard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Manually clear the clipboard if it contains .LOG-hog content.
     */
    public static void clearSecureClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String data = (String) contents.getTransferData(DataFlavor.stringFlavor);
                if (data != null && data.startsWith(LOGHOG_CLIPBOARD_MARKER)) {
                    // Clear clipboard by setting empty content
                    StringSelection emptySelection = new StringSelection("");
                    clipboard.setContents(emptySelection, emptySelection);

                    // Cancel any pending clear task
                    if (clearTask != null) {
                        clearTask.cancel(false);
                        clearTask = null;
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore clipboard access errors during clearing
        }
    }

    /**
     * Check if clipboard contains .LOG-hog secure content.
     */
    public static boolean hasSecureContent() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String data = (String) contents.getTransferData(DataFlavor.stringFlavor);
                return data != null && data.startsWith(LOGHOG_CLIPBOARD_MARKER);
            }
        } catch (Exception e) {
            // Silently ignore clipboard access errors
        }
        return false;
    }

    /**
     * Schedule automatic clearing of secure clipboard content.
     */
    private static void scheduleClipboardClearing() {
        // Cancel any existing task
        if (clearTask != null) {
            clearTask.cancel(false);
        }

        // Schedule new clearing task
        clearTask = scheduler.schedule(() -> {
            SwingUtilities.invokeLater(() -> {
                if (hasSecureContent()) {
                    clearSecureClipboard();
                    // Show notification that clipboard was cleared
                    Toast.showToast(null, "Clipboard automatically cleared for security.");
                }
            });
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Get the current timeout setting.
     */
    public static int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Shutdown the scheduler (call on application exit).
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}