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
import java.awt.event.*;
import javax.swing.*;
import utils.TooltipHelper;

public class SearchDialog extends JDialog {
    private final HighlightableTextPane textPane;
    private final JTextField searchField;
    private final JCheckBox wholeWordCheck;
    private final JCheckBox caseSensitiveCheck;
    private final JLabel matchCountLabel;
    private final JButton findBtn;
    private final JButton findNextBtn;
    private final JButton findPrevBtn;
    private final JButton closeBtn;

    public SearchDialog(Frame parent, HighlightableTextPane textPane) {
        super(parent, "Find in Log", true); // Modal dialog
        this.textPane = textPane;

        setLayout(new BorderLayout());
        setSize(400, 200);
        setLocationRelativeTo(parent);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Find:"));
        searchField = new JTextField(20);
        searchField.addActionListener(e -> performSearch());
        searchPanel.add(searchField);

        // Options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wholeWordCheck = new JCheckBox("Whole word only");
        caseSensitiveCheck = new JCheckBox("Case sensitive");
        optionsPanel.add(wholeWordCheck);
        optionsPanel.add(caseSensitiveCheck);

        // Match count
        matchCountLabel = new JLabel("No matches");
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        countPanel.add(matchCountLabel);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        findBtn = new AccentButton("Find");
        findBtn.addActionListener(e -> performSearch());
        findNextBtn = new AccentButton("Find Next");
        findNextBtn.addActionListener(e -> findNext());
        findPrevBtn = new AccentButton("Find Previous");
        findPrevBtn.addActionListener(e -> findPrevious());
        closeBtn = new AccentButton("Close");
        closeBtn.addActionListener(e -> setVisible(false));

        buttonPanel.add(findBtn);
        buttonPanel.add(findNextBtn);
        buttonPanel.add(findPrevBtn);
        buttonPanel.add(closeBtn);

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(searchPanel, BorderLayout.NORTH);
        topPanel.add(optionsPanel, BorderLayout.CENTER);
        topPanel.add(countPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Enable tooltips on disabled buttons
        TooltipHelper.enableTooltipOnDisabled(findNextBtn, "Find next occurrence");
        TooltipHelper.enableTooltipOnDisabled(findPrevBtn, "Find previous occurrence");

        // Key bindings
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        searchField.requestFocusInWindow();
    }

    private void performSearch() {
        String query = searchField.getText();
        boolean wholeWord = wholeWordCheck.isSelected();
        boolean caseSensitive = caseSensitiveCheck.isSelected();

        int matchCount = textPane.highlightText(query, wholeWord, caseSensitive);
        updateMatchCount(matchCount);
        updateButtons(matchCount > 0);
    }

    private void findNext() {
        textPane.navigateHighlights(true);
    }

    private void findPrevious() {
        textPane.navigateHighlights(false);
    }

    private void updateMatchCount(int count) {
        if (count == 0) {
            matchCountLabel.setText("No matches");
        } else if (count == 1) {
            matchCountLabel.setText("1 match");
        } else {
            matchCountLabel.setText(count + " matches");
        }
    }

    private void updateButtons(boolean hasMatches) {
        findNextBtn.setEnabled(hasMatches);
        findPrevBtn.setEnabled(hasMatches);
    }
}