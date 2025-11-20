import java.awt.*;
import java.util.*;
import javax.swing.*;

public class SplashScreen extends JDialog {
    private int animationFrame = 0;
    private javax.swing.Timer animationTimer;
    private JButton okButton;

    public SplashScreen() {
        super((Frame) null, "Splash", true); // modal dialog
        setUndecorated(true);
        setSize(450, 300);
        setLocationRelativeTo(null);

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
            }
        };
        panel.setLayout(null); // for absolute positioning

        okButton = new JButton("OK");
        okButton.setBounds(200, 250, 50, 30);
        okButton.addActionListener(e -> {
            animationTimer.stop();
            setVisible(false);
            dispose();
        });
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

    private void drawFrogAndNotepad(Graphics2D g2d) {
        int manX = 50;
        int manY = 50;
        int notepadX = 150;
        int notepadY = 80;

        // Draw man
        g2d.setStroke(new BasicStroke(2));
        Color skin = new Color(255, 218, 185);
        Color shirt = Color.RED; // red shirt
        Color pants = Color.BLUE;

        // Head
        g2d.setColor(skin);
        g2d.fillOval(manX + 15, manY, 30, 30);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 15, manY, 30, 30);

        // Hair - fuller brown
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillOval(manX + 15, manY - 10, 30, 15);
        g2d.fillOval(manX + 10, manY, 10, 10); // left side
        g2d.fillOval(manX + 40, manY, 10, 10); // right side
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 15, manY - 10, 30, 15);
        g2d.drawOval(manX + 10, manY, 10, 10);
        g2d.drawOval(manX + 40, manY, 10, 10);

        // Cap - remove or change
        // Keep or remove cap

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

        // Mouth
        g2d.drawArc(manX + 25, manY + 18, 10, 5, 0, -120);

        // Mustache
        GradientPaint mustacheGradient = new GradientPaint(manX + 22, manY + 15, new Color(139, 69, 19), manX + 22, manY + 18, new Color(101, 67, 33));
        g2d.setPaint(mustacheGradient);
        g2d.drawLine(manX + 22, manY + 17, manX + 28, manY + 16);
        g2d.drawLine(manX + 32, manY + 17, manX + 38, manY + 16);

        // Bowtie
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.RED);
        g2d.fillOval(manX + 25, manY + 35, 10, 10);
        g2d.fillOval(manX + 20, manY + 40, 5, 5);
        g2d.fillOval(manX + 35, manY + 40, 5, 5);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(manX + 25, manY + 35, 10, 10);

        // Body - gradient white shirt
        GradientPaint shirtGradient = new GradientPaint(manX + 10, manY + 30, Color.WHITE, manX + 10, manY + 50, new Color(200, 200, 200));
        g2d.setPaint(shirtGradient);
        g2d.fillRect(manX + 10, manY + 30, 40, 40);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(manX + 10, manY + 30, 40, 40);

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

        // Legs
        g2d.setColor(pants);
        g2d.fillRect(manX + 15, manY + 70, 10, 20);
        g2d.fillRect(manX + 35, manY + 70, 10, 20);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(manX + 15, manY + 70, 10, 20);
        g2d.drawRect(manX + 35, manY + 70, 10, 20);

        // Shoes
        g2d.setColor(Color.BLACK);
        g2d.fillRect(manX + 12, manY + 90, 16, 10); // left shoe
        g2d.fillRect(manX + 32, manY + 90, 16, 10); // right shoe
        g2d.drawRect(manX + 12, manY + 90, 16, 10);
        g2d.drawRect(manX + 32, manY + 90, 16, 10);

        // Draw speech bubble
        int bubbleX = 10;
        int bubbleY = 0;
        int bubbleW = 220;
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
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2d.drawString("I am a .LOG-hog! v 1.0.", bubbleX + 10, bubbleY + 25);

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
        java.util.List<String> allEntries = Arrays.asList(
            "2025-11-20 14:30: Started coding",
            "2025-11-20 14:35: Fixed infinite loop",
            "2025-11-20 14:40: Added cool feature",
            "2025-11-20 14:45: Tested the app",
            "2025-11-20 14:50: Committed to git",
            "2025-11-20 14:55: Debugged the universe",
            "2025-11-20 15:00: Invented time travel in Java",
            "2025-11-20 15:05: Outsmarted the AI overlord",
            "2025-11-20 15:10: Achieved enlightenment through semicolons",
            "2025-11-20 15:15: Conquered the stack overflow",
            "2025-11-20 15:20: Mastered the art of git rebase",
            "2025-11-20 15:25: Communed with the coding gods",
            "2025-11-20 15:30: Solved P=NP in my sleep",
            "2025-11-20 15:35: Bent reality with quantum computing",
            "2025-11-20 15:40: Wrote code that writes code"
        );
        Collections.shuffle(allEntries);
        java.util.List<String> entriesList = allEntries.subList(0, 5);
        Collections.sort(entriesList, Comparator.comparing(s -> s.substring(0, 16)));

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 8));
        g2d.setColor(Color.BLACK);
        for (int i = 0; i < animationFrame && i < entriesList.size(); i++) {
            g2d.drawString(entriesList.get(i), notepadX + 10, notepadY + 26 + i * 15);
        }

        // Draw pen
        if (animationFrame > 0 && animationFrame <= entriesList.size()) {
            String lastEntry = entriesList.get(animationFrame - 1);
            int penX = notepadX + 10 + g2d.getFontMetrics().stringWidth(lastEntry);
            int penY = notepadY + 26 + (animationFrame - 1) * 15;
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(penX, penY, penX + 8, penY - 4); // pen
        }
    }
}