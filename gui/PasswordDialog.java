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

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        String welcomeText = customMessage != null ? customMessage : "Welcome back! Enter your password to unlock your encrypted log.";
        JLabel welcomeLabel = new JLabel("<html><center>" + welcomeText + "</center></html>", SwingConstants.CENTER);
        welcomeLabel.setFont(welcomeLabel.getFont().deriveFont(Font.PLAIN, 12f));
        topPanel.add(welcomeLabel, BorderLayout.NORTH);

        if (reminder != null && !reminder.trim().isEmpty()) {
            JLabel reminderLabel = new JLabel("Reminder: " + reminder, SwingConstants.CENTER);
            reminderLabel.setForeground(Color.GRAY);
            topPanel.add(reminderLabel, BorderLayout.SOUTH);
        }

        centerPanel.add(topPanel, BorderLayout.NORTH);

        passwordField = new JPasswordField(20);

        toggleButton = new JButton("Show");
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

        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.add(passwordField, BorderLayout.CENTER);
        fieldPanel.add(toggleButton, BorderLayout.EAST);

        centerPanel.add(fieldPanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");

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
            toggleButton.setText("Hide");
        } else {
            passwordField.setEchoChar('*');
            toggleButton.setText("Show");
        }
        passwordField.requestFocusInWindow();
    }

    public char[] getPassword() {
        return password;
    }

    public static PasswordResult showPasswordDialog(Frame parent, String title, String reminder) {
        PasswordDialog dialog = new PasswordDialog(parent, title, reminder, null);
        dialog.setVisible(true);
        return new PasswordResult(dialog.getPassword());
    }

    public static PasswordResult showPasswordDialog(Frame parent, String title, String reminder, String customMessage) {
        PasswordDialog dialog = new PasswordDialog(parent, title, reminder, customMessage);
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
