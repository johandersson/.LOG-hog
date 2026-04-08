package main;

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



import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import filehandling.LogFileHandler;
import gui.DialogHelper;
import gui.FullLogPanel;
import gui.LogListPanel;
import gui.SystemTrayMenu;
import utils.Toast;

public class ActionHandler {
    private final LogTextEditor editor;
    private final LogFileHandler logFileHandler;
    private final JList<String> logList;
    private final DefaultListModel<String> listModel;
    private LogListPanel logListPanel;
    private FullLogPanel fullLogPanel;

    public ActionHandler(LogTextEditor editor, LogFileHandler logFileHandler,
                        JList<String> logList, DefaultListModel<String> listModel) {
        this.editor = editor;
        this.logFileHandler = logFileHandler;
        this.logList = logList;
        this.listModel = listModel;
    }

    public void setPanels(LogListPanel logListPanel, FullLogPanel fullLogPanel) {
        this.logListPanel = logListPanel;
        this.fullLogPanel = fullLogPanel;
    }

    public ActionListener createCopyLogEntryAction() {
        return e -> {
            String selectedItem = logList.getSelectedValue();
            // Debug: show which item the menu action is attempting to copy
            try {
                String dbg = selectedItem == null ? "(no selection)" : selectedItem;
                Toast.showToast(editor, "Copy action invoked for: " + dbg);
            } catch (Exception ignored) {}
            if (selectedItem != null) {
                // Check if file is encrypted and show enhanced warning
                if (logFileHandler.isEncrypted()) {
                    if (!clipboard.ClipboardSecurityWarner.showEncryptedFileWarning(editor)) {
                        return; // User chose not to copy
                    }
                }

                //Copy both timestamp and entry text
                try {
                    var rawTs = logFileHandler.getRawTimestamp(selectedItem);
                    String logContent = logFileHandler.loadEntry(rawTs);

                    // Show debug dialog with internal values to help diagnose copy issues
                    String snippet = "(no content)";
                    int len = 0;
                    if (logContent != null && !logContent.isEmpty()) {
                        len = logContent.length();
                        snippet = logContent.length() > 200 ? logContent.substring(0, 200) + "..." : logContent;
                    }
                    // Basic HTML-escape to avoid breaking the dialog when showing raw snippets
                    String escaped = snippet.replace("&", "&amp;").replace("<", "&lt;")
                        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
                    String details = "Raw timestamp: " + rawTs + "<br>Length: " + len + "<br><br>Snippet:<br>" + escaped;
                    DialogHelper.showInfo(editor, "Copy Debug", "Preparing to copy entry:", details);

                    clipboard.SecureClipboardManager.getInstance().copySecureTextToClipboard(
                        selectedItem + "\n\n" + logContent, editor,
                        "Log entry copied to clipboard securely.");
                } catch (Exception ex) {
                    // If anything goes wrong, show a user-friendly message
                    DialogHelper.showError(editor, "Copy Failed", "Unable to copy the selected entry.", null);
                }
            }
        };
    }

