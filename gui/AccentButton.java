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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class AccentButton extends JButton {
    private static final Color NORMAL_COLOR = new Color(0x2F80ED);
    private static final Color HOVER_COLOR = new Color(0x1565C0); // Darker hover
    private static final Color PRESSED_COLOR = new Color(0x0D47A1); // Much darker pressed
    private static final Color SHADOW_COLOR_1 = new Color(47, 128, 237, 60); // Light shadow
    private static final Color SHADOW_COLOR_2 = new Color(47, 128, 237, 30); // Lighter shadow
    private static final Color SHADOW_COLOR_3 = new Color(47, 128, 237, 15); // Very light shadow

    private int shadowOffset = 3; // Increased from 2 for more shadow
    private int cornerRadius = 8;

    public AccentButton(String text) {
        super(text);
        setForeground(Color.WHITE);
        setBackground(NORMAL_COLOR);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false); // Make transparent so we can paint our own background
        setContentAreaFilled(false);
        setFont(getFont().deriveFont(Font.BOLD, 12f));

        // Add hover and press effects
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(HOVER_COLOR);
                shadowOffset = 6; // Increased from 4 for more shadow on hover
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(NORMAL_COLOR);
                shadowOffset = 3; // Back to increased normal shadow
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(PRESSED_COLOR);
                shadowOffset = 1; // Press down effect
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (getBounds().contains(e.getPoint())) {
                    setBackground(HOVER_COLOR);
                    shadowOffset = 6; // Increased hover shadow
                } else {
                    setBackground(NORMAL_COLOR);
                    shadowOffset = 3; // Increased normal shadow
                }
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw Material Design shadows (multiple layers)
        for (int i = shadowOffset; i >= 1; i--) {
            int alpha = 70 - (i * 10); // Adjusted for more shadow layers with increased offsets
            if (alpha > 0) {
                g2.setColor(new Color(47, 128, 237, Math.max(3, alpha)));
                g2.fillRoundRect(i, i, width - 2*i, height - 2*i, cornerRadius, cornerRadius);
            }
        }

        // Draw main button background
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        g2.dispose();

        // Paint text and other components
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Don't paint default border, we handle shadows in paintComponent
    }
}
