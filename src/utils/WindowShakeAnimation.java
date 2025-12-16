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

package utils;

import java.awt.Window;
import javax.swing.SwingUtilities;

/**
 * Utility class for creating window shake animations, similar to macOS password error effects.
 */
public class WindowShakeAnimation {

    /**
     * Performs a shake animation on the specified window.
     * The window will shake horizontally for a short duration.
     *
     * @param window the window to shake
     */
    public static void shake(Window window) {
        SwingUtilities.invokeLater(() -> {
            int originalX = window.getX();
            int shakeDistance = 10;
            int shakeCount = 6;
            int shakeDelay = 50;

            for (int i = 0; i < shakeCount; i++) {
                int direction = (i % 2 == 0) ? 1 : -1;
                int newX = originalX + (direction * shakeDistance);
                window.setLocation(newX, window.getY());

                try {
                    Thread.sleep(shakeDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Return to original position
            window.setLocation(originalX, window.getY());
        });
    }
}