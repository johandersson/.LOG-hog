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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

/**
 * Handles rendering of individual markdown entries.
 * Extracted from MarkdownRenderer for better modularity.
 */
public class MarkdownEntryRenderer {

    private static final Pattern INLINE_HEADING_PATTERN = Pattern.compile("(###|##|#) ");

    public static void renderEntry(List<String> entry, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        processEntryLines(entry, doc, styles);
    }

    private static void processEntryLines(List<String> entry, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        Style defaultStyle = styles.get("default");
        boolean inCodeBlock = false;
        List<String> paragraphLines = new ArrayList<>();

        for (int i = 0; i < entry.size(); i++) {
            String line = entry.get(i);

            boolean isTimestamp = (i == 0) && isTimestampLine(line);
            boolean isCodeBlockMarker = line.trim().equals("```");
            boolean isBlank = line.trim().isEmpty();
            boolean isList = line.startsWith("- ");
            boolean isQuote = line.startsWith("> ");
            boolean isHeading = isHeadingLine(line);
            boolean hasInlineHeading = INLINE_HEADING_PATTERN.matcher(line).find();

            // If we have accumulated paragraph lines and this line starts a new block, render the paragraph first
            if (!paragraphLines.isEmpty() && (isBlank || isList || isQuote || isHeading || hasInlineHeading || isCodeBlockMarker || inCodeBlock)) {
                renderParagraph(paragraphLines, doc, defaultStyle, styles);
                paragraphLines.clear();
            }

            if (isCodeBlockMarker) {
                inCodeBlock = handleCodeBlockMarker(inCodeBlock, doc, styles);
                continue;
            }

            if (inCodeBlock) {
                renderCodeLine(line, doc, styles.get("code"));
            } else if (isTimestamp) {
                renderTimestamp(line, doc, styles.get("timestamp"));
            } else if (isBlank) {
                continue;
            } else if (isList) {
                List<String> listLines = collectListLines(entry, i);
                i += listLines.size() - 1;
                renderListBlock(listLines, doc, styles.get("list"), styles);
            } else if (isQuote) {
                List<String> quoteLines = collectQuoteLines(entry, i);
                renderBlockquote(doc, quoteLines, styles);
                i += quoteLines.size() - 1;
            } else if (isHeading) {
                renderHeading(line, doc, styles);
            } else if (hasInlineHeading) {
                renderInlineHeading(line, doc, defaultStyle, styles);
            } else {
                paragraphLines.add(line);
            }
        }

        // Render any remaining paragraph lines
        if (!paragraphLines.isEmpty()) {
            renderParagraph(paragraphLines, doc, defaultStyle, styles);
        }
    }

    private static boolean isTimestampLine(String line) {
        return line.trim().matches("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$");
    }

    private static boolean isHeadingLine(String line) {
        return line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ");
    }

