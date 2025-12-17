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
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

public class PasswordStrengthIndicator extends JPanel {
    private JProgressBar strengthBar;
    private JLabel strengthLabel;

    public PasswordStrengthIndicator() {
        setLayout(new BorderLayout());
        strengthBar = new JProgressBar(0, 100);
        strengthBar.setStringPainted(true);
        strengthBar.setPreferredSize(new Dimension(200, 20));
        strengthLabel = new JLabel("Password Strength: Weak");
        strengthLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(strengthLabel, BorderLayout.NORTH);
        add(strengthBar, BorderLayout.CENTER);
    }

    public void updateStrength(char[] password) {
        int score = calculateStrength(password);
        strengthBar.setValue(score);

        String level;
        Color color;
        if (score < 25) {
            level = "Weak";
            color = Color.RED;
        } else if (score < 50) {
            level = "Fair";
            color = Color.ORANGE;
        } else if (score < 75) {
            level = "Good";
            color = Color.YELLOW;
        } else {
            level = "Strong";
            color = Color.GREEN;
        }

        strengthBar.setForeground(color);
        strengthLabel.setText("Password Strength: " + level);
    }

    public static int calculateStrength(char[] password) {
        int score = 0;
        boolean hasLower = false, hasUpper = false, hasDigit = false, hasSpecial = false;

        for (char c : password) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        // Length score: up to 40 points
        score += Math.min(password.length * 2, 40);

        // Character variety: 15 points each
        if (hasLower) score += 15;
        if (hasUpper) score += 15;
        if (hasDigit) score += 15;
        if (hasSpecial) score += 15;

        return Math.min(score, 100);
    }
}