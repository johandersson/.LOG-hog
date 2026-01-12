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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
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
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class MarkdownRenderer {

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]*)`");
    private static final Pattern RED_PATTERN = Pattern.compile("<span style=\"color:red\">(.*?)</span>", Pattern.DOTALL);
    private static final Pattern INLINE_HEADING_PATTERN = Pattern.compile("(###|##|#) ");
    
    // Quick check patterns for early exit optimization
    private static final Pattern HAS_MARKDOWN_PATTERN = Pattern.compile("[\\[*`<#>-]");

    private record TextElement(int start, int end, String type, String text, String href) {}

    public static void renderMarkdown(JTextPane pane, List<String> lines) {
        StyledDocument doc = pane.getStyledDocument();
        Map<String, Style> styles = createStyles(doc);
        try {
            List<List<String>> entries = filehandling.LogParser.parseEntriesForFullLog(lines);
            renderEntries(entries, doc, styles);
        } catch (BadLocationException e) {
            throw new RuntimeException("Error rendering markdown", e);
        }
        // Removed: pane.setCaretPosition(pane.getDocument().getLength()); to prevent auto-scroll to bottom
    }
    
    /**
     * Render markdown from pre-parsed entries (avoids duplicate parsing).
     * Used by lazy loading to render only a subset of entries.
     */
    public static void renderMarkdownFromEntries(JTextPane pane, List<List<String>> entries) {
        StyledDocument doc = pane.getStyledDocument();
        Map<String, Style> styles = createStyles(doc);
        try {
            renderEntries(entries, doc, styles);
        } catch (BadLocationException e) {
            throw new RuntimeException("Error rendering markdown", e);
        }
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
        StyleConstants.setFontFamily(defaultStyle, "Segoe UI");
        StyleConstants.setFontSize(defaultStyle, 14);
        StyleConstants.setForeground(defaultStyle, Color.DARK_GRAY);
        styles.put("default", defaultStyle);

        Style tsStyle = doc.addStyle("timestamp", null);
        StyleConstants.setFontFamily(tsStyle, "Segoe UI");
        StyleConstants.setFontSize(tsStyle, 16);
        StyleConstants.setBold(tsStyle, true);
        StyleConstants.setForeground(tsStyle, Color.BLACK);
        styles.put("timestamp", tsStyle);

        Style sepStyle = doc.addStyle("sep", null);
        StyleConstants.setFontFamily(sepStyle, "Segoe UI");
        StyleConstants.setFontSize(sepStyle, 14);
        styles.put("sep", sepStyle);

        Style boldStyle = doc.addStyle("bold", defaultStyle);
        StyleConstants.setBold(boldStyle, true);
        styles.put("bold", boldStyle);

        Style italicStyle = doc.addStyle("italic", defaultStyle);
        StyleConstants.setItalic(italicStyle, true);
        styles.put("italic", italicStyle);

        Style h1Style = doc.addStyle("h1", defaultStyle);
        StyleConstants.setFontSize(h1Style, 18);
        StyleConstants.setBold(h1Style, true);
        styles.put("h1", h1Style);

        Style h2Style = doc.addStyle("h2", defaultStyle);
        StyleConstants.setFontSize(h2Style, 16);
        StyleConstants.setBold(h2Style, true);
        styles.put("h2", h2Style);

        Style h3Style = doc.addStyle("h3", defaultStyle);
        StyleConstants.setFontSize(h3Style, 14);
        StyleConstants.setBold(h3Style, true);
        styles.put("h3", h3Style);

        Style listStyle = doc.addStyle("list", defaultStyle);
        StyleConstants.setLeftIndent(listStyle, 20);
        styles.put("list", listStyle);

        Style quoteStyle = doc.addStyle("quote", defaultStyle);
        StyleConstants.setLeftIndent(quoteStyle, 10); // Less indentation since we have the bar
        StyleConstants.setBackground(quoteStyle, new Color(47, 128, 237, 20)); // Very light button blue background
        styles.put("quote", quoteStyle);

        Style quoteBorderStyle = doc.addStyle("quoteBorder", defaultStyle);
        StyleConstants.setForeground(quoteBorderStyle, new Color(47, 128, 237)); // Button blue color
        StyleConstants.setFontFamily(quoteBorderStyle, "Monospaced");
        StyleConstants.setFontSize(quoteBorderStyle, 14);
        styles.put("quoteBorder", quoteBorderStyle);

        Style codeStyle = doc.addStyle("code", defaultStyle);
        StyleConstants.setFontFamily(codeStyle, "Consolas");
        StyleConstants.setBackground(codeStyle, new Color(180, 220, 250)); // Lighter and more blue background
        StyleConstants.setLeftIndent(codeStyle, 5); // Add left padding
        StyleConstants.setRightIndent(codeStyle, 5); // Add right padding
        styles.put("code", codeStyle);

        return styles;
    }

    private static void renderEntries(List<List<String>> entries, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        Style defaultStyle = styles.get("default");
        Style tsStyle = styles.get("timestamp");
        Style sepStyle = styles.get("sep");
        boolean firstEntry = true;
        boolean previousHadCode = false;
        
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
            if (!firstEntry && !previousHadCode) {
                doc.insertString(doc.getLength(), "\n\n", sepStyle); // Two blank lines between entries
            }
            firstEntry = false;
            boolean inCodeBlock = false;
            boolean currentHasCode = false;
            for (int i = 0; i < entry.size(); i++) {
                String line = entry.get(i);
                
                // Early exit optimization: Skip regex processing for plain text lines
                boolean isTimestamp = (i == 0);
                boolean isCodeBlockMarker = line.trim().equals("```");
                
                if (!isTimestamp && !isCodeBlockMarker && !inCodeBlock && 
                    !HAS_MARKDOWN_PATTERN.matcher(line).find()) {
                    // Plain text line with no markdown - fast path
                    doc.insertString(doc.getLength(), line, defaultStyle);
                    if (i < entry.size() - 1) {
                        doc.insertString(doc.getLength(), "\n", defaultStyle);
                    }
                    continue;
                }
                
                if (line.trim().equals("```")) {
                    inCodeBlock = !inCodeBlock;
                    currentHasCode = true;
                    continue;
                }
                if (inCodeBlock) {
                    doc.insertString(doc.getLength(), line, styles.get("code"));
                    doc.insertString(doc.getLength(), "\n", styles.get("code"));
                } else if (i == 0 && line.trim().matches("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$")) {
                    // Render timestamp
                    doc.insertString(doc.getLength(), line + "\n", tsStyle);
                } else if (line.trim().isEmpty()) {
                    // Only preserve blank lines within an entry, not between entries
                    doc.insertString(doc.getLength(), "\n", sepStyle);
                } else if (line.startsWith("- ")) {
                    String text = "• " + line.substring(2);
                    Style listStyle = styles.get("list");
                    appendLineWithFormatting(doc, text, listStyle, styles);
                    doc.insertString(doc.getLength(), "\n", listStyle);
                } else if (line.startsWith("> ")) {
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
                    currentHasCode = true; // Prevent extra spacing
                } else if (line.startsWith("# ")) {
                    String text = line.substring(2);
                    appendLineWithFormatting(doc, text, styles.get("h1"), styles);
                    doc.insertString(doc.getLength(), "\n", styles.get("h1"));
                } else if (line.startsWith("## ")) {
                    String text = line.substring(3);
                    appendLineWithFormatting(doc, text, styles.get("h2"), styles);
                    doc.insertString(doc.getLength(), "\n", styles.get("h2"));
                } else if (line.startsWith("### ")) {
                    String text = line.substring(4);
                    appendLineWithFormatting(doc, text, styles.get("h3"), styles);
                    doc.insertString(doc.getLength(), "\n", styles.get("h3"));
                } else {
                    // Parse for inline headings - optimized with single pattern
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
                        appendLineWithFormatting(doc, text, partStyle, styles);
                        doc.insertString(doc.getLength(), "\n", partStyle);
                    }
                }
            }
            previousHadCode = currentHasCode;
        }
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
            appendLineWithFormatting(doc, text, quoteStyle, styles);
            if (k < quoteLines.size() - 1) {
                doc.insertString(doc.getLength(), "\n", quoteStyle);
            }
        }
        doc.insertString(doc.getLength(), "\n", quoteStyle);
    }

    private static void appendLineWithFormatting(StyledDocument doc, String line, Style baseStyle, Map<String, Style> styles) throws BadLocationException {
        // Early exit optimization: if line has no markdown syntax, insert as plain text
        if (!HAS_MARKDOWN_PATTERN.matcher(line).find()) {
            doc.insertString(doc.getLength(), line, baseStyle);
            return;
        }
        
        // Pre-size with reasonable capacity (most lines have < 10 formatting elements)
        List<TextElement> elements = new ArrayList<>(10);

        // Find bold
        Matcher boldMatcher = BOLD_PATTERN.matcher(line);
        while (boldMatcher.find()) {
            elements.add(new TextElement(boldMatcher.start(), boldMatcher.end(), "bold", boldMatcher.group(1), null));
        }

        // Find italic
        Matcher italicMatcher = ITALIC_PATTERN.matcher(line);
        while (italicMatcher.find()) {
            elements.add(new TextElement(italicMatcher.start(), italicMatcher.end(), "italic", italicMatcher.group(1), null));
        }

        // Find links
        Matcher linkMatcher = LINK_PATTERN.matcher(line);
        while (linkMatcher.find()) {
            String display = linkMatcher.group(1);
            String target = linkMatcher.group(2);
            elements.add(new TextElement(linkMatcher.start(), linkMatcher.end(), "link", display, target));
        }

        // Find inline code
        Matcher codeMatcher = INLINE_CODE_PATTERN.matcher(line);
        while (codeMatcher.find()) {
            elements.add(new TextElement(codeMatcher.start(), codeMatcher.end(), "inlineCode", codeMatcher.group(1), null));
        }

        // Find red text spans
        Matcher redMatcher = RED_PATTERN.matcher(line);
        while (redMatcher.find()) {
            elements.add(new TextElement(redMatcher.start(), redMatcher.end(), "red", redMatcher.group(1), null));
        }

        // Sort by start position (only if we have elements to sort)
        if (elements.isEmpty()) {
            doc.insertString(doc.getLength(), line, baseStyle);
            return;
        }
        elements.sort(Comparator.comparingInt(TextElement::start));

        // Insert
        int last = 0;
        int lastEnd = 0;
        for (TextElement elem : elements) {
            if (elem.type.equals("link")) {
                if (elem.start >= lastEnd && elem.start > last) {
                    String before = line.substring(last, elem.start);
                    doc.insertString(doc.getLength(), before, baseStyle);
                }
                if (elem.start >= lastEnd) {
                    SimpleAttributeSet linkAttr = new SimpleAttributeSet(baseStyle);
                    StyleConstants.setForeground(linkAttr, Color.BLUE);
                    StyleConstants.setUnderline(linkAttr, true);
                    linkAttr.addAttribute("href", elem.href);
                    doc.insertString(doc.getLength(), elem.text, linkAttr);
                    last = elem.end;
                    lastEnd = elem.end;
                }
            } else if (elem.type.equals("inlineCode")) {
                if (elem.start >= lastEnd && elem.start > last) {
                    String before = line.substring(last, elem.start);
                    doc.insertString(doc.getLength(), before, baseStyle);
                }
                if (elem.start >= lastEnd) {
                    AttributeSet codeAttr = styles.get("code");
                    doc.insertString(doc.getLength(), elem.text, codeAttr);
                    last = elem.end;
                    lastEnd = elem.end;
                }
            } else if (elem.type.equals("red")) {
                if (elem.start >= lastEnd && elem.start > last) {
                    String before = line.substring(last, elem.start);
                    doc.insertString(doc.getLength(), before, baseStyle);
                }
                if (elem.start >= lastEnd) {
                    SimpleAttributeSet redAttr = new SimpleAttributeSet(baseStyle);
                    StyleConstants.setForeground(redAttr, Color.RED);
                    doc.insertString(doc.getLength(), elem.text, redAttr);
                    last = elem.end;
                    lastEnd = elem.end;
                }
            } else {
                if (elem.start >= lastEnd && elem.start > last) {
                    String before = line.substring(last, elem.start);
                    doc.insertString(doc.getLength(), before, baseStyle);
                }
                if (elem.start >= lastEnd) {
                    AttributeSet style = switch (elem.type) {
                        case "bold" -> {
                            SimpleAttributeSet boldAttr = new SimpleAttributeSet(baseStyle);
                            StyleConstants.setBold(boldAttr, true);
                            yield boldAttr;
                        }
                        case "italic" -> {
                            SimpleAttributeSet italicAttr = new SimpleAttributeSet(baseStyle);
                            StyleConstants.setItalic(italicAttr, true);
                            yield italicAttr;
                        }
                        default -> baseStyle;
                    };

                    doc.insertString(doc.getLength(), elem.text, style);
                    last = elem.end;
                    lastEnd = elem.end;
                }
            }
        }

        if (last < line.length()) {
            String after = line.substring(last);
            doc.insertString(doc.getLength(), after, baseStyle);
        }
    }
}
