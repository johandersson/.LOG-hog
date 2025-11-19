import java.awt.*;
import javax.swing.*;

public class SplashScreen extends JWindow {
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

                // Text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                FontMetrics fm = g2d.getFontMetrics();

                String text1 = "Just write and CTRL+S!";
                String text2 = ".LOG-hog v.0.0.1";

                int x1 = (getWidth() - fm.stringWidth(text1)) / 2;
                int y1 = getHeight() / 2 - fm.getHeight() / 2 - 10;
                g2d.drawString(text1, x1, y1);

                int x2 = (getWidth() - fm.stringWidth(text2)) / 2;
                int y2 = getHeight() / 2 + fm.getHeight() / 2 + 10;
                g2d.drawString(text2, x2, y2);
            }
        };
        panel.setPreferredSize(new Dimension(400, 200));
        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}