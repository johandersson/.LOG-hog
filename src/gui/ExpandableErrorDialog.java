package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Simple reusable dialog that shows an HTML message and an expandable developer area
 * containing a full stack trace when a Throwable is provided.
 */
public class ExpandableErrorDialog {

    public static void show(Component parent, String title, String htmlMessage, Throwable t) {
        Runnable r = () -> {
            JPanel main = new JPanel(new BorderLayout(8, 8));
            JLabel messageLabel = new JLabel(htmlMessage);
            main.add(messageLabel, BorderLayout.NORTH);

            JPanel bottom = new JPanel();
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

            // Developer area (hidden by default)
            JTextArea traceArea = new JTextArea();
            traceArea.setEditable(false);
            traceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            traceArea.setLineWrap(false);

            JScrollPane traceScroll = new JScrollPane(traceArea);
            traceScroll.setPreferredSize(new Dimension(640, 240));
            traceScroll.setBorder(BorderFactory.createEtchedBorder());
            traceScroll.setVisible(false);

            JButton toggle = new JButton("Show details");
            toggle.addActionListener(e -> {
                boolean visible = !traceScroll.isVisible();
                traceScroll.setVisible(visible);
                toggle.setText(visible ? "Hide details" : "Show details");
                // Resize parent dialog
                SwingUtilities.getWindowAncestor(main).pack();
            });

            bottom.add(toggle);
            bottom.add(Box.createVerticalStrut(8));
            bottom.add(traceScroll);

            if (t != null) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                traceArea.setText(sw.toString());
            } else {
                // No exception - disable toggle
                toggle.setEnabled(false);
            }

            main.add(bottom, BorderLayout.CENTER);

            final javax.swing.JOptionPane pane = new javax.swing.JOptionPane(main, javax.swing.JOptionPane.ERROR_MESSAGE, javax.swing.JOptionPane.DEFAULT_OPTION);
            final JDialog dialog = pane.createDialog(parent, title == null ? "Error" : title);
            dialog.setResizable(true);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        };

        if (SwingUtilities.isEventDispatchThread()) r.run(); else SwingUtilities.invokeLater(r);
    }
}
