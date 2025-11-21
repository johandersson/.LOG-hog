package notepad;

import java.nio.file.*;
import javax.swing.*;

public class NotepadOpener {
    public static void openLogInNotepad() {
        Path logPath = findLogPath();
        if (logPath == null) {
            JOptionPane.showMessageDialog(null, "log.txt not found in user home or current working directory.", "File Not Found", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            new ProcessBuilder("notepad.exe", logPath.toAbsolutePath().toString()).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error opening log in Notepad: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            // Try vim on Linux/Mac
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux") || os.contains("mac")) {
                    new ProcessBuilder("vim", logPath.toAbsolutePath().toString()).start();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error opening log in vim: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static Path findLogPath() {
        String userHome = System.getProperty("user.home");
        Path homePath = Path.of(userHome, "log.txt");
        Path cwdPath = Path.of(System.getProperty("user.dir"), "log.txt");
        if (Files.exists(homePath)) return homePath;
        if (Files.exists(cwdPath)) return cwdPath;
        return null;
    }
}