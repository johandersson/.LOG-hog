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

public class StandardButton extends JButton {
    private final Color normalColor;
    private final Color shadowColor;
    private final Color disabledColor;
    private int shadowOffset = 3;
    private final int cornerRadius = 12;

    public StandardButton(String text, Color normalColor, Color shadowColor) {
        this(text, normalColor, shadowColor, new Color(0xB0B0B0));
    }

    public StandardButton(String text, Color normalColor, Color shadowColor, Color disabledColor) {
        super(text);
        this.normalColor = normalColor;
        this.shadowColor = shadowColor;
        this.disabledColor = disabledColor;

        setForeground(Color.BLACK);
        setBackground(normalColor);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        setContentAreaFilled(false);
        setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Add hover effect
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                setBackground(new Color(0xD0D0D0));
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                setBackground(normalColor);
                repaint();
            }
        });
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        setForeground(b ? Color.BLACK : new Color(0x202020));
    }

    @Override
    protected void paintComponent(Graphics g) {
        var g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        var width = getWidth();
        var height = getHeight();

        Color bgColor = getBackground();
        if (!isEnabled()) {
            bgColor = disabledColor;
        }

        // Draw shadows (multiple layers) only if enabled
        if (isEnabled()) {
            for (var i = shadowOffset; i >= 1; i--) {
                var alpha = 60 - (i * 8);
                if (alpha > 0) {
                    var shadow = new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), Math.max(3, alpha));
                    g2.setColor(shadow);
                    g2.fillRoundRect(i, i, width - 2*i, height - 2*i, cornerRadius, cornerRadius);
                }
            }
        }

        // Draw main button background
        g2.setColor(bgColor);
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