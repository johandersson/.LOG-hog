package gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.text.StyledDocument;

import org.junit.jupiter.api.Test;

import markdown.MarkdownRenderer;

class TimestampClickHandlerTest {

    @Test
    void onlyRealTimestampLinesUseTimestampStyle() throws Exception {
        StyledDocument doc = MarkdownRenderer.buildDocumentFromEntries(List.of(
            List.of(
                "13:00 2026-09-11",
                "Body text before",
                "12:34 2026-09-11",
                "Body text after"
            )
        ), null);

        String text = doc.getText(0, doc.getLength());
        int realTimestampOffset = text.indexOf("13:00 2026-09-11");
        int embeddedTimestampOffset = text.indexOf("12:34 2026-09-11");

        assertTrue(realTimestampOffset >= 0, "Expected real timestamp line in rendered document");
        assertTrue(embeddedTimestampOffset >= 0, "Expected embedded timestamp-like text in rendered document");
        assertTrue(TimestampClickHandler.hasTimestampStyleAtPosition(doc, realTimestampOffset));
        assertFalse(TimestampClickHandler.hasTimestampStyleAtPosition(doc, embeddedTimestampOffset));
    }
}
