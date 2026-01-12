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
 * Handles clicking on timestamps to edit entries.
 * Provides visual feedback (cursor change and tooltip) when hovering over timestamps.
 */
public class TimestampClickHandler {
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$");
    
    private final JTextPane textPane;
    private final TimestampClickListener clickListener;
    
    private boolean isHoveringTimestamp = false;
    
    /**
     * Callback interface for timestamp clicks.
     */
    public interface TimestampClickListener {
        void onTimestampClick(String timestamp);
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
        // Global event listener for click detection
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(createClickListener(), 
            AWTEvent.MOUSE_EVENT_MASK);
        
        // Motion listener for visual feedback
        textPane.addMouseMotionListener(createHoverListener());
    }
    
    /**
     * Creates the AWTEventListener for detecting clicks.
     */
    private AWTEventListener createClickListener() {
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
            
            java.awt.Point currentPoint = e.getPoint();
            
            // Convert point to textPane coordinates if needed
            if (e.getComponent() != textPane) {
                currentPoint = SwingUtilities.convertPoint(e.getComponent(), currentPoint, textPane);
            }
            
            // Get position in document
            int pos = textPane.viewToModel2D(currentPoint);
            if (pos < 0) {
                return;
            }
            
            String lineText = getLineAtPosition(pos);
            if (lineText != null && isHoveringOverTimestamp(pos, lineText)) {
                clickListener.onTimestampClick(lineText);
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
                        if (isHoveringTimestamp) {
                            isHoveringTimestamp = false;
                            resetCursor();
                        }
                        return;
                    }
                    
                    String lineText = getLineAtPosition(pos);
                    
                    // Check if hovering over a timestamp in a timestamp line
                    boolean shouldHover = lineText != null && isHoveringOverTimestamp(pos, lineText);
                    if (shouldHover != isHoveringTimestamp) {
                        isHoveringTimestamp = shouldHover;
                        if (shouldHover) {
                            textPane.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                            textPane.setToolTipText(UIStrings.TOOLTIP_CLICK_EDIT);
                        } else {
                            resetCursor();
                        }
                    }
                } catch (Exception ex) {
                    if (isHoveringTimestamp) {
                        isHoveringTimestamp = false;
                        resetCursor();
                    }
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
     * Checks if hovering over the timestamp part of a timestamp line.
     */
    private boolean isHoveringOverTimestamp(int pos, String lineText) {
        if (!isTimestampLine(lineText)) {
            return false;
        }
        
        // Find the position within the line
        try {
            javax.swing.text.StyledDocument doc = textPane.getStyledDocument();
            String text = doc.getText(0, doc.getLength());
            
            int lineStart = pos;
            while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
                lineStart--;
            }
            
            int posInLine = pos - lineStart;
            
            // Find the length of the timestamp
            java.util.regex.Matcher matcher = TIMESTAMP_PATTERN.matcher(lineText);
            if (matcher.find()) {
                int timestampEnd = matcher.end();
                return posInLine < timestampEnd;
            }
        } catch (Exception e) {
            // ignore
        }
        
        return false;
    }
    
    /**
     * Resets the cursor and tooltip to defaults.
     */
    private void resetCursor() {
        textPane.setCursor(java.awt.Cursor.getDefaultCursor());
        textPane.setToolTipText(null);
    }
}
