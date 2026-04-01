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
            if (selectedItem != null) {
                // Check if file is encrypted and show enhanced warning
                if (logFileHandler.isEncrypted()) {
                    if (!clipboard.ClipboardSecurityWarner.showEncryptedFileWarning(editor)) {
                        return; // User chose not to copy
                    }
                }

                //Copy both timestamp and entry text
                var rawTs = logFileHandler.getRawTimestamp(selectedItem);
                String logContent = logFileHandler.loadEntry(rawTs);
                clipboard.SecureClipboardManager.getInstance().copySecureTextToClipboard(
                    selectedItem + "\n\n" + logContent, editor,
                    "Log entry copied to clipboard securely.");
            }
        };
    }

    public ActionListener createNewQuickEntryAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String[] newEntryHolder = new String[1];
                try {
                    SwingUtilities.invokeAndWait(() -> newEntryHolder[0] = (String) JOptionPane.showInputDialog(
                            editor,
                            "Enter new log entry:",
                            "New Log Entry",
                            JOptionPane.PLAIN_MESSAGE));
                } catch (Exception ex) {
                    newEntryHolder[0] = null;
                }
                String newEntry = newEntryHolder[0];
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
        if (selectedItem == null) return;

        // selectedItem contains the exact timestamp from the file (including any duplicate suffix like " (1)")
        logFileHandler.updateEntry(selectedItem, logListPanel.getEntryArea().getText());

        // Flush writes asynchronously to avoid blocking UI on large files
        logFileHandler.flushPendingWritesAsync(() -> {
            // Preserve the selection after reloading by finding the updated item
            try {
                editor.loadLogEntries();
                // Find and reselect the updated item
                for (int i = 0; i < listModel.getSize(); i++) {
                    if (selectedItem.equals(listModel.getElementAt(i))) {
                        logList.setSelectedIndex(i);
                        logList.ensureIndexIsVisible(i);
                        break;
                    }
                }
            } catch (Exception e) {
                // Security: Don't expose internal error details
                logFileHandler.showErrorDialog("<html><b>🔄 Reload Failed</b><br><br>Unable to reload log entries after update.<br><br><i>Tip: The update may have succeeded, but the list couldn't refresh.</i></html>");
            }
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

        final String[] newDateTimeHolder = new String[1];
        try {
            SwingUtilities.invokeAndWait(() -> newDateTimeHolder[0] = (String) JOptionPane.showInputDialog(editor, "Enter new date and time (format: HH:mm yyyy-MM-dd):", selectedItem));
        } catch (Exception ex) {
            newDateTimeHolder[0] = null;
        }
        String newDateTime = newDateTimeHolder[0];
        if (newDateTime == null) return;
        if (newDateTime.isBlank()) {
            DialogHelper.showError(editor, "Error", "Invalid Input", "Date and time cannot be empty.");
            return;
        }

        // validate
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");
            java.time.LocalDateTime.parse(newDateTime.trim(), formatter);
        } catch (Exception e) {
            DialogHelper.showError(editor, "Error", "Invalid Format", "Invalid format. Use HH:mm yyyy-MM-dd");
            return;
        }

        // calculate unique timestamp
        int count = logFileHandler.getDuplicateCount(newDateTime.trim());
        String uniqueNewTimestamp = count > 0 ? newDateTime.trim() + " (" + count + ")" : newDateTime.trim();

        // update
        logFileHandler.changeTimestamp(selectedItem, uniqueNewTimestamp, listModel);
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