package gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.util.function.Predicate;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import utils.LogEntryLink;

public final class LogLinkDialog extends JPanel {
    private static final long serialVersionUID = 1L;
    final JTextField timestampField = new JTextField(24);
    final JLabel errorLabel = new JLabel(" ");
    final JButton insertButton = new AccentButton("OK");
    final JButton cancelButton = new AccentButton("Cancel");

    LogLinkDialog(JTextComponent target, Predicate<String> entryExists, Runnable close) {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        var input = new JPanel(new BorderLayout(8, 8));
        input.add(new JLabel("Log date and time (HH:mm yyyy-MM-dd):"), BorderLayout.NORTH);
        input.add(timestampField, BorderLayout.CENTER);
        input.add(new JLabel("<html>Also accepts yyyy-MM-dd HH:mm, dd/MM/yyyy HH:mm,<br>"
                + "MM/dd/yyyy HH:mm, dd.MM.yyyy HH:mm, dd-MM-yyyy HH:mm.<br>"
                + "Ambiguous slash dates use day/month/year.</html>"), BorderLayout.SOUTH);
        add(input, BorderLayout.NORTH);
        errorLabel.setForeground(java.awt.Color.RED);
        add(errorLabel, BorderLayout.CENTER);
        var buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(insertButton);
        buttons.add(cancelButton);
        add(buttons, BorderLayout.SOUTH);
        insertButton.addActionListener(e -> {
            String timestamp;
            try {
                timestamp = LogEntryLink.normalize(timestampField.getText());
            } catch (IllegalArgumentException ex) {
                showError("Enter a valid date and time: HH:mm yyyy-MM-dd.");
                return;
            }
            try {
                if (!entryExists.test(timestamp)) {
                    showError("No log entry exists at that date and time.");
                    return;
                }
            } catch (RuntimeException ex) {
                showError("Unable to check log entries. Try again or cancel.");
                return;
            }
            if (!target.isEditable() || !target.isEnabled()) return;
            target.replaceSelection("[" + timestamp + "]");
            close.run();
            target.requestFocusInWindow();
        });
        cancelButton.addActionListener(e -> close.run());
    }

    private void showError(String message) {
        errorLabel.setText(message);
        timestampField.requestFocusInWindow();
    }

    public static void showInsertLogLinkDialog(JTextComponent target, Predicate<String> entryExists) {
        if (target == null || !target.isEditable() || !target.isEnabled()) return;
        var parent = SwingUtilities.getWindowAncestor(target);
        var dialog = new JDialog(parent, "Insert Log Link", Dialog.ModalityType.APPLICATION_MODAL);
        var content = new LogLinkDialog(target, entryExists, dialog::dispose);
        dialog.setContentPane(content);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getRootPane().setDefaultButton(content.insertButton);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setSize(Math.max(500, dialog.getWidth()), dialog.getHeight());
        dialog.setLocationRelativeTo(parent);
        SwingUtilities.invokeLater(() -> content.timestampField.requestFocusInWindow());
        dialog.setVisible(true);
    }
}
