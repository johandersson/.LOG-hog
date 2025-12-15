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

public class SplashPainter {
    public static void drawManAndNotepad(Graphics2D g2d, int width, int height) {
        int manX = 40, manY = 50, notepadX = 150, notepadY = 65;
        drawMan(g2d, manX, manY);
        drawSpeechBubble(g2d);
        drawNotepad(g2d, notepadX, notepadY);
        drawPen(g2d, notepadX, notepadY);
    }

    private static void drawMan(Graphics2D g2d, int manX, int manY) {
        g2d.setStroke(new BasicStroke(2));
        Color skin = new Color(255, 218, 185), shirt = Color.RED, pants = Color.BLUE;
        drawHead(g2d, manX, manY, skin);
        drawHair(g2d, manX, manY);
        drawEyes(g2d, manX, manY);
        drawBody(g2d, manX, manY, shirt, pants);
    }

    private static void drawHead(Graphics2D g2d, int x, int y, Color skin) {
        g2d.setColor(skin);
        g2d.fillOval(x, y, 20, 20);
    }

    private static void drawHair(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x - 2, y - 2, 24, 10);
    }

    private static void drawEyes(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x + 5, y + 5, 3, 3);
        g2d.fillOval(x + 12, y + 5, 3, 3);
    }

    private static void drawBody(Graphics2D g2d, int x, int y, Color shirt, Color pants) {
        g2d.setColor(shirt);
        g2d.fillRect(x + 5, y + 20, 10, 15);
        g2d.setColor(pants);
        g2d.fillRect(x + 5, y + 35, 10, 10);
    }

    private static void drawSpeechBubble(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(60, 20, 80, 30, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(60, 20, 80, 30, 10, 10);
        g2d.drawString("LogHog!", 70, 40);
        g2d.drawLine(75, 50, 85, 60); // arrow
    }

    private static void drawNotepad(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x, y, 60, 80);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, 60, 80);
        for (int i = 1; i < 5; i++) {
            g2d.drawLine(x, y + i * 16, x + 60, y + i * 16);
        }
    }

    private static void drawPen(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.BLUE);
        g2d.fillRect(x + 70, y + 20, 20, 3);
    }
}