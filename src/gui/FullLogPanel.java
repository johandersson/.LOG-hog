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
import javax.swing.*;
import main.LogTextEditor;
import markdown.MarkdownRenderer;
import notepad.NotepadOpener;

public class FullLogPanel extends JPanel {
    private final HighlightableTextPane fullLogPane;
    private final JLabel fullLogPathLabel;
    private final LogFileHandler logFileHandler;
    private final LogTextEditor editor;
    private final JButton lockFileButton;
    private final JButton copyFullLogButton;
    private final JButton openInNotepadButton;

    public FullLogPanel(LogTextEditor editor, LogFileHandler logFileHandler) {
        this.editor = editor;
        this.logFileHandler = logFileHandler;
        this.fullLogPane = new HighlightableTextPane();
        this.fullLogPathLabel = new JLabel("Log file: (not loaded)");
        this.lockFileButton = new AccentButton(editor.isLocked() ? "Unlock File" : "Lock File");
        this.copyFullLogButton = new AccentButton("Copy Full Log to Clipboard");
        this.openInNotepadButton = new AccentButton("Open in Notepad");
        initPanel();
    }

    private void initPanel() {
        setLayout(new BorderLayout(6, 6));
        setBackground(Color.WHITE);

        fullLogPane.setEditable(false);
        fullLogPane.setBackground(Color.WHITE);
        fullLogPane.setContentType("text/plain");
        fullLogPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        // fullLogPane.setFont(new Font("Georgia", Font.PLAIN, 14)); // Remove to let document styles control font
        var scroll = new JScrollPane(fullLogPane);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE6E9EB)));
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
        var rightBottomSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rightBottomSearchPanel.setOpaque(false);
        var searchLabel = new JLabel("Search:");
        rightBottomSearchPanel.add(searchLabel);
        var searchField = new JTextField(15);
        searchField.addActionListener(e -> performSearchInFullLog(searchField.getText()));
        rightBottomSearchPanel.add(searchField);
        var searchBtn = new AccentButton("Find");
        searchBtn.addActionListener(e -> performSearchInFullLog(searchField.getText()));
        rightBottomSearchPanel.add(searchBtn);

        var prevHighlightBtn = new AccentButton("<-");
        var nextHighlightBtn = new AccentButton("->");
        prevHighlightBtn.addActionListener(e -> navigateToHighlight(false));
        nextHighlightBtn.addActionListener(e -> navigateToHighlight(true));
        rightBottomSearchPanel.add(prevHighlightBtn);
        rightBottomSearchPanel.add(nextHighlightBtn);
        return rightBottomSearchPanel;
    }

    public void updateLockButton() {
        lockFileButton.setText(editor.isLocked() ? "Unlock File" : "Lock File");
    }

    private void updateButtonStates(boolean locked) {
        copyFullLogButton.setEnabled(!locked);
        openInNotepadButton.setEnabled(!locked);
        updateLockButton();
    }

    public void performSearchInFullLog(String query) {
        if (!fullLogPane.highlightText(query)) {
            JOptionPane.showMessageDialog(this, "Text not found", "Find", JOptionPane.INFORMATION_MESSAGE);
        }
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

        // Check if file is encrypted and warn user
        if (logFileHandler.isEncrypted()) {
            var confirm = JOptionPane.showConfirmDialog(
                this,
                "<html><b>Security Warning:</b><br><br>" +
                "You are about to copy the entire log file to the clipboard.<br>" +
                "Clipboard contents may be accessible to other applications.<br><br>" +
                "Are you sure you want to copy the full log to the clipboard?</html>",
                "Copy Full Log to Clipboard - Security Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return; // User chose not to copy
            }
        }

        var selection = new StringSelection(text);
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            clipboard.setContents(selection, selection);
            JOptionPane.showMessageDialog(this, "Full log copied to clipboard.", "Copied", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException ise) {
            JOptionPane.showMessageDialog(this, "Unable to access clipboard right now. Try again.", "Clipboard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadFullLog() {
        SwingUtilities.invokeLater(() -> {
            if (editor.isLocked()) {
                fullLogPane.setText("");
                fullLogPane.clearHighlights();
                fullLogPane.setContentType("text/plain");
                fullLogPane.setText("File locked. Press Unlock file in Full log view to unlock it again.");
                fullLogPane.setForeground(Color.GRAY);
                fullLogPathLabel.setText("Log file: (locked)");
                updateButtonStates(true);
                return;
            }
            updateButtonStates(false);
            var logPath = Path.of(System.getProperty("user.home"), "log.txt");
            if (!Files.exists(logPath)) {
                showLogNotFound();
                return;
            }

            clearEditorForNewLoad(logPath);

            try {
                List<String> lines;
                if (logFileHandler.isEncrypted()) {
                    var data = Files.readAllBytes(logPath);
                    var key = EncryptionManager.deriveKey(logFileHandler.getPassword(), logFileHandler.getSalt());
                    var decrypted = EncryptionManager.decrypt(data, key);
                    lines = Arrays.asList(decrypted.split("\n", -1));
                } else {
                    lines = Files.readAllLines(logPath);
                }
                lines = getNormalized(lines);
                MarkdownRenderer.renderMarkdown(fullLogPane, lines);
                MarkdownRenderer.addLinkListeners(fullLogPane);
            } catch (Exception ex) {
                if (ex.getMessage().contains("Tag mismatch")) {
                    JOptionPane.showMessageDialog(this, "Incorrect password. Please restart the application and try again.", "Password Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    fallbackReadRaw(logPath);
                }
            }
            updateLockButton();
        });
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
                + userHome + "\\log.txt\n"
                + System.getProperty("user.dir") + "\\log.txt");
        fullLogPathLabel.setText("Log file: not found");
        fullLogPane.clearHighlights();
    }
}
