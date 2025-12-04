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
    private JCheckBox alwaysShowCheckBox;
    private char[] password;
    private boolean visible = false;
    private boolean alwaysShow = false;
    private String reminder;
    private String customMessage;

    public PasswordDialog(Frame parent, String title, String reminder, boolean initialVisible) {
        this(parent, title, reminder, initialVisible, null);
    }

    public PasswordDialog(Frame parent, String title, String reminder, boolean initialVisible, String customMessage) {
        super(parent, title, true);
        this.reminder = reminder;
        this.visible = initialVisible;
        this.customMessage = customMessage;
        initComponents();
        updateVisibility(visible);
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
        toggleButton.addActionListener(e -> updateVisibility(!visible));

        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.add(passwordField, BorderLayout.CENTER);
        fieldPanel.add(toggleButton, BorderLayout.EAST);

        centerPanel.add(fieldPanel, BorderLayout.CENTER);

        alwaysShowCheckBox = new JCheckBox("Always show password in plain text");
        alwaysShowCheckBox.setSelected(visible);
        alwaysShowCheckBox.addActionListener(e -> updateVisibility(alwaysShowCheckBox.isSelected()));
        centerPanel.add(alwaysShowCheckBox, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            password = passwordField.getPassword();
            alwaysShow = alwaysShowCheckBox.isSelected();
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
        alwaysShowCheckBox.setSelected(visible);
        passwordField.requestFocusInWindow();
    }

    public char[] getPassword() {
        return password;
    }

    public boolean isAlwaysShow() {
        return alwaysShow;
    }

    public static PasswordResult showPasswordDialog(Frame parent, String title, String reminder, boolean initialVisible) {
        PasswordDialog dialog = new PasswordDialog(parent, title, reminder, initialVisible);
        dialog.setVisible(true);
        return new PasswordResult(dialog.getPassword(), dialog.isAlwaysShow());
    }

    public static PasswordResult showPasswordDialog(Frame parent, String title, String reminder, boolean initialVisible, String customMessage) {
        PasswordDialog dialog = new PasswordDialog(parent, title, reminder, initialVisible, customMessage);
        dialog.setVisible(true);
        return new PasswordResult(dialog.getPassword(), dialog.isAlwaysShow());
    }

    public static class PasswordResult {
        public final char[] password;
        public final boolean alwaysShow;

        public PasswordResult(char[] password, boolean alwaysShow) {
            this.password = password;
            this.alwaysShow = alwaysShow;
        }
    }
}
