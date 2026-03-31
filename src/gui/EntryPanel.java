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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import main.LogTextEditor;
import markdown.LinkHandler;
import markdown.MarkdownRenderer;
import utils.UndoRedoTextArea;

public class EntryPanel extends JPanel {
    private final JTextArea textArea;
    private final LogTextEditor editor;
    private final JButton saveBtn;
    private final JButton previewBtn;
    private final JLabel lockLabel;
    private final JScrollPane scrollPane;
    private final JScrollPane previewScrollPane;
    private final JPanel textContainer;
    private final HighlightableTextPane previewPane;
    private boolean isPreviewMode; // default false, no initializer needed

    public EntryPanel(LogTextEditor editor) {
        this.editor = editor;
        this.textArea = new UndoRedoTextArea();
        this.saveBtn = new AccentButton("Save");
        this.previewBtn = new AccentButton("Preview");
        this.lockLabel = new JLabel("File locked. Press Unlock file in Full log view to unlock it again.", SwingConstants.CENTER);
        this.scrollPane = new JScrollPane(textArea);
        this.previewPane = new HighlightableTextPane();
        this.previewScrollPane = new JScrollPane(previewPane);
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
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Setup preview pane
        previewPane.setEditable(false);
        previewScrollPane.setBorder(BorderFactory.createEmptyBorder());

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
        previewBtn.addActionListener(e -> togglePreview());
        saveBtn.addActionListener(e -> editor.saveLogEntry());
        bottom.add(previewBtn);
        bottom.add(saveBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void togglePreview() {
        if (isPreviewMode) {
            // Switch to edit mode
            textContainer.remove(previewScrollPane);
            textContainer.add(scrollPane, BorderLayout.CENTER);
            previewBtn.setText("Preview");
            isPreviewMode = false;
        } else {
            // Switch to preview mode
            renderPreview();
            textContainer.remove(scrollPane);
            textContainer.add(previewScrollPane, BorderLayout.CENTER);
            previewBtn.setText("Edit");
            isPreviewMode = true;
        }
        textContainer.revalidate();
        textContainer.repaint();
    }

    private void renderPreview() {
        String content = textArea.getText().trim();
        if (content.isEmpty()) {
            previewPane.setText("No content to preview");
            previewPane.setContentType("text/plain");
            return;
        }

        // Use Arrays.asList instead of tight loop
        List<String> entryLines = Arrays.asList(content.split("\n"));

        // Wrap in a list of entries (single entry)
        List<List<String>> entries = new ArrayList<>();
        entries.add(entryLines);

        // Render using MarkdownRenderer
        MarkdownRenderer.renderMarkdownFromEntries(previewPane, entries, false);
        LinkHandler.addLinkListeners(previewPane);
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    public void setLocked(boolean locked) {
        textArea.setEditable(!locked);
        saveBtn.setEnabled(!locked);
        previewBtn.setEnabled(!locked);
        if (locked) {
            textArea.setText("");
            // Switch back to edit mode if in preview mode
            if (isPreviewMode) {
                togglePreview();
            }
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
