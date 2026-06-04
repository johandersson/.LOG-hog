/*
 * Copyright (C) 2026 Johan Andersson
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

public class LinkDialog {

    public static void showInsertLinkDialog(JTextComponent targetComponent) {
        if (targetComponent == null) return;

        var selectedText = targetComponent.getSelectedText();
        String displayText = selectedText != null && !selectedText.isEmpty() ? selectedText : "";

        // Create link input dialog
        var parent = (Frame) SwingUtilities.getWindowAncestor(targetComponent);
        var dialog = new JDialog(parent, "Insert Link", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 220);
        dialog.setLocationRelativeTo(parent);
        ((JComponent) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        var inputPanel = new JPanel(new GridLayout(3, 2, 8, 8));

        // Display text field
        inputPanel.add(new JLabel("Display Text:"));
        var displayField = new JTextField(displayText, 30);
        displayField.setPreferredSize(new Dimension(300, 30));
        inputPanel.add(displayField);

        // URL/File path field
        inputPanel.add(new JLabel("URL or File Path:"));
        var urlPanel = new JPanel(new BorderLayout(5, 0));
        var urlField = new JTextField(30);
        urlField.setPreferredSize(new Dimension(300, 30));
        urlPanel.add(urlField, BorderLayout.CENTER);
        var browseBtn = new AccentButton("Browse...");
        browseBtn.addActionListener(e -> {
            var fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select File to Link");
            if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                var filePath = fileChooser.getSelectedFile().getAbsolutePath();
                // Add file:// prefix for proper OS file opening
                urlField.setText("file://" + filePath);
            }
        });
        urlPanel.add(browseBtn, BorderLayout.EAST);
        inputPanel.add(urlPanel);

        // Empty cell for layout
        inputPanel.add(new JPanel());
        inputPanel.add(new JPanel());

        dialog.add(inputPanel, BorderLayout.CENTER);

        // Buttons
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        var okBtn = new AccentButton("OK");
        var cancelBtn = new AccentButton("Cancel");

        okBtn.addActionListener(e -> {
            var text = displayField.getText().trim();
            var url = urlField.getText().trim();

            if (!text.isEmpty() && !url.isEmpty()) {
                // Ensure file paths have the file:// prefix for proper OS opening
                if (!url.contains("://") && (url.contains("\\") || url.contains("/") || url.startsWith("C:") || url.startsWith("c:"))) {
                    url = "file://" + url;
                }

                var link = "[" + text + "](" + url + ")";
                if (selectedText != null && !selectedText.isEmpty()) {
                    // Replace selected text
                    targetComponent.replaceSelection(link);
                } else {
                    // Insert at cursor
                    targetComponent.replaceSelection(link);
                }
                targetComponent.requestFocus();
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
}