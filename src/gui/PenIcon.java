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
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.Icon;

/**
 * A pen icon that renders an SVG pen/edit icon.
 */
public class PenIcon implements Icon {
    private final int width;
    private final int height;
    private Image svgImage;

    public PenIcon(int width, int height, Color color) {
        this.width = width;
        this.height = height;
        loadSvgImage();
    }

    public PenIcon(Color color) {
        this(20, 20, color);
    }

    private void loadSvgImage() {
        try {
            // Load the SVG content
            InputStream is = getClass().getResourceAsStream("/resources/pen_icon.svg");
            if (is != null) {
                String svgContent = readSvgContent(is);
                svgImage = renderSvgToImage(svgContent);
            }
        } catch (Exception e) {
            // Fallback to programmatic drawing if SVG loading fails
            svgImage = null;
        }
    }

    private String readSvgContent(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private Image renderSvgToImage(String svgContent) {
        // Create a buffered image to render the SVG
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Parse and render the SVG paths
        renderSvgPaths(g2d, svgContent);

        g2d.dispose();
        return image;
    }

    private void renderSvgPaths(Graphics2D g2d, String svgContent) {
        // Scale factor to fit the large SVG viewBox (494.936) into our 16x16 icon
        double scale = width / 494.936;

        g2d.setColor(Color.BLACK);

        // Extract and render the two main paths from the SVG
        // Path 1: The rectangular body
        String path1 = "M389.844,182.85c-6.743,0-12.21,5.467-12.21,12.21v222.968c0,23.562-19.174,42.735-42.736,42.735H67.157 c-23.562,0-42.736-19.174,42.736-42.735V150.285c0-23.562,19.174-42.735,42.736-42.735h267.741c6.743,0,12.21-5.467,12.21-12.21 s-5.467-12.21-12.21-12.21H67.157C30.126,83.13,0,113.255,0,150.285v267.743c0,37.029,30.126,67.155,67.157,67.155h267.741 c37.03,0,67.156-30.126,67.156-67.155V195.061C402.054,188.318,396.587,182.85,389.844,182.85z";
        renderSvgPath(g2d, path1, scale);

        // Path 2: The edit cursor/nib
        String path2 = "M483.876,20.791c-14.72-14.72-38.669-14.714-53.377,0L221.352,229.944c-0.28,0.28-3.434,3.559-4.251,5.396l-28.963,65.069 c-2.057,4.619-1.056,10.027,2.521,13.6c2.337,2.336,5.461,3.576,8.639,3.576c1.675,0,3.362-0.346,4.96-1.057l65.07-28.963 c1.83-0.815,5.114-3.97,5.396-4.25L483.876,74.169c7.131-7.131,11.06-16.61,11.06-26.692 C494.936,37.396,491.007,27.915,483.876,20.791z M466.61,56.897L257.457,266.05c-0.035,0.036-0.055,0.078-0.089,0.107 l-33.989,15.131L238.51,247.3c0.03-0.036,0.071-0.055,0.107-0.09L447.765,38.058c5.038-5.039,13.819-5.033,18.846,0.005 c2.518,2.51,3.905,5.855,3.905,9.414C470.516,51.036,469.127,54.38,466.61,56.897z";
        renderSvgPath(g2d, path2, scale);
    }

    private void renderSvgPath(Graphics2D g2d, String pathData, double scale) {
        // Simple SVG path parser for basic commands
        Path2D path = new Path2D.Double();

        // Parse the path data manually
        String[] parts = pathData.split("(?=[MLCz])");
        double currentX = 0, currentY = 0;

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            char command = part.charAt(0);
            String coords = part.substring(1).trim();

            switch (command) {
                case 'M':
                    // Move to
                    String[] moveCoords = coords.split(",");
                    if (moveCoords.length == 2) {
                        currentX = Double.parseDouble(moveCoords[0]) * scale;
                        currentY = Double.parseDouble(moveCoords[1]) * scale;
                        path.moveTo(currentX, currentY);
                    }
                    break;
                case 'c':
                    // Relative cubic bezier curve - simplified to line for basic rendering
                    String[] curveCoords = coords.split(",");
                    if (curveCoords.length >= 6) {
                        double endX = currentX + Double.parseDouble(curveCoords[4]) * scale;
                        double endY = currentY + Double.parseDouble(curveCoords[5]) * scale;
                        path.lineTo(endX, endY);
                        currentX = endX;
                        currentY = endY;
                    }
                    break;
                case 'L':
                    // Line to
                    String[] lineCoords = coords.split(",");
                    if (lineCoords.length == 2) {
                        currentX = Double.parseDouble(lineCoords[0]) * scale;
                        currentY = Double.parseDouble(lineCoords[1]) * scale;
                        path.lineTo(currentX, currentY);
                    }
                    break;
                case 'z':
                    // Close path
                    path.closePath();
                    break;
            }
        }

        g2d.fill(path);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (svgImage != null) {
            g.drawImage(svgImage, x, y, width, height, null);
        } else {
            // Fallback: render programmatically if SVG loading failed
            renderFallbackIcon((Graphics2D) g, x, y);
        }
    }

    private void renderFallbackIcon(Graphics2D g2d, int x, int y) {
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