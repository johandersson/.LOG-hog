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

package main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import filehandling.LogFileHandler;
import gui.DialogHelper;
import gui.LoadingProgressDialog;
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
                    logFileHandler.saveText(newEntry, listModel);
                    try {
                        editor.loadLogEntries();
                        fullLogPanel.loadFullLog();
                        SystemTrayMenu.updateRecentLogsMenu();
                    } catch (Exception ex) {
                        // Security: Don't expose internal error details
                        logFileHandler.showErrorDialog("<html><b>🔄 Refresh Failed</b><br><br>Unable to refresh log data after save.<br><br><i>Tip: Try reloading the application.</i></html>");
                    }
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

        logFileHandler.updateEntry(selectedItem, logListPanel.getEntryArea().getText());
        
        // Flush writes immediately on explicit save
        logFileHandler.flushPendingWrites();

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
    }

    public void saveLogEntry() {
        if (editor.isLocked()) {
            DialogHelper.showFileLocked(editor);
            return;
        }
        logFileHandler.saveText(editor.getEntryPanel().getTextArea().getText(), listModel);
        editor.getEntryPanel().getTextArea().setText("");
        // Perform expensive reloads off the EDT to avoid freezing the UI.
        LoadingProgressDialog progress = new LoadingProgressDialog(editor, "Loading");
        final javax.swing.Timer showTimer = new javax.swing.Timer(150, ev -> progress.show());
        showTimer.setRepeats(false);
        showTimer.start();

        Thread reloadThread = new Thread(() -> {
            try {
                // Reload list model from disk (may be heavy) off-EDT
                editor.loadLogEntries();

                // Ensure list model is applied on EDT
                SwingUtilities.invokeLater(() -> editor.updateLogListView());

                // Refresh full log view (it runs parsing in background internally)
                fullLogPanel.loadFullLog(() -> {
                    // Close progress dialog on EDT when full log load completes
                    SwingUtilities.invokeLater(() -> {
                        if (showTimer.isRunning()) showTimer.stop();
                        progress.close();
                        SystemTrayMenu.updateRecentLogsMenu();
                        Toast.showToast(editor, "Entry saved successfully!");
                    });
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    if (showTimer.isRunning()) showTimer.stop();
                    progress.close();
                    logFileHandler.showErrorDialog("<html><b>🔄 Reload Failed</b><br><br>Unable to refresh log data after save.</html>");
                });
            }
        }, "SaveReloadThread");
        reloadThread.setDaemon(true);
        reloadThread.start();
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

        // Build preview for all selected entries
        StringBuilder previewBuilder = new StringBuilder();
        for (int i = 0; i < selectedItems.size(); i++) {
            String selectedItem = selectedItems.get(i);
            var rawTs = logFileHandler.getRawTimestamp(selectedItem);
            String entryText = logFileHandler.loadEntry(rawTs);
            String previewBody;
            if (entryText == null || entryText.isBlank()) {
                previewBody = "(no content)";
            } else {
                String trimmed = entryText.length() > 200 ? entryText.substring(0, 200) + "..." : entryText;
                previewBody = trimmed;
            }

            if (i > 0) previewBuilder.append("\n\n---\n\n");
            previewBuilder.append(selectedItem).append("\n\n").append(previewBody);
        }

        JTextArea previewArea = PreviewDialog.createPreviewArea(previewBuilder.toString());

        // Compose dialog content: question label above preview
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JLabel question = new JLabel(questionText);
        panel.add(question, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(previewArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(600, Math.min(400, 200 + numEntries * 50))); // Adjust height based on number of entries
        panel.add(scrollPane, BorderLayout.CENTER);

        boolean confirm = gui.DialogHelper.confirm(editor, title, questionText);

        if (confirm) {
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
        if (newDateTime.trim().isEmpty()) {
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