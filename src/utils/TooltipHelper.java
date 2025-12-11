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

package utils;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;

/**
 * Utility class to enable tooltips on disabled components.
 */
public class TooltipHelper {

    /**
     * Enables tooltips on a disabled component by adding a mouse listener that manually triggers ToolTipManager.
     * @param comp the component to enable tooltips on when disabled
     * @param tooltip the tooltip text to show when the component is disabled
     */
    public static void enableTooltipOnDisabled(JComponent comp, String tooltip) {
        comp.setToolTipText(tooltip);
        comp.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!comp.isEnabled()) {
                    ToolTipManager.sharedInstance().mouseEntered(e);
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!comp.isEnabled()) {
                    ToolTipManager.sharedInstance().mouseExited(e);
                }
            }
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                if (!comp.isEnabled()) {
                    ToolTipManager.sharedInstance().mouseMoved(e);
                }
            }
        });
    }
}