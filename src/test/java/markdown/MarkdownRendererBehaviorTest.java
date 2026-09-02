package markdown;

import java.util.List;

import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public final class MarkdownRendererBehaviorTest {
    private MarkdownRendererBehaviorTest() {}

    public static void main(String[] args) throws Exception {
        testHeadingRenderingFromSplitLines();
        testHeadingRenderingFromMultilineBody();
        testsupport.TestLog.out("MarkdownRendererBehaviorTest passed");
    }

    private static void testHeadingRenderingFromSplitLines() throws Exception {
        StyledDocument doc = MarkdownRenderer.buildDocumentFromEntries(List.of(
            List.of("12:00 2026-07-15", "# Heading", "Body text")
        ), null);

        assertHeadingDocument(doc);
    }

    private static void testHeadingRenderingFromMultilineBody() throws Exception {
        StyledDocument doc = MarkdownRenderer.buildDocumentFromEntries(List.of(
            List.of("12:00 2026-07-15", "# Heading\nBody text")
        ), null);

        assertHeadingDocument(doc);
    }

    private static void assertHeadingDocument(StyledDocument doc) throws Exception {
        String text = doc.getText(0, doc.getLength());
        String expected = "12:00 2026-07-15\nHeading\nBody text";
        if (!expected.equals(text)) {
            throw new AssertionError("Unexpected rendered text: [" + text + "]");
        }
        if (text.contains("\n\nHeading")) {
            throw new AssertionError("Unexpected blank line before heading: [" + text + "]");
        }

        int headingOffset = text.indexOf("Heading");
        if (headingOffset < 0) {
            throw new AssertionError("Heading text missing from rendered document");
        }

        var attrs = doc.getCharacterElement(headingOffset).getAttributes();
        int fontSize = StyleConstants.getFontSize(attrs);
        boolean bold = StyleConstants.isBold(attrs);
        if (fontSize != MarkdownStyle.FONT_SIZE_H1 || !bold) {
            throw new AssertionError("Heading style not applied correctly");
        }
    }
}