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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.text.StyledDocument;

import filehandling.FullLogFileLoader;
import filehandling.LogFileFormatter;
import filehandling.LogFileHandler;
import main.LogTextEditor;
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
    private final JButton formatButton;
    private SearchDialog searchDialog;
    private boolean suppressAutoLoad = false;

    public void setSuppressAutoLoad(boolean suppress) {
        this.suppressAutoLoad = suppress;
    }

    public boolean isSuppressAutoLoad() {
        return suppressAutoLoad;
    }
    private FullLogFileLoader fileLoader;

    public FullLogPanel(LogTextEditor editor, LogFileHandler logFileHandler) {
        this.editor = editor;
        this.logFileHandler = logFileHandler;
        this.fullLogPane = new HighlightableTextPane();
        this.fileLoader = new FullLogFileLoader(logFileHandler, fullLogPane);
        this.fullLogPathLabel = new JLabel("Log file: (not loaded)");
        this.lockFileButton = new AccentButton(editor.isLocked() ? "Unlock File" : "Lock File");
        this.copyFullLogButton = new AccentButton("Copy Full Log to Clipboard");
        
        // Platform-specific button label
        String os = System.getProperty("os.name").toLowerCase();
        String buttonLabel = os.contains("windows") ? "Open in Notepad" : "Open in Text Editor";
        this.openInNotepadButton = new AccentButton(buttonLabel);
        
        this.searchButton = new AccentButton("Search");
        this.formatButton = new AccentButton("Fix Linebreak Formatting");
        initPanel();
        updateLockButton(); // Ensure buttons are in correct state based on lock status
    }

    private void initPanel() {
        setLayout(new BorderLayout(6, 6));
        setBackground(Color.WHITE);

        fullLogPane.setEditable(false);
        fullLogPane.setBackground(Color.WHITE);
        // Enable tooltips for the text pane
        ToolTipManager.sharedInstance().registerComponent(fullLogPane);
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

        // Add click on timestamp to edit entry (registered once)
        new TimestampClickHandler(fullLogPane, this::openEntryForEditing);

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
        
        formatButton.addActionListener(e -> fixLinebreakFormatting());
        rightBottomPanel.add(formatButton);

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
        formatButton.setEnabled(!editor.isLocked());
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
            DialogHelper.showWarning(this, "Copy Failed", "Log is empty or not loaded.");
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
            loadAndProcessLogFile(logPath, true);
            updateLockButton();
        });
    }

    public void loadFullLogNoScroll() {
        loadFullLogNoScroll(null);
    }

    public void loadFullLogNoScroll(Runnable callback) {
        suppressAutoLoad = true;
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
            loadAndProcessLogFile(logPath, false);
            updateLockButton();
            if (callback != null) {
                callback.run();
            }
            suppressAutoLoad = false;
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
        loadAndProcessLogFile(logPath, true);
    }

    private void loadAndProcessLogFile(Path logPath, boolean scrollToBottom) {
        try {
            fileLoader.loadAndProcessLogFile(logPath, scrollToBottom);
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
            DialogHelper.showDecryptionFailed(this);
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
        fileLoader.fallbackReadRaw(chosen);
        fullLogPathLabel.setText("Log file: error reading file");
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
            int choice = DialogHelper.showOptions(
                this,
                "Encrypted File Warning",
                "⚠️ File is Encrypted",
                "Your log file is encrypted with AES-256 encryption.<br>" +
                "Opening it in a text editor will show encrypted data (unreadable gibberish),<br>" +
                "not your actual log entries.<br><br>" +
                "<b>To read the content:</b><br>" +
                "• Use the Full Log view in .LOG-hog (decrypts automatically)<br>" +
                "• Or disable encryption first in Settings<br><br>" +
                "Do you still want to open the encrypted file?",
                JOptionPane.WARNING_MESSAGE,
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
     * Fixes linebreak formatting in the log file by normalizing spacing between entries.
     * Creates a backup before modifying the file and shows progress during the operation.
     */
    private void fixLinebreakFormatting() {
        if (editor.isLocked()) {
            DialogHelper.showFileLocked(this);
            return;
        }
        
        // Confirm action
        if (!DialogHelper.confirm(this,
            "Confirm Formatting",
            "Fix Linebreak Formatting",
            "This will normalize spacing between log entries to ensure consistency.<br>" +
            "A backup will be created automatically before making changes.<br><br>" +
            "Do you want to continue?")) {
            return;
        }
        
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        LogFileFormatter.performFormatting(parentFrame, logFileHandler, this::loadFullLog);
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
        DialogHelper.showEntryNotFound(this);
    }

    public void scrollToEntry(String timestamp) {
        try {
            StyledDocument doc = fullLogPane.getStyledDocument();
            String text = doc.getText(0, doc.getLength());
            int index = text.indexOf(timestamp);
            if (index != -1) {
                fullLogPane.setCaretPosition(index);
                fullLogPane.requestFocusInWindow();
            }
        } catch (javax.swing.text.BadLocationException e) {
            // Ignore errors
        }
    }
}
