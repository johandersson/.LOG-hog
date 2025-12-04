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

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.*;

// Window dragger for the undecorated frame
public class WindowDragger {
    static void makeDraggable(JFrame frame, JComponent comp) {
        final Point[] start = {null};
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { start[0] = e.getPoint(); }
            public void mouseReleased(MouseEvent e) { start[0] = null; }
        });
        comp.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (start[0] == null) return;
                Point loc = frame.getLocationOnScreen();
                frame.setLocation(loc.x + e.getX() - start[0].x, loc.y + e.getY() - start[0].y);
            }
        });
    }
}
