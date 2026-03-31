package gui;

import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import utils.Log;

/**
 * Specialized progress dialog for encryption operations.
 * Stays visible at 100% and adds an OK button when the operation completes.
 */
public class EncryptionProgressDialog extends LoadingProgressDialog {
    private boolean completionShown; // default false, no initializer needed
    private Runnable onOkCallback;   // default null, no initializer needed

    public EncryptionProgressDialog(Frame parent, String title) {
        super(parent, title);
    }

    /**
     * Show the dialog (non-modal) on the EDT.
     */
    @Override
    public void show() {
        super.show();
    }

    /**
     * Transition the dialog to a completed state and add an OK button.
     * This method is safe to call from any thread.
     */
    public void showCompletion() {
        if (completionShown) return;
        completionShown = true;
        SwingUtilities.invokeLater(() -> {
            setIndeterminate(false);
            setProgress(100);
            setStatus("Encryption complete");

            // Add an OK button below the existing content, left-aligned with spacing
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            btnPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 12, 12));
            AccentButton ok = new AccentButton("OK");
            ok.addActionListener(e -> {
                close();
                if (onOkCallback != null) {
                    try {
                        onOkCallback.run();
                    } catch (Exception ex) {
                        Log.error("Error running onOk callback.", ex);
                    }
                }
            });
            btnPanel.add(ok);

            // Place the button panel after the progress bar so it appears visually below it
            dialog.getContentPane().add(btnPanel, java.awt.BorderLayout.AFTER_LAST_LINE);
            dialog.revalidate();
            dialog.repaint();
        });
    }

    /**
     * Register a callback to be executed when the user clicks OK on the completion state.
     * The callback will be invoked on the EDT after the dialog is closed.
     */
    public void setOnOkCallback(Runnable r) {
        this.onOkCallback = r;
    }
}
