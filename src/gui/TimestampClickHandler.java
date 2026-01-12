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

package gui;

import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

/**
 * Handles double-clicking on timestamps to edit entries.
 * Provides visual feedback (cursor change and tooltip) when hovering over timestamps.
 */
public class TimestampClickHandler {
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$");
    private static final int DOUBLE_CLICK_THRESHOLD_MS = 500;
    private static final int DOUBLE_CLICK_DISTANCE_PX = 5;
    
    private final JTextPane textPane;
    private final TimestampClickListener clickListener;
    
    private long lastClickTime = 0;
    private java.awt.Point lastClickPoint = null;
    
    /**
     * Callback interface for timestamp clicks.
     */
    public interface TimestampClickListener {
        void onTimestampDoubleClick(String timestamp);
    }
    
    /**
     * Creates a handler for timestamp double-clicks on the given text pane.
     * 
     * @param textPane The text pane to monitor
     * @param clickListener Callback for when a timestamp is double-clicked
     */
    public TimestampClickHandler(JTextPane textPane, TimestampClickListener clickListener) {
        this.textPane = textPane;
        this.clickListener = clickListener;
        attachHandlers();
    }
    
    /**
     * Attaches mouse event handlers to the text pane.
     */
    private void attachHandlers() {
        // Global event listener for double-click detection
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(createDoubleClickListener(), 
            AWTEvent.MOUSE_EVENT_MASK);
        
        // Motion listener for visual feedback
        textPane.addMouseMotionListener(createHoverListener());
    }
    
    /**
     * Creates the AWTEventListener for detecting double-clicks.
     */
    private AWTEventListener createDoubleClickListener() {
        return event -> {
            if (!(event instanceof MouseEvent)) {
                return;
            }
            
            MouseEvent e = (MouseEvent) event;
            
            // Only process events on our text pane
            if (e.getComponent() != textPane && e.getComponent().getParent() != textPane) {
                return;
            }
            
            // Only handle mouse pressed events
            if (e.getID() != MouseEvent.MOUSE_PRESSED) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            java.awt.Point currentPoint = e.getPoint();
            
            // Convert point to textPane coordinates if needed
            if (e.getComponent() != textPane) {
                currentPoint = SwingUtilities.convertPoint(e.getComponent(), currentPoint, textPane);
            }
            
            boolean isDoubleClick = false;
            
            // Manual double-click detection
            if (lastClickTime > 0 && (currentTime - lastClickTime) < DOUBLE_CLICK_THRESHOLD_MS && lastClickPoint != null) {
                double distance = lastClickPoint.distance(currentPoint);
                if (distance < DOUBLE_CLICK_DISTANCE_PX) {
                    isDoubleClick = true;
                }
            }
            
            lastClickTime = currentTime;
            lastClickPoint = currentPoint;
            
            if (!isDoubleClick) {
                return;
            }
            
            // Get position in document
            int pos = textPane.viewToModel2D(currentPoint);
            if (pos < 0) {
                return;
            }
            
            String lineText = getLineAtPosition(pos);
            if (lineText != null && isTimestampLine(lineText)) {
                clickListener.onTimestampDoubleClick(lineText);
            }
        };
    }
    
    /**
     * Creates the MouseMotionListener for hover feedback.
     */
    private java.awt.event.MouseMotionAdapter createHoverListener() {
        return new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                try {
                    int pos = textPane.viewToModel2D(e.getPoint());
                    if (pos < 0) {
                        resetCursor();
                        return;
                    }
                    
                    String lineText = getLineAtPosition(pos);
                    
                    // Check if hovering over a timestamp line
                    if (lineText != null && isTimestampLine(lineText)) {
                        textPane.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                        textPane.setToolTipText("Double-click to edit this entry");
                    } else {
                        resetCursor();
                    }
                } catch (Exception ex) {
                    resetCursor();
                }
            }
        };
    }
    
    /**
     * Gets the text of the line at the given position.
     */
    private String getLineAtPosition(int pos) {
        try {
            javax.swing.text.StyledDocument doc = textPane.getStyledDocument();
            String text = doc.getText(0, doc.getLength());
            
            // Find line boundaries
            int lineStart = pos;
            while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
                lineStart--;
            }
            
            int lineEnd = pos;
            while (lineEnd < text.length() && text.charAt(lineEnd) != '\n') {
                lineEnd++;
            }
            
            return text.substring(lineStart, lineEnd).trim();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Checks if the given line is a timestamp line.
     */
    private boolean isTimestampLine(String line) {
        return TIMESTAMP_PATTERN.matcher(line).matches();
    }
    
    /**
     * Resets the cursor and tooltip to defaults.
     */
    private void resetCursor() {
        textPane.setCursor(java.awt.Cursor.getDefaultCursor());
        textPane.setToolTipText(null);
    }
}
