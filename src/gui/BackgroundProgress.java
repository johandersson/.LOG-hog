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

package gui;

import java.awt.Frame;
import java.awt.Window;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Handle for the indeterminate {@link LoadingProgressDialog} shown while a
 * background thread performs a potentially slow operation such as saving an
 * entry to a very large log file.
 * <p>
 * The dialog only becomes visible after a short delay, so quick operations do
 * not cause a visible flash, while slow ones give the user feedback instead of
 * an apparently frozen window.
 */
public final class BackgroundProgress {

    /** Delay before the dialog becomes visible, matching the full log view. */
    private static final int SHOW_DELAY_MS = 150;

    private final LoadingProgressDialog dialog;
    private final Timer showTimer;
    private boolean closed;

    private BackgroundProgress(LoadingProgressDialog dialog, Timer showTimer) {
        this.dialog = dialog;
        this.showTimer = showTimer;
    }

    /**
     * Creates and schedules a progress dialog. Safe to call from any thread.
     *
     * @param title the dialog title
     * @param status the initial status message
     * @return a handle used to update the status and to close the dialog
     */
    public static BackgroundProgress show(String title, String status) {
        final BackgroundProgress[] holder = new BackgroundProgress[1];
        runOnEdtAndWait(() -> {
            LoadingProgressDialog dialog = new LoadingProgressDialog(findOwnerFrame(), title);
            dialog.setStatus(status);
            dialog.setIndeterminate(true);
            Timer timer = new Timer(SHOW_DELAY_MS, e -> dialog.show());
            timer.setRepeats(false);
            timer.start();
            holder[0] = new BackgroundProgress(dialog, timer);
        });
        if (holder[0] == null) {
            // Could not create the dialog (e.g. headless environment): use a no-op handle
            return new BackgroundProgress(null, null);
        }
        return holder[0];
    }

    /**
     * Updates the status message shown in the dialog.
     *
     * @param status the new status message
     */
    public void setStatus(String status) {
        if (dialog != null) {
            dialog.setStatus(status);
        }
    }

    /** Cancels the pending show and closes the dialog if it is already visible. */
    public void close() {
        runOnEdt(() -> {
            if (closed) return;
            closed = true;
            if (showTimer != null && showTimer.isRunning()) {
                showTimer.stop();
            }
            if (dialog != null) {
                dialog.close();
            }
        });
    }

    /** Returns the currently active application window, or null when none is available. */
    private static Frame findOwnerFrame() {
        Window active = javax.swing.FocusManager.getCurrentManager().getActiveWindow();
        if (active instanceof Frame frame) {
            return frame;
        }
        for (Frame frame : Frame.getFrames()) {
            if (frame.isShowing()) {
                return frame;
            }
        }
        return null;
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private static void runOnEdtAndWait(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Dialog creation failed; callers fall back to a no-op handle
        }
    }
}
