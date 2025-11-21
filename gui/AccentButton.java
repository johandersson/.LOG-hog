package gui;

import java.awt.*;
import javax.swing.*;

public class AccentButton extends JButton {
    public AccentButton(String text) {
        super(text);
        setForeground(Color.WHITE);
        setBackground(new Color(0x2F80ED));
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(true);
        setContentAreaFilled(true);   // ensure LAF paints the button background
        setBorderPainted(false);      // flat look, prevents LAF from drawing its own border
        setFont(getFont().deriveFont(Font.BOLD, 12f));
    }
}