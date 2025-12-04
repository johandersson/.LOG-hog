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

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;

public class MarkdownRenderer {

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]*)`");
    private static final Pattern RED_PATTERN = Pattern.compile("<span style=\"color:red\">(.*?)</span>", Pattern.DOTALL);

    private record TextElement(int start, int end, String type, String text, String href) {}

    public static void renderMarkdown(JTextPane pane, List<String> lines) {
        StyledDocument doc = pane.getStyledDocument();
        Map<String, Style> styles = createStyles(doc);
        try {
            List<List<String>> entries = new ArrayList<>();
            List<String> currentEntry = new ArrayList<>();
            Pattern tsPattern = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$", Pattern.MULTILINE);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.equalsIgnoreCase(".LOG")) continue;
                if (tsPattern.matcher(trimmed).matches()) {
                    if (!currentEntry.isEmpty()) {
                        entries.add(new ArrayList<>(currentEntry));
                        currentEntry.clear();
                    }
                    currentEntry.add(line);
                } else {
                    // Only add non-blank lines, but preserve blank lines within an entry
                    if (!currentEntry.isEmpty() || !trimmed.isEmpty()) {
                        currentEntry.add(line);
                    }
                }
            }
            if (!currentEntry.isEmpty()) {
                entries.add(currentEntry);
            }

            // Sort entries oldest first
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.getDefault());
            entries.sort((a, b) -> {
                try {
                    String dateStrA = a.get(0).trim().replaceAll(" \\(\\d+\\)", "");
                    String dateStrB = b.get(0).trim().replaceAll(" \\(\\d+\\)", "");
                    LocalDateTime dateA = LocalDateTime.parse(dateStrA, formatter);
                    LocalDateTime dateB = LocalDateTime.parse(dateStrB, formatter);
                    return dateA.compareTo(dateB);
                } catch (Exception e) {
                    return 0;
                }
            });

            renderEntries(entries, doc, styles);
        } catch (BadLocationException e) {
            throw new RuntimeException("Error rendering markdown", e);
        }
        // Removed: pane.setCaretPosition(pane.getDocument().getLength()); to prevent auto-scroll to bottom
    }

    public static void addLinkListeners(JTextPane pane) {
        // Remove existing link listeners to avoid duplicates
        for (java.awt.event.MouseListener ml : pane.getMouseListeners()) {
            if (ml instanceof MouseAdapter) {
                pane.removeMouseListener(ml);
            }
        }
        for (java.awt.event.MouseMotionListener mml : pane.getMouseMotionListeners()) {
            if (mml instanceof MouseMotionAdapter) {
                pane.removeMouseMotionListener(mml);
            }
        }

        pane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleLinkClick(pane, e);
            }
        });

        pane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleLinkHover(pane, e);
            }
        });
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
                    String filePath = href.substring(5);
                    java.io.File file = new java.io.File(filePath);
                    if (file.exists() && Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                } else {
                    if (!href.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
                        href = "http://" + href;
                    }
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(java.net.URI.create(href));
                    }
                }
            }
        } catch (Exception ex) {
            // swallow
        }
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

    private static Map<String, Style> createStyles(StyledDocument doc) {
        Map<String, Style> styles = new HashMap<>();

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
        for (List<String> entry : entries) {
            if (!firstEntry && !previousHadCode) {
                doc.insertString(doc.getLength(), "\n", sepStyle); // Only one blank line between entries
            }
            firstEntry = false;
            boolean inCodeBlock = false;
            boolean currentHasCode = false;
            for (int i = 0; i < entry.size(); i++) {
                String line = entry.get(i);
                if (line.trim().equals("```")) {
                    inCodeBlock = !inCodeBlock;
                    currentHasCode = true;
                    continue;
                }
                if (inCodeBlock) {
                    doc.insertString(doc.getLength(), line, styles.get("code"));
                    doc.insertString(doc.getLength(), "\n", styles.get("code"));
                } else if (i == 0 && line.trim().matches("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$")) {
                    doc.insertString(doc.getLength(), line + "\n", tsStyle);
                } else if (line.trim().isEmpty()) {
                    // Only preserve blank lines within an entry, not between entries
                    doc.insertString(doc.getLength(), "\n", sepStyle);
                } else if (line.startsWith("- ")) {
                    String text = "• " + line.substring(2);
                    Style listStyle = styles.get("list");
                    appendLineWithFormatting(doc, text, listStyle, styles);
                    doc.insertString(doc.getLength(), "\n", listStyle);
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
                    // Parse for inline headings
                    Set<Integer> headingSet = new TreeSet<>();
                    headingSet.add(0);
                    Matcher h3Matcher = Pattern.compile("### ").matcher(line);
                    while (h3Matcher.find()) {
                        headingSet.add(h3Matcher.start());
                    }
                    Matcher h2Matcher = Pattern.compile("## ").matcher(line);
                    while (h2Matcher.find()) {
                        headingSet.add(h2Matcher.start());
                    }
                    Matcher h1Matcher = Pattern.compile("# ").matcher(line);
                    while (h1Matcher.find()) {
                        headingSet.add(h1Matcher.start());
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

    private static void appendLineWithFormatting(StyledDocument doc, String line, Style baseStyle, Map<String, Style> styles) throws BadLocationException {
        List<TextElement> elements = new ArrayList<>();

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
            String textToShow = display;
            elements.add(new TextElement(linkMatcher.start(), linkMatcher.end(), "link", textToShow, target));
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

        // Sort by start position
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
