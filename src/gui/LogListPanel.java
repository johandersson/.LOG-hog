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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JTextPane;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import filehandling.LogFileHandler;
import filehandling.DialogHandler;
import main.LogTextEditor;
import markdown.LinkHandler;
import markdown.MarkdownRenderer;





public class LogListPanel extends JPanel {
    private final LogTextEditor editor;
    private final LogFileHandler logFileHandler;
    private final DefaultListModel<String> listModel;
    private final JList<String> logList;
    private final JProgressBar filterProgressBar;
    private final JPanel filterPanel;
    private final JComboBox<Integer> yearCombo;
    private final JComboBox<String> monthCombo;
    private boolean suppressFilterEvents;
    private boolean isPreviewMode;

    // Additional UI fields required by the panel
    private final JPanel entryContainer = new JPanel(new BorderLayout());
    private final JTextArea entryArea = new JTextArea();
    private final JScrollPane entryScroll = new JScrollPane(entryArea);
    private final JTextPane previewPane = new JTextPane();
    private final JScrollPane previewScrollPane = new JScrollPane(previewPane);
    private final JLabel lockLabel = new JLabel();
    private final JProgressBar entryProgressBar = new JProgressBar();

    // Remove any duplicate constructors below this line
    // (Removed duplicate constructor)

    // Add stub for updateCharCountLabel if missing
    private void updateCharCountLabel(JLabel label, String text) {
        if (label != null && text != null) {
            label.setText(text.length() + " chars");
        }
    }

    public LogListPanel(LogTextEditor editor, LogFileHandler logFileHandler, DefaultListModel<String> listModel, JList<String> logList) {
        this.editor = editor;
        this.logFileHandler = logFileHandler;
        this.listModel = listModel;
        this.logList = logList;
        this.filterProgressBar = new JProgressBar();
        this.filterPanel = new JPanel();
        this.yearCombo = new JComboBox<>();
        this.monthCombo = new JComboBox<>();
        // Setup filter panel
        filterProgressBar.setPreferredSize(new Dimension(120, 16));
        filterPanel.add(filterProgressBar);
        yearCombo.addActionListener(e -> applyFilter(yearCombo, monthCombo));
        monthCombo.addActionListener(e -> applyFilter(yearCombo, monthCombo));
        // ...rest of constructor code...
    }

    private void applyFilter(JComboBox<Integer> yearCombo, JComboBox<String> monthCombo) {
        if (suppressFilterEvents) return;
        // Don't filter if locked - controls should be disabled but add extra safety
        if (editor.isLocked()) {
            return;
        }
        
        var year = (Integer) yearCombo.getSelectedItem();
        var monthIndex = monthCombo.getSelectedIndex();
        if (year == null) return;

        // Provide quick feedback: show an indeterminate progress bar while computing off-EDT
        SwingUtilities.invokeLater(() -> {
            listModel.removeAllElements();
            filterProgressBar.setVisible(true);
        });

        // Compute filtered timestamps off the EDT and update model on completion
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                try {
                    if (monthIndex == 0) {
                        return logFileHandler.getEntryLoader().computeTimestampsByYear(year);
                    } else {
                        int month = monthIndex; // offset already considered in UI
                        return logFileHandler.getEntryLoader().computeTimestampsByYearMonth(year, month);
                    }
                } catch (IllegalStateException ise) {
                    // Propagate to done() to show limit dialog on EDT
                    throw ise;
                }
            }

