package utils;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

class ShortcutBindingTest {

    @Test
    void undoRedoTextAreaUsesPlatformShortcutMask() {
        UndoRedoTextArea area = new UndoRedoTextArea();
        int mask = PlatformSupport.menuShortcutMask();
        Object undoAction = area.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask));
        Object redoAction = area.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mask));
        assertEquals("Undo", undoAction);
        assertEquals("Redo", redoAction);
    }
}
