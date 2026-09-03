package markdown;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class MarkdownRendererBehaviorTest {

    @Test
    void headingRenderingFromSplitLines() throws Exception {
        StyledDocument doc = MarkdownRenderer.buildDocumentFromEntries(List.of(
            List.of("12:00 2026-07-15", "# Heading", "Body text")
        ), null);

        assertHeadingDocument(doc);
    }

    @Test
    void headingRenderingFromMultilineBody() throws Exception {
        StyledDocument doc = MarkdownRenderer.buildDocumentFromEntries(List.of(
            List.of("12:00 2026-07-15", "# Heading\nBody text")
        ), null);

        assertHeadingDocument(doc);
    }

    private static void assertHeadingDocument(StyledDocument doc) throws Exception {
        String text = doc.getText(0, doc.getLength());
        assertEquals("12:00 2026-07-15\nHeading\nBody text", text);
        assertFalse(text.contains("\n\nHeading"), "Unexpected blank line before heading: [" + text + "]");

        int headingOffset = text.indexOf("Heading");
        assertTrue(headingOffset >= 0, "Heading text missing from rendered document");

        var attrs = doc.getCharacterElement(headingOffset).getAttributes();
        assertEquals(MarkdownStyle.FONT_SIZE_H1, StyleConstants.getFontSize(attrs));
        assertTrue(StyleConstants.isBold(attrs), "Heading style should be bold");
    }
}