            @Override
            protected void done() {
                try {
                    List<String> filtered = get();
                    listModel.removeAllElements();
                    filterProgressBar.setVisible(false);
                    if (filtered == null || filtered.isEmpty()) {
                        // Keep empty list if nothing found
                        return;
                    }
                    for (String ts : filtered) {
                        listModel.addElement(ts);
                    }
                } catch (java.util.concurrent.ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    if (cause instanceof IllegalStateException) {
                        // File too large - show friendly dialog handled by DialogHandler
                        DialogHandler.showLimitExceeded("File Too Large", "The log file exceeds configured limits and cannot be filtered.");
                    } else {
                        logFileHandler.showErrorDialog("<html><b>📅 Filter Failed</b><br><br>Unable to apply date filter.<br><br><i>Tip: Check the selected year and month.</i></html>");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /**
     * Programmatically set the filter selection without triggering the filter action.
     * @param year year value (e.g. 2026)
     * @param month month value 1..12 (use 0 to select "All Months")
     */
    public void setFilterSelection(int year, int month) {
        suppressFilterEvents = true;
        try {
            if (yearCombo != null) {
                yearCombo.setSelectedItem(year);
            }
            if (monthCombo != null) {
                // month param uses 1..12 where 0 means "All Months"
                int idx = Math.max(0, Math.min(month, 12));
                monthCombo.setSelectedIndex(idx);
            }
        } finally {
            suppressFilterEvents = false;
        }
    }

    private void loadAndDisplayEntry(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            entryArea.setText("");
            if (isPreviewMode) renderPreview();
            return;
        }

        // Load heavy content off the EDT and update the editor on completion
        entryProgressBar.setIndeterminate(true);
        entryProgressBar.setVisible(true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return logFileHandler.loadEntry(timestamp);
            }

            @Override
            protected void done() {
                try {
                    String content = get();
                    entryArea.setText(content != null ? content : "");
                    entryProgressBar.setVisible(false);
                    if (isPreviewMode) renderPreview();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException ee) {
                    logFileHandler.showErrorDialog("<html><b>👁️ Display Failed</b><br><br>Unable to display the log entry.</html>");
                }
            }
        }.execute();
    }

    private void selectFirstLogIfAny() {
        if (listModel.getSize() > 0) {
            logList.setSelectedIndex(0);
            logList.ensureIndexIsVisible(0);
            var item = listModel.getElementAt(0);
            loadAndDisplayEntry(item);
        } else {
            logList.clearSelection();
            entryArea.setText("");
            if (isPreviewMode) renderPreview();
        }
    }

    public DefaultListModel<String> getListModel() {
        return listModel;
    }

    public JList<String> getLogList() {
        return logList;
    }

    public JTextArea getEntryArea() {
        return entryArea;
    }

    // Removed unused private method updateInfoFromListModel (PMD)

    // Removed invalid @Override
    protected void togglePreview(javax.swing.JButton toggleBtn) {
        if (isPreviewMode) {
            // Switch to edit mode
            entryContainer.remove(previewScrollPane);
            entryContainer.add(entryScroll, BorderLayout.CENTER);
            toggleBtn.setText("Preview");
            isPreviewMode = false;
        } else {
            // Switch to preview mode
            renderPreview();
            entryContainer.remove(entryScroll);
            entryContainer.add(previewScrollPane, BorderLayout.CENTER);
            toggleBtn.setText("Edit");
            isPreviewMode = true;
        }
        entryContainer.revalidate();
        entryContainer.repaint();
    }

    private void renderPreview() {
        String content = entryArea.getText().trim();
        if (content.isEmpty()) {
            previewPane.setText("No content to preview");
            previewPane.setContentType("text/plain");
            return;
        }

        // Use Arrays.asList instead of tight loop
        java.util.List<String> entryLines = java.util.Arrays.asList(content.split("\n"));

        // Wrap in a list of entries (single entry)
        java.util.List<java.util.List<String>> entries = new java.util.ArrayList<>();
        entries.add(entryLines);

        // Render using MarkdownRenderer
        MarkdownRenderer.renderMarkdownFromEntries(previewPane, entries, false);
        LinkHandler.addLinkListeners(previewPane);
    }

    public void setLocked(boolean locked) {
        entryArea.setEditable(!locked);
        
        // Disable filter controls when locked
        if (yearCombo != null) {
            yearCombo.setEnabled(!locked);
        }
        if (monthCombo != null) {
            monthCombo.setEnabled(!locked);
        }
        
        if (locked) {
            entryArea.setText("");
            // Switch back to edit mode if in preview mode
            if (isPreviewMode) {
                entryContainer.remove(previewScrollPane);
                isPreviewMode = false;
            } else {
                entryContainer.remove(entryScroll);
            }
            entryContainer.add(lockLabel, BorderLayout.CENTER);
        } else {
            entryContainer.remove(lockLabel);
            entryContainer.add(entryScroll, BorderLayout.CENTER);
        }
        entryContainer.revalidate();
        entryContainer.repaint();
    }
}
