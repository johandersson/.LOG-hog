package main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class LockShortcutDispatcherTest {
    private JFrame frame;
    private JTextArea text;
    private AtomicBoolean locked;
    private AtomicInteger lockCalls;
    private LockShortcutDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        frame = new JFrame();
        text = new JTextArea();
        frame.add(text);
        locked = new AtomicBoolean();
        lockCalls = new AtomicInteger();
        dispatcher = new LockShortcutDispatcher(frame, locked::get, () -> {
            lockCalls.incrementAndGet();
            locked.set(true);
        });
    }

    @AfterEach
    void tearDown() {
        if (frame != null) {
            frame.dispose();
        }
    }

    @Test
    void locksFromFocusedEditorAndConsumesShortcut() {
        KeyEvent event = key(text, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L);
        assertTrue(dispatcher.dispatchKeyEvent(event));
        assertTrue(event.isConsumed());
        assertTrue(locked.get());
        assertEquals(1, lockCalls.get());
    }

    @Test
    void locksFromOwnedModalDialog() {
        JDialog dialog = new JDialog(frame, true);
        try {
            JTextArea input = new JTextArea();
            dialog.add(input);
            dialog.pack();
            assertTrue(dispatcher.dispatchKeyEvent(
                    key(input, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L)));
            assertEquals(1, lockCalls.get());
            assertFalse(dialog.isDisplayable());
        } finally {
            dialog.dispose();
        }
    }

    @Test
    @Timeout(5)
    void closesSensitiveConfirmationWithoutConfirmingItsAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextArea preview = new JTextArea("Sensitive entry preview");
            Timer shortcut = new Timer(50, e -> dispatcher.dispatchKeyEvent(
                    key(preview, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L)));
            shortcut.setRepeats(false);
            shortcut.start();
            try {
                int result = JOptionPane.showConfirmDialog(frame, preview, "Delete Entry",
                        JOptionPane.YES_NO_OPTION);
                assertEquals(JOptionPane.CLOSED_OPTION, result);
                assertTrue(locked.get());
                assertEquals(1, lockCalls.get());
            } finally {
                shortcut.stop();
            }
        });
    }

    @Test
    void repeatedShortcutDoesNotRelockOrUnlock() {
        for (int i = 0; i < 2; i++) {
            assertTrue(dispatcher.dispatchKeyEvent(
                    key(text, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L)));
        }
        assertEquals(1, lockCalls.get());
        assertTrue(locked.get());
    }

    @Test
    void shortcutDismissesDialogsEvenWhenAlreadyLocked() {
        locked.set(true);
        JDialog dialog = new JDialog(frame, true);
        try {
            JTextArea input = new JTextArea();
            dialog.add(input);
            dialog.pack();
            assertTrue(dispatcher.dispatchKeyEvent(
                    key(input, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L)));
            assertFalse(dialog.isDisplayable());
            assertEquals(0, lockCalls.get());
            assertTrue(locked.get());
        } finally {
            dialog.dispose();
        }
    }

    @Test
    void ignoresOtherKeysModifiersAndReleasedEvents() {
        assertFalse(dispatcher.dispatchKeyEvent(key(text, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_L)));
        assertFalse(dispatcher.dispatchKeyEvent(
                key(text, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_F)));
        assertFalse(dispatcher.dispatchKeyEvent(key(text, KeyEvent.KEY_PRESSED,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_L)));
        assertFalse(dispatcher.dispatchKeyEvent(
                key(text, KeyEvent.KEY_RELEASED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L)));
        assertEquals(0, lockCalls.get());
    }

    @Test
    void ignoresUnownedComponentsAndConsumedEvents() {
        assertFalse(dispatcher.dispatchKeyEvent(
                key(new JTextArea(), KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L)));
        JFrame unrelated = new JFrame();
        try {
            JTextArea input = new JTextArea();
            unrelated.add(input);
            assertFalse(dispatcher.dispatchKeyEvent(
                    key(input, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L)));
        } finally {
            unrelated.dispose();
        }
        KeyEvent consumed = key(text, KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_L);
        consumed.consume();
        assertFalse(dispatcher.dispatchKeyEvent(consumed));
        assertEquals(0, lockCalls.get());
    }

    private static KeyEvent key(Component source, int id, int modifiers, int code) {
        return new KeyEvent(source, id, System.currentTimeMillis(), modifiers, code, KeyEvent.CHAR_UNDEFINED);
    }
}
