package gui;

import java.awt.Frame;
import javax.swing.SwingUtilities;

/**
 * Utility to run background tasks with an optional modal progress dialog.
 */
public final class UiTaskRunner {
    private UiTaskRunner() {}

    /**
     * Runs the given task on a background thread while showing a modal progress
     * dialog. The dialog will be closed when the task finishes.
     *
     * @param parent parent frame for dialog (may be null)
     * @param title dialog title
     * @param status initial status message
     * @param task the task to run
     */
    public static void runModalBackgroundTask(Frame parent, String title, String status, Runnable task) {
        LoadingProgressDialog progress = new LoadingProgressDialog(parent, title, true);
        progress.setStatus(status);
        progress.setIndeterminate(true);

        Thread bg = new Thread(() -> {
            try {
                task.run();
            } finally {
                SwingUtilities.invokeLater(progress::close);
            }
        }, "loghog-ui-task");
        bg.setDaemon(true);
        bg.start();

        progress.showModal();
    }
}
