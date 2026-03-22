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
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JProgressBar;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.AbstractDocument;

import filehandling.LogFileHandler;
import filehandling.DialogHandler;
import main.LogTextEditor;
import markdown.LinkHandler;
import markdown.MarkdownRenderer;
import utils.UndoRedoTextArea;

public class LogListPanel extends JPanel {
    private final JList<String> logList;
    private final DefaultListModel<String> listModel;
    private final JTextArea entryArea;
    private final LogFileHandler logFileHandler;
    private final LogTextEditor editor;
    private final JScrollPane entryScroll;
    private final JLabel lockLabel;
    private final JPanel entryContainer;
    private final HighlightableTextPane previewPane;
    private final JScrollPane previewScrollPane;
    private boolean isPreviewMode = false;
    private JComboBox<Integer> yearCombo;
    private JComboBox<String> monthCombo;
    private final LogInfoPanel infoPanel;
    private final JProgressBar filterProgressBar;
    private final JProgressBar entryProgressBar;

    public LogListPanel(LogTextEditor editor, LogFileHandler logFileHandler, DefaultListModel<String> listModel, JList<String> logList) {
        this.editor = editor;
        this.logFileHandler = logFileHandler;
        this.listModel = listModel;
        this.logList = logList;
        this.entryArea = new UndoRedoTextArea();
        this.entryScroll = new JScrollPane(entryArea);
        this.lockLabel = new JLabel("File locked. Press Unlock file in Full log view to unlock it again.", SwingConstants.CENTER);
        this.entryContainer = new JPanel(new BorderLayout());
        this.previewPane = new HighlightableTextPane();
        this.previewScrollPane = new JScrollPane(previewPane);
        this.infoPanel = new LogInfoPanel();
        this.filterProgressBar = new JProgressBar();
        this.entryProgressBar = new JProgressBar();
        initPanel();
    }

    private void updateCharCountLabel(javax.swing.JLabel label, String text) {
        int len = text == null ? 0 : text.length();
        int left = Math.max(0, InputLimits.ENTRY_MAX_CHARS - len);
        label.setText("Chars left: " + left);
        if (left <= 100) {
            label.setForeground(new java.awt.Color(0xA00000));
        } else {
            label.setForeground(java.awt.Color.GRAY);
        }
    }

