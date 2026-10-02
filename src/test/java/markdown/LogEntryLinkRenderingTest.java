package markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import org.junit.jupiter.api.Test;

class LogEntryLinkRenderingTest {
    private StyledDocument render(String body) throws Exception {
        return MarkdownRenderer.buildDocumentFromEntries(
                List.of(List.of("12:00 2026-07-15", body)), null);
    }

    @Test
    void rendersBothTimestampOrdersAsInternalLinks() throws Exception {
        var doc = render("See [2022-12-12 13:23] and [13:23 2022-12-12].");
        var text = doc.getText(0, doc.getLength());
        assertTrue(text.contains("[2022-12-12 13:23]"));
        for (String label : List.of("2022-12-12 13:23", "13:23 2022-12-12")) {
            assertEquals("loghog:13:23 2022-12-12",
                    doc.getCharacterElement(text.indexOf(label)).getAttributes().getAttribute("href"));
        }
    }

    @Test
    void preservesExternalLinksAndDoesNotActivateInvalidDatesOrCode() throws Exception {
        var doc = render("[2022-12-12 13:23](https://example.com) "
                + "`[13:23 2022-12-12]` [13:23 2022-02-30]");
        var text = doc.getText(0, doc.getLength());
        assertEquals("https://example.com",
                doc.getCharacterElement(text.indexOf("2022-12-12")).getAttributes().getAttribute("href"));
        assertNull(doc.getCharacterElement(text.indexOf("[13:23")).getAttributes().getAttribute("href"));
        assertNull(doc.getCharacterElement(text.lastIndexOf("[13:23")).getAttributes().getAttribute("href"));
    }

    @Test
    void clickRoutesInternallyAndListenerRegistrationStaysIdempotent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                var pane = new JTextPane();
                pane.setDocument(render("[13:23 2022-12-12]"));
                pane.setSize(600, 150);
                var selected = new AtomicReference<String>();
                LinkHandler.addLinkListeners(pane, selected::set);
                int listeners = pane.getMouseListeners().length;
                LinkHandler.addLinkListeners(pane);
                assertEquals(listeners, pane.getMouseListeners().length);
                int offset = pane.getText().indexOf("[13:23") + 2;
                var bounds = pane.modelToView2D(offset);
                var event = new MouseEvent(pane, MouseEvent.MOUSE_CLICKED, 0, 0,
                        (int) bounds.getX(), (int) bounds.getY() + 2, 1, false, MouseEvent.BUTTON1);
                for (var listener : pane.getMouseListeners()) listener.mouseClicked(event);
                assertEquals("13:23 2022-12-12", selected.get());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }
}