    public ActionListener createNewQuickEntryAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Show dialog directly - we're already on the EDT from ActionListener
                String newEntry = (String) JOptionPane.showInputDialog(
                        editor,
                        "Enter new log entry:",
                        "New Log Entry",
                        JOptionPane.PLAIN_MESSAGE);
                if (newEntry != null && !newEntry.isBlank()) {
                    // Save asynchronously so UI remains responsive for large files
                    logFileHandler.saveTextAsync(newEntry, listModel, () -> {
                        try {
                            editor.loadLogEntries();
                            fullLogPanel.loadFullLog();
                            SystemTrayMenu.updateRecentLogsMenu();
                        } catch (Exception ex) {
                            // Security: Don't expose internal error details
                            logFileHandler.showErrorDialog("<html><b>🔄 Refresh Failed</b><br><br>Unable to refresh log data after save.<br><br><i>Tip: Try reloading the application.</i></html>");
                        }
                    });
                }
            }
        };
    }

    public void saveEditedLogEntry() {
        if (editor.isLocked()) {
            DialogHelper.showFileLocked(editor);
            return;
        }
        String selectedItem = logList.getSelectedValue();
        // If nothing is selected (edge cases when jumping from Full Log), try to
        // identify the entry by matching current editor text to list contents.
        if (selectedItem == null) {
            String currentText = logListPanel.getEntryArea().getText();
            if (currentText == null || currentText.isBlank()) {
                DialogHelper.showEntryNotFound(editor);
                return;
            }
            // Try to find an entry with identical content (trimmed comparison)
            for (int i = 0; i < listModel.getSize(); i++) {
                String candidate = listModel.getElementAt(i);
                try {
                    String rawTs = logFileHandler.getRawTimestamp(candidate);
                    String content = logFileHandler.loadEntry(rawTs);
                    if (content != null && content.trim().equals(currentText.trim())) {
                        selectedItem = candidate;
                        logList.setSelectedIndex(i);
                        logList.ensureIndexIsVisible(i);
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
            if (selectedItem == null) {
                // Try to use the timestamp remembered when opening from Full Log
                String remembered = editor.getCurrentEditedDisplayTimestamp();
                if (remembered != null && !remembered.isBlank()) {
                    selectedItem = remembered;
                } else {
                    DialogHelper.showEntryNotFound(editor);
                    return;
                }
            }
        }

        // selectedItem contains display timestamp (may include suffix like " (1)")
        // updateEntry handles parsing to find correct occurrence in file
        logFileHandler.updateEntry(selectedItem, logListPanel.getEntryArea().getText());

        // Make a final copy for use inside the async lambda (must be effectively final)
        final String selectedCopy = selectedItem;

        // Flush writes asynchronously to avoid blocking UI on large files
        logFileHandler.flushPendingWritesAsync(() -> {
            // Do NOT reload the entire entry list (this would reset filters).
            // Instead, reselect the updated item in the existing model when possible.
            try {
                boolean reselected = false;
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (selectedCopy.equals(listModel.getElementAt(i))) {
                        final int selIndex = i;
                        SwingUtilities.invokeLater(() -> {
                            logList.setSelectedIndex(selIndex);
                            logList.ensureIndexIsVisible(selIndex);
                        });
                        reselected = true;
                        break;
                    }
                }
                if (!reselected) {
                    // If the item is no longer present (rare), fall back to reloading safely
                    try {
                        editor.loadLogEntries();
                    } catch (Exception ignore) {}
                }
            } catch (Exception e) {
                // If anything goes wrong, avoid resetting filters; just show a toast
            }
            // Refresh Full Log view to reflect updated content
            fullLogPanel.loadFullLog();
            SystemTrayMenu.updateRecentLogsMenu();
            Toast.showToast(editor, "Entry updated successfully!");
        });
    }

    public void saveLogEntry() {
        if (editor.isLocked()) {
            DialogHelper.showFileLocked(editor);
            return;
        }
        // Save asynchronously to avoid UI freeze
        String textToSave = editor.getEntryPanel().getTextArea().getText();
        logFileHandler.saveTextAsync(textToSave, listModel, () -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                editor.getEntryPanel().getTextArea().setText("");
                editor.updateLogListView();
                fullLogPanel.loadFullLog(); // update full log view after save
                SystemTrayMenu.updateRecentLogsMenu();
                Toast.showToast(editor, "Entry saved successfully!");
            });
        });
    }

    public void deleteSelectedEntry() {
        if (editor.isLocked()) {
            DialogHelper.showFileLocked(editor);
            return;
        }
        List<String> selectedItems = logList.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        int numEntries = selectedItems.size();
        String title = numEntries == 1 ? "Delete Entry" : "Delete " + numEntries + " Entries";
        String questionText = numEntries == 1 ?
            "Are you sure you want to delete this entry?" :
            "Are you sure you want to delete these " + numEntries + " entries?";

        // Build preview entries: each is a List<String> with timestamp and trimmed text
        java.util.List<java.util.List<String>> previewEntries = new java.util.ArrayList<>();
        for (String selectedItem : selectedItems) {
            var rawTs = logFileHandler.getRawTimestamp(selectedItem);
            String entryText = logFileHandler.loadEntry(rawTs);
            String previewBody;
            if (entryText == null || entryText.isBlank()) {
                previewBody = "(no content)";
            } else {
                String trimmed = entryText.length() > 200 ? entryText.substring(0, 200) + "..." : entryText;
                previewBody = trimmed;
            }
            java.util.List<String> entryPreview = new java.util.ArrayList<>();
            entryPreview.add(selectedItem); // timestamp
            entryPreview.add(previewBody);  // trimmed text
            previewEntries.add(entryPreview);
        }

        // Use a JTextPane and MarkdownRenderer for consistent preview styling
        gui.HighlightableTextPane previewPane = new gui.HighlightableTextPane();
        previewPane.setEditable(false);
        previewPane.setBackground(javax.swing.UIManager.getColor("Panel.background"));
        markdown.MarkdownRenderer.renderMarkdownFromEntries(previewPane, previewEntries, false);

        JScrollPane scrollPane = new JScrollPane(previewPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new java.awt.Dimension(600, Math.min(400, 200 + numEntries * 50)));

        // Compose dialog content: question label above preview
        JPanel panel = new JPanel(new java.awt.BorderLayout(6, 6));
        JLabel question = new JLabel("<html><b>" + questionText + "</b></html>");
        panel.add(question, java.awt.BorderLayout.NORTH);
        panel.add(scrollPane, java.awt.BorderLayout.CENTER);

        int result = javax.swing.JOptionPane.showConfirmDialog(editor, panel, title,
                javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);

        if (result == javax.swing.JOptionPane.YES_OPTION) {
            // Use batch delete for efficiency (single file I/O instead of N operations)
            logFileHandler.deleteLogEntries(selectedItems, listModel);
            editor.updateLogListView();
            //select top if any
            editor.selectFirstLogIfAny();
            fullLogPanel.loadFullLog(); // update full log view after deletion
            SystemTrayMenu.updateRecentLogsMenu();
            String successMessage = numEntries == 1 ? "Entry deleted successfully!" : numEntries + " entries deleted successfully!";
            Toast.showToast(editor, successMessage);
        }
    }

    public void editDateTime() {
        if (editor.isLocked()) {
            DialogHelper.showFileLocked(editor);
            return;
        }
        String selectedItem = logList.getSelectedValue();
        if (selectedItem == null) return;

        // Get raw timestamp without display suffix for editing
        String rawTimestamp = logFileHandler.getRawTimestamp(selectedItem);
        String newDateTime = rawTimestamp;
        
        // Loop until valid input or cancel
        while (true) {
            newDateTime = (String) JOptionPane.showInputDialog(editor, 
                "Enter new date and time (format: HH:mm yyyy-MM-dd):", newDateTime);
            if (newDateTime == null) return; // User cancelled
            
            if (newDateTime.isBlank()) {
                DialogHelper.showError(editor, "Error", "Invalid Input", "Date and time cannot be empty.");
                continue;
            }

            // Validate format
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");
                java.time.LocalDateTime.parse(newDateTime.trim(), formatter);
                break; // Valid input, exit loop
            } catch (Exception e) {
                DialogHelper.showError(editor, "Error", "Invalid Format", "Invalid format. Use HH:mm yyyy-MM-dd");
                // Continue loop to let user try again
            }
        }

        // Store plain timestamp (no suffix) - suffixes are display-only
        String plainTimestamp = newDateTime.trim();

        // update
        logFileHandler.changeTimestamp(selectedItem, plainTimestamp, listModel);
        try {
            editor.loadLogEntries();
        } catch (Exception e) {
            // Failed to reload entries after timestamp change
        }
        // reload full log and update menu
        fullLogPanel.loadFullLog();
        SystemTrayMenu.updateRecentLogsMenu();
    }
}