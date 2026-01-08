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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class StatusBar extends JPanel {
    private final JLabel messageLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 12, 8, 12));
        setBackground(Color.WHITE);

        messageLabel = new JLabel(""); // Start empty, will be set by tab listener
        messageLabel.setFont(messageLabel.getFont().deriveFont(Font.PLAIN, 12f));
        messageLabel.setForeground(new Color(0x394B54));
        add(messageLabel, BorderLayout.WEST);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}