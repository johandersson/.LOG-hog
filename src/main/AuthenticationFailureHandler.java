package main;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import filehandling.LogFileHandler;
import gui.DialogHelper;
import gui.LockOverlay;
import gui.SecurityDelayDialog;

/**
 * Handles authentication and file-loading failures for password attempts.
 * Shows appropriate dialogs (hiding the lock overlay while visible) and
 * enforces progressive delays and locking behavior.
 */
public final class AuthenticationFailureHandler {
    private AuthenticationFailureHandler() {}

    /**
     * Handle a failed load attempt.
     * @param e the exception thrown while loading
     * @param attempts current attempt count (>=1)
     * @param parentFrame parent frame for dialogs
     * @param lo optional LockOverlay (may be null)
     * @param logFileHandler used to show file error dialogs
     * @param exitOnCancel whether to exit on non-auth failures
     * @return true if caller should continue prompting (retry), false to stop
     */
    public static boolean handleFailure(Exception e, int attempts, JFrame parentFrame, LockOverlay lo, LogFileHandler logFileHandler, boolean exitOnCancel) {
        if (attempts >= 4) {
            try {
                if (lo != null) {
                    lo.withOverlayHidden(() -> { DialogHelper.showError(parentFrame, "Security Error", "🚫 Security Lock", "Too many failed password attempts.<br>The application is now locked for security.<br><br>Please restart the application to try again."); return null; });
                } else {
                    DialogHelper.showError(parentFrame, "Security Error", "🚫 Security Lock", "Too many failed password attempts.<br>The application is now locked for security.<br><br>Please restart the application to try again.");
                }
            } catch (Exception ignore) {}
            System.exit(0);
            return false;
        }

        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String exceptionType = e.getClass().getSimpleName().toLowerCase();

        boolean isAuthError = errorMsg.contains("tag mismatch") ||
                errorMsg.contains("bad tag") ||
                errorMsg.contains("badpadding") ||
                errorMsg.contains("illegal block size") ||
                errorMsg.contains("aeadbadtag") ||
                errorMsg.contains("integrity check failed") ||
                errorMsg.contains("mac check failed") ||
                errorMsg.contains("decryption failed") ||
                errorMsg.contains("unable to open your file") ||
                errorMsg.contains("your password might be incorrect") ||
                errorMsg.contains("malformedinput") ||
                errorMsg.contains("input length") ||
                exceptionType.contains("indexoutofbounds") ||
                exceptionType.contains("nullpointer") ||
                errorMsg.contains("malformed") ||
                errorMsg.contains("index") ||
                errorMsg.contains("split");

        if (isAuthError) {
            int remaining = 4 - attempts;
            try {
                if (lo != null) {
                    lo.withOverlayHidden(() -> { DialogHelper.showError(parentFrame, "Authentication Failed", "🔒 Authentication Failed",
                        "The password you entered appears to be incorrect, or the encrypted file has an unexpected format.<br><br>" +
                        "You have <b>" + remaining + "</b> attempt" + (remaining == 1 ? "" : "s") + " remaining before the application locks for security.<br><br>Tip: Double-check your password or password manager."); return null; });
                } else {
                    DialogHelper.showError(parentFrame, "Authentication Failed", "🔒 Authentication Failed",
                        "The password you entered appears to be incorrect, or the encrypted file has an unexpected format.<br><br>" +
                        "You have <b>" + remaining + "</b> attempt" + (remaining == 1 ? "" : "s") + " remaining before the application locks for security.<br><br>Tip: Double-check your password or password manager.");
                }
            } catch (Exception ignore) {}

            long delay = switch (attempts) {
                case 1 -> 3000L;
                case 2 -> 15000L;
                case 3 -> 30000L;
                default -> 0L;
            };

            // Show security delay dialog on EDT, hiding overlay while visible
            if (SwingUtilities.isEventDispatchThread()) {
                try {
                    if (lo != null) {
                        lo.runWithOverlayHidden(() -> SecurityDelayDialog.showDialog(delay, parentFrame));
                    } else {
                        SecurityDelayDialog.showDialog(delay, parentFrame);
                    }
                } catch (Exception ignore) {}
            } else {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            if (lo != null) {
                                lo.runWithOverlayHidden(() -> SecurityDelayDialog.showDialog(delay, parentFrame));
                            } else {
                                SecurityDelayDialog.showDialog(delay, parentFrame);
                            }
                        } catch (Exception ignore) {}
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> SecurityDelayDialog.showDialog(delay, parentFrame));
                }
            }

            return true; // allow retry
        }

        // Non-authentication error: show file error and stop
        try {
            if (lo != null) {
                lo.runWithOverlayHidden(() -> logFileHandler.showErrorDialog("<html><b>📁 Load Failed</b><br><br>Unable to load log entries due to a file error.<br><br><i>Technical details: " + e.getClass().getSimpleName() + "</i><br><br><i>Tip: The file may be corrupted. Try restoring from a backup.</i></html>"));
            } else {
                logFileHandler.showErrorDialog("<html><b>📁 Load Failed</b><br><br>Unable to load log entries due to a file error.<br><br><i>Technical details: " + e.getClass().getSimpleName() + "</i><br><br><i>Tip: The file may be corrupted. Try restoring from a backup.</i></html>");
            }
        } catch (Exception ignore) {}

        if (exitOnCancel) {
            System.exit(0);
        }
        return false;
    }
}
