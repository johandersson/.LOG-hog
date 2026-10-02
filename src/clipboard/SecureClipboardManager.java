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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;
import gui.DialogHelper;

import security.AppPathPolicy;
import security.SecurityFilePolicy;
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
    private static ScheduledExecutorService scheduler = createScheduler();
    private static ScheduledExecutorService createScheduler() {
        return Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
        });
    }

    // Thread-safe mutable static fields with synchronization
    private static final Object LOCK = new Object();
    private static ScheduledFuture<?> clearTask;
    private static int timeoutSeconds = 15; // Default 15 seconds (reduced from 30 for tighter security)
    private static boolean autoClearEnabled = true;
    private static byte[] lastCopiedDigest; // Track hash of content we last copied
    // Track ownership: true when we set clipboard contents and still own it
    private static volatile boolean weOwnClipboard = false;
    private static java.lang.ref.WeakReference<Transferable> ownedContents = new java.lang.ref.WeakReference<>(null);
    private static boolean shutdownRequested;
    private static boolean clearRequested;
    private static long clipboardGeneration;
    private static final Path CLIPBOARD_MARKER = AppPathPolicy.appDataDirectory().resolve("clipboard.pending");

    private static final SecureClipboardManager INSTANCE = new SecureClipboardManager();
    private static java.util.function.Supplier<Clipboard> clipboardProvider =
        () -> Toolkit.getDefaultToolkit().getSystemClipboard();

    // NOTE: Shutdown hook for clipboard clearing was REMOVED because it causes deadlock.
    // The AWT clipboard operations require the AWT event thread, but when System.exit()
    // is called from the EDT, the shutdown hook tries to use AWT which is waiting for
    // shutdown hooks to complete - classic deadlock.
    // Instead, clipboard is cleared explicitly in UIInitializer.windowClosing() BEFORE
    // calling System.exit().

    // Defensive getter for system clipboard; returns null if unavailable
    private static Clipboard getSystemClipboardSafe() {
        try {
            return clipboardProvider.get();
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
            if (!enabled && !clearRequested && clearTask != null) {
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
            synchronized (LOCK) {
                clipboard.setContents(selection, INSTANCE);
                clipboardGeneration++;
                ownedContents = new java.lang.ref.WeakReference<>(selection);
                shutdownRequested = false;
                clearRequested = false;
                try {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                    lastCopiedDigest = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception e) {
                    lastCopiedDigest = null;
                }
                weOwnClipboard = true;
                writeClipboardMarker();
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
        synchronized (LOCK) {
            if (lastCopiedDigest == null) return;
            clearRequested = true;
            try {
                Clipboard clipboard = getSystemClipboardSafe();
                if (clipboard == null) throw new IllegalStateException("Clipboard unavailable");
                if (matchesTrackedContent(clipboard)) {
                    clipboard.setContents(new StringSelection(""), INSTANCE);
                    if (matchesTrackedContent(clipboard)) throw new IllegalStateException("Clipboard unchanged");
                }
                forgetTracking();
            } catch (Exception ignored) {
                writeClipboardMarker();
                scheduleRetry();
            }
        }
    }

    private static boolean matchesTrackedContent(Clipboard clipboard) throws Exception {
        Transferable contents = clipboard.getContents(null);
        if (contents == null || !contents.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
        String data = (String) contents.getTransferData(DataFlavor.stringFlavor);
        if (data == null) return false;
        return java.util.Arrays.equals(lastCopiedDigest, java.security.MessageDigest.getInstance("SHA-256")
            .digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static void forgetTracking() {
        clipboardGeneration++;
        if (lastCopiedDigest != null) java.util.Arrays.fill(lastCopiedDigest, (byte) 0);
        lastCopiedDigest = null;
        weOwnClipboard = false;
        ownedContents.clear();
        clearRequested = false;
        clearClipboardMarker();
        if (clearTask != null) { clearTask.cancel(false); clearTask = null; }
        if (shutdownRequested) scheduler.shutdown();
    }

    private static void scheduleRetry() {
        if (clearTask != null) clearTask.cancel(false);
        if (scheduler.isShutdown()) scheduler = createScheduler();
        final long generation = clipboardGeneration;
        clearTask = scheduler.schedule(() -> {
            synchronized (LOCK) {
                if (generation == clipboardGeneration) clearSecureClipboard();
            }
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * Called when the application is locking or user is logging out.
     * Cancels any pending clear tasks and clears the recorded digest to minimize exposure.
     */
    public static void onLock() {
        clearSecureClipboard();
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
            final long generation = clipboardGeneration;
            // Cancel any existing task
            if (clearTask != null) {
                clearTask.cancel(false);
            }

            try {
                if (scheduler.isShutdown()) scheduler = createScheduler();
                // Schedule new clearing task
                clearTask = scheduler.schedule(() -> {
                    SwingUtilities.invokeLater(() -> {
                        synchronized (LOCK) {
                            if (generation == clipboardGeneration) clearSecureClipboard();
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
        synchronized (LOCK) {
            try {
                // Delayed ownership notifications must not forget a newer copy.
                if (lastCopiedDigest != null &&
                        (contents == ownedContents.get() || !matchesTrackedContent(clipboard))) forgetTracking();
            } catch (Exception ignored) { scheduleRetry(); }
        }
    }

    public static void recoverClipboardAfterCrash() {
        synchronized (LOCK) {
        try {
            if (!Files.exists(CLIPBOARD_MARKER)) {
                return;
            }
            String marker = Files.readString(CLIPBOARD_MARKER, StandardCharsets.UTF_8).trim();
            if (marker.length() != 64) return; // Legacy markers cannot identify clipboard ownership.
            lastCopiedDigest = java.util.HexFormat.of().parseHex(marker);
            clipboardGeneration++;
            weOwnClipboard = true;
            clearSecureClipboard();
        } catch (Exception ignored) {
            // best-effort recovery
        }
        }
    }

    private static void writeClipboardMarker() {
        try {
            Files.createDirectories(CLIPBOARD_MARKER.getParent());
            if (lastCopiedDigest == null) return;
            Files.writeString(CLIPBOARD_MARKER, java.util.HexFormat.of().formatHex(lastCopiedDigest), StandardCharsets.UTF_8);
            SecurityFilePolicy.ensureOwnerOnlyPermissionsOrThrow(CLIPBOARD_MARKER);
        } catch (Exception ignored) {
            // best-effort marker, clipboard clear remains timeout-based
        }
    }

    private static void clearClipboardMarker() {
        try {
            Files.deleteIfExists(CLIPBOARD_MARKER);
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    public static void shutdown() {
        synchronized (LOCK) {
            shutdownRequested = true;
            clearSecureClipboard();
            if (lastCopiedDigest == null) {
                if (clearTask != null) { clearTask.cancel(false); clearTask = null; }
                scheduler.shutdown();
            }
            // Pending clears retain their daemon retry and crash-recovery marker.
        }
    }
}