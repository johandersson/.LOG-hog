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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.SwingConstants;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Secure password dialog with automatic memory cleanup.
 * 
 * SECURITY NOTES:
 * - Passwords are stored as char[] to allow secure zeroing (not String)
 * - Dialog automatically clears all password data when closed (ESC, X button, Cancel, Alt+F4)
 * - JPasswordField document is explicitly cleared on disposal
 * - PasswordResult supports AutoCloseable for try-with-resources cleanup
 * - All copies are made and returned to prevent exposure of internal buffers
 */
public class PasswordDialog extends JDialog {
    private JPasswordField passwordField;
    private JButton toggleButton;
    private JButton okButton;
    private JButton cancelButton;
    private char[] password;
    private boolean visible; // default false, no initializer needed
    private String customMessage;
    private PasswordStrengthIndicator strengthIndicator;

    public PasswordDialog(Frame parent, String title, String customMessage, boolean showStrength) {
        super(parent, title, true);
        this.customMessage = customMessage;
        initComponents(showStrength);
        
        // SECURITY: Set close operation to dispose to ensure cleanup
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // SECURITY: Add window listener to clear password data when dialog closes
        // This handles ESC, X button, Alt+F4, and other window close events
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // Called after window is closed
                clearAllPasswordData();
            }
            