    private void initPanel() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Color.WHITE);

        // Top: filter controls
        var filterPanel = createFilterPanel();
        add(filterPanel, BorderLayout.NORTH);

        // Center: list and editor pane in a split
        var split = createLogSplitPane();
        add(split, BorderLayout.CENTER);

        // Bottom: info panel (reuse same LogInfoPanel used in FullLog view)
        var bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        var infoWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        infoWrapper.setOpaque(false);
        infoWrapper.add(infoPanel);
        bottom.add(infoWrapper, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);

        setupListeners(split);

        // Populate initial info from the current listModel (most recent view)
        updateInfoFromListModel();

        // Indicate scope in the info panel
        infoPanel.updateYearScope("Most recent (from list view)");
    }

    private JPanel createFilterPanel() {
        var filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        filterPanel.setOpaque(false);
        var filterLabel = new JLabel("Filter on date");
        filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));
        filterPanel.add(filterLabel);

        // Populate year combo from the currently displayed entries in the list (most recent view)
        java.util.Set<Integer> yearsSet = new java.util.LinkedHashSet<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            try {
                String ts = listModel.getElementAt(i);
                java.time.LocalDateTime dt = utils.DateHandler.parseTimestamp(ts);
                yearsSet.add(dt.getYear());
            } catch (Exception ignored) {
            }
        }
        Integer[] yearsArr;
        if (!yearsSet.isEmpty()) {
            yearsArr = yearsSet.toArray(new Integer[0]);
        } else {
            var currentYear = Year.now().getValue();
            yearsArr = IntStream.rangeClosed(currentYear, currentYear).boxed().toArray(Integer[]::new);
        }
        yearCombo = new JComboBox<>(yearsArr);
        // Select the most recent year by default if present
        if (yearsArr.length > 0) yearCombo.setSelectedItem(yearsArr[0]);
        filterPanel.add(yearCombo);

        var months = new String[]{
                "All Months",
                "01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr",
                "05 - May", "06 - Jun", "07 - Jul", "08 - Aug",
                "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec"
        };
        monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(LocalDate.now().getMonthValue()); // Offset by 1 due to "All Months" at index 0
        filterPanel.add(monthCombo);

        // Lightweight non-blocking progress indicator for filters
        filterProgressBar.setIndeterminate(true);
        filterProgressBar.setVisible(false);
        filterProgressBar.setPreferredSize(new Dimension(120, 16));
        filterPanel.add(filterProgressBar);

        // Filter actions
        yearCombo.addActionListener(e -> applyFilter(yearCombo, monthCombo));
        monthCombo.addActionListener(e -> applyFilter(yearCombo, monthCombo));

        return filterPanel;
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

    private JSplitPane createLogSplitPane() {
        var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.33);
        split.setBorder(null);
        split.setDividerSize(1);

        logList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logList.setModel(listModel);
        logList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        logList.setBackground(Color.WHITE);

        var listScroll = new JScrollPane(logList);
        listScroll.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        listScroll.setBackground(Color.WHITE);
        listScroll.getViewport().setBackground(Color.WHITE);

        var entryContainer = this.entryContainer;
        entryContainer.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        entryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        entryArea.setLineWrap(true);
        entryArea.setWrapStyleWord(true);
        // Enforce maximum entry length in the editor
        try {
            if (entryArea.getDocument() instanceof AbstractDocument) {
                ((AbstractDocument) entryArea.getDocument()).setDocumentFilter(new LengthLimitFilter(InputLimits.ENTRY_MAX_CHARS));
            }
        } catch (Exception ignore) {
        }
        entryArea.setBackground(Color.WHITE);
        entryScroll.setPreferredSize(new Dimension(600, 220));
        entryScroll.setBorder(BorderFactory.createEmptyBorder());
        entryContainer.add(entryScroll, BorderLayout.CENTER);

        // Setup preview pane
        previewPane.setEditable(false);
        previewScrollPane.setPreferredSize(new Dimension(600, 220));
        previewScrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Add formatting buttons panel
        var formattingPanel = new FormattingPanel(entryArea);
        entryContainer.add(formattingPanel, BorderLayout.NORTH);

        lockLabel.setForeground(Color.GRAY);
        // Initially not added

        split.setLeftComponent(listScroll);
        split.setRightComponent(entryContainer);

        // Key bindings
        entryArea.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK), "saveEntry");
        entryArea.getActionMap().put("saveEntry", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                editor.saveEditedLogEntry();
            }
        });

        // Override ctrl+c to use secure clipboard
        entryArea.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK), "copySecure");
        entryArea.getActionMap().put("copySecure", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                String selectedText = entryArea.getSelectedText();
                if (selectedText != null && !selectedText.isEmpty()) {
                    clipboard.SecureClipboardManager.getInstance().copySecureTextToClipboard(selectedText, entryArea);
                }
            }
        });

        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK), "newEntryGlobal");
        editor.getRootPane().getActionMap().put("newEntryGlobal", editor.createNewQuickEntry());

        // Save button
        var entryBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        entryBottom.setOpaque(false);

        // Character counter label to indicate remaining characters
        var charCountLabel = new JLabel();
        charCountLabel.setOpaque(false);
        updateCharCountLabel(charCountLabel, entryArea.getText());
        entryBottom.add(charCountLabel);

        // Entry-specific progress indicator (hidden by default)
        entryProgressBar.setIndeterminate(true);
        entryProgressBar.setVisible(false);
        entryProgressBar.setPreferredSize(new Dimension(140, 16));
        entryBottom.add(entryProgressBar);

        var previewToggleBtn = new AccentButton("Preview");
        previewToggleBtn.addActionListener(e -> togglePreview(previewToggleBtn));
        entryBottom.add(previewToggleBtn);
        var saveEntryBtn = new AccentButton("Save Entry");
        saveEntryBtn.addActionListener(e -> editor.saveEditedLogEntry());
        entryBottom.add(saveEntryBtn);
        entryContainer.add(entryBottom, BorderLayout.SOUTH);

        // Update the char counter as the user types (keep UI updates on EDT)
        entryArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                javax.swing.SwingUtilities.invokeLater(() -> updateCharCountLabel(charCountLabel, entryArea.getText()));
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        return split;
    }

    private void insertLink() {
        var selectedText = entryArea.getSelectedText();
        String displayText = selectedText != null && !selectedText.isEmpty() ? selectedText : "";

        // Create link input dialog
        var dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Insert Link", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 180);
        dialog.setLocationRelativeTo(this);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        var inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Display text field
        inputPanel.add(new JLabel("Display Text:"));
        var displayField = new JTextField(displayText, 20);
        // Limit display text length
        try {
            if (displayField.getDocument() instanceof AbstractDocument) {
                ((AbstractDocument) displayField.getDocument()).setDocumentFilter(new LengthLimitFilter(InputLimits.DISPLAY_TEXT_MAX));
            }
        } catch (Exception ignore) {
        }
        inputPanel.add(displayField);

        // URL/File path field
        inputPanel.add(new JLabel("URL or File Path:"));
        var urlPanel = new JPanel(new BorderLayout());
        var urlField = new JTextField(20);
        // Limit URL/path length
        try {
            if (urlField.getDocument() instanceof AbstractDocument) {
                ((AbstractDocument) urlField.getDocument()).setDocumentFilter(new LengthLimitFilter(InputLimits.FIELD_MAX_CHARS));
            }
        } catch (Exception ignore) {
        }
        urlPanel.add(urlField, BorderLayout.CENTER);
        var browseBtn = new StandardButton("Browse...", new Color(0xE0E0E0), new Color(0xB0B0B0));
        browseBtn.addActionListener(e -> {
            var fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select File to Link");
            if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                urlField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        urlPanel.add(browseBtn, BorderLayout.EAST);
        inputPanel.add(urlPanel);

        // Empty cell for layout
        inputPanel.add(new JPanel());
        inputPanel.add(new JPanel());

        dialog.add(inputPanel, BorderLayout.CENTER);

        // Buttons
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        var okBtn = new StandardButton("OK", new Color(0xE0E0E0), new Color(0xB0B0B0));
        var cancelBtn = new StandardButton("Cancel", new Color(0xE0E0E0), new Color(0xB0B0B0));

        okBtn.addActionListener(e -> {
            var text = displayField.getText().trim();
            var url = urlField.getText().trim();

            // Basic validation: accept http(s)://, file: or local filesystem paths
            if (!url.isEmpty()) {
                String u = url.toLowerCase();
                boolean looksLikeUrl = u.startsWith("http://") || u.startsWith("https://") || u.startsWith("file:");
                boolean looksLikePath = url.matches("^[a-zA-Z]:\\\\.*") || url.startsWith("/");
                if (!looksLikeUrl && !looksLikePath) {
                    DialogHelper.showError(dialog, "Invalid Link", "Invalid URL or file path.", "Only HTTP/HTTPS/file URLs or local file paths are allowed.");
                    return;
                }
            }

            if (!text.isEmpty() && !url.isEmpty()) {
                var link = "[" + text + "](" + url + ")";
                var pos = entryArea.getCaretPosition();
                if (selectedText != null && !selectedText.isEmpty()) {
                    // Replace selected text
                    var start = entryArea.getSelectionStart();
                    var end = entryArea.getSelectionEnd();
                    entryArea.replaceRange(link, start, end);
                } else {
                    // Insert at cursor
                    entryArea.insert(link, pos);
                }
                entryArea.requestFocus();
            }
            dialog.dispose();
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Handle Enter key
        dialog.getRootPane().setDefaultButton(okBtn);

        dialog.setVisible(true);
    }

    private void loadAndDisplayEntry(String timestamp) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
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

    private void setupListeners(JSplitPane split) {
        // Popup menu
        var contextMenu = new JPopupMenu();
        var copyItem = new JMenuItem("Copy Entry to Clipboard");
        copyItem.addActionListener(editor.copyLogEntryTextToClipBoard());
        contextMenu.add(copyItem);
        logList.setComponentPopupMenu(contextMenu);
        var previewInFullLogItem = new JMenuItem("Preview in Full Log View");
        previewInFullLogItem.addActionListener(e -> {
            String selectedTimestamp = logList.getSelectedValue();
            if (selectedTimestamp != null) {
                editor.getFullLogPanel().setSuppressAutoLoad(true);
                editor.getTabPane().setSelectedIndex(2); // Switch to full log tab
                editor.getFullLogPanel().loadFullLogNoScroll(() -> editor.getFullLogPanel().scrollToEntry(selectedTimestamp));
            }
        });
        contextMenu.add(previewInFullLogItem);
        var deleteItem = new JMenuItem("Delete Selected Entries");
        deleteItem.addActionListener(e -> editor.deleteSelectedEntry());
        contextMenu.add(deleteItem);
        var editDateTimeItem = new JMenuItem("Edit Date/Time");
        editDateTimeItem.addActionListener(e -> editor.editDateTime());
        contextMenu.add(editDateTimeItem);

        // Selection listeners
        logList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                var selectedItem = logList.getSelectedValue();
                loadAndDisplayEntry(selectedItem);
            }
        });

        logList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                var selectedItem = logList.getSelectedValue();
                loadAndDisplayEntry(selectedItem);
            }
        });

        // Update preview when text changes
        entryArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (isPreviewMode) {
                    renderPreview();
                }
            }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (isPreviewMode) {
                    renderPreview();
                }
            }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (isPreviewMode) {
                    renderPreview();
                }
            }
        });

        SwingUtilities.invokeLater(() -> selectFirstLogIfAny());
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

    private void updateInfoFromListModel() {
        try {
            var stats = new LogStatistics(listModel.getSize(), new java.util.ArrayList<>(), logFileHandler.getFilePath());
            infoPanel.updateStatistics(stats);
        } catch (Exception ex) {
            infoPanel.resetStatistics();
        }
    }

    private void togglePreview(javax.swing.JButton toggleBtn) {
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

        // Parse content into lines (single entry)
        String[] lines = content.split("\n");
        List<String> entryLines = new ArrayList<>();
        for (String line : lines) {
            entryLines.add(line);
        }

        // Wrap in a list of entries (single entry)
        List<List<String>> entries = new ArrayList<>();
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
