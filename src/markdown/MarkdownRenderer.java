/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package markdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Markdown renderer with centralized styling and consistent spacing.
 * 
 * SINGLE POINT OF REFERENCE for all markdown rendering in LogHog.
 * All markdown display goes through this class to ensure consistency.
 * 
 * SPACING RULES (strictly enforced):
 * - Between entries: Always 2 blank lines (controlled by LogFileFormat.DISPLAY_ENTRY_SEPARATOR_BLANKS)
 * - Within entries: Single newline between lines (MarkdownStyle.DOCUMENT_LINE_SEPARATOR)
 * - After special blocks (quotes, code, headings): Same as above - consistency guaranteed
 * 
 * All rendering operations use the centralized constants from LogFileFormat and MarkdownStyle
 * to ensure no variance in spacing regardless of content type.
 */
public class MarkdownRenderer {

    public static void renderMarkdown(JTextPane pane, List<String> lines) {
        renderMarkdown(pane, lines, false);
    }
    
    public static void renderMarkdown(JTextPane pane, List<String> lines, boolean scrollToBottom) {
        StyledDocument doc = pane.getStyledDocument();
        // Clear existing content
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            // Document already empty
        }
        Map<String, Style> styles = createStyles(doc);
        try {
            List<List<String>> entries = filehandling.LogParser.parseEntriesForFullLog(lines);
            renderEntries(entries, doc, styles);
        } catch (BadLocationException e) {
            throw new RuntimeException("Error rendering markdown", e);
        }
        // Set caret position based on scroll preference
        pane.setCaretPosition(scrollToBottom ? doc.getLength() : 0);
    }
    
    /**
     * Render markdown from pre-parsed entries (avoids duplicate parsing).
     * Used by lazy loading to render only a subset of entries.
     */
    public static void renderMarkdownFromEntries(JTextPane pane, List<List<String>> entries) {
        renderMarkdownFromEntries(pane, entries, false);
    }
    
    /**
     * Render markdown from pre-parsed entries with scroll control.
     * @param pane The text pane to render into
     * @param entries Pre-parsed entries to render
     * @param scrollToBottom If true, scroll to bottom (latest entries); if false, scroll to top
     */
    public static void renderMarkdownFromEntries(JTextPane pane, List<List<String>> entries, boolean scrollToBottom) {
        StyledDocument doc = pane.getStyledDocument();
        // Clear existing content
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            // Document already empty
        }
        Map<String, Style> styles = createStyles(doc);
        try {
            renderEntries(entries, doc, styles);
        } catch (BadLocationException e) {
            throw new RuntimeException("Error rendering markdown", e);
        }
        // Set caret position based on scroll preference
        pane.setCaretPosition(scrollToBottom ? doc.getLength() : 0);
    }

    private static Map<String, Style> createStyles(StyledDocument doc) {
        Map<String, Style> styles = new HashMap<>(12); // Pre-size with expected capacity

        Style defaultStyle = doc.addStyle("default", null);
        StyleConstants.setFontFamily(defaultStyle, MarkdownStyle.FONT_FAMILY_DEFAULT);
        StyleConstants.setFontSize(defaultStyle, MarkdownStyle.FONT_SIZE_DEFAULT);
        StyleConstants.setForeground(defaultStyle, MarkdownStyle.COLOR_DEFAULT_TEXT);
        styles.put("default", defaultStyle);

        Style tsStyle = doc.addStyle("timestamp", null);
        StyleConstants.setFontFamily(tsStyle, MarkdownStyle.FONT_FAMILY_DEFAULT);
        StyleConstants.setFontSize(tsStyle, MarkdownStyle.FONT_SIZE_TIMESTAMP);
        StyleConstants.setBold(tsStyle, true);
        StyleConstants.setForeground(tsStyle, MarkdownStyle.COLOR_TIMESTAMP);
        styles.put("timestamp", tsStyle);

        Style sepStyle = doc.addStyle("sep", null);
        StyleConstants.setFontFamily(sepStyle, MarkdownStyle.FONT_FAMILY_DEFAULT);
        StyleConstants.setFontSize(sepStyle, MarkdownStyle.FONT_SIZE_DEFAULT);
        styles.put("sep", sepStyle);

        Style boldStyle = doc.addStyle("bold", defaultStyle);
        StyleConstants.setBold(boldStyle, true);
        styles.put("bold", boldStyle);

        Style italicStyle = doc.addStyle("italic", defaultStyle);
        StyleConstants.setItalic(italicStyle, true);
        styles.put("italic", italicStyle);

        Style h1Style = doc.addStyle("h1", defaultStyle);
        StyleConstants.setFontSize(h1Style, MarkdownStyle.FONT_SIZE_H1);
        StyleConstants.setBold(h1Style, true);
        styles.put("h1", h1Style);

        Style h2Style = doc.addStyle("h2", defaultStyle);
        StyleConstants.setFontSize(h2Style, MarkdownStyle.FONT_SIZE_H2);
        StyleConstants.setBold(h2Style, true);
        styles.put("h2", h2Style);

        Style h3Style = doc.addStyle("h3", defaultStyle);
        StyleConstants.setFontSize(h3Style, MarkdownStyle.FONT_SIZE_H3);
        StyleConstants.setBold(h3Style, true);
        styles.put("h3", h3Style);

        Style listStyle = doc.addStyle("list", defaultStyle);
        StyleConstants.setLeftIndent(listStyle, MarkdownStyle.INDENT_LIST);
        styles.put("list", listStyle);

        Style quoteStyle = doc.addStyle("quote", defaultStyle);
        StyleConstants.setLeftIndent(quoteStyle, MarkdownStyle.INDENT_QUOTE);
        StyleConstants.setBackground(quoteStyle, MarkdownStyle.COLOR_QUOTE_BG);
        styles.put("quote", quoteStyle);

        Style quoteBorderStyle = doc.addStyle("quoteBorder", defaultStyle);
        StyleConstants.setForeground(quoteBorderStyle, MarkdownStyle.COLOR_QUOTE_BORDER);
        StyleConstants.setFontFamily(quoteBorderStyle, MarkdownStyle.FONT_FAMILY_MONOSPACED);
        StyleConstants.setFontSize(quoteBorderStyle, MarkdownStyle.FONT_SIZE_QUOTE_BORDER);
        styles.put("quoteBorder", quoteBorderStyle);

        Style codeStyle = doc.addStyle("code", defaultStyle);
        StyleConstants.setFontFamily(codeStyle, MarkdownStyle.FONT_FAMILY_CODE);
        StyleConstants.setBackground(codeStyle, MarkdownStyle.COLOR_CODE_BLOCK_BG);
        StyleConstants.setLeftIndent(codeStyle, MarkdownStyle.INDENT_CODE_LEFT);
        StyleConstants.setRightIndent(codeStyle, MarkdownStyle.INDENT_CODE_RIGHT);
        styles.put("code", codeStyle);

        return styles;
    }

    private static void renderEntries(List<List<String>> entries, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        Style defaultStyle = styles.get("default");
        Style tsStyle = styles.get("timestamp");
        Style sepStyle = styles.get("sep");
        boolean firstEntry = true;
        
        // Trim trailing blank lines from entries - pre-size for efficiency
        List<List<String>> trimmedEntries = new ArrayList<>(entries.size());
        for (List<String> entry : entries) {
            List<String> trimmed = new ArrayList<>(entry);
            while (!trimmed.isEmpty() && trimmed.get(trimmed.size() - 1).trim().isEmpty()) {
                trimmed.remove(trimmed.size() - 1);
            }
            trimmedEntries.add(trimmed);
        }
        
        for (List<String> entry : trimmedEntries) {
            if (!firstEntry) {
                // Use centralized format rules for display spacing
                // ALWAYS add separator between entries for consistency
                String separator = filehandling.LogFileFormat.INTERNAL_LINE_SEPARATOR.repeat(filehandling.LogFileFormat.DISPLAY_ENTRY_SEPARATOR_BLANKS);
                doc.insertString(doc.getLength(), separator, sepStyle);
            }
            firstEntry = false;
            
            MarkdownEntryRenderer.renderEntry(entry, new MarkdownRenderingContext(doc, styles));
            
            // Trim trailing newlines from the rendered entry to prevent extra spacing between entries
            try {
                while (doc.getLength() > 0 && doc.getText(doc.getLength() - 1, 1).equals("\n")) {
                    doc.remove(doc.getLength() - 1, 1);
                }
            } catch (BadLocationException e) {
                // Ignore if can't trim
            }
        }
    }
}
