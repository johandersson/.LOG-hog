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

import java.util.Timer;
import java.util.TimerTask;
import javax.swing.text.*;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

public class CompoundUndoDocumentFilter extends DocumentFilter {
    private final UndoManager undoManager;
    private CompoundEdit compoundEdit;
    private Timer timer;

    public CompoundUndoDocumentFilter(UndoManager undoManager) {
        this.undoManager = undoManager;
        this.timer = new Timer();
    }

    private void startCompoundEdit() {
        if (compoundEdit == null || !compoundEdit.isInProgress()) {
            compoundEdit = new CompoundEdit();
            undoManager.addEdit(compoundEdit);
        }
        restartTimer();
    }

    private void restartTimer() {
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                endCompoundEdit();
            }
        }, 1000); // End compound edit after 1 second of inactivity
    }

    private void endCompoundEdit() {
        if (compoundEdit != null && compoundEdit.isInProgress()) {
            compoundEdit.end();
            compoundEdit = null;
        }
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        startCompoundEdit();
        super.insertString(fb, offset, string, attr);
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        startCompoundEdit();
        super.remove(fb, offset, length);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        startCompoundEdit();
        super.replace(fb, offset, length, text, attrs);
    }
}
