import java.awt.*;
import java.awt.image.BufferedImage;

class SystemTrayMenu {
    public static void createTrayMenu(LogTextEditor logTextEditor) {
        try {
            if (!SystemTray.isSupported()) {
                System.out.println("System tray not supported!");
                return;
            }

            // Create a minimal 1x1 transparent image
            Image image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

            // Create popup menu
            PopupMenu popup = new PopupMenu();

            // Create menu items
            MenuItem addNoteItem = new MenuItem("Add Quick Note");
            MenuItem openItem = new MenuItem("Open");
            MenuItem exitItem = new MenuItem("Exit");

            // Add items to popup menu
            popup.add(addNoteItem);
            popup.addSeparator();
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
            addNoteItem.addActionListener(e -> {
                logTextEditor.quickEntry();
            });

            openItem.addActionListener(e -> {
                System.out.println("Open selected");
                // You can show a window or perform another action here
            });

            exitItem.addActionListener(e -> {
                System.out.println("Exiting...");
                System.exit(0);
            });

            // Add tray icon to system tray
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(trayIcon);

        } catch (AWTException e) {
            System.out.println("Failed to add tray icon.");
            e.printStackTrace();

        }
    }
}