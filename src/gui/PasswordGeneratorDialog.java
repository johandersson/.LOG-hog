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

public class PasswordGeneratorDialog extends JDialog {
    private JTextField resultField;
    private JRadioButton passwordRadio;
    private JRadioButton passphraseRadio;
    private JSpinner lengthSpinner;
    private JButton generateButton;
    private JButton copyButton;

    public PasswordGeneratorDialog(Frame parent) {
        super(parent, "Password Generator", true);
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(0xF7FAFC));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        var header = new JLabel("Generate Secure Passwords & Passphrases");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 16f));
        header.setForeground(new Color(0x2B3A42));
        add(header, BorderLayout.NORTH);

        // Center panel
        var centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(0xF7FAFC));
        var gbc = new GridBagLayout().createGridBagConstraints();

        // Type selection
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(new JLabel("Type:"), gbc);

        var typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        typePanel.setBackground(new Color(0xF7FAFC));
        passwordRadio = new JRadioButton("Password", true);
        passphraseRadio = new JRadioButton("Passphrase");
        var group = new ButtonGroup();
        group.add(passwordRadio);
        group.add(passphraseRadio);
        typePanel.add(passwordRadio);
        typePanel.add(passphraseRadio);

        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(typePanel, gbc);

        // Length/Word count
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(new JLabel("Length/Words:"), gbc);

        lengthSpinner = new JSpinner(new SpinnerNumberModel(20, 8, 50, 1));
        passwordRadio.addActionListener(e -> {
            lengthSpinner.setModel(new SpinnerNumberModel(20, 8, 50, 1));
            ((JSpinner.DefaultEditor)lengthSpinner.getEditor()).getTextField().setText("20");
        });
        passphraseRadio.addActionListener(e -> {
            lengthSpinner.setModel(new SpinnerNumberModel(4, 3, 8, 1));
            ((JSpinner.DefaultEditor)lengthSpinner.getEditor()).getTextField().setText("4");
        });

        gbc.gridx = 1; gbc.gridy = 1;
        centerPanel.add(lengthSpinner, gbc);

        // Result field
        gbc.gridx = 0; gbc.gridy = 2;
        centerPanel.add(new JLabel("Generated:"), gbc);

        resultField = new JTextField(30);
        resultField.setEditable(false);
        resultField.setBackground(Color.WHITE);
        resultField.setFont(new Font("Monospaced", Font.PLAIN, 12));

        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(resultField, gbc);

        // Warning message
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        var warning = new JLabel("<html><b>⚠️ Important:</b> Save this password in your password manager immediately!<br>Do not rely on memory alone.</html>");
        warning.setForeground(new Color(0xD32F2F));
        centerPanel.add(warning, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // Buttons
        var buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(0xF7FAFC));

        generateButton = new AccentButton("Generate");
        generateButton.addActionListener(e -> generate());

        copyButton = new StandardButton("Copy to Clipboard", new Color(0xE0E0E0), new Color(0xB0B0B0));
        copyButton.addActionListener(e -> {
            if (!resultField.getText().isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection(resultField.getText()), null);
                JOptionPane.showMessageDialog(this, "Password copied to clipboard!\nRemember to save it in your password manager.");
            }
        });

        var closeButton = new StandardButton("Close", new Color(0xE0E0E0), new Color(0xB0B0B0));
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(generateButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void generate() {
        int length = (Integer) lengthSpinner.getValue();
        String result;
        if (passwordRadio.isSelected()) {
            result = PasswordGenerator.generatePassword(length);
        } else {
            result = PasswordGenerator.generatePassphrase(length);
        }
        resultField.setText(result);
    }

    public static void showDialog(Frame parent) {
        new PasswordGeneratorDialog(parent).setVisible(true);
    }
}