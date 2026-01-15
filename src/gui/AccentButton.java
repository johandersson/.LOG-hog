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

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import utils.TooltipHelper;

public class AccentButton extends StandardButton {
    private static final Color NORMAL_COLOR = new Color(0x2F80ED);
    private static final Color HOVER_COLOR = new Color(0x1565C0); // Darker hover
    private static final Color PRESSED_COLOR = new Color(0x0D47A1); // Much darker pressed
    private static final Color SHADOW_COLOR = new Color(47, 128, 237, 60); // Base shadow color

    public AccentButton(String text) {
        super(text, NORMAL_COLOR, SHADOW_COLOR);
        setForeground(Color.WHITE);

        // Add hover and press effects
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(HOVER_COLOR);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(NORMAL_COLOR);
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(PRESSED_COLOR);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (getBounds().contains(e.getPoint())) {
                    setBackground(HOVER_COLOR);
                } else {
                    setBackground(NORMAL_COLOR);
                }
                repaint();
            }
        });
    }

    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        setForeground(b ? Color.WHITE : new Color(0x202020)); // Even darker gray for disabled text
        if (!b) {
            TooltipHelper.enableTooltipOnDisabled(this, "Disabled in locked mode");
        } else {
            setToolTipText(null);
        }
    }
}
