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

import javax.swing.JComponent;
import javax.swing.JDialog;

/**
 * A reusable hover window that appears on hover and disappears after a delay.
 * Can contain any JComponent.
 */
public class HoverWindow {
    private final JDialog dialog;
    private Runnable onShowAction;
    private Runnable onHideAction;

    public HoverWindow(JComponent content) {
        dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.add(content);
        dialog.pack();
        dialog.setAlwaysOnTop(true);
    }

    public void showAt(int x, int y) {
        dialog.setLocation(x, y);
        dialog.setVisible(true);
        if (onShowAction != null) {
            onShowAction.run();
        }
    }

    public void hide() {
        dialog.setVisible(false);
        if (onHideAction != null) {
            onHideAction.run();
        }
    }

    public void setOnShowAction(Runnable action) {
        this.onShowAction = action;
    }

    public void setOnHideAction(Runnable action) {
        this.onHideAction = action;
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void dispose() {
        hide();
        if (dialog != null) {
            dialog.dispose();
        }
    }
}