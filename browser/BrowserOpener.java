package browser;

import java.awt.*;
import java.net.URI;

public class BrowserOpener {
    public static void openInBrowser(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            // Handle exception, perhaps show a message
            System.err.println("Error opening URL in browser: " + e.getMessage());
        }
    }
}