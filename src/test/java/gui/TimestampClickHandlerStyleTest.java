package gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import markdown.MarkdownRenderer;

public class TimestampClickHandlerStyleTest {

    @Test
    void onlyRealEntryTimestampHasTimestampStyle() {
        HighlightableTextPane pane = new HighlightableTextPane();
        List<List<String>> entries = List.of(
            List.of(
                "10:00 2026-09-10",
                "Regular line",
                "11:11 2026-09-10",
                "Another regular line"
            )
        );

        MarkdownRenderer.renderMarkdownFromEntries(pane, entries, false);
        String rendered = pane.getText();

        int entryTimestampPos = rendered.indexOf("10:00 2026-09-10");
        int inlineTimestampPos = rendered.indexOf("11:11 2026-09-10");

        assertTrue(entryTimestampPos >= 0);
        assertTrue(inlineTimestampPos >= 0);
        assertTrue(TimestampClickHandler.hasTimestampStyleAtPosition(pane.getStyledDocument(), entryTimestampPos));
        assertFalse(TimestampClickHandler.hasTimestampStyleAtPosition(pane.getStyledDocument(), inlineTimestampPos));
    }
}
