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

    public void updateStrength(char... password) {
        int score = calculateStrength(password);
        strengthBar.setValue(score);

        String level;
        Color color;
        if (score < 25) {
            level = "Weak";
            color = Color.RED;
        } else if (score < 45) {
            level = "Fair";
            color = Color.ORANGE;
        } else if (score < 65) {
            level = "Good";
            color = Color.YELLOW;
        } else if (score < 80) {
            level = "Strong";
            color = new Color(34, 139, 34); // Forest green
        } else {
            level = "Very Strong";
            color = new Color(0, 128, 0); // Darker green
        }

        strengthBar.setForeground(color);
        strengthLabel.setText("Password Strength: " + level);
    }

    public static int calculateStrength(char... password) {
        int score = 0;
        int length = password.length;
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        int uniqueChars;

        // Count unique characters for entropy estimation
        java.util.Set<Character> unique = new java.util.HashSet<>();
        for (char c : password) {
            unique.add(c);
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (c != ' ') hasSpecial = true; // FIX: Don't count spaces as special characters
        }
        uniqueChars = unique.size();

        // Length score: more aggressive scaling for passphrases
        // 20 chars = 30 points, 30 chars = 45 points, 40+ chars = 60 points
        if (length >= 40) {
            score += 60;
        } else if (length >= 30) {
            score += 45;
        } else if (length >= 20) {
            score += 30;
        } else {
            score += length; // 1 point per char under 20
        }

        // Character variety: 10 points each (max 40)
        if (hasLower) score += 10;
        if (hasUpper) score += 10;
        if (hasDigit) score += 10;
        if (hasSpecial) score += 10;

        // Unique characters bonus (entropy indicator)
        // More unique chars = higher entropy
        if (uniqueChars >= 20) {
            score += 15; // Very diverse
        } else if (uniqueChars >= 15) {
            score += 10;
        } else if (uniqueChars >= 10) {
            score += 5;
        }

        // Bonus for very long passwords (likely passphrases)
        if (length >= 50) {
            score += 10; // Passphrase bonus
        }

        // PENALTY: Check for repetitive characters (e.g., "aaaaaaaaaa", "111111")
        int maxConsecutive = 1;
        int currentConsecutive = 1;
        for (int i = 1; i < length; i++) {
            if (password[i] == password[i - 1]) {
                currentConsecutive++;
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
            } else {
                currentConsecutive = 1;
            }
        }
        
        if (maxConsecutive >= 4) {
            score -= Math.min(maxConsecutive * 3, 30); // Up to -30 points for repetitive chars
        }

        // PENALTY: Check for sequential patterns (123456, abcdef, fedcba)
        int sequentialCount = 0;
        for (int i = 2; i < length; i++) {
            char c1 = password[i - 2];
            char c2 = password[i - 1];
            char c3 = password[i];
            
            // Check ascending sequences (123, abc)
            if (c2 == c1 + 1 && c3 == c2 + 1) {
                sequentialCount++;
            }
            // Check descending sequences (321, cba)
            else if (c2 == c1 - 1 && c3 == c2 - 1) {
                sequentialCount++;
            }
        }
        
        if (sequentialCount > 0) {
            score -= Math.min(sequentialCount * 5, 25); // Up to -25 points for sequential patterns
        }

        // PENALTY: Check for repeated patterns (e.g., "abcabcabc")
        if (length >= 6) {
            for (int patternLen = 3; patternLen <= length / 2; patternLen++) {
                int repetitions = 0;
                int pos = 0;
                while (pos + patternLen <= length) {
                    // Check if current pattern matches the initial pattern
                    boolean matches = true;
                    for (int i = 0; i < patternLen; i++) {
                        if (password[pos + i] != password[i]) {
                            matches = false;
                            break;
                        }
                    }
                    
                    if (matches) {
                        repetitions++;
                        pos += patternLen;
                    } else {
                        break;
                    }
                }
                
                if (repetitions >= 3) {
                    score -= 20; // -20 points for repeated patterns
                    break; // Only apply penalty once
                }
            }
        }

        return Math.max(0, Math.min(score, 100)); // Ensure score stays in 0-100 range
    }
}