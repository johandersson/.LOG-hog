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

        // Create a minimal 1x1 transparent image
        Image image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        // Create popup menu
        PopupMenu popup = new PopupMenu();

        // Create menu items
        MenuItem openItem = new MenuItem("Open");
        MenuItem exitItem = new MenuItem("Exit");

        // Add items to popup menu
        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);

        // Create tray icon with tooltip
        TrayIcon trayIcon = new TrayIcon(image, "My Tray App", popup);
        trayIcon.setImageAutoSize(true);

        // Add click listener to tray icon
        trayIcon.addActionListener(e -> {
            System.out.println("Tray icon clicked!");
        });

        // Add action listeners to menu items
        openItem.addActionListener(e -> {
            System.out.println("Open selected");
            // You can show a window or perform another action here
        });

        exitItem.addActionListener(e -> {
            System.out.println("Exiting...");
            System.exit(0);
        });

        // Add tray icon to system tray
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }

        SwingUtilities.invokeLater(() -> LogTextEditor.main(args));
    }
}
