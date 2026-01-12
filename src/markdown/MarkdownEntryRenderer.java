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
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;

/**
 * Handles rendering of individual markdown entries.
 * Extracted from MarkdownRenderer for better modularity.
 */
public class MarkdownEntryRenderer {

    private static final Pattern INLINE_HEADING_PATTERN = Pattern.compile("(###|##|#) ");

    public static void renderEntry(List<String> entry, MarkdownRenderingContext context) throws BadLocationException {
        MarkdownEntryProcessor processor = new MarkdownEntryProcessor(entry, context);
        processor.processEntry();
    }

    private static void processEntryLines(List<String> entry, MarkdownRenderingContext context) throws BadLocationException {
        Style defaultStyle = context.getDefaultStyle();
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
            flushParagraphIfNeeded(isBlank || isList || isQuote || isHeading || hasInlineHeading || isCodeBlockMarker || inCodeBlock, paragraphLines, context);

            if (isCodeBlockMarker) {
                inCodeBlock = handleCodeBlockMarker(inCodeBlock, context);
                continue;
            }

            if (inCodeBlock) {
                renderCodeLine(line, context);
            } else if (isTimestamp) {
                renderTimestamp(line, context);
            } else if (isBlank) {
                continue;
            } else if (isList) {
                i += handleList(i, entry, context);
            } else if (isQuote) {
                i += handleQuote(i, entry, context);
            } else if (isHeading) {
                renderHeading(line, context);
            } else if (hasInlineHeading) {
                renderInlineHeading(line, context);
            } else {
                paragraphLines.add(line);
            }
        }

        // Render any remaining paragraph lines
        if (!paragraphLines.isEmpty()) {
            renderParagraph(paragraphLines, context);
        }
    }

    private static void flushParagraphIfNeeded(boolean condition, List<String> paragraphLines, MarkdownRenderingContext context) throws BadLocationException {
        if (condition && !paragraphLines.isEmpty()) {
            renderParagraph(paragraphLines, context);
            paragraphLines.clear();
        }
    }

    private static boolean isHeadingLine(String line) {
        return line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ");
    }

    private static boolean isTimestampLine(String line) {
        return line.trim().matches("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$");
    }

    private static boolean handleCodeBlockMarker(boolean inCodeBlock, MarkdownRenderingContext context) throws BadLocationException {
        boolean wasInCodeBlock = inCodeBlock;
        inCodeBlock = !inCodeBlock;
        if (wasInCodeBlock && !inCodeBlock) {
            context.getDocument().insertString(context.getDocument().getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, context.getCodeStyle());
        }
        return inCodeBlock;
    }

    private static void renderTimestamp(String line, MarkdownRenderingContext context) throws BadLocationException {
        context.getDocument().insertString(context.getDocument().getLength(), line + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, context.getTimestampStyle());
    }

    private static void renderListBlock(List<String> listLines, MarkdownRenderingContext context) throws BadLocationException {
        Style listStyle = context.getListStyle();
        for (int j = 0; j < listLines.size(); j++) {
            String line = listLines.get(j);
            String text = "• " + line.substring(2);
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, listStyle, context.getStyles());
            if (j < listLines.size() - 1) {
                context.getDocument().insertString(context.getDocument().getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, listStyle);
            } else {
                context.getDocument().insertString(context.getDocument().getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, listStyle);
            }
        }
    }

    private static void renderHeading(String line, MarkdownRenderingContext context) throws BadLocationException {
        String text = line.startsWith("### ") ? line.substring(4) :
                     line.startsWith("## ") ? line.substring(3) : line.substring(2);
        Style headingStyle = line.startsWith("### ") ? context.getH3Style() :
                            line.startsWith("## ") ? context.getH2Style() : context.getH1Style();
        MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, headingStyle, context.getStyles());
        context.getDocument().insertString(context.getDocument().getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, headingStyle);
    }

    private static void renderInlineHeading(String line, MarkdownRenderingContext context) throws BadLocationException {
        Style defaultStyle = context.getDefaultStyle();
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
                    case "### " -> context.getH3Style();
                    case "## " -> context.getH2Style();
                    case "# " -> context.getH1Style();
                    default -> defaultStyle;
                };
                text = part.substring(marker.length());
            }
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, partStyle, context.getStyles());
            context.getDocument().insertString(context.getDocument().getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, partStyle);
        }
    }

    private static void renderCodeLine(String line, MarkdownRenderingContext context) throws BadLocationException {
        Style codeStyle = context.getCodeStyle();
        context.getDocument().insertString(context.getDocument().getLength(), line, codeStyle);
        context.getDocument().insertString(context.getDocument().getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, codeStyle);
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

    private static int handleList(int i, List<String> entry, MarkdownRenderingContext context) throws BadLocationException {
        List<String> listLines = collectListLines(entry, i);
        renderListBlock(listLines, context);
        return listLines.size() - 1;
    }

    private static int handleQuote(int i, List<String> entry, MarkdownRenderingContext context) throws BadLocationException {
        List<String> quoteLines = collectQuoteLines(entry, i);
        renderBlockquote(context, quoteLines);
        return quoteLines.size() - 1;
    }

    private static void renderParagraph(List<String> lines, MarkdownRenderingContext context) throws BadLocationException {
        if (lines.isEmpty()) return;

        Style defaultStyle = context.getDefaultStyle();

        // Join lines with spaces to form a paragraph
        StringBuilder paragraph = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) paragraph.append(" ");
            paragraph.append(lines.get(i));
        }

        String paragraphText = paragraph.toString();

        // Check if the paragraph has markdown formatting
        if (MarkdownFormatter.hasMarkdown(paragraphText)) {
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), paragraphText, defaultStyle, context.getStyles());
        } else {
            context.getDocument().insertString(context.getDocument().getLength(), paragraphText, defaultStyle);
        }

        // Add spacing after paragraph
        context.getDocument().insertString(context.getDocument().getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, defaultStyle);
    }

    private static void renderBlockquote(MarkdownRenderingContext context, List<String> quoteLines) throws BadLocationException {
        Style quoteStyle = context.getQuoteStyle();
        Style quoteBorderStyle = context.getQuoteBorderStyle();

        for (int k = 0; k < quoteLines.size(); k++) {
            String line = quoteLines.get(k);
            String text = line.substring(2);

            // Insert vertical bar as border
            context.getDocument().insertString(context.getDocument().getLength(), "│ ", quoteBorderStyle);
            // Insert the text with formatting
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, quoteStyle, context.getStyles());
            if (k < quoteLines.size() - 1) {
                context.getDocument().insertString(context.getDocument().getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, quoteStyle);
            }
        }
        context.getDocument().insertString(context.getDocument().getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, quoteStyle);
    }
}