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

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Progress dialog for showing loading status when decrypting/loading large files.
 * Provides visual feedback to users during potentially long-running operations.
 */
public class LoadingProgressDialog {
    
    private final JDialog dialog;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    
    /**
     * Creates a new loading progress dialog.
     *
     * @param parent the parent frame
     * @param title the dialog title
     */
    public LoadingProgressDialog(Frame parent, String title) {
        dialog = new JDialog(parent, title, false); // Non-modal so UI stays responsive
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));
        
        // Status label
        statusLabel = new JLabel("Loading encrypted file...", SwingConstants.CENTER);
        statusLabel.setFont(statusLabel.getFont().deriveFont(12.0f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); // Indeterminate by default
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        dialog.add(statusLabel, BorderLayout.CENTER);
        dialog.add(progressBar, BorderLayout.SOUTH);
        
        dialog.setSize(350, 100);
        dialog.setLocationRelativeTo(parent);
    }
    
    /**
     * Shows the dialog.
     */
    public void show() {
        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }
    
    /**
     * Updates the status message.
     *
     * @param status the status message
     */
    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }
    
    /**
     * Sets the progress value (0-100).
     *
     * @param value the progress value
     */
    public void setProgress(int value) {
        SwingUtilities.invokeLater(() -> {
            if (progressBar.isIndeterminate()) {
                progressBar.setIndeterminate(false);
            }
            progressBar.setValue(value);
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
