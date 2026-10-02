package gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.StyledDocument;

import org.junit.jupiter.api.Test;

import markdown.MarkdownRenderer;

class FullLogPanelLockTest {

    @Test
    void resetDocumentClearsContentAndAttributes() throws Exception {
        HighlightableTextPane pane = new HighlightableTextPane();
        StyledDocument doc = MarkdownRenderer.buildDocumentFromEntries(List.of(
            List.of("13:00 2026-09-11", "secret [link](https://example.com) https://example.org")), null);
        pane.setDocument(doc);
        assertTrue(pane.getDocument().getLength() > 0);

        FullLogPanel.resetDocument(pane);
        assertEquals(0, pane.getDocument().getLength());

        pane.setText("File locked.");
        StyledDocument fresh = pane.getStyledDocument();
        for (int i = 0; i < fresh.getLength(); i++) {
            AttributeSet a = fresh.getCharacterElement(i).getAttributes();
            for (java.util.Enumeration<?> e = a.getAttributeNames(); e.hasMoreElements();) {
                Object n = e.nextElement();
                if (n == AttributeSet.NameAttribute || n == AttributeSet.ResolveAttribute) continue;
                String name = String.valueOf(n).toLowerCase();
                assertTrue(!name.contains("link") && !name.contains("url") && !name.contains("href"),
                    "leaked attribute " + n);
            }
        }
        assertNull(fresh.getCharacterElement(0).getAttributes().getAttribute("link"));
    }
}
