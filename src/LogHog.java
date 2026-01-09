/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import main.LogTextEditor;

public class LogHog {
    public static void main(String[] args) {
    // Set up native look and feel for all platforms
    try {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            // macOS-specific settings
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", "LogHog");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } else if (os.contains("linux")) {
            // Try GTK+ on Linux for native look
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            } catch (Exception e) {
                // Fall back to system default
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } else {
            // Windows or other - use Windows L&F if available
            for (var info : UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
    } catch (Exception ignored) {
    }

    // Let the OS draw the title bar and buttons (native chrome)
    JFrame.setDefaultLookAndFeelDecorated(false);

    SwingUtilities.invokeLater(() -> LogTextEditor.main(args));
    }
}
