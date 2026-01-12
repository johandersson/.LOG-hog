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

import encryption.EncryptionManager;
import filehandling.LogFileHandler;
import java.awt.*;
import java.awt.datatransfer.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import main.LogTextEditor;
import markdown.LinkHandler;
import markdown.MarkdownRenderer;
import notepad.NotepadOpener;

public class FullLogPanel extends LogPanel {
    private final HighlightableTextPane fullLogPane;
    private final JLabel fullLogPathLabel;
    private final LogFileHandler logFileHandler;
    private final LogTextEditor editor;
    private final JButton lockFileButton;
    private final JButton copyFullLogButton;
    private final JButton openInNotepadButton;
    private final JButton searchButton;
    private SearchDialog searchDialog;
    private static final int MAX_ENTRIES_TO_RENDER = 5000; // Limit for performance

    public FullLogPanel(LogTextEditor editor, LogFileHandler logFileHandler) {
        this.editor = editor;
        this.logFileHandler = logFileHandler;
        this.fullLogPane = new HighlightableTextPane();
        this.fullLogPathLabel = new JLabel("Log file: (not loaded)");
        this.lockFileButton = new AccentButton(editor.isLocked() ? "Unlock File" : "Lock File");
        this.copyFullLogButton = new AccentButton("Copy Full Log to Clipboard");
        
        // Platform-specific button label
        String os = System.getProperty("os.name").toLowerCase();
        String buttonLabel = os.contains("windows") ? "Open in Notepad" : "Open in Text Editor";
        this.openInNotepadButton = new AccentButton(buttonLabel);
        
        this.searchButton = new AccentButton("Search");
        initPanel();
        updateLockButton(); // Ensure buttons are in correct state based on lock status
    }

