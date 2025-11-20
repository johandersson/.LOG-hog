import java.awt.*;
import javax.swing.*;

public class SplashScreen extends JWindow {
    private int animationFrame = 0;
    private Timer animationTimer;

    public SplashScreen() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background from blue to cyan
                GradientPaint gp = new GradientPaint(0, 0, new Color(0, 102, 204), getWidth(), getHeight(), new Color(0, 204, 255));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw frog and notepad
                drawFrogAndNotepad(g2d);

                // Text
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();

                String text1 = "Just write and CTRL+S!";
                String text2 = ".LOG-hog v.0.0.1";

                // Draw outlines
                g2d.setColor(Color.BLACK);
                int x1 = (getWidth() - fm.stringWidth(text1)) / 2;
                int y1 = getHeight() - 80;
                g2d.drawString(text1, x1 - 1, y1 - 1);
                g2d.drawString(text1, x1 + 1, y1 - 1);
                g2d.drawString(text1, x1 - 1, y1 + 1);
                g2d.drawString(text1, x1 + 1, y1 + 1);

                int x2 = (getWidth() - fm.stringWidth(text2)) / 2;
                int y2 = getHeight() - 50;
                g2d.drawString(text2, x2 - 1, y2 - 1);
                g2d.drawString(text2, x2 + 1, y2 - 1);
                g2d.drawString(text2, x2 - 1, y2 + 1);
                g2d.drawString(text2, x2 + 1, y2 + 1);

                // Draw text
                g2d.setColor(Color.WHITE);
                g2d.drawString(text1, x1, y1);
                g2d.drawString(text2, x2, y2);
            }
        };
        panel.setPreferredSize(new Dimension(450, 300)); // increased width and height
        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Start animation
        animationTimer = new Timer(300, e -> {
            animationFrame = (animationFrame + 1) % 15; // 15 frames for writing lines
            panel.repaint();
        });
        animationTimer.start();
    }

    private void drawFrogAndNotepad(Graphics2D g2d) {
        int manX = 50;
        int manY = 50;
        int notepadX = 150;
        int notepadY = 80;

        // Draw man
        g2d.setStroke(new BasicStroke(2));
        Color skin = new Color(255, 218, 185);
        Color shirt = new Color(200, 200, 200); // light gray
        Color pants = Color.BLUE;

        // Head
        g2d.setColor(skin);
        g2d.fillOval(manX + 15, manY, 30, 30);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 15, manY, 30, 30);

        // Hair - shorter brown
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillOval(manX + 15, manY - 5, 30, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 15, manY - 5, 30, 10);

        // Cap - remove or change
        // Keep or remove cap

        // Beard - smaller
        g2d.setColor(Color.WHITE);
        g2d.fillOval(manX + 20, manY + 25, 20, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 20, manY + 25, 20, 10);

        // Eyes
        g2d.setColor(Color.WHITE);
        g2d.fillOval(manX + 20, manY + 8, 8, 8);
        g2d.fillOval(manX + 32, manY + 8, 8, 8);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 20, manY + 8, 8, 8);
        g2d.drawOval(manX + 32, manY + 8, 8, 8);
        g2d.fillOval(manX + 23, manY + 11, 3, 3);
        g2d.fillOval(manX + 35, manY + 11, 3, 3);

        // Glasses - thicker
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(manX + 18, manY + 6, 12, 12);
        g2d.drawOval(manX + 30, manY + 6, 12, 12);
        g2d.drawLine(manX + 30, manY + 12, manX + 30, manY + 12); // bridge

        // Mouth
        g2d.drawArc(manX + 25, manY + 18, 10, 5, 0, -180);

        // Bowtie
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.RED);
        g2d.fillOval(manX + 25, manY + 35, 10, 10);
        g2d.fillOval(manX + 20, manY + 40, 5, 5);
        g2d.fillOval(manX + 35, manY + 40, 5, 5);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 25, manY + 35, 10, 10);

        // Arms - rectangles with hands
        g2d.setColor(skin);
        g2d.fillRect(manX, manY + 35, 8, 25); // left arm
        g2d.fillRect(manX + 52, manY + 35, 8, 25); // right arm
        g2d.setColor(Color.BLACK);
        g2d.drawRect(manX, manY + 35, 8, 25);
        g2d.drawRect(manX + 52, manY + 35, 8, 25);
        // Hands
        g2d.setColor(skin);
        g2d.fillOval(manX - 2, manY + 55, 12, 10); // left hand
        g2d.fillOval(manX + 50, manY + 55, 12, 10); // right hand
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX - 2, manY + 55, 12, 10);
        g2d.drawOval(manX + 50, manY + 55, 12, 10);

        // Legs
        g2d.setColor(pants);
        g2d.fillRect(manX + 15, manY + 70, 10, 20);
        g2d.fillRect(manX + 35, manY + 70, 10, 20);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(manX + 15, manY + 70, 10, 20);
        g2d.drawRect(manX + 35, manY + 70, 10, 20);

        // Draw speech bubble
        int bubbleX = 10;
        int bubbleY = 10;
        int bubbleW = 160;
        int bubbleH = 40;
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(bubbleX, bubbleY, bubbleW, bubbleH, 20, 20);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(bubbleX, bubbleY, bubbleW, bubbleH, 20, 20);
        // Pointer
        int[] xPoints = {bubbleX + 20, bubbleX + 30, bubbleX + 25};
        int[] yPoints = {bubbleY + bubbleH, bubbleY + bubbleH + 10, bubbleY + bubbleH};
        g2d.fillPolygon(xPoints, yPoints, 3);
        g2d.drawPolygon(xPoints, yPoints, 3);
        // Text
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("I am a .LOG-hog!", bubbleX + 10, bubbleY + 25);

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

        // Fake log entries
        String[] entries = {
            "2025-11-20 14:30: Started coding",
            "2025-11-20 14:35: Fixed infinite loop",
            "2025-11-20 14:40: Added cool feature",
            "2025-11-20 14:45: Tested the app",
            "2025-11-20 14:50: Committed to git"
        };

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 8));
        g2d.setColor(Color.BLACK);
        for (int i = 0; i < animationFrame && i < entries.length; i++) {
            g2d.drawString(entries[i], notepadX + 10, notepadY + 20 + i * 15);
        }

        // Draw pen
        if (animationFrame > 0 && animationFrame <= entries.length) {
            String lastEntry = entries[animationFrame - 1];
            int penX = notepadX + 10 + g2d.getFontMetrics().stringWidth(lastEntry);
            int penY = notepadY + 20 + (animationFrame - 1) * 15;
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(penX, penY, penX + 8, penY - 4); // pen
        }
    }
}