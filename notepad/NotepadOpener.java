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
