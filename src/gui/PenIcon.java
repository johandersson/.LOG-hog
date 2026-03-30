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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * A pen icon that loads a PNG image.
 */
public class PenIcon implements Icon {
    private final int width;
    private final int height;
    private Image image;

    public PenIcon(int width, int height, Color color) {
        this.width = width;
        this.height = height;
        loadImage();
    }

    public PenIcon(Color color) {
        this(20, 20, color);
    }

    private void loadImage() {
        try {
            // Try JAR root first (when running from JAR)
            java.net.URL resourceUrl = getClass().getResource("/pencil-line.png");
            if (resourceUrl == null) {
                // Fall back to resources folder (when running from classpath)
                resourceUrl = getClass().getResource("/resources/pencil-line.png");
            }
            
            if (resourceUrl != null) {
                ImageIcon icon = new ImageIcon(resourceUrl);
                image = icon.getImage();
                // System.out.println("PenIcon: Loaded image from " + resourceUrl + ", size: " + image.getWidth(null) + "x" + image.getHeight(null));
            } else {
                // System.out.println("PenIcon: Could not find resource /pencil-line.png or /resources/pencil-line.png");
                image = null;
            }
        } catch (Exception e) {
            // System.out.println("PenIcon: Exception loading image: " + e.getMessage());
            image = null;
        }
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        }
        // If no image, just display button text (no icon)
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