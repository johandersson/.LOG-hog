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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import main.LogTextEditor;
import utils.UndoRedoTextArea;

public class EntryPanel extends JPanel {
    private final JTextArea textArea;
    private final LogTextEditor editor;
    private final JButton saveBtn;
    private final JLabel lockLabel;
    private final JScrollPane scrollPane;
    private final JPanel textContainer;

    public EntryPanel(LogTextEditor editor) {
        this.editor = editor;
        this.textArea = new UndoRedoTextArea();
        this.saveBtn = new AccentButton("Save");
        this.lockLabel = new JLabel("File locked. Press Unlock file in Full log view to unlock it again.", SwingConstants.CENTER);
        this.scrollPane = new JScrollPane(textArea);
        this.textContainer = new JPanel(new BorderLayout());
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

        // Create container for text area and formatting
        textContainer.setOpaque(false);

        // Add formatting panel above text area
        var formattingPanel = new FormattingPanel(textArea);
        textContainer.add(formattingPanel, BorderLayout.NORTH);
        textContainer.add(scrollPane, BorderLayout.CENTER);

        add(textContainer, BorderLayout.CENTER);

        // Key binding for Ctrl+S
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        textArea.getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                editor.saveLogEntry();
            }
        });

        lockLabel.setForeground(Color.GRAY);

        var bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
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
            remove(textContainer);
            add(lockLabel, BorderLayout.CENTER);
        } else {
            remove(lockLabel);
            add(textContainer, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }
}
