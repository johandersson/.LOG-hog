package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class PasswordDialog extends JDialog {
    private JPasswordField passwordField;
    private JButton toggleButton;
    private JButton okButton;
    private JButton cancelButton;
    private char[] password;
    private boolean visible = false;
    private String reminder;

    public PasswordDialog(Frame parent, String title, String reminder) {
        super(parent, title, true);
        this.reminder = reminder;
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    private void addIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.GRAY);
        g2.fillRect(5, 8, 6, 6); // lock body
        g2.setColor(Color.BLACK);
        g2.drawRect(5, 8, 6, 6);
        g2.fillRect(7, 5, 2, 5); // shackle
        g2.drawRect(7, 5, 2, 5);
        g2.dispose();
        setIconImage(image);
    }

    private void initComponents() {
        addIcon();
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("<html><center>Welcome back!<br>Enter your password to unlock your encrypted log.</center></html>", SwingConstants.CENTER);
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
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleVisibility();
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

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                password = passwordField.getPassword();
                setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                password = null;
                setVisible(false);
            }
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
    }

    private void toggleVisibility() {
        visible = !visible;
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

    public static char[] showPasswordDialog(Frame parent, String title, String reminder) {
        PasswordDialog dialog = new PasswordDialog(parent, title, reminder);
        dialog.setVisible(true);
        return dialog.getPassword();
    }
}