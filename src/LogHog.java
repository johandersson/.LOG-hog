import javax.swing.*;
import main.LogTextEditor;

public class LogHog {
    public static void main(String[] args) {
    try {
        for (var info : UIManager.getInstalledLookAndFeels()) {
            if ("Windows".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
    } catch (Exception ignored) {
    }

    // Let the OS draw the title bar and buttons (native chrome)
    JFrame.setDefaultLookAndFeelDecorated(false);

    // Load settings to check splash
    java.util.Properties settings = new java.util.Properties();
    java.nio.file.Path settingsPath = java.nio.file.Paths.get(System.getProperty("user.home"), "loghog_settings.properties");
    if (java.nio.file.Files.exists(settingsPath)) {
        try (var fis = new java.io.FileInputStream(settingsPath.toFile())) {
            settings.load(fis);
        } catch (Exception ignored) {
        }
    }

    // Show splash screen if enabled
    if ("true".equals(settings.getProperty("showSplashOnStartup", "true"))) {
        new gui.SplashScreen();
    }

    LogTextEditor.main(args);
    }
}
