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

import java.awt.*;
import javax.swing.*;

public class SecurityDelayDialog {
    public static void showDialog(long delayMillis, Frame parent) {
        showDialog(delayMillis, parent, "Security delay after failed password attempt...");
    }

    public static void showDialog(long delayMillis, Frame parent, String message) {
        if (delayMillis <= 0) return;
        
        // Add ±20% randomization to prevent timing attacks
        long randomizedDelay = delayMillis + (long)(delayMillis * 0.2 * (Math.random() - 0.5));
        // Ensure minimum 1 second delay
        randomizedDelay = Math.max(1000, randomizedDelay);
        
        var dialog = new JDialog(parent, "Security Delay", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        // Create center panel with message and countdown
        var centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JLabel(message, SwingConstants.CENTER), BorderLayout.NORTH);
        var countdownLabel = new JLabel("", SwingConstants.CENTER);
        countdownLabel.setFont(countdownLabel.getFont().deriveFont(14.0f));
        centerPanel.add(countdownLabel, BorderLayout.SOUTH);

        dialog.add(centerPanel, BorderLayout.CENTER);
        var progressBar = new JProgressBar(0, 100);
        dialog.add(progressBar, BorderLayout.SOUTH);
        dialog.setSize(350, 120);
        dialog.setLocationRelativeTo(parent);

        // Use Swing Timer for smooth progress updates
        var timer = new javax.swing.Timer(50, null);
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + randomizedDelay;

        timer.addActionListener(e -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime >= endTime) {
                progressBar.setValue(100);
                countdownLabel.setText("0 seconds remaining");
                timer.stop();
                dialog.dispose();
            } else {
                long elapsed = currentTime - startTime;
                int progress = (int) (elapsed * 100 / delayMillis);
                progressBar.setValue(progress);

                // Update countdown every second
                long remainingMillis = endTime - currentTime;
                long remainingSeconds = (remainingMillis + 999) / 1000; // Round up
                countdownLabel.setText(remainingSeconds + " seconds remaining");
            }
        });

        timer.start();
        dialog.setVisible(true);
    }
}