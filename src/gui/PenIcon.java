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

        // Draw pen body (angled blue rectangle)
        g2d.setColor(new Color(0x4A90E2)); // Blue
        int[] xBody = {x + 2, x + 14, x + 12, x + 0};
        int[] yBody = {y + 6, y + 3, y + 13, y + 16};
        g2d.fillPolygon(xBody, yBody, 4);

        // Draw pen body highlight
        g2d.setColor(new Color(0x6BB3FF)); // Lighter blue
        int[] xHighlight = {x + 3, x + 13, x + 11, x + 1};
        int[] yHighlight = {y + 7, y + 4, y + 12, y + 15};
        g2d.fillPolygon(xHighlight, yHighlight, 4);

        // Draw pen tip (pointed end)
        g2d.setColor(Color.BLACK);
        int[] xTip = {x + 12, x + 16, x + 14};
        int[] yTip = {y + 13, y + 10, y + 16};
        g2d.fillPolygon(xTip, yTip, 3);

        // Draw pen cap (red)
        g2d.setColor(Color.RED);
        int[] xCap = {x + 0, x + 10, x + 8, x + 0};
        int[] yCap = {y + 4, y + 1, y + 5, y + 8};
        g2d.fillPolygon(xCap, yCap, 4);

        // Draw pen clip
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(x + 6, y + 1, x + 8, y + 0);
        g2d.drawLine(x + 8, y + 0, x + 10, y + 2);

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