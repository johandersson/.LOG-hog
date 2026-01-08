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

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

/**
 * Base class for progress dialogs with consistent styling.
 * Provides common layout with message label, progress info label, and progress bar.
 */
public abstract class ProgressDialogBase {
    
    protected final JDialog dialog;
    protected final JProgressBar progressBar;
    protected final JLabel messageLabel;
    protected final JLabel progressLabel;
    
    /**
     * Creates a new progress dialog with standard layout.
     *
     * @param parent the parent frame
     * @param title the dialog title
     * @param modal whether the dialog should be modal
     */
    protected ProgressDialogBase(Frame parent, String title, boolean modal) {
        dialog = new JDialog(parent, title, modal);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        
        // Create center panel with message and progress info
        var centerPanel = new JPanel(new BorderLayout());
        messageLabel = new JLabel("", SwingConstants.CENTER);
        centerPanel.add(messageLabel, BorderLayout.NORTH);
        
        progressLabel = new JLabel("", SwingConstants.CENTER);
        progressLabel.setFont(progressLabel.getFont().deriveFont(14.0f));
        centerPanel.add(progressLabel, BorderLayout.SOUTH);
        
        dialog.add(centerPanel, BorderLayout.CENTER);
        
        // Progress bar
        progressBar = new JProgressBar(0, 100);
        dialog.add(progressBar, BorderLayout.SOUTH);
        
        dialog.setSize(350, 120);
        dialog.setLocationRelativeTo(parent);
    }
    
    /**
     * Formats the progress display with percentage and optional time remaining.
     *
     * @param progress the progress percentage (0-100)
     * @param remainingSeconds the estimated seconds remaining, or -1 if unknown
     * @return formatted progress string
     */
    protected String formatProgress(int progress, long remainingSeconds) {
        if (remainingSeconds > 0) {
            return progress + "% - " + remainingSeconds + " seconds remaining";
        } else if (remainingSeconds == 0) {
            return progress + "% - 0 seconds remaining";
        } else {
            return progress + "%";
        }
    }
}
