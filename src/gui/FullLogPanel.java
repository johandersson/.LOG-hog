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
        fullLogPane.setContentType("text/plain");
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
        openInNotepadButton.addActionListener(e -> NotepadOpener.openLogInNotepad());
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
            MarkdownRenderer.renderMarkdown(fullLogPane, lines);
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
            fullLogPane.setText("Error reading " + chosen.toAbsolutePath().toString() + " : " + e.getMessage());
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

    @Override
    public void loadLog() {
        // Load logic is handled by the editor's unlock mechanism
        // This method can be used for manual reload if needed
    }

    @Override
    public void copyToClipboard() {
        copyFullLogToClipboard();
    }
}
