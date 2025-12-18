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

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import utils.Toast;

/**
 * Secure clipboard manager that automatically clears clipboard contents after a timeout
 * to prevent sensitive data from remaining accessible to other applications.
 */
public class SecureClipboardManager implements ClipboardHandler {
    private static final String LOGHOG_CLIPBOARD_MARKER = "";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static ScheduledFuture<?> clearTask;
    private static int timeoutSeconds = 30; // Default 30 seconds
    private static boolean autoClearEnabled = true;

    /**
     * Set the automatic clipboard clearing timeout in seconds.
     * Valid range: 5-30 seconds
     */
    public static void setTimeoutSeconds(int seconds) {
        if (seconds < 5 || seconds > 30) {
            throw new IllegalArgumentException("Timeout must be between 5 and 30 seconds");
        }
        timeoutSeconds = seconds;
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
        // Input validation
        if (text == null) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(parent, "Cannot copy null text.", "Copy Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (text.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(parent, "Text is empty.", "Copy Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (successMessage == null) {
            successMessage = "Text copied to clipboard securely.";
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
        } catch (SecurityException se) {
            JOptionPane.showMessageDialog(parent, "Clipboard access denied by security manager.", "Security Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Unexpected error accessing clipboard. Please try again.", "Clipboard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void copySecureTextToClipboard(String text, Component parent) {
        SecureClipboardManager.copySecureTextToClipboard(text, parent);
    }

    @Override
    public void copySecureTextToClipboard(String text, Component parent, String successMessage) {
        SecureClipboardManager.copySecureTextToClipboard(text, parent, successMessage);
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
        } catch (IllegalStateException ise) {
            // Clipboard not available - silently ignore
        } catch (UnsupportedFlavorException ufe) {
            // Data flavor not supported - silently ignore
        } catch (IOException ioe) {
            // I/O error accessing clipboard - silently ignore
        } catch (Exception e) {
            // Any other unexpected error - log but don't show to user
            System.err.println("Unexpected error clearing secure clipboard: " + e.getMessage());
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
        } catch (IllegalStateException ise) {
            // Clipboard not available
        } catch (UnsupportedFlavorException ufe) {
            // Data flavor not supported
        } catch (IOException ioe) {
            // I/O error accessing clipboard
        } catch (Exception e) {
            // Any other unexpected error
            System.err.println("Unexpected error checking secure clipboard content: " + e.getMessage());
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

        try {
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
        } catch (Exception e) {
            System.err.println("Failed to schedule clipboard clearing: " + e.getMessage());
        }
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