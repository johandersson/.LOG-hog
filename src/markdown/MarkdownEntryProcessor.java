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
 * Processes individual markdown entries with encapsulated context.
 * Reduces method parameters by holding the entry and rendering context as fields.
 */
public class MarkdownEntryProcessor {

    private static final Pattern INLINE_HEADING_PATTERN = Pattern.compile("(###|##|#) ");

    private final List<String> entry;
    private final MarkdownRenderingContext context;

    public MarkdownEntryProcessor(List<String> entry, MarkdownRenderingContext context) {
        this.entry = entry;
        this.context = context;
    }

    public void processEntry() throws BadLocationException {
        processEntryLines();
    }

    private void processEntryLines() throws BadLocationException {
        boolean inCodeBlock = false;
        List<String> paragraphLines = new ArrayList<>();

        for (int i = 0; i < entry.size(); i++) {
            String line = entry.get(i);

            boolean isTimestamp = (i == 0) && isTimestampLine(line);
            boolean isCodeBlockMarker = line.trim().equals("```");
            boolean isBlank = line.trim().isEmpty();
            boolean isList = line.startsWith("- ");
            boolean isQuote = line.startsWith(">");
            boolean isHeading = isHeadingLine(line);
            boolean hasInlineHeading = INLINE_HEADING_PATTERN.matcher(line).find();

            // If we have accumulated paragraph lines and this line starts a new block, render the paragraph first
            flushParagraphIfNeeded(isBlank || isList || isQuote || isHeading || hasInlineHeading || isCodeBlockMarker || inCodeBlock, paragraphLines);

            if (isCodeBlockMarker) {
                inCodeBlock = handleCodeBlockMarker(inCodeBlock);
                continue;
            }

            if (inCodeBlock) {
                renderCodeLine(line);
            } else if (isTimestamp) {
                renderTimestamp(line);
            } else if (isBlank) {
                continue;
            } else if (isList) {
                i += handleList(i);
            } else if (isQuote) {
                i += handleQuote(i);
            } else if (isHeading) {
                renderHeading(line);
            } else if (hasInlineHeading) {
                renderInlineHeading(line);
            } else {
                paragraphLines.add(line);
            }
        }

        // Render any remaining paragraph lines
        if (!paragraphLines.isEmpty()) {
            renderParagraph(paragraphLines);
        }
    }

    private void flushParagraphIfNeeded(boolean condition, List<String> paragraphLines) throws BadLocationException {
        if (condition && !paragraphLines.isEmpty()) {
            renderParagraph(paragraphLines);
            paragraphLines.clear();
        }
    }

    private static boolean isHeadingLine(String line) {
        return line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ");
    }

    private static boolean isTimestampLine(String line) {
        return line.trim().matches("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$");
    }

    private boolean handleCodeBlockMarker(boolean inCodeBlock) throws BadLocationException {
        boolean wasInCodeBlock = inCodeBlock;
        inCodeBlock = !inCodeBlock;
        if (wasInCodeBlock && !inCodeBlock) {
            context.insertDoubleLineSeparator();
        }
        return inCodeBlock;
    }

    private void renderTimestamp(String line) throws BadLocationException {
        context.insertString(line + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, context.getTimestampStyle());
    }

    private void renderListBlock(List<String> listLines) throws BadLocationException {
        for (int j = 0; j < listLines.size(); j++) {
            String line = listLines.get(j);
            String text = "• " + line.substring(2);
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, context.getListStyle(), context.getStyles());
            if (j < listLines.size() - 1) {
                context.insertLineSeparator();
            } else {
                context.insertDoubleLineSeparator();
            }
        }
    }

    private void renderHeading(String line) throws BadLocationException {
        String text = line.startsWith("### ") ? line.substring(4) :
                     line.startsWith("## ") ? line.substring(3) : line.substring(2);
        Style headingStyle = line.startsWith("### ") ? context.getH3Style() :
                            line.startsWith("## ") ? context.getH2Style() : context.getH1Style();
        MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, headingStyle, context.getStyles());
        context.insertDoubleLineSeparator();
    }

    private void renderInlineHeading(String line) throws BadLocationException {
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
            Style partStyle = context.getDefaultStyle();
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
                    default -> context.getDefaultStyle();
                };
                text = part.substring(marker.length());
            }
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, partStyle, context.getStyles());
            context.insertDoubleLineSeparator();
        }
    }

    private void renderCodeLine(String line) throws BadLocationException {
        context.insertString(line, context.getCodeStyle());
        context.insertLineSeparator();
    }

    private List<String> collectQuoteLines(int startIndex) {
        List<String> quoteLines = new ArrayList<>();
        for (int j = startIndex; j < entry.size() && entry.get(j).startsWith(">"); j++) {
            quoteLines.add(entry.get(j));
        }
        return quoteLines;
    }

    private List<String> collectListLines(int startIndex) {
        List<String> listLines = new ArrayList<>();
        for (int j = startIndex; j < entry.size() && entry.get(j).startsWith("- "); j++) {
            listLines.add(entry.get(j));
        }
        return listLines;
    }

    private int handleList(int i) throws BadLocationException {
        List<String> listLines = collectListLines(i);
        renderListBlock(listLines);
        return listLines.size() - 1;
    }

    private int handleQuote(int i) throws BadLocationException {
        List<String> quoteLines = collectQuoteLines(i);
        renderBlockquote(quoteLines);
        return quoteLines.size() - 1;
    }

    private void renderParagraph(List<String> lines) throws BadLocationException {
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
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), paragraphText, context.getDefaultStyle(), context.getStyles());
        } else {
            context.insertString(paragraphText, context.getDefaultStyle());
        }

        // Add spacing after paragraph
        context.insertDoubleLineSeparator();
    }

    private void renderBlockquote(List<String> quoteLines) throws BadLocationException {
        for (int k = 0; k < quoteLines.size(); k++) {
            String line = quoteLines.get(k);
            String trimmed = line.trim();
            int level = 0;
            while (level < trimmed.length() && trimmed.charAt(level) == '>') {
                level++;
            }
            String text = trimmed.substring(level).trim();
            // Insert borders for each level
            for (int l = 0; l < level; l++) {
                context.insertString("│ ", context.getQuoteBorderStyle());
            }
            // Insert the text with formatting
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, context.getQuoteStyle(), context.getStyles());
            if (k < quoteLines.size() - 1) {
                context.insertLineSeparator();
            }
        }
        context.insertDoubleLineSeparator();
    }
}