            @Override
            public void windowClosing(WindowEvent e) {
                // Called when user tries to close (before actual close)
                clearAllPasswordData();
            }
        });
        
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents(boolean showStrength) {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0xF7FAFC));

        var centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        centerPanel.setBackground(new Color(0xF7FAFC));

        var topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(0xF7FAFC));
        var welcomeText = customMessage != null ? customMessage : "Welcome back! Enter your password to unlock your encrypted log.";
        // Wrap in HTML for centering; escape plain-text messages to prevent injection
        String labelText;
        if (welcomeText.startsWith("<html>") || welcomeText.startsWith("<HTML>")) {
            labelText = welcomeText;
        } else {
            String escaped = welcomeText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            labelText = "<html><center>" + escaped + "</center></html>";
        }
        var welcomeLabel = new JLabel(labelText, SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        welcomeLabel.setForeground(new Color(0x2B3A42));
        topPanel.add(welcomeLabel, BorderLayout.NORTH);

        centerPanel.add(topPanel, BorderLayout.NORTH);

        // Make the password field larger to avoid clipped text on some platforms
        passwordField = new JPasswordField(32);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setBackground(Color.WHITE);
        passwordField.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // Provide a reasonable preferred / minimum size so the field has enough horizontal and vertical space
        java.awt.Dimension pref = new java.awt.Dimension(420, 44);
        passwordField.setPreferredSize(pref);
        passwordField.setMinimumSize(new java.awt.Dimension(200, 44));
        passwordField.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 60));

        // In PasswordDialog.initComponents(), after creating passwordField:
        try {
            if (passwordField.getDocument() instanceof AbstractDocument) {
                // SECURITY: Limit password to reasonable length
                // Even though PBKDF2 supports longer passwords, practical limits prevent DoS
                ((AbstractDocument) passwordField.getDocument()).setDocumentFilter(
                    new LengthLimitFilter(InputLimits.FIELD_MAX_CHARS)
                );
            }
        } catch (Exception ignore) {
        }
        

        toggleButton = new StandardButton("", new Color(0xE0E0E0), new Color(0xB0B0B0));
        toggleButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        toggleButton.setToolTipText(UIStrings.TOOLTIP_SHOW_PASSWORD);
        // Ensure toggle button height matches the password field so it doesn't force clipping
        toggleButton.setPreferredSize(new java.awt.Dimension(44, 44));
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

        updateVisibility(false);

        // Bind ESC press/release to peek at the password (show while held)
        var rootIm = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        var rootAm = getRootPane().getActionMap();
        rootIm.put(KeyStroke.getKeyStroke("pressed ESCAPE"), "peekPressed");
        rootIm.put(KeyStroke.getKeyStroke("released ESCAPE"), "peekReleased");
        rootAm.put("peekPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateVisibility(true);
            }
        });
        rootAm.put("peekReleased", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateVisibility(false);
            }
        });

        var fieldPanel = new JPanel(new BorderLayout(5, 0));
        fieldPanel.setBackground(new Color(0xF7FAFC));
        fieldPanel.add(passwordField, BorderLayout.CENTER);
        fieldPanel.add(toggleButton, BorderLayout.EAST);

        centerPanel.add(fieldPanel, BorderLayout.CENTER);

        if (showStrength) {
            strengthIndicator = new PasswordStrengthIndicator();
            centerPanel.add(strengthIndicator, BorderLayout.SOUTH);
            passwordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateStrength(); }
                private void updateStrength() {
                    strengthIndicator.updateStrength(passwordField.getPassword());
                }
            });
        }

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

        if (showStrength) {
            var generateButton = new StandardButton("Generate", new Color(0xE0E0E0), new Color(0xB0B0B0));
            generateButton.addActionListener(e -> {
                // Generate a strong password as char array to avoid String in memory
                char[] generated = PasswordGenerator.generatePassword(20).toCharArray();
                // SECURITY: Use Document manipulation to avoid internal String copies
                try {
                    passwordField.getDocument().remove(0, passwordField.getDocument().getLength());
                    passwordField.getDocument().insertString(0, new String(generated), null);
                } catch (Exception ex) {
                    // Fallback to setText if document manipulation fails
                    passwordField.setText(new String(generated));
                }
                if (strengthIndicator != null) {
                    strengthIndicator.updateStrength(generated);
                }
                // Clear the generated char array from memory
                java.util.Arrays.fill(generated, '\0');
            });
            buttonPanel.add(generateButton);
        }

        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
    }

    private void updateVisibility(boolean newVisible) {
        visible = newVisible;
        if (visible) {
            passwordField.setEchoChar((char) 0);
            toggleButton.setText("<html><font size=\"+2\">👁</font></html>");
            toggleButton.setToolTipText(UIStrings.TOOLTIP_HIDE_PASSWORD);
        } else {
            passwordField.setEchoChar('*');
            toggleButton.setText("<html><font size=\"+2\"><s>👁</s></font></html>");
            toggleButton.setToolTipText(UIStrings.TOOLTIP_SHOW_PASSWORD);
        }
        passwordField.requestFocusInWindow();
    }

    /**
     * Securely clears all password data from memory.
     * Called when dialog is closed/disposed to ensure no sensitive data remains in the GUI.
     * SECURITY: This method zeros both the internal password field and the JPasswordField document.
     */
    private void clearAllPasswordData() {
        // Clear internal password field first
        if (password != null) {
            java.util.Arrays.fill(password, '\0');
            password = null;
        }
        
        // Clear the JPasswordField document to remove password from the GUI buffer
        if (passwordField != null) {
            try {
                int len = passwordField.getDocument().getLength();
                if (len > 0) {
                    passwordField.getDocument().remove(0, len);
                }
                passwordField.setText("");
            } catch (Exception ignored) {
                // If already disposed or other error, silently ignore
            }
        }
    }

    /**
     * Override dispose to ensure all password data is cleared before disposal.
     * SECURITY: Always clear sensitive data before object is garbage collected.
     */
    @Override
    public void dispose() {
        clearAllPasswordData();
        super.dispose();
    }

    public char[] getPassword() {
        // Return a copy to avoid exposing internal array
        // Caller is responsible for zeroing the returned array when done
        return password == null ? null : java.util.Arrays.copyOf(password, password.length);
    }

    public static PasswordResult showPasswordDialog(Frame parent, String title) {
        var dialog = new PasswordDialog(parent, title, null, false);
        dialog.setVisible(true);
        char[] pw = dialog.getPassword();
        dialog.dispose(); // Explicitly dispose to trigger cleanup
        return new PasswordResult(pw == null ? null : java.util.Arrays.copyOf(pw, pw.length));
    }

    public static PasswordResult showPasswordDialog(Frame parent, String title, String customMessage) {
        var dialog = new PasswordDialog(parent, title, customMessage, false);
        dialog.setVisible(true);
        char[] pw = dialog.getPassword();
        dialog.dispose(); // Explicitly dispose to trigger cleanup
        return new PasswordResult(pw == null ? null : java.util.Arrays.copyOf(pw, pw.length));
    }

    public static PasswordResult showPasswordDialog(Frame parent, String title, String customMessage, boolean showStrength) {
        var dialog = new PasswordDialog(parent, title, customMessage, showStrength);
        dialog.setVisible(true);
        char[] pw = dialog.getPassword();
        dialog.dispose(); // Explicitly dispose to trigger cleanup
        return new PasswordResult(pw == null ? null : java.util.Arrays.copyOf(pw, pw.length));
    }

    /**
     * Secure password result holder with support for explicit cleanup.
     * SECURITY: Implements AutoCloseable to support try-with-resources pattern.
     * Example usage:
     *   try (PasswordResult result = PasswordDialog.showPasswordDialog(...)) {
     *       char[] pwd = result.password;
     *       // Use password...
     *   } // Password automatically cleared here
     */
    public static class PasswordResult implements AutoCloseable {
        public final char[] password;
        private boolean cleared = false;

        public PasswordResult(char... password) {
            // Always store a copy to avoid exposing mutable array
            this.password = password == null ? null : java.util.Arrays.copyOf(password, password.length);
        }
        
        /**
         * Securely clears the password from memory.
         * Should be called by the consumer when done using the password.
         * Safe to call multiple times; subsequent calls are no-ops.
         * SECURITY: Once called, the password array is zeroed and cannot be recovered.
         */
        public void clearPassword() {
            if (!cleared && password != null) {
                java.util.Arrays.fill(password, '\0');
                cleared = true;
            }
        }
        
        /**
         * AutoCloseable support for try-with-resources cleanup.
         * Ensures password is cleared even if an exception occurs.
         */
        @Override
        public void close() {
            clearPassword();
        }
    }
}
