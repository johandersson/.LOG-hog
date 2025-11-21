package gui;

import java.awt.*;
import java.time.LocalDate;
import java.time.Year;
import java.util.stream.IntStream;
import javax.swing.*;
import utils.UndoRedoTextArea;
import filehandling.LogFileHandler;
import main.LogTextEditor;

public class LogListPanel extends JPanel {
    private final JList<String> logList;
    private final DefaultListModel<String> listModel;
    private final JTextArea entryArea;
    private final LogFileHandler logFileHandler;
    private final LogTextEditor editor;

    public LogListPanel(LogTextEditor editor, LogFileHandler logFileHandler, DefaultListModel<String> listModel, JList<String> logList) {
        this.editor = editor;
        this.logFileHandler = logFileHandler;
        this.listModel = listModel;
        this.logList = logList;
        this.entryArea = new UndoRedoTextArea();
        initPanel();
    }

    private void initPanel() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Color.WHITE);

        // Top: filter controls
        JPanel filterPanel = createFilterPanel();
        add(filterPanel, BorderLayout.NORTH);

        // Center: list and editor pane in a split
        JSplitPane split = createLogSplitPane();
        add(split, BorderLayout.CENTER);

        setupListeners(split);
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        filterPanel.setOpaque(false);
        JLabel filterLabel = new JLabel("Filter on date");
        filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));
        filterPanel.add(filterLabel);

        int currentYear = Year.now().getValue();
        Integer[] years = IntStream.rangeClosed(2000, currentYear).boxed().toArray(Integer[]::new);
        JComboBox<Integer> yearCombo = new JComboBox<>(years);
        yearCombo.setSelectedItem(currentYear);
        filterPanel.add(yearCombo);

        String[] months = new String[]{
                "01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr",
                "05 - May", "06 - Jun", "07 - Jul", "08 - Aug",
                "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec"
        };
        JComboBox<String> monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        filterPanel.add(monthCombo);

        // Filter actions
        yearCombo.addActionListener(e -> applyFilter(yearCombo, monthCombo));
        monthCombo.addActionListener(e -> applyFilter(yearCombo, monthCombo));

        return filterPanel;
    }

    private void applyFilter(JComboBox<Integer> yearCombo, JComboBox<String> monthCombo) {
        try {
            Integer year = (Integer) yearCombo.getSelectedItem();
            int month = monthCombo.getSelectedIndex() + 1;
            if (year != null) {
                logFileHandler.loadFilteredEntries(listModel, year, month);
            }
        } catch (Exception ex) {
            logFileHandler.showErrorDialog("Error applying date filter: " + ex.getMessage());
        }
    }

    private JSplitPane createLogSplitPane() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.33);
        split.setBorder(null);

        logList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logList.setModel(listModel);
        logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane listScroll = new JScrollPane(logList);
        listScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE6E9EB)),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));

        JPanel entryContainer = new JPanel(new BorderLayout());
        entryContainer.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        entryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        entryArea.setLineWrap(true);
        entryArea.setWrapStyleWord(true);
        JScrollPane entryScroll = new JScrollPane(entryArea);
        entryScroll.setPreferredSize(new Dimension(600, 220));
        entryContainer.add(entryScroll, BorderLayout.CENTER);

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
        JPanel entryBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        entryBottom.setOpaque(false);
        JButton saveEntryBtn = new AccentButton("Save Entry");
        saveEntryBtn.addActionListener(e -> editor.saveEditedLogEntry());
        entryBottom.add(saveEntryBtn);
        entryContainer.add(entryBottom, BorderLayout.SOUTH);

        return split;
    }

    private void setupListeners(JSplitPane split) {
        // Popup menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy Entry to Clipboard");
        copyItem.addActionListener(editor.copyLogEntryTextToClipBoard());
        contextMenu.add(copyItem);
        logList.setComponentPopupMenu(contextMenu);
        JMenuItem deleteItem = new JMenuItem("Delete Entry");
        deleteItem.addActionListener(e -> editor.deleteSelectedEntry());
        contextMenu.add(deleteItem);
        JMenuItem editDateTimeItem = new JMenuItem("Edit Date/Time");
        editDateTimeItem.addActionListener(e -> editor.editDateTime());
        contextMenu.add(editDateTimeItem);

        // Selection listeners
        logList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                String selectedItem = logList.getSelectedValue();
                if (selectedItem != null) {
                    String logContent = logFileHandler.loadEntry(selectedItem);
                    entryArea.setText(logContent);
                }
            }
        });

        logList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedItem = logList.getSelectedValue();
                if (selectedItem != null) {
                    String logContent = logFileHandler.loadEntry(selectedItem);
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
            String item = listModel.getElementAt(0);
            if (item != null) {
                String content = logFileHandler.loadEntry(item);
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
}