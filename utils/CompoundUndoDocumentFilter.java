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