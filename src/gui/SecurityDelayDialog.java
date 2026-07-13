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
import main.RandomizationUtils;

import javax.swing.Timer;

public class SecurityDelayDialog extends ProgressDialogBase {

    public static void showDialog(long delayMillis, Frame parent) {
        showDialog(delayMillis, parent, "Security delay after failed password attempt...");
    }

    public static void showDialog(long delayMillis, Frame parent, String message) {
        if (delayMillis <= 0) return;
        
        // Use RandomizationUtils for advanced randomization
        long randomizedDelay = RandomizationUtils.randomizeDelay(delayMillis);
        randomizedDelay = Math.max(1000, randomizedDelay);
        
        var securityDialog = new SecurityDelayDialog(parent, "Security Delay", message);
        securityDialog.startCountdown(randomizedDelay);
    }
    
    /**
     * Creates a security delay dialog.
     *
     * @param parent the parent frame
     * @param title the dialog title
     * @param message the message to display
     */
    private SecurityDelayDialog(Frame parent, String title, String message) {
        super(parent, title, true); // Modal dialog
        messageLabel.setText(message);
    }
    
    /**
     * Starts the countdown timer.
     *
     * @param delayMillis the delay in milliseconds
     */
    private void startCountdown(long delayMillis) {
        final long delayNanos = delayMillis * 1_000_000L;
        final long startNanos = System.nanoTime();

        progressBar.setValue(0);
        long initialRemainingSeconds = (delayMillis + 999L) / 1000L;
        progressLabel.setText(formatProgress(0, initialRemainingSeconds));
        
        // Use Swing Timer for smooth progress updates
        var timer = new Timer(50, null);
        timer.addActionListener(e -> {
            long elapsedNanos = System.nanoTime() - startNanos;
            if (elapsedNanos >= delayNanos) {
                progressBar.setValue(100);
                progressLabel.setText(formatProgress(100, 0));
                timer.stop();
                dialog.dispose();
            } else {
                int progress = (int) ((elapsedNanos * 100L) / delayNanos);
                progressBar.setValue(progress);
                
                // Update countdown every tick
                long remainingNanos = delayNanos - elapsedNanos;
                long remainingMillis = Math.max(0L, remainingNanos / 1_000_000L);
                long remainingSeconds = (remainingMillis + 999L) / 1000L; // Round up
                progressLabel.setText(formatProgress(progress, remainingSeconds));
            }
        });
        
        timer.start();
        dialog.setVisible(true);
    }
}