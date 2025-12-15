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

package utils;

import java.awt.*;
import javax.swing.*;

public class Toast {
    public static void showToast(Component parent, String message) {
        showToast(parent, message, 1000);
    }

    public static void showToast(Component parent, String message, int duration) {
        JWindow toast = new JWindow();
        toast.setBackground(new Color(0, 0, 0, 0)); // Transparent background

        // Create a panel with rounded corners
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout());

        JLabel label = new JLabel(message);
        label.setOpaque(false);
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        panel.add(label, BorderLayout.CENTER);

        toast.add(panel);
        toast.pack();

        // Position at center of the parent component or screen center if parent is null
        Point position = calculateToastPosition(parent, toast);
        toast.setLocation(position.x, position.y);

        toast.setVisible(true);

        // Fade out after specified duration
        javax.swing.Timer timer = new javax.swing.Timer(duration, e -> {
            javax.swing.Timer fadeTimer = new javax.swing.Timer(25, null);
            fadeTimer.addActionListener(ev -> {
                float opacity = toast.getOpacity();
                if (opacity > 0) {
                    toast.setOpacity(Math.max(0.0f, opacity - 0.1f));
                } else {
                    fadeTimer.stop();
                    toast.dispose();
                }
            });
            fadeTimer.start();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static Point calculateToastPosition(Component parent, Component toast) {
        int x, y;
        if (parent != null) {
            Dimension parentSize = parent.getSize();
            Point parentLocation = parent.getLocationOnScreen();
            x = parentLocation.x + (parentSize.width - toast.getWidth()) / 2;
            y = parentLocation.y + (parentSize.height - toast.getHeight()) / 2;
        } else {
            // Center on screen if no parent provided
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            x = (screenSize.width - toast.getWidth()) / 2;
            y = (screenSize.height - toast.getHeight()) / 2;
        }
        return new Point(x, y);
    }
}