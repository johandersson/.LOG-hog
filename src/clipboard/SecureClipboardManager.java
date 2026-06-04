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

import javax.swing.SwingUtilities;
import gui.DialogHelper;

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
 *   <li><b>Explicit Clearing:</b> Clipboard is cleared explicitly before application exit (not via shutdown hook due to AWT deadlock)</li>
 * </ul>
 *
 * <h2>Security Assumptions</h2>
 * <ul>
 *   <li>Clipboard content is only accessible by the local user and system processes</li>
 *   <li>Other applications cannot prevent clipboard clearing operations</li>
 *   <li>System clipboard implementation is trustworthy</li>
 * </ul>
 */
public class SecureClipboardManager implements ClipboardHandler, java.awt.datatransfer.ClipboardOwner {
    // marker removed (unused) - kept behavior via digest comparison
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Thread-safe mutable static fields with synchronization
    private static final Object LOCK = new Object();
    private static ScheduledFuture<?> clearTask;
    private static int timeoutSeconds = 15; // Default 15 seconds (reduced from 30 for tighter security)
    private static boolean autoClearEnabled = true;
    private static byte[] lastCopiedDigest; // Track hash of content we last copied
    // Track ownership: true when we set clipboard contents and still own it
    private static volatile boolean weOwnClipboard = false;

    private static final SecureClipboardManager INSTANCE = new SecureClipboardManager();

    // NOTE: Shutdown hook for clipboard clearing was REMOVED because it causes deadlock.
    // The AWT clipboard operations require the AWT event thread, but when System.exit()
    // is called from the EDT, the shutdown hook tries to use AWT which is waiting for
    // shutdown hooks to complete - classic deadlock.
    // Instead, clipboard is cleared explicitly in UIInitializer.windowClosing() BEFORE
    // calling System.exit().

    // Defensive getter for system clipboard; returns null if unavailable
    private static Clipboard getSystemClipboardSafe() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (Exception e) {
            return null;
        }
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
            DialogHelper.showError(parent, "Copy Failed", "Cannot copy null text.");
            return;
        }
        if (text.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            DialogHelper.showWarning(parent, "Copy Failed", "Text is empty.");
            return;
        }
        final String successMsg = (successMessage == null) ? "Text copied to clipboard securely." : successMessage;

        // Mark content as secure (no prefix needed - just copy the text directly)
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = getSystemClipboardSafe();

        try {
            if (clipboard == null) throw new IllegalStateException("Clipboard not available");
            // Use ClipboardOwner to track ownership instead of relying solely on digest
            clipboard.setContents(selection, INSTANCE);
            synchronized (LOCK) {
                try {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                    lastCopiedDigest = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception e) {
                    lastCopiedDigest = null;
                }
                weOwnClipboard = true;
            }

            // Show success message
            Component toastParent = parent;
            Window window = SwingUtilities.getWindowAncestor(parent);
            if (window != null) {
                toastParent = window;
            }
                synchronized (LOCK) {
                    if (autoClearEnabled) {
                        Toast.showToast(toastParent, successMsg + " (Auto-clear in " + timeoutSeconds + "s)");
                    } else {
                        Toast.showToast(toastParent, successMsg);
                    }
                }

            // Schedule automatic clearing if enabled
                synchronized (LOCK) {
                    if (autoClearEnabled) {
                        scheduleClipboardClearing();
                    }
                }

        } catch (IllegalStateException ise) {
            DialogHelper.showError(parent, "Clipboard Error", "Unable to access clipboard right now. Try again.");
        } catch (SecurityException se) {
            DialogHelper.showError(parent, "Security Error", "Clipboard access denied by security manager.");
        } catch (Exception e) {
            DialogHelper.showError(parent, "Clipboard Error", "Unexpected error accessing clipboard. Please try again.");
        }
    }

    /**
     * Manually clear the clipboard if it contains .LOG-hog secure content.
     */
    public static void clearSecureClipboard() {
        try {
            Clipboard clipboard = getSystemClipboardSafe();
            if (clipboard == null) return;
            
                synchronized (LOCK) {
                    // Clear if we have tracked content (compare hashes instead of storing full text)
                    if (lastCopiedDigest != null && weOwnClipboard) {
                        Transferable contents = clipboard.getContents(null);
                        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                            String data = (String) contents.getTransferData(DataFlavor.stringFlavor);
                            if (data != null) {
                                try {
                                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                                    byte[] now = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                    if (java.util.Arrays.equals(now, lastCopiedDigest)) {
                                        // Clear clipboard by setting empty content and claim no ownership
                                        StringSelection emptySelection = new StringSelection("");
                                        clipboard.setContents(emptySelection, INSTANCE);
                                        lastCopiedDigest = null;
                                        weOwnClipboard = false;

                                        // Cancel any pending clear task
                                        if (clearTask != null) {
                                            clearTask.cancel(false);
                                            clearTask = null;
                                        }
                                    } else {
                                        // Clipboard was changed by user - don't clear
                                        lastCopiedDigest = null;
                                        weOwnClipboard = false;
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
     * Called when the application is locking or user is logging out.
     * Cancels any pending clear tasks and clears the recorded digest to minimize exposure.
     */
    public static void onLock() {
        synchronized (LOCK) {
            try {
                if (clearTask != null) {
                    clearTask.cancel(false);
                    clearTask = null;
                }
                byte[] oldDigest = lastCopiedDigest;
                lastCopiedDigest = null;

                // Also attempt to clear clipboard contents if they match the previously tracked digest
                if (oldDigest != null) {
                    try {
                        Clipboard clipboard = getSystemClipboardSafe();
                        if (clipboard != null) {
                            Transferable contents = clipboard.getContents(null);
                            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                                String data = (String) contents.getTransferData(DataFlavor.stringFlavor);
                                if (data != null) {
                                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                                    byte[] now = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                    if (java.util.Arrays.equals(now, oldDigest)) {
                                        // Only clear if we still own the clipboard
                                        if (weOwnClipboard) {
                                            clipboard.setContents(new StringSelection(""), INSTANCE);
                                            weOwnClipboard = false;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {
                // best-effort
            }
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
            Clipboard clipboard = getSystemClipboardSafe();
            if (clipboard == null) return false;
            Transferable contents = clipboard.getContents(null);

            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String data = (String) contents.getTransferData(DataFlavor.stringFlavor);
                // Check if clipboard still contains what we copied by comparing digest
                if (data == null) return false;
                    try {
                        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                        byte[] now = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        synchronized (LOCK) {
                            // Consider secure content only if we still own the clipboard
                            return weOwnClipboard && java.util.Arrays.equals(now, lastCopiedDigest);
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
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // Clipboard ownership lost: clear internal tracking to avoid clearing other's data
        synchronized (LOCK) {
            weOwnClipboard = false;
            lastCopiedDigest = null;
        }
    }

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