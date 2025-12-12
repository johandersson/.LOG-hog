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
import javax.swing.text.JTextComponent;

public class FormattingPanel extends JPanel {
    private final JTextComponent targetComponent;

    public FormattingPanel(JTextComponent targetComponent) {
        this.targetComponent = targetComponent;
        initPanel();
        setupKeyboardShortcuts();
    }

    private void initPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        // Bold button
        var boldBtn = new StandardButton("B", new Color(0xE0E0E0), new Color(0xB0B0B0));
        boldBtn.setPreferredSize(new Dimension(35, 28));
        boldBtn.setFont(boldBtn.getFont().deriveFont(Font.BOLD));
        boldBtn.setToolTipText("Bold (Ctrl+B)");
        boldBtn.addActionListener(e -> applyFormatting("**", "**"));
        add(boldBtn);

        // Italic button
        var italicBtn = new StandardButton("I", new Color(0xE0E0E0), new Color(0xB0B0B0));
        italicBtn.setPreferredSize(new Dimension(35, 28));
        italicBtn.setFont(italicBtn.getFont().deriveFont(Font.ITALIC));
        italicBtn.setToolTipText("Italic (Ctrl+I)");
        italicBtn.addActionListener(e -> applyFormatting("*", "*"));
        add(italicBtn);

        // Heading buttons
        var h1Btn = new StandardButton("H1", new Color(0xE0E0E0), new Color(0xB0B0B0));
        h1Btn.setPreferredSize(new Dimension(40, 28));
        h1Btn.setToolTipText("Heading 1");
        h1Btn.addActionListener(e -> applyFormatting("# ", ""));
        add(h1Btn);

        var h2Btn = new StandardButton("H2", new Color(0xE0E0E0), new Color(0xB0B0B0));
        h2Btn.setPreferredSize(new Dimension(40, 28));
        h2Btn.setToolTipText("Heading 2");
        h2Btn.addActionListener(e -> applyFormatting("## ", ""));
        add(h2Btn);

        var h3Btn = new StandardButton("H3", new Color(0xE0E0E0), new Color(0xB0B0B0));
        h3Btn.setPreferredSize(new Dimension(40, 28));
        h3Btn.setToolTipText("Heading 3");
        h3Btn.addActionListener(e -> applyFormatting("### ", ""));
        add(h3Btn);

        // Link button
        var linkBtn = new StandardButton("Link", new Color(0xE0E0E0), new Color(0xB0B0B0));
        linkBtn.setPreferredSize(new Dimension(45, 28));
        linkBtn.setToolTipText("Insert Link");
        linkBtn.addActionListener(e -> LinkDialog.showInsertLinkDialog(targetComponent));
        add(linkBtn);
    }

    private void setupKeyboardShortcuts() {
        if (targetComponent != null) {
            // Formatting shortcuts
            targetComponent.getInputMap(JComponent.WHEN_FOCUSED).put(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.CTRL_DOWN_MASK), "formatBold");
            targetComponent.getActionMap().put("formatBold", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    applyFormatting("**", "**");
                }
            });

            targetComponent.getInputMap(JComponent.WHEN_FOCUSED).put(
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK), "formatItalic");
            targetComponent.getActionMap().put("formatItalic", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    applyFormatting("*", "*");
                }
            });
        }
    }

    private void applyFormatting(String prefix, String suffix) {
        if (targetComponent == null) return;

        var selectedText = targetComponent.getSelectedText();
        if (selectedText == null || selectedText.isEmpty()) {
            // Insert at cursor position
            var pos = targetComponent.getCaretPosition();
            targetComponent.replaceSelection(prefix + suffix);
            targetComponent.setCaretPosition(pos + prefix.length());
        } else {
            // Wrap selected text
            var start = targetComponent.getSelectionStart();
            var newText = prefix + selectedText + suffix;
            targetComponent.replaceSelection(newText);
            targetComponent.setSelectionStart(start);
            targetComponent.setSelectionEnd(start + newText.length());
        }
        targetComponent.requestFocus();
    }
}