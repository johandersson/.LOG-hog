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
    // Constants for dimensions and positioning
    private static final int WINDOW_WIDTH = 450;
    private static final int WINDOW_HEIGHT = 300;
    private static final int CORNER_RADIUS = 25;
    private static final int MAN_X = 40;
    private static final int MAN_Y = 50;
    private static final int NOTEPAD_X = 150;
    private static final int NOTEPAD_Y = 65;
    private static final int NOTEPAD_WIDTH = 280;
    private static final int NOTEPAD_HEIGHT = 150;
    private static final int OK_BUTTON_WIDTH = 50;
    private static final int OK_BUTTON_HEIGHT = 30;
    private static final int OK_BUTTON_X = 200;
    private static final int OK_BUTTON_Y = 250;

    // Colors
    private static final Color SKIN_COLOR = new Color(255, 218, 185);
    private static final Color SHIRT_COLOR = Color.RED;
    private static final Color PANTS_COLOR = Color.BLUE;
    private static final Color HAIR_COLOR = new Color(139, 69, 19);
    private static final Color DARK_HAIR_COLOR = new Color(101, 67, 33);
    private static final Color EYE_IRIS_COLOR = new Color(0, 0, 255);
    private static final Color GRADIENT_START = new Color(0, 102, 204);
    private static final Color GRADIENT_END = new Color(0, 204, 255);
    private static final Color NOTEPAD_COLOR = new Color(255, 255, 204);
    private static final Color BLUE_LINE_COLOR = new Color(173, 216, 230);

    private int animationFrame = 0;
    private javax.swing.Timer animationTimer;
    private JButton okButton;
    private java.util.List<String> entriesList;
    private Point initialClick;
    // private final CharacterDrawer characterDrawer;

    public SplashScreen() {
        super((Frame) null, "Splash", true); // modal dialog
        // characterDrawer = new CharacterDrawer();
        initializeDialog();
        setupContentPanel();
        setupAnimation();
        showDialog();
    }

    private void initializeDialog() {
        setUndecorated(true);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setShape(new java.awt.geom.RoundRectangle2D.Float(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, CORNER_RADIUS, CORNER_RADIUS));
    }

    private void setupContentPanel() {
        entriesList = SplashEntryLoader.loadSplashEntries();

        var panel = createMainPanel();
        setupMouseListeners(panel);
        setupOkButton(panel);

        setContentPane(panel);
    }

    private JPanel createMainPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBackground((Graphics2D) g);
                drawSceneElements((Graphics2D) g);
            }
        };
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawGlowEffect(g2d);
        drawGradientBackground(g2d);
    }

    private void drawGlowEffect(Graphics2D g2d) {
        for (int i = 3; i > 0; i--) {
            int offset = i * 2;
            int alpha = 60 / i;
            g2d.setColor(new Color(255, 255, 255, alpha));
            g2d.fillRoundRect(offset, offset, getWidth() - offset, getHeight() - offset, CORNER_RADIUS, CORNER_RADIUS);
        }
    }

    private void drawGradientBackground(Graphics2D g2d) {
        var gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), getHeight(), GRADIENT_END);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawCharacter(Graphics2D g2d, int x, int y) {
        // Head
        g2d.setColor(SKIN_COLOR);
        g2d.fillOval(x + 20, y, 20, 20);
        // Hair
        g2d.setColor(HAIR_COLOR);
        g2d.fillOval(x + 18, y - 2, 24, 8);
        // Eyes
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 25, y + 5, 4, 4);
        g2d.fillOval(x + 31, y + 5, 4, 4);
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x + 26, y + 6, 2, 2);
        g2d.fillOval(x + 32, y + 6, 2, 2);
        // Body
        g2d.setColor(SHIRT_COLOR);
        g2d.fillRect(x + 25, y + 20, 10, 30);
        // Arms
        g2d.setColor(SKIN_COLOR);
        g2d.fillRect(x + 15, y + 25, 10, 5);
        g2d.fillRect(x + 35, y + 25, 10, 5);
        // Legs
        g2d.setColor(PANTS_COLOR);
        g2d.fillRect(x + 25, y + 50, 5, 20);
        g2d.fillRect(x + 30, y + 50, 5, 20);
        // Shoes
        g2d.setColor(Color.BLACK);
        g2d.fillRect(x + 23, y + 70, 7, 5);
        g2d.fillRect(x + 30, y + 70, 7, 5);
    }

    private void drawSceneElements(Graphics2D g2d) {
        drawCharacter(g2d, MAN_X, MAN_Y);
        drawSpeechBubble(g2d);
        drawNotepad(g2d, NOTEPAD_X, NOTEPAD_Y);
        drawPen(g2d, NOTEPAD_X, NOTEPAD_Y);
    }

    private void setupMouseListeners(JPanel panel) {
        panel.setLayout(null);

        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                initialClick = e.getPoint();
                handleCharacterClick(e.getX(), e.getY());
            }
        });

        panel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                int x = getLocation().x + e.getX() - initialClick.x;
                int y = getLocation().y + e.getY() - initialClick.y;
                setLocation(x, y);
            }
        });
    }

    private void handleCharacterClick(int clickX, int clickY) {
        // Check if clicked anywhere on the man character
        // Character bounds: X: 35-105, Y: 45-155 (includes all parts: head, body, arms, hands, legs, shoes)
        if (clickX >= 35 && clickX <= 105 && clickY >= 45 && clickY <= 155) {
            utils.WindowShakeAnimation.shake(SplashScreen.this);
        }
    }

    private void setupOkButton(JPanel panel) {
        okButton = new StandardButton("OK", new Color(0xE0E0E0), new Color(0xB0B0B0));
        okButton.setBounds(OK_BUTTON_X, OK_BUTTON_Y, OK_BUTTON_WIDTH, OK_BUTTON_HEIGHT);
        okButton.addActionListener(e -> dispose());
        panel.add(okButton);
    }

    private void setupAnimation() {
        animationTimer = new javax.swing.Timer(600, e -> {
            animationFrame++;
            if (animationFrame > 5) {
                animationTimer.stop();
            }
            repaint();
        });
        animationTimer.start();
    }

    private void showDialog() {
        setVisible(true);
        toFront();
        requestFocus();
    }

    private void drawSpeechBubble(Graphics2D g2d) {
        String text = "I am a .LOG-hog! v 1.0.";
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        FontMetrics fm = g2d.getFontMetrics();

        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int padding = 12;
        int bubbleW = textWidth + (padding * 2);
        int bubbleH = textHeight + (padding * 2);

        int bubbleX = 15;
        int bubbleY = 5;

        drawBubbleGlow(g2d, bubbleX, bubbleY, bubbleW, bubbleH);
        drawBubbleShape(g2d, bubbleX, bubbleY, bubbleW, bubbleH);
        drawBubbleText(g2d, text, bubbleX, bubbleY, bubbleW, padding, fm);
    }

    private void drawBubbleGlow(Graphics2D g2d, int bubbleX, int bubbleY, int bubbleW, int bubbleH) {
        int glowLayers = 3;
        for (int i = glowLayers; i > 0; i--) {
            int glowOffset = i * 1;
            int glowAlpha = 180 / (i + 1);
            g2d.setColor(new Color(255, 255, 255, glowAlpha));

            // Glow for main bubble
            g2d.fillRoundRect(bubbleX - glowOffset, bubbleY - glowOffset,
                            bubbleW + (glowOffset * 2), bubbleH + (glowOffset * 2), 20, 20);

            // Glow for pointer
            int[] glowXPoints = {bubbleX + 20 - glowOffset, bubbleX + 30 + glowOffset, bubbleX + 25};
            int[] glowYPoints = {bubbleY + bubbleH + glowOffset, bubbleY + bubbleH + 10 + glowOffset, bubbleY + bubbleH + glowOffset};
            g2d.fillPolygon(glowXPoints, glowYPoints, 3);
        }
    }

    private void drawBubbleShape(Graphics2D g2d, int bubbleX, int bubbleY, int bubbleW, int bubbleH) {
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
    }

    private void drawBubbleText(Graphics2D g2d, String text, int bubbleX, int bubbleY, int bubbleW, int padding, FontMetrics fm) {
        int textX = bubbleX + (bubbleW - fm.stringWidth(text)) / 2;
        int textY = bubbleY + padding + fm.getAscent();
        g2d.drawString(text, textX, textY);
    }

    private void drawNotepad(Graphics2D g2d, int notepadX, int notepadY) {
        drawNotepadShadow(g2d, notepadX, notepadY);
        drawNotepadBackground(g2d, notepadX, notepadY);
        drawNotepadLines(g2d, notepadX, notepadY);
        drawNotepadEntries(g2d, notepadX, notepadY);
    }

    private void drawNotepadShadow(Graphics2D g2d, int notepadX, int notepadY) {
        g2d.setColor(new Color(200, 200, 200, 100));
        g2d.fillRect(notepadX + 5, notepadY + 5, NOTEPAD_WIDTH, NOTEPAD_HEIGHT);
    }

    private void drawNotepadBackground(Graphics2D g2d, int notepadX, int notepadY) {
        g2d.setColor(NOTEPAD_COLOR);
        g2d.fillRect(notepadX, notepadY, NOTEPAD_WIDTH, NOTEPAD_HEIGHT);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(notepadX, notepadY, NOTEPAD_WIDTH, NOTEPAD_HEIGHT);
    }

    private void drawNotepadLines(Graphics2D g2d, int notepadX, int notepadY) {
        g2d.setColor(BLUE_LINE_COLOR);
        for (int i = 1; i <= 8; i++) {
            int y = notepadY + 20 + i * 15;
            g2d.drawLine(notepadX + 10, y, notepadX + NOTEPAD_WIDTH - 10, y);
        }
    }

    private void drawNotepadEntries(Graphics2D g2d, int notepadX, int notepadY) {
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2d.setColor(Color.BLACK);
        for (int i = 0; i < animationFrame && i < entriesList.size(); i++) {
            g2d.drawString(entriesList.get(i), notepadX + 10, notepadY + 30 + i * 15);
        }
    }

    private void drawPen(Graphics2D g2d, int notepadX, int notepadY) {
        if (animationFrame > 0 && animationFrame <= entriesList.size()) {
            String lastEntry = entriesList.get(animationFrame - 1);
            int penX = notepadX + 10 + g2d.getFontMetrics().stringWidth(lastEntry);
            int penY = notepadY + 30 + (animationFrame - 1) * 15;
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(penX, penY, penX + 8, penY - 4);
        }
    }
}
