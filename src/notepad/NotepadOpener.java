/*
 * Copyright (C) 2026 Johan Andersson
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

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.nio.file.Files;
import java.nio.file.Path;

// Unused import removed for PMD compliance

import gui.DialogHelper;

public class NotepadOpener {
    public static void openLogInNotepad() {
        Path logPath = findLogPath();
        if (logPath == null) {
            DialogHelper.showFileNotFound(null);
            return;
        }

        // Validate path for security
        if (!isValidLogPath(logPath)) {
            gui.DialogHelper.showError(null, "Security Error", "Invalid log file path detected.");
            return;
        }

        // Use Desktop API only to avoid spawning external shell commands.
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.EDIT)) {
            try {
                Desktop.getDesktop().edit(logPath.toFile());
                return;
            } catch (Exception e) {
                // Try open as a fallback if editor integration is unavailable.
            }
        }

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN)) {
            try {
                Desktop.getDesktop().open(logPath.toFile());
                return;
            } catch (Exception ignored) {
            }
        }

        gui.DialogHelper.showError(null, "Error", "Unable to Open Editor",
            "Could not open the log file in an editor on this system.<br>Please open it manually:<br><code>" + logPath.toAbsolutePath() + "</code>");
    }

    private static Path findLogPath() {
        String userHome = System.getProperty("user.home");
        Path homePath = Path.of(userHome, "log.txt");
        Path cwdPath = Path.of(System.getProperty("user.dir"), "log.txt");
        if (Files.exists(homePath)) return homePath;
        if (Files.exists(cwdPath)) return cwdPath;
        return null;
    }

    private static boolean isValidLogPath(Path path) {
        try {
            // Convert to absolute path and normalize
            Path absolutePath = path.toAbsolutePath().normalize();

            // Check that the path ends with log.txt
            if (!"log.txt".equals(absolutePath.getFileName().toString())) {
                return false;
            }

            // Check that the path doesn't contain suspicious characters or command injection attempts
            String pathString = absolutePath.toString();
            // Note: backslash and forward slash are normal path separators, not forbidden
            String[] forbiddenChars = {"&", "|", ";", "`", "<", ">", "*", "?", "[", "]", "{", "}",
                                     "\"", "'", "\n", "\r", "\t", "\0"};
            for (String forbidden : forbiddenChars) {
                if (pathString.contains(forbidden)) {
                    return false;
                }
            }
            
            // Check for ".." in the normalized path (path traversal attempt)
            if (pathString.contains("..")) {
                return false;
            }

            // Check for command injection patterns
            if (pathString.matches(".*\\$\\(.*\\).*") || // $(command)
                pathString.matches(".*`.*`.*") ||       // `command`
                pathString.matches(".*\\$\\{.*\\}.*")) { // ${variable}
                return false;
            }

            // Ensure the path is within user home or current working directory
            String userHome = System.getProperty("user.home");
            String cwd = System.getProperty("user.dir");
            if (userHome == null || cwd == null) {
                return false; // Can't validate without these properties
            }

            Path userHomePath = Path.of(userHome).toAbsolutePath().normalize();
            Path cwdPath = Path.of(cwd).toAbsolutePath().normalize();

            // Additional check: ensure path doesn't escape the allowed directories
            if (!absolutePath.startsWith(userHomePath) && !absolutePath.startsWith(cwdPath)) {
                return false;
            }

            // Final check: ensure the file actually exists and is readable
            return Files.exists(absolutePath) && Files.isReadable(absolutePath);

        } catch (Exception e) {
            return false;
        }
    }
}
