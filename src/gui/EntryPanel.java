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

import java.awt.*;
import javax.swing.*;
import main.LogTextEditor;
import utils.UndoRedoTextArea;

public class EntryPanel extends JPanel {
    private final JTextArea textArea;
    private final LogTextEditor editor;
    private final JButton saveBtn;
    private final JLabel lockLabel;
    private final JScrollPane scrollPane;

    public EntryPanel(LogTextEditor editor) {
        this.editor = editor;
        this.textArea = new UndoRedoTextArea();
        this.saveBtn = new AccentButton("Save");
        this.lockLabel = new JLabel("File locked. Press Unlock file in Full log view to unlock it again.", SwingConstants.CENTER);
        this.scrollPane = new JScrollPane(textArea);
        initPanel();
    }

    private void initPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(true);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xE6E9EB)));
        add(scrollPane, BorderLayout.CENTER);

        lockLabel.setForeground(Color.GRAY);

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
        if (locked) {
            textArea.setText("");
            remove(scrollPane);
            add(lockLabel, BorderLayout.CENTER);
        } else {
            remove(lockLabel);
            add(scrollPane, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }
}
