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