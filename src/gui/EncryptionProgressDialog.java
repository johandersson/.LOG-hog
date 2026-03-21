package gui;

import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Specialized progress dialog for encryption operations.
 * Stays visible at 100% and adds an OK button when the operation completes.
 */
public class EncryptionProgressDialog extends LoadingProgressDialog {
    private boolean completionShown = false;

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

            // Add an OK button below the existing content
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton ok = new JButton("OK");
            ok.addActionListener(e -> close());
            btnPanel.add(ok);

            // Add to dialog; BorderLayout.PAGE_END will replace existing SOUTH component,
            // but ProgressDialogBase uses SOUTH for the progress bar — instead we add
            // the button panel to the dialog's content pane after the progress bar so
            // it appears visually below it.
            dialog.getContentPane().add(btnPanel, java.awt.BorderLayout.AFTER_LAST_LINE);
            dialog.revalidate();
            dialog.repaint();
        });
    }
}
