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

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.regex.Pattern;

import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
// PenIcon import will be added here

/**
 * Handles clicking on timestamps to edit entries.
 * Provides visual feedback (cursor change and tooltip) when hovering over timestamps.
 */
public class TimestampClickHandler {
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$");
    
    private final JTextPane textPane;
    private final TimestampClickListener clickListener;
    
    private boolean isHoveringTimestamp = false;
    private boolean buttonVisible = false;
    private JWindow overlayWindow;
    private StandardButton overlayButton;
    private PenIcon penIcon;
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
        createOverlayButton();
    }
    
    /**
     * Attaches mouse event handlers to the text pane.
     */
    private void attachHandlers() {
        // Motion listener for visual feedback
        textPane.addMouseMotionListener(createHoverListener());
    }
    
    private void createOverlayButton() {
        overlayWindow = new JWindow();
        overlayWindow.setAlwaysOnTop(true);
        overlayWindow.setFocusableWindowState(false);
        
        overlayButton = new StandardButton("", new Color(0xE0E0E0), new Color(0xB0B0B0));
        overlayButton.setOpaque(false);
        overlayButton.setContentAreaFilled(false);
        overlayButton.setBorderPainted(false);
        overlayButton.setFocusPainted(false);
        overlayButton.setForeground(Color.BLACK);
        overlayButton.setFont(textPane.getFont());
        overlayButton.setHorizontalAlignment(SwingConstants.LEFT);
        overlayButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        
        // Set up pen icon
        penIcon = new PenIcon(Color.BLACK);
        overlayButton.setIcon(penIcon);
        overlayButton.setIconTextGap(2); // Small gap between icon and text
        
        overlayButton.addActionListener(e -> {
            if (currentTimestamp != null) {
                clickListener.onTimestampClick(currentTimestamp);
                hideOverlayButton();
            }
        });
        
        // Hide when mouse exits the button area
        overlayButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                // Check if mouse is still within button bounds
                java.awt.Point mousePos = overlayButton.getMousePosition();
                if (mousePos == null || !overlayButton.contains(mousePos)) {
                    hideOverlayButton();
                }
            }
        });
        
        overlayWindow.add(overlayButton);
        
        // Timer to hide the overlay after 3 seconds
        hideTimer = new javax.swing.Timer(3000, e -> hideOverlayButton());
        hideTimer.setRepeats(false);
    }
    
    private void hideOverlayButton() {
        if (overlayWindow != null) {
            overlayWindow.setVisible(false);
        }
        buttonVisible = false;
        if (hideTimer != null) {
            hideTimer.stop();
        }
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
                            // Position the overlay button directly over the timestamp text
                            try {
                                javax.swing.text.StyledDocument doc = textPane.getStyledDocument();
                                String text = doc.getText(0, doc.getLength());
                                int lineStart = pos;
                                while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
                                    lineStart--;
                                }
                                java.util.regex.Matcher matcher = TIMESTAMP_PATTERN.matcher(lineText);
                                if (matcher.find()) {
                                    int timestampStart = lineStart + matcher.start();
                                    int timestampEnd = lineStart + matcher.end();
                                    
                                    // Get the bounds of the timestamp text
                                    java.awt.geom.Rectangle2D startRect = textPane.modelToView2D(timestampStart);
                                    java.awt.geom.Rectangle2D endRect = textPane.modelToView2D(timestampEnd);
                                    
                                    // Calculate the full bounds of the timestamp
                                    double x = startRect.getX();
                                    double y = startRect.getY();
                                    double width = endRect.getX() + endRect.getWidth() - startRect.getX();
                                    double height = Math.max(startRect.getHeight(), endRect.getHeight());
                                    
                                    // Position overlay window over the timestamp
                                    java.awt.Point screenPoint = textPane.getLocationOnScreen();
                                    screenPoint.translate((int)x, (int)y);
                                    
                                    overlayWindow.setLocation(screenPoint);
                                    overlayButton.setText(lineText.trim());
                                    
                                    // Use button's preferred size to ensure text and icon fit properly
                                    java.awt.Dimension prefSize = overlayButton.getPreferredSize();
                                    overlayButton.setBounds(0, 0, prefSize.width, (int)height);
                                    overlayWindow.setSize(prefSize.width, (int)height);
                                    overlayWindow.setVisible(true);
                                    buttonVisible = true;
                                    hideTimer.restart();
                                }
                            } catch (Exception ex) {
                                // Fallback positioning
                                java.awt.Point screenPoint = textPane.getLocationOnScreen();
                                screenPoint.translate(e.getX(), e.getY());
                                overlayWindow.setLocation(screenPoint);
                                overlayButton.setText(lineText.trim());
                                
                                // Use button's preferred size for proper text/icon fit
                                java.awt.Dimension prefSize = overlayButton.getPreferredSize();
                                overlayWindow.setSize(prefSize.width, prefSize.height);
                                overlayWindow.setVisible(true);
                                buttonVisible = true;
                                hideTimer.restart();
                            }
                        } else if (!buttonVisible) {
                            // Only hide immediately if button was never shown
                            hideOverlayButton();
                        }
                        // If buttonVisible is true, let the timer handle hiding
                    } else if (!shouldHover && isHoveringTimestamp) {
                        // Double-check: if we're marked as hovering but shouldn't be, hide immediately
                        hideOverlayButton();
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
     * Disposes the overlay window if it exists.
     */
    public void dispose() {
        if (overlayWindow != null) {
            overlayWindow.dispose();
            overlayWindow = null;
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
            scrollPane.getViewport().addChangeListener(e -> {
                // Hide immediately on scroll to prevent lingering
                hideOverlayButton();
            });
        }
        
        // Also hide on mouse wheel events
        textPane.addMouseWheelListener(e -> hideOverlayButton());
    }
}
