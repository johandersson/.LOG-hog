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
 *
 * <h2>Security Properties</h2>
 * <ul>
 *   <li><b>Automatic Clearing:</b> Clipboard contents are automatically cleared after a configurable timeout (5-30 seconds)</li>
 *   <li><b>Thread Safety:</b> All static mutable fields are properly synchronized to prevent race conditions</li>
 *   <li><b>Content Tracking:</b> Only clears clipboard content that was copied by this application</li>
 *   <li><b>Shutdown Hook:</b> Ensures clipboard is cleared even on abnormal application termination</li>
 * </ul>
 *
 * <h2>Security Assumptions</h2>
 * <ul>
 *   <li>Clipboard content is only accessible by the local user and system processes</li>
 *   <li>Other applications cannot prevent clipboard clearing operations</li>
 *   <li>System clipboard implementation is trustworthy</li>
 * </ul>
 */
public class SecureClipboardManager implements ClipboardHandler {
    // marker removed (unused) - kept behavior via digest comparison
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Thread-safe mutable static fields with synchronization
    private static final Object LOCK = new Object();
    private static ScheduledFuture<?> clearTask;
    private static int timeoutSeconds = 30; // Default 30 seconds
    private static boolean autoClearEnabled = true;
    private static byte[] lastCopiedDigest; // Track hash of content we last copied

    private static final SecureClipboardManager INSTANCE = new SecureClipboardManager();

    static {
        // Add shutdown hook to guarantee clipboard clearing even if app crashes
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Clear clipboard on any app termination (normal or crash)
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(""), null);
            } catch (Exception e) {
                // Ignore - app is shutting down anyway
            }
        }, "ClipboardClearShutdownHook"));
    }

    public static SecureClipboardManager getInstance() {
        return INSTANCE;
    }

    /**
     * Set the automatic clipboard clearing timeout in seconds.
     * Valid range: 5-30 seconds
     *
     * @param seconds the timeout in seconds
     * @throws IllegalArgumentException if seconds is outside the valid range
     */
    public static void setTimeoutSeconds(int seconds) {
        if (seconds < 5 || seconds > 30) {
            throw new IllegalArgumentException("Timeout must be between 5 and 30 seconds");
        }
        synchronized (LOCK) {
            timeoutSeconds = seconds;
        }
    }

    /**
     * Enable or disable automatic clipboard clearing.
     *
     * @param enabled true to enable automatic clearing, false to disable
     */
    public static void setAutoClearEnabled(boolean enabled) {
        synchronized (LOCK) {
            autoClearEnabled = enabled;
            if (!enabled && clearTask != null) {
                clearTask.cancel(false);
                clearTask = null;
            }
        }
    }

    /**
     * Check if automatic clearing is enabled.
     *
     * @return true if automatic clearing is enabled, false otherwise
     */
    public static boolean isAutoClearEnabled() {
        synchronized (LOCK) {
            return autoClearEnabled;
        }
    }

    /**
     * Securely copy text to clipboard with automatic clearing.
     * Marks content as coming from .LOG-hog for security tracking.
     */
    @Override
    public void copySecureTextToClipboard(String text, Component parent) {
        copySecureTextToClipboard(text, parent, "Text copied to clipboard securely.");
    }

    /**
     * Securely copy text to clipboard with automatic clearing and custom message.
     */
    @Override
    public void copySecureTextToClipboard(String text, Component parent, String successMessage) {
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

        // Mark content as secure (no prefix needed - just copy the text directly)
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        try {
            clipboard.setContents(selection, selection);
                synchronized (LOCK) {
                    try {
                        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                        lastCopiedDigest = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        lastCopiedDigest = null;
                    }
                }

            // Show success message
            Component toastParent = parent;
            Window window = SwingUtilities.getWindowAncestor(parent);
            if (window != null) {
                toastParent = window;
            }
                synchronized (LOCK) {
                    if (autoClearEnabled) {
                        Toast.showToast(toastParent, successMessage + " (Auto-clear in " + timeoutSeconds + "s)");
                    } else {
                        Toast.showToast(toastParent, successMessage);
                    }
                }

            // Schedule automatic clearing if enabled
                synchronized (LOCK) {
                    if (autoClearEnabled) {
                        scheduleClipboardClearing();
                    }
                }

        } catch (IllegalStateException ise) {
            JOptionPane.showMessageDialog(parent, "Unable to access clipboard right now. Try again.", "Clipboard Error", JOptionPane.ERROR_MESSAGE);
        } catch (SecurityException se) {
            JOptionPane.showMessageDialog(parent, "Clipboard access denied by security manager.", "Security Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Unexpected error accessing clipboard. Please try again.", "Clipboard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Manually clear the clipboard if it contains .LOG-hog secure content.
     */
    public static void clearSecureClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            
                synchronized (LOCK) {
                    // Clear if we have tracked content (compare hashes instead of storing full text)
                    if (lastCopiedDigest != null) {
                        Transferable contents = clipboard.getContents(null);
                        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                            String data = (String) contents.getTransferData(DataFlavor.stringFlavor);
                            if (data != null) {
                                try {
                                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                                    byte[] now = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                    if (java.util.Arrays.equals(now, lastCopiedDigest)) {
                                        // Clear clipboard by setting empty content
                                        StringSelection emptySelection = new StringSelection("");
                                        clipboard.setContents(emptySelection, emptySelection);
                                        lastCopiedDigest = null;

                                        // Cancel any pending clear task
                                        if (clearTask != null) {
                                            clearTask.cancel(false);
                                            clearTask = null;
                                        }
                                    } else {
                                        // Clipboard was changed by user - don't clear
                                        lastCopiedDigest = null;
                                    }
                                } catch (Exception e) {
                                    // On digest errors, clear tracked value to avoid repeated failures
                                    lastCopiedDigest = null;
                                }
                            } else {
                                lastCopiedDigest = null;
                            }
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
            // Security: Don't log exception details to console
            // Any other unexpected error - silently ignore
        }
    }

    /**
     * Check if clipboard contains .LOG-hog secure content.
     *
     * @return true if the clipboard contains content that was copied by this application, false otherwise
     */
    public static boolean hasSecureContent() {
        synchronized (LOCK) {
            if (lastCopiedDigest == null) {
                return false;
            }
        }
        
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String data = (String) contents.getTransferData(DataFlavor.stringFlavor);
                // Check if clipboard still contains what we copied by comparing digest
                if (data == null) return false;
                try {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                    byte[] now = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    synchronized (LOCK) {
                        return java.util.Arrays.equals(now, lastCopiedDigest);
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        } catch (IllegalStateException ise) {
            // Clipboard not available
        } catch (UnsupportedFlavorException ufe) {
            // Data flavor not supported
        } catch (IOException ioe) {
            // I/O error accessing clipboard
        } catch (Exception e) {
            // Security: Don't log exception details to console
            // Any other unexpected error - return false
        }
        return false;
    }

    /**
     * Schedule automatic clearing of secure clipboard content.
     */
    private static void scheduleClipboardClearing() {
        synchronized (LOCK) {
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
                // Security: Don't log exception details to console
            }
        }
    }

    /**
     * Get the current timeout setting.
     *
     * @return the current timeout in seconds
     */
    public static int getTimeoutSeconds() {
        synchronized (LOCK) {
            return timeoutSeconds;
        }
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