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

public class SplashScreen extends JDialog {
    private int animationFrame = 0;
    private javax.swing.Timer animationTimer;
    private JButton okButton;
    private java.util.List<String> entriesList;

    public SplashScreen() {
        super((Frame) null, "Splash", true); // modal dialog
        setUndecorated(true);
        setSize(450, 300);
        setLocationRelativeTo(null);

        // Load and shuffle entries once
        entriesList = SplashEntryLoader.loadSplashEntries();

        var panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                var g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Shadow
                g2d.setColor(new Color(0, 0, 0, 120));
                g2d.fillRoundRect(12, 12, getWidth() - 12, getHeight() - 12, 30, 30);

                // Gradient background from blue to cyan
                var gp = new GradientPaint(0, 0, new Color(0, 102, 204), getWidth(), getHeight(), new Color(0, 204, 255));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw man and notepad
                drawManAndNotepad(g2d);
            }
        };
        panel.setLayout(null); // for absolute positioning

        okButton = new StandardButton("OK", Color.WHITE, Color.BLACK);
        okButton.setBounds(200, 250, 50, 30);
        okButton.addActionListener(e -> dispose());
        panel.add(okButton);

        setContentPane(panel);

        // Start animation before showing
        animationTimer = new javax.swing.Timer(600, e -> {
            animationFrame++;
            if (animationFrame > 5) {
                animationTimer.stop();
            }
            panel.repaint();
        });
        animationTimer.start();

        setVisible(true);
    }

    private void drawManAndNotepad(Graphics2D g2d) {
        var manX = 40; // Moved further right, closer to notepad
        var manY = 50;
        var notepadX = 150;
        var notepadY = 65; // Moved up slightly for better composition

        drawMan(g2d, manX, manY);
        drawSpeechBubble(g2d);
        drawNotepad(g2d, notepadX, notepadY);
        drawPen(g2d, notepadX, notepadY);
    }

    private void drawMan(Graphics2D g2d, int manX, int manY) {
        g2d.setStroke(new BasicStroke(2));
        var skin = new Color(255, 218, 185);
        var shirt = Color.RED; // red shirt
        var pants = Color.BLUE;

        drawHead(g2d, manX, manY, skin);
        drawHair(g2d, manX, manY);
        drawEyes(g2d, manX, manY);
        drawMouth(g2d, manX, manY);
        drawMustache(g2d, manX, manY);
        drawBowtie(g2d, manX, manY);
        drawBody(g2d, manX, manY);
        drawArms(g2d, manX, manY, skin);
        drawHands(g2d, manX, manY, skin);
        drawLegs(g2d, manX, manY, pants);
        drawShoes(g2d, manX, manY);
    }

    private void drawHead(Graphics2D g2d, int manX, int manY, Color skin) {
        // Head
        g2d.setColor(skin);
        g2d.fillOval(manX + 15, manY, 30, 30);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 15, manY, 30, 30);
    }

    private void drawHair(Graphics2D g2d, int manX, int manY) {
        // Hair - fuller brown
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillOval(manX + 15, manY - 10, 30, 15);
        g2d.fillOval(manX + 10, manY, 10, 10); // left side
        g2d.fillOval(manX + 40, manY, 10, 10); // right side
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 15, manY - 10, 30, 15);
        g2d.drawOval(manX + 10, manY, 10, 10);
        g2d.drawOval(manX + 40, manY, 10, 10);
    }

    private void drawEyes(Graphics2D g2d, int manX, int manY) {
        // Eyes - nice
        g2d.setColor(Color.WHITE);
        g2d.fillOval(manX + 20, manY + 8, 8, 8);
        g2d.fillOval(manX + 32, manY + 8, 8, 8);
        g2d.setColor(new Color(0, 0, 255)); // blue irises
        g2d.fillOval(manX + 22, manY + 10, 4, 4);
        g2d.fillOval(manX + 34, manY + 10, 4, 4);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 20, manY + 8, 8, 8);
        g2d.drawOval(manX + 32, manY + 8, 8, 8);
        g2d.fillOval(manX + 24, manY + 12, 2, 2); // pupils
        g2d.fillOval(manX + 36, manY + 12, 2, 2);
        // Eyelashes
        g2d.drawLine(manX + 20, manY + 8, manX + 18, manY + 6);
        g2d.drawLine(manX + 24, manY + 8, manX + 26, manY + 6);
        g2d.drawLine(manX + 32, manY + 8, manX + 30, manY + 6);
        g2d.drawLine(manX + 36, manY + 8, manX + 38, manY + 6);
    }

    private void drawMouth(Graphics2D g2d, int manX, int manY) {
        // Mouth
        g2d.drawArc(manX + 25, manY + 18, 10, 5, 0, -120);
    }

    private void drawMustache(Graphics2D g2d, int manX, int manY) {
        // Mustache
        var mustacheGradient = new GradientPaint(manX + 22, manY + 15, new Color(139, 69, 19), manX + 22, manY + 18, new Color(101, 67, 33));
        g2d.setPaint(mustacheGradient);
        g2d.drawLine(manX + 22, manY + 17, manX + 28, manY + 16);
        g2d.drawLine(manX + 32, manY + 17, manX + 38, manY + 16);
    }

    private void drawBowtie(Graphics2D g2d, int manX, int manY) {
        // Bowtie
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.RED);
        g2d.fillOval(manX + 25, manY + 35, 10, 10);
        g2d.fillOval(manX + 20, manY + 40, 5, 5);
        g2d.fillOval(manX + 35, manY + 40, 5, 5);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 25, manY + 35, 10, 10);
    }

    private void drawBody(Graphics2D g2d, int manX, int manY) {
        // Body - gradient white shirt
        var shirtGradient = new GradientPaint(manX + 10, manY + 30, Color.WHITE, manX + 10, manY + 50, new Color(200, 200, 200));
        g2d.setPaint(shirtGradient);
        g2d.fillRect(manX + 10, manY + 30, 40, 40);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(manX + 10, manY + 30, 40, 40);

        // Add text on shirt
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 8));
        FontMetrics fm = g2d.getFontMetrics();
        String line1 = ".LOG-hog";
        String line2 = "License:";
        String line3 = "GPL3";
        int textX = manX + 10 + (40 - fm.stringWidth(line1)) / 2; // center horizontally
        int textY = manY + 42; // position in middle of shirt

        // Draw shadows first
        g2d.setColor(new Color(64, 64, 64, 180)); // Semi-transparent dark gray
        g2d.drawString(line1, textX + 1, textY + 1);
        g2d.drawString(line2, textX + 1, textY + fm.getHeight() + 1);
        g2d.drawString(line3, textX + 1, textY + 2 * fm.getHeight() + 1);

        // Draw main text
        g2d.setColor(Color.BLACK);
        g2d.drawString(line1, textX, textY);
        g2d.drawString(line2, textX, textY + fm.getHeight());
        g2d.drawString(line3, textX, textY + 2 * fm.getHeight());
    }

    private void drawArms(Graphics2D g2d, int manX, int manY, Color skin) {
        // Arms - rectangles with hands
        g2d.setColor(skin);
        g2d.fillRect(manX, manY + 35, 8, 25); // left arm
        g2d.fillRect(manX + 52, manY + 35, 8, 25); // right arm
        g2d.setColor(Color.BLACK);
        g2d.drawRect(manX, manY + 35, 8, 25);
        g2d.drawRect(manX + 52, manY + 35, 8, 25);
    }

    private void drawHands(Graphics2D g2d, int manX, int manY, Color skin) {
        // Hands
        g2d.setColor(skin);
        g2d.fillOval(manX - 2, manY + 55, 12, 10); // left hand
        g2d.fillOval(manX + 50, manY + 55, 12, 10); // right hand
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX - 2, manY + 55, 12, 10);
        g2d.drawOval(manX + 50, manY + 55, 12, 10);
        // Fingers for left hand
        g2d.drawLine(manX + 2, manY + 60, manX + 2, manY + 65); // thumb
        g2d.drawLine(manX + 5, manY + 60, manX + 5, manY + 68);
        g2d.drawLine(manX + 7, manY + 60, manX + 7, manY + 68);
        g2d.drawLine(manX + 9, manY + 60, manX + 9, manY + 68);
        // Fingers for right hand
        g2d.drawLine(manX + 54, manY + 60, manX + 54, manY + 65); // thumb
        g2d.drawLine(manX + 57, manY + 60, manX + 57, manY + 68);
        g2d.drawLine(manX + 59, manY + 60, manX + 59, manY + 68);
        g2d.drawLine(manX + 61, manY + 60, manX + 61, manY + 68);
        // Pencil in right hand
        g2d.setColor(Color.BLACK);
        g2d.fillRect(manX + 51, manY + 55, 4, 15); // pencil body
        g2d.setColor(Color.RED);
        g2d.fillRect(manX + 51, manY + 55, 4, 3); // eraser
        g2d.setColor(Color.BLACK);
        g2d.drawRect(manX + 51, manY + 55, 4, 15);
    }

    private void drawLegs(Graphics2D g2d, int manX, int manY, Color pants) {
        // Legs
        g2d.setColor(pants);
        g2d.fillRect(manX + 15, manY + 70, 10, 20);
        g2d.fillRect(manX + 35, manY + 70, 10, 20);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(manX + 15, manY + 70, 10, 20);
        g2d.drawRect(manX + 35, manY + 70, 10, 20);
    }

    private void drawShoes(Graphics2D g2d, int manX, int manY) {
        // Shoes
        g2d.setColor(Color.BLACK);
        g2d.fillRect(manX + 12, manY + 90, 16, 10); // left shoe
        g2d.fillRect(manX + 32, manY + 90, 16, 10); // right shoe
        g2d.drawRect(manX + 12, manY + 90, 16, 10);
        g2d.drawRect(manX + 32, manY + 90, 16, 10);
    }

    private void drawSpeechBubble(Graphics2D g2d) {
        // Text content
        String text = "I am a .LOG-hog! v 1.0.";
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        FontMetrics fm = g2d.getFontMetrics();

        // Calculate bubble size based on text
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int padding = 12; // reduced padding for smaller bubble
        int bubbleW = textWidth + (padding * 2);
        int bubbleH = textHeight + (padding * 2);

        // Position bubble - above character's head to indicate he's speaking
        int bubbleX = 15; // Moved to utmost left possible while keeping glow on screen
        int bubbleY = 5;

        // Draw glowing white shadow effect - reduced intensity
        int glowLayers = 3;
        for (int i = glowLayers; i > 0; i--) {
            int glowOffset = i * 1; // Reduced offset for subtler glow
            int glowAlpha = 180 / (i + 1); // Reduced base opacity for less intense glow
            g2d.setColor(new Color(255, 255, 255, glowAlpha));

            // Glow for main bubble
            g2d.fillRoundRect(bubbleX - glowOffset, bubbleY - glowOffset,
                            bubbleW + (glowOffset * 2), bubbleH + (glowOffset * 2), 20, 20);

            // Glow for pointer
            int[] glowXPoints = {bubbleX + 20 - glowOffset, bubbleX + 30 + glowOffset, bubbleX + 25};
            int[] glowYPoints = {bubbleY + bubbleH + glowOffset, bubbleY + bubbleH + 10 + glowOffset, bubbleY + bubbleH + glowOffset};
            g2d.fillPolygon(glowXPoints, glowYPoints, 3);
        }

        // Draw speech bubble background
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(bubbleX, bubbleY, bubbleW, bubbleH, 20, 20);

        // Draw speech bubble border
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(bubbleX, bubbleY, bubbleW, bubbleH, 20, 20);

        // Draw pointer
        int[] xPoints = {bubbleX + 20, bubbleX + 30, bubbleX + 25};
        int[] yPoints = {bubbleY + bubbleH, bubbleY + bubbleH + 10, bubbleY + bubbleH};
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.drawPolygon(xPoints, yPoints, 3);

        // Draw text centered in bubble
        int textX = bubbleX + (bubbleW - textWidth) / 2;
        int textY = bubbleY + padding + fm.getAscent();
        g2d.drawString(text, textX, textY);
    }

    private void drawNotepad(Graphics2D g2d, int notepadX, int notepadY) {
        // Draw notepad - legal pad style with shadow
        // Shadow
        g2d.setColor(new Color(200, 200, 200, 100)); // semi-transparent gray
        g2d.fillRect(notepadX + 5, notepadY + 5, 250, 150);
        // Pad
        g2d.setColor(new Color(255, 255, 204)); // light yellow
        g2d.fillRect(notepadX, notepadY, 250, 150);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(notepadX, notepadY, 250, 150);
        // Blue lines
        g2d.setColor(new Color(173, 216, 230)); // light blue
        for (int i = 1; i <= 8; i++) {
            int y = notepadY + 20 + i * 15;
            g2d.drawLine(notepadX + 10, y, notepadX + 240, y);
        }

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 8));
        g2d.setColor(Color.BLACK);
        for (int i = 0; i < animationFrame && i < entriesList.size(); i++) {
            g2d.drawString(entriesList.get(i), notepadX + 10, notepadY + 30 + i * 15);
        }
    }

    private void drawPen(Graphics2D g2d, int notepadX, int notepadY) {
        // Draw pen
        if (animationFrame > 0 && animationFrame <= entriesList.size()) {
            String lastEntry = entriesList.get(animationFrame - 1);
            int penX = notepadX + 10 + g2d.getFontMetrics().stringWidth(lastEntry);
            int penY = notepadY + 30 + (animationFrame - 1) * 15;
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(penX, penY, penX + 8, penY - 4); // pen
        }
    }
}
