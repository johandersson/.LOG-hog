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

package main;

import java.awt.KeyEventDispatcher;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;

import javax.swing.SwingUtilities;

final class LockShortcutDispatcher implements KeyEventDispatcher {
    private final Window owner;
    private final BooleanSupplier isLocked;
    private final Runnable lock;

    LockShortcutDispatcher(Window owner, BooleanSupplier isLocked, Runnable lock) {
        this.owner = owner;
        this.isLocked = isLocked;
        this.lock = lock;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.isConsumed() || event.getID() != KeyEvent.KEY_PRESSED
                || event.getKeyCode() != KeyEvent.VK_L
                || event.getModifiersEx() != InputEvent.CTRL_DOWN_MASK) {
            return false;
        }

        Window window = event.getComponent() instanceof Window
                ? (Window) event.getComponent()
                : SwingUtilities.getWindowAncestor(event.getComponent());
        while (window != null && window != owner) {
            window = window.getOwner();
        }
        if (window == null) {
            return false;
        }

        event.consume();
        if (!isLocked.getAsBoolean()) {
            lock.run();
        }
        dismissOwnedWindows(owner);
        return true;
    }

    private static void dismissOwnedWindows(Window parent) {
        for (Window window : parent.getOwnedWindows()) {
            dismissOwnedWindows(window);
            window.dispose();
        }
    }
}
