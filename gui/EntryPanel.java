package gui;

import java.awt.*;
import javax.swing.*;
import utils.UndoRedoTextArea;
import main.LogTextEditor;

public class EntryPanel extends JPanel {
    private final JTextArea textArea;
    private final LogTextEditor editor;

    public EntryPanel(LogTextEditor editor) {
        this.editor = editor;
        this.textArea = new UndoRedoTextArea();
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

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.setBackground(Color.WHITE);
        JButton saveBtn = new AccentButton("Save");
        saveBtn.addActionListener(e -> editor.saveLogEntry());
        bottom.add(saveBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    public JTextArea getTextArea() {
        return textArea;
    }
}