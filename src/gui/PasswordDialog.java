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

public class PasswordDialog extends JDialog {
    private JPasswordField passwordField;
    private JButton toggleButton;
    private JButton okButton;
    private JButton cancelButton;
    private char[] password;
    private boolean visible = false;
    private String reminder;
    private String customMessage;

    public PasswordDialog(Frame parent, String title, String reminder, String customMessage) {
        super(parent, title, true);
        this.reminder = reminder;
        this.customMessage = customMessage;
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0xF7FAFC));

        var centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        centerPanel.setBackground(new Color(0xF7FAFC));

        var topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(0xF7FAFC));
        var welcomeText = customMessage != null ? customMessage : "Welcome back! Enter your password to unlock your encrypted log.";
        var welcomeLabel = new JLabel("<html><center>" + welcomeText + "</center></html>", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        welcomeLabel.setForeground(new Color(0x2B3A42));
        topPanel.add(welcomeLabel, BorderLayout.NORTH);

        if (reminder != null && !reminder.trim().isEmpty()) {
            var reminderLabel = new JLabel("Reminder: " + reminder, SwingConstants.CENTER);
            reminderLabel.setForeground(new Color(0x5E6A70));
            reminderLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            topPanel.add(reminderLabel, BorderLayout.SOUTH);
        }

        centerPanel.add(topPanel, BorderLayout.NORTH);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        passwordField.setBackground(Color.WHITE);
        passwordField.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        toggleButton = new StandardButton("", new Color(0xE0E0E0), new Color(0xB0B0B0));
        toggleButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        toggleButton.setToolTipText("Show password");
        toggleButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                updateVisibility(true);
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                updateVisibility(false);
            }
        });

        var fieldPanel = new JPanel(new BorderLayout(5, 0));
        fieldPanel.setBackground(new Color(0xF7FAFC));
        fieldPanel.add(passwordField, BorderLayout.CENTER);
        fieldPanel.add(toggleButton, BorderLayout.EAST);

        centerPanel.add(fieldPanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(0xF7FAFC));
        okButton = new AccentButton("OK");
        cancelButton = new StandardButton("Cancel", new Color(0xE0E0E0), new Color(0xB0B0B0));

        okButton.addActionListener(e -> {
            password = passwordField.getPassword();
            setVisible(false);
        });

        cancelButton.addActionListener(e -> {
            password = null;
            setVisible(false);
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
    }

    private void updateVisibility(boolean newVisible) {
        visible = newVisible;
        if (visible) {
            passwordField.setEchoChar((char) 0);
            toggleButton.setText("<html><font size=\"+3\">👁</font></html>");
            toggleButton.setToolTipText("Hide password");
        } else {
            passwordField.setEchoChar('*');
            toggleButton.setText("<html><font size=\"+3\"><s>👁</s></font></html>");
            toggleButton.setToolTipText("Show password");
        }
        passwordField.requestFocusInWindow();
    }

    public char[] getPassword() {
        return password;
    }

    public static PasswordResult showPasswordDialog(Frame parent, String title, String reminder) {
        var dialog = new PasswordDialog(parent, title, reminder, null);
        dialog.setVisible(true);
        return new PasswordResult(dialog.getPassword());
    }

    public static PasswordResult showPasswordDialog(Frame parent, String title, String reminder, String customMessage) {
        var dialog = new PasswordDialog(parent, title, reminder, customMessage);
        dialog.setVisible(true);
        return new PasswordResult(dialog.getPassword());
    }

    public static class PasswordResult {
        public final char[] password;

        public PasswordResult(char[] password) {
            this.password = password;
        }
    }
}
