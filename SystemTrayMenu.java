import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class SystemTrayMenu {
    private static SystemTrayMenu instance = null;
    public static LogTextEditor logTextEditor;
    SystemTrayMenu(LogTextEditor logTextEditor) {
        this.logTextEditor = logTextEditor;
    }
    public static void initSystemTray(){
        try {
            if (!SystemTray.isSupported()) {
                System.out.println("System tray not supported!");
                return;
            }

            //add standard java icon as tray icon
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setColor(Color.BLUE);
            g2.fillRect(0, 0, 16, 16);
            g2.setColor(Color.WHITE);
            g2.drawString("L", 4, 12);
            g2.dispose();

            // Create popup menu
            PopupMenu popup = new PopupMenu();

            // Create menu items
            MenuItem addNoteItem = new MenuItem("Add Quick Note");
            MenuItem openItem = new MenuItem("Open");
            MenuItem exitItem = new MenuItem("Exit");
            //Recent logs item with submenu with the 5 most recent logs
            Menu recentLogsMenu = new Menu("Recent Logs");


            // Add items to popup menu
            popup.add(addNoteItem);
            popup.addSeparator();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            popup.addSeparator();
            popup.add(recentLogsMenu);

            // Create tray icon with tooltip
            TrayIcon trayIcon = new TrayIcon(image, "LogHog", popup);
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
                logTextEditor.setVisible(true);
                logTextEditor.setExtendedState(JFrame.NORMAL);
                logTextEditor.toFront();
            });

            //init recent logs menu
            logTextEditor.updateRecentLogsMenu(recentLogsMenu);


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
    public static synchronized SystemTrayMenu getInstance() {
        if (instance == null) {
            instance = new SystemTrayMenu(logTextEditor);
        }
        return instance;
    }
}