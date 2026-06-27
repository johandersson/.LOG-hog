/*
 * Copyright (C) 2026 Johan Andersson
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
import java.awt.Cursor;
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
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.text.StyledDocument;
import javax.swing.JProgressBar;

import java.time.LocalDateTime;

import filehandling.FullLogFileLoader;
import filehandling.LogFileFormatter;
import filehandling.LogFileHandler;
import filehandling.ParsedLogData;
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
    private final LogInfoPanel infoPanel;
    private final JProgressBar fullLoadProgress;
    private SearchDialog searchDialog;
    private boolean suppressAutoLoad; // default false, no initializer needed
    private TimestampClickHandler timestampClickHandler;
    // private SwingWorker<StyledDocument, Integer> currentLoadWorker; // No longer used

    public void setSuppressAutoLoad(boolean suppress) {
        this.suppressAutoLoad = suppress;
    }

    public boolean isSuppressAutoLoad() {
        return suppressAutoLoad;
    }
    private FullLogFileLoader fileLoader;
    private Runnable cacheInvalidationListener;

    public FullLogPanel(LogTextEditor editor, LogFileHandler logFileHandler) {
        this.editor = editor;
        this.logFileHandler = logFileHandler;
        this.fullLogPane = new HighlightableTextPane();
        this.fileLoader = new FullLogFileLoader(logFileHandler, fullLogPane);
        // Register listener so FullLogFileLoader cache and markdown caches are invalidated when filehandler updates
        this.cacheInvalidationListener = () -> SwingUtilities.invokeLater(() -> {
            fileLoader.invalidateCache();
            // Invalidate markdown renderer caches so document-level and entry-level caches are cleared
            markdown.MarkdownRenderer.invalidateAllCaches();
            // If this panel is currently visible, refresh the view so newly saved entries appear
            try {
                // If part of the tab pane and currently selected, reload without forcing scroll
                if (FullLogPanel.this.isShowing()) {
                    loadFullLogNoScroll(null);
                }
            } catch (Exception ignored) {}
        });
        this.logFileHandler.addCacheInvalidationListener(this.cacheInvalidationListener);
        this.fullLogPathLabel = new JLabel("Log file: (not loaded)");
        this.lockFileButton = new AccentButton(editor.isLocked() ? "Unlock File" : "Lock File");
        this.copyFullLogButton = new AccentButton("Copy Full Log to Clipboard");

        // Platform-specific button label
        String os = System.getProperty("os.name").toLowerCase();
        String buttonLabel = os.contains("windows") ? "Open in Notepad" : "Open in Text Editor";
        this.openInNotepadButton = new AccentButton(buttonLabel);

        this.searchButton = new AccentButton("Search");
        this.formatButton = new AccentButton("Fix Linebreak Formatting");

        // Initialize info panel component
        this.infoPanel = new LogInfoPanel();
        this.fullLoadProgress = new JProgressBar();

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
        timestampClickHandler = new TimestampClickHandler(fullLogPane, this::openEntryForEditing);

        JScrollPane scroll = new JScrollPane(fullLogPane);
        // Prevent horizontal scrolling by forcing wrapping in the text pane
        scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        timestampClickHandler.addScrollListeners();

        JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.setOpaque(false);
        fullLogPathLabel.setForeground(new Color(0x3A4A52));
        pathPanel.add(fullLogPathLabel, BorderLayout.WEST);
        fullLoadProgress.setIndeterminate(true);
        fullLoadProgress.setVisible(false);
        fullLoadProgress.setPreferredSize(new java.awt.Dimension(140, 16));
        pathPanel.add(fullLoadProgress, BorderLayout.EAST);
        add(pathPanel, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);

        // Left side: info panel in a wrapper to control sizing
        JPanel infoWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        infoWrapper.setOpaque(false);
        infoWrapper.add(infoPanel);
        bottom.add(infoWrapper, BorderLayout.WEST);

        // Right side: buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        copyFullLogButton.addActionListener(e -> copyFullLogToClipboard());
        buttonPanel.add(copyFullLogButton);
        openInNotepadButton.addActionListener(e -> openInExternalEditor());
        buttonPanel.add(openInNotepadButton);
        lockFileButton.addActionListener(e -> {
            if (editor.isLocked()) {
                editor.manualUnlock();
            } else {
                editor.manualLock();
            }
        });
        buttonPanel.add(lockFileButton);

        JPanel rightBottomPanel = getRightBottomPanel();
        buttonPanel.add(rightBottomPanel, 0);
        bottom.add(buttonPanel, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel getRightBottomPanel() {
        JPanel rightBottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rightBottomPanel.setOpaque(false);

        searchButton.addActionListener(e -> openSearchDialog());
        rightBottomPanel.add(searchButton);

        formatButton.addActionListener(e -> fixLinebreakFormatting());
        rightBottomPanel.add(formatButton);

        return rightBottomPanel;
    }

    private void resetLogStatistics() {
        infoPanel.resetStatistics();
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

    public void clearSearch() {
        fullLogPane.clearHighlights();
        if (searchDialog != null) {
            searchDialog.clearSearchState();
        }
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


    private void copyFullLogToClipboard() {
        String text = fullLogPane.getText();
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
            java.nio.file.Path logPath = java.nio.file.Paths.get(System.getProperty("user.home"), "log.txt");
            if (!Files.exists(logPath)) {
                showLogNotFound();
                return;
            }
            clearEditorForNewLoad(logPath);

            // Show loading feedback for large files
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            fullLoadProgress.setVisible(true);

            // Use SwingWorker to keep UI responsive during large file loads
            new SwingWorker<ParsedLogData, Void>() {
                @Override
                protected ParsedLogData doInBackground() throws Exception {
                    // Parse in background thread
                    return fileLoader.parseLogFile(logPath);
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    try {
                        ParsedLogData parsedData = get();
                        // Render on EDT
                        fileLoader.renderParsedData(parsedData, true);
                        LogStatistics stats = new LogStatistics(parsedData.getTotalEntryCount(), parsedData.entriesToRender, logPath);
                        infoPanel.updateStatistics(stats);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        handleLoadException(cause instanceof Exception ? (Exception) cause : new Exception(cause), logPath);
                    }
                    fullLoadProgress.setVisible(false);
                    updateLockButton();
                }
            }.execute();
        });
    }

    /**
     * Compatibility overload allowing callers to request a load and receive a callback when started.
     */
    public void loadFullLog(Runnable onStarted) {
        loadFullLog();
        if (onStarted != null) {
            try { onStarted.run(); } catch (Exception ignore) {}
        }
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
            java.nio.file.Path logPath = java.nio.file.Paths.get(System.getProperty("user.home"), "log.txt");
            if (!Files.exists(logPath)) {
                showLogNotFound();
                return;
            }
            clearEditorForNewLoad(logPath);

            // Show loading feedback for large files
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            fullLoadProgress.setVisible(true);
            fullLogPane.setText("");

            // Use SwingWorker to keep UI responsive during large file loads
            new SwingWorker<ParsedLogData, Void>() {
                @Override
                protected ParsedLogData doInBackground() throws Exception {
                    // Parse in background thread
                    return fileLoader.parseLogFile(logPath);
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    fullLoadProgress.setVisible(false);
                    try {
                        ParsedLogData parsedData = get();
                        // Render on EDT and always scroll to bottom when loading
                        fileLoader.renderParsedData(parsedData, true);
                        // Ensure caret is at end and visible
                        try {
                            fullLogPane.setCaretPosition(fullLogPane.getDocument().getLength());
                            fullLogPane.requestFocusInWindow();
                        } catch (Exception ignored) {}
                        LogStatistics stats = new LogStatistics(parsedData.getTotalEntryCount(), parsedData.entriesToRender, logPath);
                        infoPanel.updateStatistics(stats);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        handleLoadException(cause instanceof Exception ? (Exception) cause : new Exception(cause), logPath);
                    }
                    updateLockButton();
                    if (callback != null) {
                        callback.run();
                    }
                    suppressAutoLoad = false;
                }
            }.execute();
        });
    }

    private void handleLockedState() {
        fullLogPane.setText("");
        fullLogPane.clearHighlights();
        fullLogPane.setContentType("text/plain");
        fullLogPane.setText("File locked. Use the Unlock File button to unlock.");
        fullLogPane.setForeground(Color.GRAY);
        fullLogPathLabel.setText("Log file: (locked)");
        updateButtonStates(true);
        resetLogStatistics();
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
        String userHome = System.getProperty("user.home");
        fullLogPane.setText("log.txt not found in user home or current working directory.\n"
            + "Checked paths:\n"
            + java.nio.file.Paths.get(userHome, "log.txt") + "\n"
            + java.nio.file.Paths.get(System.getProperty("user.dir"), "log.txt"));
        fullLogPathLabel.setText("Log file: not found");
        fullLogPane.clearHighlights();
        resetLogStatistics();
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
     * @param timestamp The timestamp of the entry to edit (may include display suffix)
     */
    private void openEntryForEditing(String timestamp) {
        // Switch to Log List tab (index 1)
        editor.getTabPane().setSelectedIndex(1);
        
        LogListPanel logListPanel = editor.getLogListPanel();
        DefaultListModel<String> listModel = logListPanel.getListModel();
        JList<String> logList = logListPanel.getLogList();

        // Strip suffix only for date-parsing purposes (year/month filter).
        String rawTimestamp = timestamp.replaceAll(" \\(\\d+\\)$", "").trim();
        
        // Get the entry content from Full Log for content-based fallback matching
        String fullLogContent = fileLoader.getEntryContent(timestamp);

        // Parse the timestamp to get year and month for filter adjustment
        int targetYear = 0;
        int targetMonth = 0;
        try {
            LocalDateTime dt = utils.DateHandler.parseTimestamp(rawTimestamp);
            targetYear = dt.getYear();
            targetMonth = dt.getMonthValue();
        } catch (Exception e) {
            // If parsing fails, try raw selection without filter change
        }

        // Pass full display timestamp (including suffix) so occurrence index is used directly
        int matchIndex = findMatchingEntry(listModel, timestamp, fullLogContent);
        if (matchIndex >= 0) {
            logList.setSelectedIndex(matchIndex);
            logList.ensureIndexIsVisible(matchIndex);
            SwingUtilities.invokeLater(() -> logListPanel.getEntryArea().requestFocusInWindow());
            // Verify selection shortly after to guard against other async model updates
            javax.swing.Timer verifyTimer1 = new javax.swing.Timer(80, ev -> {
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (logList.getSelectedIndex() != matchIndex && matchIndex >= 0 && matchIndex < listModel.getSize()) {
                            logList.clearSelection();
                            logList.setSelectedIndex(matchIndex);
                            logList.ensureIndexIsVisible(matchIndex);
                        }
                    } catch (Exception ignored) {}
                });
            });
            verifyTimer1.setRepeats(false);
            verifyTimer1.start();
            return;
        }

        // Entry not found in current list - adjust filter to the entry's year/month and reload
        if (targetYear > 0 && targetMonth > 0) {
            final int finalYear = targetYear;
            final int finalMonth = targetMonth;
            final String finalContent = fullLogContent;
            
            LoadingProgressDialog progress = new LoadingProgressDialog(editor, "Loading");
            final javax.swing.Timer showTimer = new javax.swing.Timer(150, ev -> progress.show());
            showTimer.setRepeats(false);
            showTimer.start();

            // Use callback to select entry after filter completes
            logListPanel.setFilterAndApply(finalYear, finalMonth, () -> {
                if (showTimer.isRunning()) showTimer.stop();
                progress.close();
                
                // Now search for and select the entry with content matching
                    int idx = findMatchingEntry(listModel, timestamp, finalContent);
                    if (idx >= 0) {
                        final int sel = idx;
                        // Ensure selection happens on EDT after model update
                        SwingUtilities.invokeLater(() -> {
                            try {
                                logList.clearSelection();
                                logList.setSelectedIndex(sel);
                                logList.ensureIndexIsVisible(sel);
                                // Populate entry editor area with the content we got from Full Log
                                try {
                                    String displayTs = listModel.getElementAt(sel);
                                    String content = fileLoader.getEntryContent(displayTs);
                                    if (content == null || content.isBlank()) content = finalContent;
                                    logListPanel.getEntryArea().setText(content == null ? "" : content);
                                    editor.setCurrentEditedDisplayTimestamp(displayTs);
                                } catch (Exception ignored) {
                                    logListPanel.getEntryArea().setText(finalContent == null ? "" : finalContent);
                                }
                                logListPanel.getEntryArea().requestFocusInWindow();
                            } catch (Exception e) {
                                // If selection fails for any reason, show not-found dialog as fallback
                                DialogHelper.showEntryNotFound(FullLogPanel.this);
                            }
                        });
                        // Re-assert selection after a short delay in case model is modified
                        javax.swing.Timer verifyTimer2 = new javax.swing.Timer(100, ev -> {
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    if (logList.getSelectedIndex() != sel && sel >= 0 && sel < listModel.getSize()) {
                                        logList.clearSelection();
                                        logList.setSelectedIndex(sel);
                                        logList.ensureIndexIsVisible(sel);
                                    }
                                } catch (Exception ignored) {}
                            });
                        });
                        verifyTimer2.setRepeats(false);
                        verifyTimer2.start();
                    } else {
                        // Attempt more robust matching strategies before giving up
                        int fallbackIdx = -1;
                        // 1) Try base timestamp match (ignore any " (n)" suffix)
                        for (int i = 0; i < listModel.getSize(); i++) {
                            String el = listModel.getElementAt(i);
                            if (el == null) continue;
                            String base = el.replaceAll(" \\([0-9]+\\)$", "").trim();
                            if (base.equals(rawTimestamp)) { fallbackIdx = i; break; }
                        }
                        // 2) Try substring or contains match
                        if (fallbackIdx < 0) {
                            for (int i = 0; i < listModel.getSize(); i++) {
                                String el = listModel.getElementAt(i);
                                if (el != null && el.contains(rawTimestamp)) { fallbackIdx = i; break; }
                            }
                        }
                        // 3) As last resort, try content match (slower)
                        if (fallbackIdx < 0 && finalContent != null) {
                            String normTarget = finalContent.trim().replaceAll("\\s+", " ");
                            for (int i = 0; i < listModel.getSize(); i++) {
                                String el = listModel.getElementAt(i);
                                try {
                                    String entryContent = logFileHandler.loadEntry(logFileHandler.getRawTimestamp(el));
                                    if (entryContent != null) {
                                        String norm = entryContent.trim().replaceAll("\\s+", " ");
                                        if (norm.equals(normTarget)) { fallbackIdx = i; break; }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }

                        if (fallbackIdx >= 0) {
                            final int sel = fallbackIdx;
                            SwingUtilities.invokeLater(() -> {
                                logList.clearSelection();
                                logList.setSelectedIndex(sel);
                                logList.ensureIndexIsVisible(sel);
                                String displayTs = listModel.getElementAt(sel);
                                String content = fileLoader.getEntryContent(displayTs);
                                if (content == null || content.isBlank()) content = finalContent;
                                logListPanel.getEntryArea().setText(content == null ? "" : content);
                                editor.setCurrentEditedDisplayTimestamp(displayTs);
                                logListPanel.getEntryArea().requestFocusInWindow();
                            });
                            // Re-assert selection after a short delay in case model is modified
                            javax.swing.Timer verifyTimer3 = new javax.swing.Timer(100, ev -> {
                                SwingUtilities.invokeLater(() -> {
                                    try {
                                        if (logList.getSelectedIndex() != sel && sel >= 0 && sel < listModel.getSize()) {
                                            logList.clearSelection();
                                            logList.setSelectedIndex(sel);
                                            logList.ensureIndexIsVisible(sel);
                                        }
                                    } catch (Exception ignored) {}
                                });
                            });
                            verifyTimer3.setRepeats(false);
                            verifyTimer3.start();
                        } else {
                            DialogHelper.showEntryNotFound(FullLogPanel.this);
                        }
                    }
            });
        } else {
            // Fallback: couldn't parse timestamp, try a full reload
            fallbackFullReload(timestamp, fullLogContent, logListPanel, listModel, logList);
        }
    }
    
    /**
     * Finds the correct entry in the list model by matching timestamp and content.
     * When multiple entries have the same timestamp, uses content to identify the correct one.
     * 
     * @param listModel The list model to search
     * @param rawTimestamp The raw timestamp (without display suffix)
     * @param targetContent The content to match (from Full Log), or null to match first by timestamp
     * @return Index of matching entry, or -1 if not found
     */
    private int findMatchingEntry(DefaultListModel<String> listModel, String displayTimestamp, String targetContent) {
        // Derive raw timestamp and occurrence index from the display timestamp.
        // The occurrence index is the canonical locator for duplicate timestamps —
        // it directly maps to the Nth candidate in list-model order.
        String rawTimestamp = displayTimestamp.replaceAll(" \\(\\d+\\)$", "").trim();
        java.util.regex.Matcher occMatcher =
            java.util.regex.Pattern.compile(" \\((\\d+)\\)$").matcher(displayTimestamp.trim());
        int occurrenceIdx = occMatcher.find() ? Integer.parseInt(occMatcher.group(1)) : 0;

        java.util.List<Integer> candidates = new java.util.ArrayList<>();
        
        // Collect all list-model entries whose raw timestamp matches
        for (int i = 0; i < listModel.getSize(); i++) {
            String entry = listModel.getElementAt(i);
            if (entry != null) {
                String entryRaw = entry.replaceAll(" \\(\\d+\\)$", "").trim();
                if (entryRaw.equals(rawTimestamp)) {
                    candidates.add(i);
                }
            }
        }
        
        if (candidates.isEmpty()) return -1;
        if (candidates.size() == 1) return candidates.get(0);

        // Use the occurrence index directly — reliable even when entries share identical content.
        // candidates is collected in list-model order, and the list model stores entries in
        // increasing suffix order (no-suffix = idx 0, (1) = idx 1, …), so candidates.get(N)
        // is exactly the entry with suffix N.
        if (occurrenceIdx >= 0 && occurrenceIdx < candidates.size()) {
            return candidates.get(occurrenceIdx);
        }

        // Fallback: content comparison (for edge cases where occurrence index is out of range)
        if (targetContent != null) {
            for (int idx : candidates) {
                String ts = listModel.getElementAt(idx);
                try {
                    String entryContent = logFileHandler.loadEntry(ts);
                    if (entryContent != null && entryContent.trim().equals(targetContent.trim())) {
                        return idx;
                    }
                } catch (Exception e) {
                    // Skip entries that fail to load
                }
            }
        }
        
        return candidates.get(0);
    }

    /**
     * Fallback method when timestamp parsing fails - loads all entries and tries to find the entry.
     */
    private void fallbackFullReload(String timestamp, String targetContent, LogListPanel logListPanel, 
                                    DefaultListModel<String> listModel, JList<String> logList) {
        LoadingProgressDialog progress = new LoadingProgressDialog(editor, "Loading");
        final javax.swing.Timer showTimer = new javax.swing.Timer(150, ev -> progress.show());
        showTimer.setRepeats(false);
        showTimer.start();

        Thread loader = new Thread(() -> {
            try {
                editor.loadLogEntries();
                SwingUtilities.invokeLater(() -> {
                    int idx = findMatchingEntry(listModel, timestamp, targetContent);
                    if (idx >= 0) {
                        logList.setSelectedIndex(idx);
                        logList.ensureIndexIsVisible(idx);
                        SwingUtilities.invokeLater(() -> logListPanel.getEntryArea().requestFocusInWindow());
                    } else {
                        DialogHelper.showEntryNotFound(this);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> logFileHandler.showErrorDialog(
                    "<html><b>🔄 Load Failed</b><br><br>Unable to load log entries.</html>"));
            } finally {
                if (showTimer.isRunning()) showTimer.stop();
                SwingUtilities.invokeLater(() -> progress.close());
            }
        }, "FallbackEntryLoader");
        loader.setDaemon(true);
        loader.start();
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

    public void dispose() {
        if (timestampClickHandler != null) {
            timestampClickHandler.dispose();
            timestampClickHandler = null;
        }
        if (this.cacheInvalidationListener != null) {
            this.logFileHandler.removeCacheInvalidationListener(this.cacheInvalidationListener);
            this.cacheInvalidationListener = null;
        }
    }
}
