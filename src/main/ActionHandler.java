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

import filehandling.LogFileHandler;
import gui.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
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
                // Check if file is encrypted and warn user
                if (logFileHandler.isEncrypted()) {
                    Object[] options = {"Copy anyway", "Don't copy to clipboard"};
                    int confirm = JOptionPane.showOptionDialog(
                        editor,
                        "<html><b>Security Information:</b><br><br>" +
                        "This log file is encrypted. The copied text will be <b>unencrypted</b> in the clipboard.<br>" +
                        "Clipboard contents may be accessible to other applications.<br><br>" +
                        "Are you sure you want to copy this entry to the clipboard?</html>",
                        "Copy to Clipboard - Encrypted File",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[1]);

                    if (confirm != JOptionPane.YES_OPTION) {
                        return; // User chose not to copy
                    }
                }

                //Copy both timestamp and entry text
                String logContent = logFileHandler.loadEntry(selectedItem);
                clipboard.ClipboardManager.copyLogEntryToClipboard(selectedItem, logContent, editor);
                //show a small popup message "Copied to clipboard"
                JOptionPane.showMessageDialog(
                        editor,
                        "Log entry copied to clipboard.",
                        "Copied",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
    }

    public ActionListener createNewQuickEntryAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newEntry = JOptionPane.showInputDialog(
                        editor,
                        "Enter new log entry:",
                        "New Log Entry",
                        JOptionPane.PLAIN_MESSAGE);
                if (newEntry != null && !newEntry.isBlank()) {
                    logFileHandler.saveText(newEntry, listModel);
                    try {
                        editor.loadLogEntries();
                        fullLogPanel.loadFullLog();
                        SystemTrayMenu.updateRecentLogsMenu();
                    } catch (Exception ex) {
                        logFileHandler.showErrorDialog("Error refreshing data: " + ex.getMessage());
                    }
                }
            }
        };
    }

    public void saveEditedLogEntry() {
        if (editor.isLocked()) {
            JOptionPane.showMessageDialog(editor, "File is locked. Press Unlock file in Full log view to unlock it again.", "Locked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String selectedItem = logList.getSelectedValue();
        if (selectedItem == null) return;

        logFileHandler.updateEntry(selectedItem, logListPanel.getEntryArea().getText());
        editor.updateLogListView();
        logList.setSelectedValue(selectedItem, true);
        fullLogPanel.loadFullLog();
        SystemTrayMenu.updateRecentLogsMenu();
        Toast.showToast(editor, "Entry updated successfully!");
    }

    public void saveLogEntry() {
        if (editor.isLocked()) {
            JOptionPane.showMessageDialog(editor, "File is locked. Press Unlock file in Full log view to unlock it again.", "Locked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        logFileHandler.saveText(editor.getEntryPanel().getTextArea().getText(), listModel);
        editor.getEntryPanel().getTextArea().setText("");
        editor.updateLogListView();
        fullLogPanel.loadFullLog(); // update full log view after save
        SystemTrayMenu.updateRecentLogsMenu();
        Toast.showToast(editor, "Entry saved successfully!");
    }

    public void deleteSelectedEntry() {
        if (editor.isLocked()) {
            JOptionPane.showMessageDialog(editor, "File is locked. Press Unlock file in Full log view to unlock it again.", "Locked", JOptionPane.WARNING_MESSAGE);
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
            String entryText = logFileHandler.loadEntry(selectedItem);
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

        int confirm = JOptionPane.showConfirmDialog(editor,
                panel,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // Delete all selected entries
            for (String selectedItem : selectedItems) {
                logFileHandler.deleteEntry(selectedItem, listModel);
            }
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
            JOptionPane.showMessageDialog(editor, "File is locked. Press Unlock file in Full log view to unlock it again.", "Locked", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String selectedItem = logList.getSelectedValue();
        if (selectedItem == null) return;

        String newDateTime = JOptionPane.showInputDialog(editor, "Enter new date and time (format: HH:mm yyyy-MM-dd):", selectedItem);
        if (newDateTime == null) return;
        if (newDateTime.trim().isEmpty()) {
            JOptionPane.showMessageDialog(editor, "Date and time cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // validate
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd");
            java.time.LocalDateTime.parse(newDateTime.trim(), formatter);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(editor, "Invalid format. Use HH:mm yyyy-MM-dd", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // update
        logFileHandler.changeTimestamp(selectedItem, newDateTime.trim(), listModel);
        // reload full log and update menu
        fullLogPanel.loadFullLog();
        SystemTrayMenu.updateRecentLogsMenu();
    }
}