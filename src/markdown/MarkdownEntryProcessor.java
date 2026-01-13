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

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;

/**
 * Processes individual markdown entries with encapsulated context.
 * Reduces method parameters by holding the entry and rendering context as fields.
 */
public class MarkdownEntryProcessor {

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

            // If we have accumulated paragraph lines and this line starts a new block, render the paragraph first
            flushParagraphIfNeeded(isBlank || isList || isQuote || isHeading || isCodeBlockMarker || inCodeBlock, paragraphLines);

            if (isCodeBlockMarker) {
                inCodeBlock = handleCodeBlockMarker(inCodeBlock);
                continue;
            }

            if (inCodeBlock) {
                renderCodeLine(line);
            } else if (isTimestamp) {
                renderTimestamp(line);
            } else if (isBlank) {
                // Preserve blank lines within an entry as paragraph breaks
                flushParagraphIfNeeded(true, paragraphLines);
                context.insertLineSeparator();
                continue;
            } else if (isList) {
                i += handleList(i);
            } else if (isQuote) {
                i += handleQuote(i);
            } else if (isHeading) {
                renderHeading(line);
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

        // Join lines using the document line separator to preserve paragraph breaks
        String paragraphText = String.join(MarkdownStyle.DOCUMENT_LINE_SEPARATOR, lines);

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