    private void initPanel() {
        setLayout(new BorderLayout(6, 6));
        setBackground(Color.WHITE);

        fullLogPane.setEditable(false);
        fullLogPane.setBackground(Color.WHITE);
        // Don't set content type to "text/plain" - we need StyledDocument for markdown links
        // fullLogPane.setContentType("text/plain");
        fullLogPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        // fullLogPane.setFont(new Font("Georgia", Font.PLAIN, 14)); // Remove to let document styles control font

        // Override ctrl+c to use secure clipboard
        fullLogPane.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK), "copySecure");
        fullLogPane.getActionMap().put("copySecure", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String selectedText = fullLogPane.getSelectedText();
                if (selectedText != null && !selectedText.isEmpty()) {
                    clipboard.SecureClipboardManager.getInstance().copySecureTextToClipboard(selectedText, fullLogPane);
                }
            }
        });

        // Add double-click on timestamp to edit entry
        addTimestampDoubleClickHandler();

        var scroll = new JScrollPane(fullLogPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        var pathPanel = new JPanel(new BorderLayout());
        pathPanel.setOpaque(false);
        fullLogPathLabel.setForeground(new Color(0x3A4A52));
        pathPanel.add(fullLogPathLabel, BorderLayout.WEST);
        add(pathPanel, BorderLayout.NORTH);

        var bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        copyFullLogButton.addActionListener(e -> copyFullLogToClipboard());
        bottom.add(copyFullLogButton);
        openInNotepadButton.addActionListener(e -> openInExternalEditor());
        bottom.add(openInNotepadButton);
        lockFileButton.addActionListener(e -> {
            if (editor.isLocked()) {
                editor.manualUnlock();
            } else {
                editor.manualLock();
            }
        });
        bottom.add(lockFileButton);

        var rightBottomPanel = getRightBottomPanel();
        bottom.add(rightBottomPanel, 0);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel getRightBottomPanel() {
        var rightBottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rightBottomPanel.setOpaque(false);

        searchButton.addActionListener(e -> openSearchDialog());
        rightBottomPanel.add(searchButton);

        return rightBottomPanel;
    }

    public void openSearchDialog() {
        if (searchDialog == null) {
            searchDialog = new SearchDialog((Frame) SwingUtilities.getWindowAncestor(this), fullLogPane);
        }
        searchDialog.setVisible(true);
    }

    public void updateLockButton() {
        lockFileButton.setText(editor.isLocked() ? "Unlock File" : "Lock File");
        searchButton.setEnabled(!editor.isLocked());
    }

    private void updateButtonStates(boolean locked) {
        copyFullLogButton.setEnabled(!locked);
        openInNotepadButton.setEnabled(!locked);
        updateLockButton();
    }

    public void performSearchInFullLog(String query) {
        // Legacy method, now handled by SearchDialog
        openSearchDialog();
    }

    private void navigateToHighlight(boolean next) {
        fullLogPane.navigateHighlights(next);
    }

    private void copyFullLogToClipboard() {
        var text = fullLogPane.getText();
        if (text == null || text.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this, "Log is empty or not loaded.", "Copy Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show enhanced security warning
        if (!clipboard.ClipboardSecurityWarner.showFullLogWarning(this)) {
            return; // User chose not to copy
        }

        // Use secure clipboard with automatic clearing
        clipboard.SecureClipboardManager.getInstance().copySecureTextToClipboard(text, this,
            "Full log copied to clipboard securely.");
    }

    public void loadFullLog() {
        SwingUtilities.invokeLater(() -> {
            if (editor.isLocked()) {
                handleLockedState();
                return;
            }
            updateButtonStates(false);
            var logPath = Path.of(System.getProperty("user.home"), "log.txt");
            if (!Files.exists(logPath)) {
                showLogNotFound();
                return;
            }
            clearEditorForNewLoad(logPath);
            loadAndProcessLogFile(logPath);
            updateLockButton();
        });
    }

    private void handleLockedState() {
        fullLogPane.setText("");
        fullLogPane.clearHighlights();
        fullLogPane.setContentType("text/plain");
        fullLogPane.setText("File locked. Press Unlock file in Full log view to unlock it again.");
        fullLogPane.setForeground(Color.GRAY);
        fullLogPathLabel.setText("Log file: (locked)");
        updateButtonStates(true);
    }

    private void loadAndProcessLogFile(Path logPath) {
        try {
            List<String> lines;
            if (logFileHandler.isEncrypted()) {
                var data = Files.readAllBytes(logPath);
                var decrypted = EncryptionManager.getInstance().decryptWithFallback(data, logFileHandler.getPassword(), logFileHandler.getSalt());
                lines = Arrays.asList(decrypted.split("\n", -1));
            } else {
                lines = Files.readAllLines(logPath);
            }
            // Remove secure clipboard markers from lines
            lines = lines.stream().map(LogFileHandler::removeSecureMarker).collect(Collectors.toList());
            
            // Lazy loading: Only render recent N entries for performance
            List<List<String>> allEntries = filehandling.LogParser.parseEntriesForFullLog(lines);
            List<List<String>> entriesToRender;
            
            if (allEntries.size() > MAX_ENTRIES_TO_RENDER) {
                // Take the most recent N entries (already sorted newest first)
                entriesToRender = allEntries.subList(0, MAX_ENTRIES_TO_RENDER);
                // Add info message at top
                List<String> infoEntry = new ArrayList<>();
                infoEntry.add("Showing " + MAX_ENTRIES_TO_RENDER + " most recent entries (out of " + allEntries.size() + " total)");
                infoEntry.add("Use the Log List view with filters to browse older entries.");
                entriesToRender = new ArrayList<>(entriesToRender);
                entriesToRender.add(0, infoEntry);
            } else {
                entriesToRender = allEntries;
            }
            
            MarkdownRenderer.renderMarkdownFromEntries(fullLogPane, entriesToRender);
            LinkHandler.addLinkListeners(fullLogPane);
        } catch (Exception ex) {
            handleLoadException(ex, logPath);
        }
    }

    private void handleLoadException(Exception ex, Path logPath) {
        String errorMsg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (errorMsg.contains("tag mismatch") || errorMsg.contains("decryption failed")) {
            // If decryption fails when file should be unlocked, it means there's a state inconsistency
            // Set the file as locked and show appropriate message
            editor.setLocked(true);
            JOptionPane.showMessageDialog(this, "Decryption failed. The file has been locked for security. Please use the Unlock button to try again.", "Security Error", JOptionPane.ERROR_MESSAGE);
            handleLockedState();
        } else {
            fallbackReadRaw(logPath);
        }
    }

    private void clearEditorForNewLoad(Path chosen) {
        fullLogPane.setText("");
        fullLogPane.clearHighlights();
        fullLogPathLabel.setText("Log file: " + chosen.toAbsolutePath().toString());
    }

    private void fallbackReadRaw(Path chosen) {
        try {
            var bytes = Files.readAllBytes(chosen);
            var content = new String(bytes);
            fullLogPane.setText(content);
            fullLogPane.clearHighlights();
            fullLogPane.setCaretPosition(0);
        } catch (Exception e) {
            // Security: Don't expose file paths or internal error details
            fullLogPane.setText("Error reading log file. Please check file permissions and format.");
            fullLogPathLabel.setText("Log file: error reading file");
            fullLogPane.clearHighlights();
        }
    }

    private static List<String> getNormalized(List<String> updatedLines) {
        var normalized = new ArrayList<String>();
        var prevBlank = false;
        for (var l : updatedLines) {
            var isBlank = l.trim().isEmpty();
            if (isBlank) {
                if (!prevBlank) {
                    normalized.add(""); // keep single blank line
                    prevBlank = true;
                } // else skip additional blank lines
            } else {
                normalized.add(l);
                prevBlank = false;
            }
        }
        return normalized;
    }

    private void showLogNotFound() {
        var userHome = System.getProperty("user.home");
        fullLogPane.setText("log.txt not found in user home or current working directory.\n"
                + "Checked paths:\n"
                + Path.of(userHome, "log.txt") + "\n"
                + Path.of(System.getProperty("user.dir"), "log.txt"));
        fullLogPathLabel.setText("Log file: not found");
        fullLogPane.clearHighlights();
    }
    
    private void openInExternalEditor() {
        // Warn if file is encrypted
        if (logFileHandler.isEncrypted()) {
            int choice = JOptionPane.showOptionDialog(
                this,
                "<html><b>⚠️ File is Encrypted</b><br><br>" +
                "Your log file is encrypted with AES-256 encryption.<br>" +
                "Opening it in a text editor will show encrypted data (unreadable gibberish),<br>" +
                "not your actual log entries.<br><br>" +
                "<b>To read the content:</b><br>" +
                "• Use the Full Log view in .LOG-hog (decrypts automatically)<br>" +
                "• Or disable encryption first in Settings<br><br>" +
                "Do you still want to open the encrypted file?</html>",
                "Encrypted File Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{"Open Anyway", "Cancel"},
                "Cancel"
            );
            
            if (choice != 0) { // User chose Cancel or closed dialog
                return;
            }
        }
        
        NotepadOpener.openLogInNotepad();
    }

    @Override
    public void loadLog() {
        // Load logic is handled by the editor's unlock mechanism
        // This method can be used for manual reload if needed
    }

    @Override
    public void copyToClipboard() {
        copyFullLogToClipboard();
    }
    
    public HighlightableTextPane getFullLogPane() {
        return fullLogPane;
    }

    /**
     * Adds double-click handler to timestamps for quick entry editing.
     * Double-clicking on a timestamp line switches to Log List tab and selects that entry.
     */
    private void addTimestampDoubleClickHandler() {
        java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(new java.awt.event.AWTEventListener() {
            private long lastClickTime = 0;
            private java.awt.Point lastClickPoint = null;
            
            @Override
            public void eventDispatched(java.awt.AWTEvent event) {
                if (!(event instanceof java.awt.event.MouseEvent)) {
                    return;
                }
                
                java.awt.event.MouseEvent e = (java.awt.event.MouseEvent) event;
                
                // Only process events on our fullLogPane
                if (e.getComponent() != fullLogPane && e.getComponent().getParent() != fullLogPane) {
                    return;
                }
                
                // Only handle mouse pressed events
                if (e.getID() != java.awt.event.MouseEvent.MOUSE_PRESSED) {
                    return;
                }
                
                long currentTime = System.currentTimeMillis();
                java.awt.Point currentPoint = e.getPoint();
                
                // Convert point to fullLogPane coordinates if needed
                if (e.getComponent() != fullLogPane) {
                    currentPoint = SwingUtilities.convertPoint(e.getComponent(), currentPoint, fullLogPane);
                }
                
                boolean isDoubleClick = false;
                
                // Manual double-click detection
                if (lastClickTime > 0 && (currentTime - lastClickTime) < 500 && lastClickPoint != null) {
                    double distance = lastClickPoint.distance(currentPoint);
                    if (distance < 5) {
                        isDoubleClick = true;
                    }
                }
                
                lastClickTime = currentTime;
                lastClickPoint = currentPoint;
                
                if (!isDoubleClick) {
                    return;
                }
                
                // Get position in document
                int pos = fullLogPane.viewToModel2D(currentPoint);
                if (pos < 0) {
                    return;
                }
                
                try {
                    // Find the start and end of the current line
                    javax.swing.text.StyledDocument doc = fullLogPane.getStyledDocument();
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
                    
                    String lineText = text.substring(lineStart, lineEnd).trim();
                    
                    // Check if it's a timestamp line (format: "HH:MM YYYY-MM-DD" or "HH:MM YYYY-MM-DD (N)")
                    if (lineText.matches("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$")) {
                        openEntryForEditing(lineText);
                    }
                } catch (Exception ex) {
                    // Silently ignore errors - not a valid timestamp position
                }
            }
        }, java.awt.AWTEvent.MOUSE_EVENT_MASK);
        
        // Add visual feedback: change cursor and show tooltip when hovering over timestamps
        fullLogPane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                try {
                    int pos = fullLogPane.viewToModel2D(e.getPoint());
                    if (pos < 0) {
                        fullLogPane.setCursor(java.awt.Cursor.getDefaultCursor());
                        fullLogPane.setToolTipText(null);
                        return;
                    }
                    
                    javax.swing.text.StyledDocument doc = fullLogPane.getStyledDocument();
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
                    
                    String lineText = text.substring(lineStart, lineEnd).trim();
                    
                    // Check if hovering over a timestamp line
                    if (lineText.matches("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$")) {
                        fullLogPane.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                        fullLogPane.setToolTipText("Double-click to edit this entry");
                    } else {
                        fullLogPane.setCursor(java.awt.Cursor.getDefaultCursor());
                        fullLogPane.setToolTipText(null);
                    }
                } catch (Exception ex) {
                    fullLogPane.setCursor(java.awt.Cursor.getDefaultCursor());
                    fullLogPane.setToolTipText(null);
                }
            }
        });
    }

    /**
     * Opens the specified entry for editing in the Log List tab.
     * @param timestamp The timestamp of the entry to edit (format: HH:mm yyyy-MM-dd)
     */
    private void openEntryForEditing(String timestamp) {
        // Switch to Log List tab (index 1)
        editor.getTabPane().setSelectedIndex(1);
        
        // Find and select the entry in the log list
        LogListPanel logListPanel = editor.getLogListPanel();
        DefaultListModel<String> listModel = logListPanel.getListModel();
        JList<String> logList = logListPanel.getLogList();
        
        // Search for the entry with matching timestamp
        for (int i = 0; i < listModel.getSize(); i++) {
            String entry = listModel.getElementAt(i);
            if (entry.startsWith(timestamp)) {
                // Found it - select and scroll to it
                logList.setSelectedIndex(i);
                logList.ensureIndexIsVisible(i);
                
                // Focus the entry text area for editing
                SwingUtilities.invokeLater(() -> {
                    logListPanel.getEntryArea().requestFocusInWindow();
                });
                return;
            }
        }
        
        // If not found in current view, show a message
        JOptionPane.showMessageDialog(this,
            "<html><b>Entry Not Found</b><br><br>" +
            "This entry is not visible in the current Log List view.<br>" +
            "You may need to adjust the year/month filter to see it.</html>",
            "Entry Not Found",
            JOptionPane.INFORMATION_MESSAGE);
    }
}
