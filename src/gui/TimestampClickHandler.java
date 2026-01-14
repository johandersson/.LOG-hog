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

import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

import javax.swing.JTextPane;
import javax.swing.JWindow;

/**
 * Handles clicking on timestamps to edit entries.
 * Provides visual feedback (cursor change and tooltip) when hovering over timestamps.
 */
public class TimestampClickHandler {
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$");
    
    private final JTextPane textPane;
    private final TimestampClickListener clickListener;
    
    private boolean isHoveringTimestamp = false;
    private JWindow editWindow;
    private AccentButton editButton;
    private String currentTimestamp;
    private javax.swing.Timer hideTimer;
    
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
        createEditWindow();
    }
    
    /**
     * Attaches mouse event handlers to the text pane.
     */
    private void attachHandlers() {
        // Motion listener for visual feedback
        textPane.addMouseMotionListener(createHoverListener());
    }
    
    private void createEditWindow() {
        editWindow = new JWindow();
        this.editButton = new AccentButton("Edit");
        editButton.setPreferredSize(new java.awt.Dimension(160, 30)); // Wider for timestamp text
        editButton.setFont(editButton.getFont().deriveFont(11f)); // Smaller font
        editButton.addActionListener(e -> {
            if (currentTimestamp != null) {
                clickListener.onTimestampClick(currentTimestamp);
                hideEditWindow();
            }
        });
        editWindow.add(editButton);
        editWindow.pack();
        editWindow.setAlwaysOnTop(true);
        
        // Timer to hide the window after 3 seconds of no hover
        hideTimer = new javax.swing.Timer(3000, e -> hideEditWindow());
        hideTimer.setRepeats(false);
    }
    
    private void hideEditWindow() {
        editWindow.setVisible(false);
        hideTimer.stop();
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
                            hideTimer.restart();
                        }
                        return;
                    }
                    
                    String lineText = getLineAtPosition(pos);
                    
                    // Check if hovering over a timestamp in a timestamp line
                    boolean shouldHover = lineText != null && isHoveringOverTimestamp(pos, lineText);
                    if (shouldHover != isHoveringTimestamp) {
                        isHoveringTimestamp = shouldHover;
                        if (shouldHover) {
                            currentTimestamp = lineText;
                            editButton.setText("Edit " + lineText);
                            // Position the edit window to the right of the timestamp
                            try {
                                javax.swing.text.StyledDocument doc = textPane.getStyledDocument();
                                String text = doc.getText(0, doc.getLength());
                                int lineStart = pos;
                                while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
                                    lineStart--;
                                }
                                java.util.regex.Matcher matcher = TIMESTAMP_PATTERN.matcher(lineText);
                                int timestampEndPos = lineStart;
                                if (matcher.find()) {
                                    timestampEndPos = lineStart + matcher.end();
                                }
                                java.awt.geom.Rectangle2D rect = textPane.modelToView2D(timestampEndPos);
                                java.awt.Point screenPoint = textPane.getLocationOnScreen();
                                screenPoint.translate((int)(rect.getX() + 5), (int)(rect.getY() - 10));
                                editWindow.setLocation(screenPoint);
                                editWindow.setVisible(true);
                                hideTimer.restart();
                            } catch (Exception ex) {
                                // Fallback to mouse position
                                java.awt.Point screenPoint = textPane.getLocationOnScreen();
                                screenPoint.translate(e.getX() + 10, e.getY() - 10);
                                editWindow.setLocation(screenPoint);
                                editWindow.setVisible(true);
                                hideTimer.restart();
                            }
                        } else {
                            hideTimer.restart();
                        }
                    }
                } catch (Exception ex) {
                    if (isHoveringTimestamp) {
                        isHoveringTimestamp = false;
                        hideTimer.restart();
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
     * Disposes the edit window if it exists.
     */
    public void dispose() {
        if (editWindow != null) {
            editWindow.dispose();
            editWindow = null;
        }
        if (hideTimer != null) {
            hideTimer.stop();
            hideTimer = null;
        }
    }
    
    /**
     * Adds scroll listeners if the text pane is in a scroll pane.
     */
    public void addScrollListeners() {
        if (textPane.getParent() instanceof javax.swing.JScrollPane scrollPane) {
            scrollPane.getViewport().addChangeListener(e -> hideEditWindow());
        }
    }
}
