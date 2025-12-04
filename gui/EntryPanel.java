package gui;

import java.awt.*;
import javax.swing.*;
import main.LogTextEditor;
import utils.UndoRedoTextArea;

public class EntryPanel extends JPanel {
    private final JTextArea textArea;
    private final LogTextEditor editor;
    private final JButton saveBtn;
    private final JLabel lockLabel;

    public EntryPanel(LogTextEditor editor) {
        this.editor = editor;
        this.textArea = new UndoRedoTextArea();
        this.saveBtn = new AccentButton("Save");
        this.lockLabel = new JLabel("File locked. Press Unlock file in Full log view to unlock it again.", SwingConstants.CENTER);
        initPanel();
    }

    private void initPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(true);
        JScrollPane sp = new JScrollPane(textArea);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xE6E9EB)));
        add(sp, BorderLayout.CENTER);

        lockLabel.setForeground(Color.GRAY);
        lockLabel.setVisible(false);
        add(lockLabel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.setBackground(Color.WHITE);
        saveBtn.addActionListener(e -> editor.saveLogEntry());
        bottom.add(saveBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    public void setLocked(boolean locked) {
        textArea.setEditable(!locked);
        saveBtn.setEnabled(!locked);
        lockLabel.setVisible(locked);
        if (locked) {
            textArea.setText("");
        }
    }
}