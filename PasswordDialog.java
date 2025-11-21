import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (reminder != null && !reminder.trim().isEmpty()) {
            JLabel reminderLabel = new JLabel("Reminder: " + reminder);
            reminderLabel.setForeground(Color.GRAY);
            centerPanel.add(reminderLabel, BorderLayout.NORTH);
        }

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