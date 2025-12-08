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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class NavItem extends JPanel {
    private final JLabel label;
    private final int tabIndex;
    private final JTabbedPane tabPane;
    private final Runnable onClick;
    private boolean hovered = false;

    public NavItem(String title, int tabIndex, JTabbedPane tabPane, Runnable onClick) {
        this.tabIndex = tabIndex;
        this.tabPane = tabPane;
        this.onClick = onClick;

        setLayout(new BorderLayout());
        setOpaque(false); // never fill with an opaque rectangular background by default
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        setPreferredSize(new Dimension(160, 36));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(0, 0, 0, 0));

        label = new JLabel(title);
        label.setBorder(new EmptyBorder(8, 8, 8, 8));
        label.setOpaque(false);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
        label.setForeground(new Color(0x5E6A70));
        add(label, BorderLayout.WEST);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) {
                    onClick.run();
                } else if (tabPane != null && tabIndex >= 0 && tabIndex < tabPane.getTabCount()) {
                    tabPane.setSelectedIndex(tabIndex);
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                // only request repaint; do not call setOpaque(true) — painting is done in paintComponent
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });

        if (tabPane != null) {
            tabPane.addChangeListener(e -> updateVisualState());
        }

        updateVisualState();
    }

    private void updateVisualState() {
        var active = tabPane != null && tabPane.getSelectedIndex() == tabIndex;
        label.setFont(label.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN, 13f));
        label.setForeground(active ? new Color(0x2B3A42) : new Color(0x5E6A70));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Paint nothing when not active/hovered to avoid any rectangular artifacts
        var active = tabPane != null && tabPane.getSelectedIndex() == tabIndex;
        if (active || hovered) {
            var g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                var fill = active ? new Color(0xEAF3FF) : new Color(0xEEF6FF);
                g2.setColor(fill);
                var arc = 10;
                var pad = 2;
                g2.fillRoundRect(pad, pad, getWidth() - pad * 2, getHeight() - pad * 2, arc, arc);
            } finally {
                g2.dispose();
            }
        }
        super.paintComponent(g); // allow children (label) to paint on top
    }
}
