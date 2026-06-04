package main;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import gui.PasswordDialog;
import gui.LockOverlay;

/**
 * Utility to prompt for password while ensuring the lock overlay is hidden
 * while modal dialogs are shown and that the dialog is invoked on the EDT.
 */
public final class PasswordPrompter {
    private PasswordPrompter() {}

    public static PasswordDialog.PasswordResult prompt(JFrame parent, String dialogTitle, LockOverlay lo) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            if (lo != null) {
                return lo.withOverlayHidden(() -> PasswordDialog.showPasswordDialog(parent, dialogTitle));
            }
            return PasswordDialog.showPasswordDialog(parent, dialogTitle);
        }

        final PasswordDialog.PasswordResult[] holder = new PasswordDialog.PasswordResult[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                if (lo != null) {
                    holder[0] = lo.withOverlayHidden(() -> PasswordDialog.showPasswordDialog(parent, dialogTitle));
                } else {
                    holder[0] = PasswordDialog.showPasswordDialog(parent, dialogTitle);
                }
            } catch (Exception e) {
                holder[0] = null;
            }
        });
        return holder[0];
    }
}
