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

package gui;

import java.awt.Frame;

import javax.swing.SwingUtilities;

/**
 * Progress dialog for showing loading status when decrypting/loading large files.
 * Provides visual feedback to users during potentially long-running operations.
 */
public class LoadingProgressDialog extends ProgressDialogBase {
    
    private long startTime;
    private long totalBytes;
    private long processedBytes;
    
    /**
     * Creates a new loading progress dialog.
     *
     * @param parent the parent frame
     * @param title the dialog title
     */
    public LoadingProgressDialog(Frame parent, String title) {
        super(parent, title, false); // Non-modal so UI stays responsive
        messageLabel.setText("Loading encrypted file...");
    }
    
    /**
     * Shows the dialog.
     */
    public void show() {
        startTime = System.currentTimeMillis();
        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }
    
    /**
     * Updates the status message.
     *
     * @param status the status message
     */
    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> messageLabel.setText(status));
    }
    
    /**
     * Sets the total bytes to process (for calculating percentage).
     *
     * @param bytes the total bytes
     */
    public void setTotalBytes(long bytes) {
        this.totalBytes = bytes;
        this.processedBytes = 0;
    }
    
    /**
     * Updates the processed bytes and calculates progress.
     *
     * @param bytes the number of bytes processed so far
     */
    public void setProcessedBytes(long bytes) {
        this.processedBytes = bytes;
        updateProgress();
    }
    
    /**
     * Sets the progress value (0-100) directly.
     *
     * @param value the progress value
     */
    public void setProgress(int value) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            progressLabel.setText(value + "%");
        });
    }
    
    /**
     * Updates progress display with percentage and estimated time.
     */
    private void updateProgress() {
        if (totalBytes <= 0) return;
        
        SwingUtilities.invokeLater(() -> {
            int progress = (int) ((processedBytes * 100) / totalBytes);
            progressBar.setValue(progress);
            
            // Calculate estimated time remaining
            long elapsed = System.currentTimeMillis() - startTime;
            long remainingSeconds = -1;
            
            if (elapsed > 0 && processedBytes > 0) {
                long estimatedTotal = (elapsed * totalBytes) / processedBytes;
                long remaining = estimatedTotal - elapsed;
                remainingSeconds = (remaining + 999) / 1000; // Round up
            }
            
            progressLabel.setText(formatProgress(progress, remainingSeconds));
        });
    }
    
    /**
     * Closes and disposes the dialog.
     */
    public void close() {
        SwingUtilities.invokeLater(() -> {
            dialog.setVisible(false);
            dialog.dispose();
        });
    }
}
