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

import filehandling.LogFileHandler;
import java.awt.*;
import java.time.LocalDate;
import java.time.Year;
import java.util.stream.IntStream;
import javax.swing.*;
import main.LogTextEditor;
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

    public LogListPanel(LogTextEditor editor, LogFileHandler logFileHandler, DefaultListModel<String> listModel, JList<String> logList) {
        this.editor = editor;
        this.logFileHandler = logFileHandler;
        this.listModel = listModel;
        this.logList = logList;
        this.entryArea = new UndoRedoTextArea();
        this.entryScroll = new JScrollPane(entryArea);
        this.lockLabel = new JLabel("File locked. Press Unlock file in Full log view to unlock it again.", SwingConstants.CENTER);
        this.entryContainer = new JPanel(new BorderLayout());
        initPanel();
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

        setupListeners(split);
    }

    private JPanel createFilterPanel() {
        var filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        filterPanel.setOpaque(false);
        var filterLabel = new JLabel("Filter on date");
        filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));
        filterPanel.add(filterLabel);

        var currentYear = Year.now().getValue();
        var years = IntStream.rangeClosed(2000, currentYear).boxed().toArray(Integer[]::new);
        var yearCombo = new JComboBox<>(years);
        yearCombo.setSelectedItem(currentYear);
        filterPanel.add(yearCombo);

        var months = new String[]{
                "01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr",
                "05 - May", "06 - Jun", "07 - Jul", "08 - Aug",
                "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec"
        };
        var monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        filterPanel.add(monthCombo);

        // Filter actions
        yearCombo.addActionListener(e -> applyFilter(yearCombo, monthCombo));
        monthCombo.addActionListener(e -> applyFilter(yearCombo, monthCombo));

        return filterPanel;
    }

    private void applyFilter(JComboBox<Integer> yearCombo, JComboBox<String> monthCombo) {
        try {
            var year = (Integer) yearCombo.getSelectedItem();
            var month = monthCombo.getSelectedIndex() + 1;
            if (year != null) {
                logFileHandler.loadFilteredEntries(listModel, year, month);
            }
        } catch (Exception ex) {
            logFileHandler.showErrorDialog("Error applying date filter: " + ex.getMessage());
        }
    }

    private JSplitPane createLogSplitPane() {
        var split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.33);
        split.setBorder(null);

        logList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logList.setModel(listModel);
        logList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        logList.setBackground(Color.WHITE);

        var listScroll = new JScrollPane(logList);
        listScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE6E9EB)),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        listScroll.setBackground(Color.WHITE);
        listScroll.getViewport().setBackground(Color.WHITE);

        var entryContainer = this.entryContainer;
        entryContainer.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        entryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        entryArea.setLineWrap(true);
        entryArea.setWrapStyleWord(true);
        entryArea.setBackground(Color.WHITE);
        entryScroll.setPreferredSize(new Dimension(600, 220));
        entryScroll.setBorder(BorderFactory.createLineBorder(new Color(0xE6E9EB)));
        entryContainer.add(entryScroll, BorderLayout.CENTER);

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

        editor.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK), "newEntryGlobal");
        editor.getRootPane().getActionMap().put("newEntryGlobal", editor.createNewQuickEntry());

        // Save button
        var entryBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        entryBottom.setOpaque(false);
        var saveEntryBtn = new AccentButton("Save Entry");
        saveEntryBtn.addActionListener(e -> editor.saveEditedLogEntry());
        entryBottom.add(saveEntryBtn);
        entryContainer.add(entryBottom, BorderLayout.SOUTH);

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
        inputPanel.add(displayField);

        // URL/File path field
        inputPanel.add(new JLabel("URL or File Path:"));
        var urlPanel = new JPanel(new BorderLayout());
        var urlField = new JTextField(20);
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

    private void setupListeners(JSplitPane split) {
        // Popup menu
        var contextMenu = new JPopupMenu();
        var copyItem = new JMenuItem("Copy Entry to Clipboard");
        copyItem.addActionListener(editor.copyLogEntryTextToClipBoard());
        contextMenu.add(copyItem);
        logList.setComponentPopupMenu(contextMenu);
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
                if (selectedItem != null) {
                    var logContent = logFileHandler.loadEntry(selectedItem);
                    entryArea.setText(logContent);
                }
            }
        });

        logList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                var selectedItem = logList.getSelectedValue();
                if (selectedItem != null) {
                    var logContent = logFileHandler.loadEntry(selectedItem);
                    entryArea.setText(logContent);
                } else {
                    entryArea.setText("");
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
            if (item != null) {
                var content = logFileHandler.loadEntry(item);
                entryArea.setText(content);
            }
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

    public void setLocked(boolean locked) {
        entryArea.setEditable(!locked);
        if (locked) {
            entryArea.setText("");
            entryContainer.remove(entryScroll);
            entryContainer.add(lockLabel, BorderLayout.CENTER);
        } else {
            entryContainer.remove(lockLabel);
            entryContainer.add(entryScroll, BorderLayout.CENTER);
        }
        entryContainer.revalidate();
        entryContainer.repaint();
    }
}
