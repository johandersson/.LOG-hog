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
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                FontMetrics fm = g2d.getFontMetrics();

                String text1 = "Just write and CTRL+S!";
                String text2 = ".LOG-hog v.0.0.1";

                int x1 = (getWidth() - fm.stringWidth(text1)) / 2;
                int y1 = getHeight() / 2 + 50;
                g2d.drawString(text1, x1, y1);

                int x2 = (getWidth() - fm.stringWidth(text2)) / 2;
                int y2 = getHeight() / 2 + fm.getHeight() + 60;
                g2d.drawString(text2, x2, y2);
            }
        };
        panel.setPreferredSize(new Dimension(400, 250)); // increased height for animation
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
        int frogX = 50;
        int frogY = 50;
        int notepadX = 200;
        int notepadY = 30;

        // Draw frog
        g2d.setColor(Color.GREEN);
        g2d.fillOval(frogX, frogY, 60, 50); // body
        g2d.setColor(new Color(0, 100, 0));
        g2d.fillOval(frogX + 10, frogY + 10, 10, 10); // left eye
        g2d.fillOval(frogX + 40, frogY + 10, 10, 10); // right eye
        g2d.setColor(Color.BLACK);
        g2d.fillOval(frogX + 12, frogY + 12, 6, 6); // pupils
        g2d.fillOval(frogX + 42, frogY + 12, 6, 6);
        g2d.setColor(Color.GREEN);
        g2d.fillOval(frogX + 20, frogY + 30, 20, 15); // mouth area
        g2d.setColor(Color.BLACK);
        g2d.drawArc(frogX + 25, frogY + 35, 10, 5, 0, 180); // smile
        // legs
        g2d.setColor(Color.GREEN);
        g2d.fillOval(frogX + 5, frogY + 45, 15, 10);
        g2d.fillOval(frogX + 40, frogY + 45, 15, 10);

        // Draw notepad
        g2d.setColor(Color.WHITE);
        g2d.fillRect(notepadX, notepadY, 120, 80);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(notepadX, notepadY, 120, 80);
        // lines on notepad
        for (int i = 1; i <= 5; i++) {
            int y = notepadY + 10 + i * 10;
            g2d.drawLine(notepadX + 10, y, notepadX + 110, y);
        }

        // Animate writing: draw lines based on animationFrame
        g2d.setColor(Color.BLUE);
        for (int i = 0; i < animationFrame && i < 5; i++) {
            int y = notepadY + 10 + (i + 1) * 10;
            g2d.drawLine(notepadX + 10, y, notepadX + 10 + (animationFrame - i) * 20, y); // growing line
        }

        // Draw pen
        if (animationFrame < 5) {
            int penX = notepadX + 10 + animationFrame * 20;
            int penY = notepadY + 10 + (animationFrame + 1) * 10;
            g2d.setColor(Color.BLACK);
            g2d.drawLine(penX, penY, penX + 10, penY - 5); // simple pen
        }
    }
}