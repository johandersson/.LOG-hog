import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class LogHog {
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Let the OS draw the title bar and buttons (native chrome)
        JFrame.setDefaultLookAndFeelDecorated(false);

        // Check if system tray is supported
        if (!SystemTray.isSupported()) {
            System.out.println("System tray not supported!");
            return;
        }
        SwingUtilities.invokeLater(() -> LogTextEditor.main(args));
    }

}