    private static boolean handleCodeBlockMarker(boolean inCodeBlock, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        boolean wasInCodeBlock = inCodeBlock;
        inCodeBlock = !inCodeBlock;
        if (wasInCodeBlock && !inCodeBlock) {
            doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, styles.get("code"));
        }
        return inCodeBlock;
    }

    private static void renderTimestamp(String line, StyledDocument doc, Style tsStyle) throws BadLocationException {
        doc.insertString(doc.getLength(), line + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, tsStyle);
    }

    private static void renderListBlock(List<String> listLines, StyledDocument doc, Style listStyle, Map<String, Style> styles) throws BadLocationException {
        for (int j = 0; j < listLines.size(); j++) {
            String line = listLines.get(j);
            String text = "• " + line.substring(2);
            MarkdownFormatter.appendLineWithFormatting(doc, text, listStyle, styles);
            if (j < listLines.size() - 1) {
                doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, listStyle);
            } else {
                doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, listStyle);
            }
        }
    }

    private static void renderHeading(String line, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        String text = line.startsWith("### ") ? line.substring(4) :
                     line.startsWith("## ") ? line.substring(3) : line.substring(2);
        Style headingStyle = line.startsWith("### ") ? styles.get("h3") :
                            line.startsWith("## ") ? styles.get("h2") : styles.get("h1");
        MarkdownFormatter.appendLineWithFormatting(doc, text, headingStyle, styles);
        doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, headingStyle);
    }

    private static void renderInlineHeading(String line, StyledDocument doc, Style defaultStyle, Map<String, Style> styles) throws BadLocationException {
        Matcher headingMatcher = INLINE_HEADING_PATTERN.matcher(line);
        Set<Integer> headingSet = new TreeSet<>();
        headingSet.add(0);
        while (headingMatcher.find()) {
            headingSet.add(headingMatcher.start());
        }

        List<Integer> headingStarts = new ArrayList<>(headingSet);
        headingStarts.add(line.length());
        for (int j = 0; j < headingStarts.size() - 1; j++) {
            int start = headingStarts.get(j);
            int end = headingStarts.get(j + 1);
            String part = line.substring(start, end);
            Style partStyle = defaultStyle;
            String text = part;
            String marker = null;
            if (part.startsWith("### ")) marker = "### ";
            else if (part.startsWith("## ")) marker = "## ";
            else if (part.startsWith("# ")) marker = "# ";
            if (marker != null) {
                partStyle = switch (marker) {
                    case "### " -> styles.get("h3");
                    case "## " -> styles.get("h2");
                    case "# " -> styles.get("h1");
                    default -> defaultStyle;
                };
                text = part.substring(marker.length());
            }
            MarkdownFormatter.appendLineWithFormatting(doc, text, partStyle, styles);
            doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, partStyle);
        }
    }

    private static void renderCodeLine(String line, StyledDocument doc, Style codeStyle) throws BadLocationException {
        doc.insertString(doc.getLength(), line, codeStyle);
        doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, codeStyle);
    }

    private static List<String> collectQuoteLines(List<String> entry, int startIndex) {
        List<String> quoteLines = new ArrayList<>();
        for (int j = startIndex; j < entry.size() && entry.get(j).startsWith("> "); j++) {
            quoteLines.add(entry.get(j));
        }
        return quoteLines;
    }

    private static List<String> collectListLines(List<String> entry, int startIndex) {
        List<String> listLines = new ArrayList<>();
        for (int j = startIndex; j < entry.size() && entry.get(j).startsWith("- "); j++) {
            listLines.add(entry.get(j));
        }
        return listLines;
    }

    private static void renderParagraph(List<String> lines, StyledDocument doc, Style defaultStyle, Map<String, Style> styles) throws BadLocationException {
        if (lines.isEmpty()) return;

        // Join lines with spaces to form a paragraph
        StringBuilder paragraph = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) paragraph.append(" ");
            paragraph.append(lines.get(i));
        }

        String paragraphText = paragraph.toString();

        // Check if the paragraph has markdown formatting
        if (MarkdownFormatter.hasMarkdown(paragraphText)) {
            MarkdownFormatter.appendLineWithFormatting(doc, paragraphText, defaultStyle, styles);
        } else {
            doc.insertString(doc.getLength(), paragraphText, defaultStyle);
        }

        // Add spacing after paragraph
        doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, defaultStyle);
    }

    private static void renderBlockquote(StyledDocument doc, List<String> quoteLines, Map<String, Style> styles) throws BadLocationException {
        Style quoteStyle = styles.get("quote");
        Style quoteBorderStyle = styles.get("quoteBorder");

        for (int k = 0; k < quoteLines.size(); k++) {
            String line = quoteLines.get(k);
            String text = line.substring(2);

            // Insert vertical bar as border
            doc.insertString(doc.getLength(), "│ ", quoteBorderStyle);
            // Insert the text with formatting
            MarkdownFormatter.appendLineWithFormatting(doc, text, quoteStyle, styles);
            if (k < quoteLines.size() - 1) {
                doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, quoteStyle);
            }
        }
        doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, quoteStyle);
    }
}