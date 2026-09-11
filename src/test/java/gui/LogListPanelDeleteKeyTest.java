package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

class LogListPanelDeleteKeyTest {

    @Test
    void deleteKeyBindingTriggersDeleteAction() {
        JList<String> logList = new JList<>();
        AtomicInteger invocations = new AtomicInteger();

        LogListPanel.bindDeleteSelectedEntries(logList, invocations::incrementAndGet);

        InputMap inputMap = logList.getInputMap(JComponent.WHEN_FOCUSED);
        Object actionKey = inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        assertEquals("deleteSelectedEntries", actionKey);

        Action action = logList.getActionMap().get(actionKey);
        assertNotNull(action);

        action.actionPerformed(new ActionEvent(logList, ActionEvent.ACTION_PERFORMED, "delete"));

        assertEquals(1, invocations.get());
    }
}
