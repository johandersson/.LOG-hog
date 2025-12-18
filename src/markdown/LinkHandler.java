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

package markdown;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyledDocument;

public class LinkHandler {

    public static void addLinkListeners(JTextPane pane) {
        // Remove existing link listeners to avoid duplicates
        for (java.awt.event.MouseListener ml : pane.getMouseListeners()) {
            if (ml instanceof MouseAdapter) {
                pane.removeMouseListener(ml);
            }
        }
        for (java.awt.event.MouseMotionListener mml : pane.getMouseMotionListeners()) {
            if (mml instanceof MouseMotionAdapter) {
                pane.removeMouseMotionListener(mml);
            }
        }

        pane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleLinkClick(pane, e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showLinkPopup(pane, e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showLinkPopup(pane, e);
                }
            }
        });

        pane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleLinkHover(pane, e);
            }
        });
    }

    private static void handleLinkClick(JTextPane pane, MouseEvent e) {
        try {
            int pos = pane.viewToModel2D(e.getPoint());
            if (pos < 0) return;
            StyledDocument doc = pane.getStyledDocument();
            AttributeSet attrs = doc.getCharacterElement(pos).getAttributes();
            Object hrefObj = attrs.getAttribute("href");
            if (hrefObj instanceof String href) {
                if (href.startsWith("file:")) {
                    handleFileLink(pane, href);
                } else {
                    handleWebLink(pane, href);
                }
            }
        } catch (Exception ex) {
            showLinkError(pane, "Failed to open link: " + ex.getMessage());
        }
    }

    private static void handleFileLink(JTextPane pane, String href) {
        if (!Desktop.isDesktopSupported()) {
            showLinkError(pane, "Desktop is not supported on this system.");
            return;
        }

        java.io.File file;
        try {
            // First try to parse as URI
            java.net.URI uri = java.net.URI.create(href);
            file = new java.io.File(uri);
        } catch (Exception uriEx) {
            // Fallback: try to extract path manually
            try {
                String filePath;
                if (href.startsWith("file:///")) {
                    filePath = href.substring(8); // Remove "file:///"
                } else if (href.startsWith("file://")) {
                    filePath = href.substring(7); // Remove "file://"
                } else {
                    filePath = href.substring(5); // Remove "file:"
                }
                file = new java.io.File(filePath);
            } catch (Exception pathEx) {
                showLinkError(pane, "Invalid file path format: " + href);
                return;
            }
        }

        // Check if file exists
        if (!file.exists()) {
            showLinkError(pane, "File does not exist: " + file.getAbsolutePath());
            return;
        }

        // Check if it's actually a file (not a directory)
        if (!file.isFile()) {
            showLinkError(pane, "Path is not a file: " + file.getAbsolutePath());
            return;
        }

        // Try to open the file
        try {
            Desktop.getDesktop().open(file);
        } catch (java.io.IOException ioEx) {
            showLinkError(pane, "Failed to open file: " + ioEx.getMessage());
        } catch (Exception ex) {
            showLinkError(pane, "Unexpected error opening file: " + ex.getMessage());
        }
    }

    private static void handleWebLink(JTextPane pane, String href) {
        if (!Desktop.isDesktopSupported()) {
            showLinkError(pane, "Desktop is not supported on this system.");
            return;
        }

        try {
            String finalHref = href;
            if (!href.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
                finalHref = "http://" + href;
            }
            Desktop.getDesktop().browse(java.net.URI.create(finalHref));
        } catch (java.io.IOException ioEx) {
            showLinkError(pane, "Failed to open URL: " + ioEx.getMessage());
        } catch (Exception ex) {
            showLinkError(pane, "Invalid URL format: " + href);
        }
    }

    private static void handleLinkHover(JTextPane pane, MouseEvent e) {
        try {
            int pos = pane.viewToModel2D(e.getPoint());
            if (pos < 0) {
                pane.setCursor(Cursor.getDefaultCursor());
                return;
            }
            AttributeSet attrs = pane.getStyledDocument().getCharacterElement(pos).getAttributes();
            Object hrefObj = attrs.getAttribute("href");
            pane.setCursor(hrefObj instanceof String ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        } catch (Exception ex) {
            pane.setCursor(Cursor.getDefaultCursor());
        }
    }

    private static void showLinkPopup(JTextPane pane, MouseEvent e) {
        try {
            int pos = pane.viewToModel2D(e.getPoint());
            if (pos < 0) return;
            StyledDocument doc = pane.getStyledDocument();
            AttributeSet attrs = doc.getCharacterElement(pos).getAttributes();
            Object hrefObj = attrs.getAttribute("href");
            if (hrefObj instanceof String href) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem copyItem = new JMenuItem("Copy Link");
                copyItem.addActionListener(ae -> clipboard.SecureClipboardManager.getInstance().copySecureTextToClipboard(href, pane, "Link copied to clipboard securely!"));
                popup.add(copyItem);
                popup.show(pane, e.getX(), e.getY());
            }
        } catch (Exception ex) {
            // swallow
        }
    }

    private static void showLinkError(JTextPane pane, String message) {
        // Find the parent window to show the error dialog
        java.awt.Window parent = javax.swing.SwingUtilities.getWindowAncestor(pane);
        javax.swing.JOptionPane.showMessageDialog(
            parent,
            message,
            "Link Error",
            javax.swing.JOptionPane.ERROR_MESSAGE
        );
    }
}