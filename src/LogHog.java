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
