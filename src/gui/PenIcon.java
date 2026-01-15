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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/**
 * A simple pen icon for edit buttons.
 */
public class PenIcon implements Icon {
    private final int width;
    private final int height;
    private final Color color;

    public PenIcon(int width, int height, Color color) {
        this.width = width;
        this.height = height;
        this.color = color;
    }

    public PenIcon(Color color) {
        this(16, 16, color);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw pen body (main cylinder - blue)
        g2d.setColor(new Color(0x4A90E2)); // Blue
        g2d.fillRoundRect(x + 3, y + 2, 10, 12, 2, 2);

        // Draw pen body highlight
        g2d.setColor(new Color(0x6BB3FF)); // Lighter blue
        g2d.fillRoundRect(x + 4, y + 3, 8, 10, 1, 1);

        // Draw pen tip (black nib)
        g2d.setColor(Color.BLACK);
        int[] xNib = {x + 6, x + 10, x + 8};
        int[] yNib = {y + 14, y + 14, y + 16};
        g2d.fillPolygon(xNib, yNib, 3);

        // Draw pen cap (red)
        g2d.setColor(Color.RED);
        g2d.fillRoundRect(x + 5, y + 0, 6, 4, 1, 1);

        // Draw pen clip (metallic silver)
        g2d.setColor(new Color(0xC0C0C0));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(x + 9, y + 0, x + 12, y + 3);
        g2d.drawLine(x + 10, y + 0, x + 13, y + 3);

        // Draw click mechanism (small silver dot)
        g2d.setColor(new Color(0xC0C0C0));
        g2d.fillOval(x + 7, y + 5, 2, 2);

        g2d.dispose();
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}