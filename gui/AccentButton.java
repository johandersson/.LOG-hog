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
