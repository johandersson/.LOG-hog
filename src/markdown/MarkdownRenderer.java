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

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
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

    private static final Pattern INLINE_HEADING_PATTERN = Pattern.compile("(###|##|#) ");

    public static void renderMarkdown(JTextPane pane, List<String> lines) {
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
        // Set caret to top of document
        pane.setCaretPosition(0);
    }
    
    /**
     * Render markdown from pre-parsed entries (avoids duplicate parsing).
     * Used by lazy loading to render only a subset of entries.
     */
    public static void renderMarkdownFromEntries(JTextPane pane, List<List<String>> entries) {
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
        // Set caret to top of document
        pane.setCaretPosition(0);
    }

    private static void handleLinkClick(JTextPane pane, MouseEvent e) {
        try {
            int pos = pane.viewToModel2D(e.getPoint());
            if (pos < 0) return;
            StyledDocument doc = pane.getStyledDocument();
            AttributeSet attrs = doc.getCharacterElement(pos).getAttributes();
            Object hrefObj = attrs.getAttribute("href");
            if (hrefObj instanceof String) {
                String href = (String) hrefObj;
                if (href.startsWith("file:")) {
                    handleFileLink(pane, href);
                } else {
                    handleWebLink(pane, href);
                }
            }
        } catch (Exception ex) {
            // Security: Don't expose internal error details
            showLinkError(pane, "Unable to open link.");
        }
    }

    private static void handleFileLink(JTextPane pane, String href) {
        if (!Desktop.isDesktopSupported()) {
            showLinkError(pane, "Desktop is not supported on this system.");
            return;
        }

        java.io.File file = null;
        try {
            // First try to parse as URI
            java.net.URI uri = java.net.URI.create(href);
            file = new java.io.File(uri);
        } catch (Exception uriEx) {
            // Fallback: try to extract path manually
            try {
                String filePath;
                if (href.startsWith("file:///")) {
                    filePath = href.substring(8); // Remove "file:///"
                } else if (href.startsWith("file://")) {
                    filePath = href.substring(7); // Remove "file://"
                } else {
                    filePath = href.substring(5); // Remove "file:"
                }
                file = new java.io.File(filePath);
            } catch (Exception pathEx) {
                showLinkError(pane, "Invalid file path format: " + href);
                return;
            }
        }

        // Check if file exists
        if (!file.exists()) {
            showLinkError(pane, "File does not exist: " + file.getAbsolutePath());
            return;
        }

        // Check if it's actually a file (not a directory)
        if (!file.isFile()) {
            showLinkError(pane, "Path is not a file: " + file.getAbsolutePath());
            return;
        }

        // Try to open the file
        try {
            Desktop.getDesktop().open(file);
        } catch (java.io.IOException ioEx) {
            // Security: Don't expose internal error details
            showLinkError(pane, "Unable to open file. Check if you have the appropriate application installed.");
        } catch (Exception ex) {
            showLinkError(pane, "Unable to open file.");
        }
    }

    private static void handleWebLink(JTextPane pane, String href) {
        if (!Desktop.isDesktopSupported()) {
            showLinkError(pane, "Desktop is not supported on this system.");
            return;
        }

        try {
            String finalHref = href;
            if (!href.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
                finalHref = "http://" + href;
            }
            Desktop.getDesktop().browse(java.net.URI.create(finalHref));
        } catch (java.io.IOException ioEx) {
            // Security: Don't expose internal error details
            showLinkError(pane, "Unable to open URL. Check your internet connection and browser settings.");
        } catch (Exception ex) {
            showLinkError(pane, "Invalid URL format: " + href);
        }
    }

    private static void showLinkError(JTextPane pane, String message) {
        // Find the parent window to show the error dialog
        java.awt.Window parent = javax.swing.SwingUtilities.getWindowAncestor(pane);
        javax.swing.JOptionPane.showMessageDialog(
            parent,
            message,
            "Link Error",
            javax.swing.JOptionPane.ERROR_MESSAGE
        );
    }

    private static void handleLinkHover(JTextPane pane, MouseEvent e) {
        try {
            int pos = pane.viewToModel2D(e.getPoint());
            if (pos < 0) {
                pane.setCursor(Cursor.getDefaultCursor());
                return;
            }
            AttributeSet attrs = pane.getStyledDocument().getCharacterElement(pos).getAttributes();
            Object hrefObj = attrs.getAttribute("href");
            pane.setCursor(hrefObj instanceof String ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        } catch (Exception ex) {
            pane.setCursor(Cursor.getDefaultCursor());
        }
    }

    private static void showLinkPopup(JTextPane pane, MouseEvent e) {
        try {
            int pos = pane.viewToModel2D(e.getPoint());
            if (pos < 0) return;
            StyledDocument doc = pane.getStyledDocument();
            AttributeSet attrs = doc.getCharacterElement(pos).getAttributes();
            Object hrefObj = attrs.getAttribute("href");
            if (hrefObj instanceof String) {
                String href = (String) hrefObj;
                JPopupMenu popup = new JPopupMenu();
                JMenuItem copyItem = new JMenuItem("Copy Link");
                copyItem.addActionListener(ae -> clipboard.SecureClipboardManager.getInstance().copySecureTextToClipboard(href, pane, "Link copied to clipboard securely!"));
                popup.add(copyItem);
                popup.show(pane, e.getX(), e.getY());
            }
        } catch (Exception ex) {
            // swallow
        }
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
            
            renderEntryContent(entry, doc, styles);
        }
    }
    
    private static void renderEntryContent(List<String> entry, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        Style defaultStyle = styles.get("default");
        Style tsStyle = styles.get("timestamp");
        Style sepStyle = styles.get("sep");
        
        boolean inCodeBlock = false;
        List<String> paragraphLines = new ArrayList<>();
        
        for (int i = 0; i < entry.size(); i++) {
            String line = entry.get(i);
            
            boolean isTimestamp = (i == 0);
            boolean isCodeBlockMarker = line.trim().equals("```");
            boolean isBlank = line.trim().isEmpty();
            boolean isList = line.startsWith("- ");
            boolean isQuote = line.startsWith("> ");
            boolean isHeading = line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ");
            boolean hasInlineHeading = INLINE_HEADING_PATTERN.matcher(line).find();
            
            // If we have accumulated paragraph lines and this line starts a new block, render the paragraph first
            if (!paragraphLines.isEmpty() && (isBlank || isList || isQuote || isHeading || hasInlineHeading || isCodeBlockMarker || inCodeBlock)) {
                renderParagraph(paragraphLines, doc, defaultStyle, styles);
                paragraphLines.clear();
            }
            
            if (isCodeBlockMarker) {
                inCodeBlock = !inCodeBlock;
                continue;
            }
            
            if (inCodeBlock) {
                doc.insertString(doc.getLength(), line, styles.get("code"));
                doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, styles.get("code"));
            } else if (isTimestamp && line.trim().matches("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$")) {
                // Render timestamp
                doc.insertString(doc.getLength(), line + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, tsStyle);
            } else if (isBlank) {
                // Blank lines create paragraph breaks
                doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, sepStyle);
            } else if (isList) {
                String text = "• " + line.substring(2);
                Style listStyle = styles.get("list");
                MarkdownFormatter.appendLineWithFormatting(doc, text, listStyle, styles);
                doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, listStyle);
            } else if (isQuote) {
                // Handle multi-line blockquotes
                List<String> quoteLines = new ArrayList<>();
                quoteLines.add(line);
                int j = i + 1;
                while (j < entry.size() && entry.get(j).startsWith("> ")) {
                    quoteLines.add(entry.get(j));
                    j++;
                }
                // Render the blockquote block
                renderBlockquote(doc, quoteLines, styles);
                i = j - 1; // Skip the processed lines
            } else if (isHeading) {
                String text = line.startsWith("### ") ? line.substring(4) :
                             line.startsWith("## ") ? line.substring(3) : line.substring(2);
                Style headingStyle = line.startsWith("### ") ? styles.get("h3") :
                                    line.startsWith("## ") ? styles.get("h2") : styles.get("h1");
                MarkdownFormatter.appendLineWithFormatting(doc, text, headingStyle, styles);
                doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, headingStyle);
            } else if (hasInlineHeading) {
                // Parse for inline headings
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
                    doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, partStyle);
                }
            } else {
                // Regular text line - accumulate for paragraph rendering
                paragraphLines.add(line);
            }
        }
        
        // Render any remaining paragraph lines
        if (!paragraphLines.isEmpty()) {
            renderParagraph(paragraphLines, doc, defaultStyle, styles);
        }
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
        
        // Add newline after paragraph
        doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, defaultStyle);
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
        doc.insertString(doc.getLength(), MarkdownStyle.DOCUMENT_LINE_SEPARATOR, quoteStyle);
    }
}
