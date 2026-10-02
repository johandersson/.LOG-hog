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
    @Test
    void alternateEntryHeadersKeepTimestampStyleAndBodyLinksRemainClickable() throws Exception {
        for (String header : List.of("2022-12-19 13:23", "19/12/2022 13:23",
                "12/19/2022 13:23", "19.12.2022 13:23", "19-12-2022 13:23")) {
            var pane = new JTextPane();
            MarkdownRenderer.renderMarkdown(pane,
                    List.of(".LOG", "", header, "See [2020-01-01 00:00]."), false);
            var doc = pane.getStyledDocument();
            var text = pane.getText();
            assertEquals("timestamp", doc.getCharacterElement(0).getAttributes()
                    .getAttribute(javax.swing.text.StyleConstants.NameAttribute), header);
            assertEquals("loghog:00:00 2020-01-01",
                    doc.getCharacterElement(text.indexOf("[2020")).getAttributes().getAttribute("href"));
        }
    }

    @Test
    void invalidAlternateDatesCodeAndOrdinaryBracketsRemainPlainText() throws Exception {
        for (String label : List.of("[29/02/2023 13:23]", "[02/29/2023 13:23]",
                "[31.04.2022 13:23]", "[31-04-2022 13:23]", "[ordinary text]")) {
            var doc = render("See " + label);
            var text = doc.getText(0, doc.getLength());
            assertNull(doc.getCharacterElement(text.indexOf(label)).getAttributes().getAttribute("href"));
        }
        var doc = render("`[19/12/2022 13:23]` [19/12/2022 13:23](https://example.com)");
        var text = doc.getText(0, doc.getLength());
        assertNull(doc.getCharacterElement(text.indexOf("[19/12")).getAttributes().getAttribute("href"));
        assertEquals("https://example.com", doc.getCharacterElement(text.lastIndexOf("19/12"))
                .getAttributes().getAttribute("href"));
    }

    @Test
    void rendersEverySupportedDateFormatAndRoutesClicksCanonically() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                for (String timestamp : List.of("19/12/2022 13:23", "12/19/2022 13:23",
                        "19.12.2022 13:23", "19-12-2022 13:23")) {
                    var pane = new JTextPane();
                    pane.setDocument(render("See [" + timestamp + "]."));
                    pane.setSize(600, 150);
                    var text = pane.getText();
                    int offset = text.indexOf(timestamp);
                    assertEquals("loghog:13:23 2022-12-19",
                            pane.getStyledDocument().getCharacterElement(offset)
                                    .getAttributes().getAttribute("href"), timestamp);
                    var selected = new AtomicReference<String>();
                    LinkHandler.addLinkListeners(pane, selected::set);
                    var bounds = pane.modelToView2D(offset);
                    var event = new MouseEvent(pane, MouseEvent.MOUSE_CLICKED, 0, 0,
                            (int) bounds.getX(), (int) bounds.getY() + 2, 1, false, MouseEvent.BUTTON1);
                    for (var listener : pane.getMouseListeners()) listener.mouseClicked(event);
                    assertEquals("13:23 2022-12-19", selected.get(), timestamp);
                }
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        });
    }

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
