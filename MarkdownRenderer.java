import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;

public class MarkdownRenderer {

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");

    private record TextElement(int start, int end, String type, String text, String href) {}

    public static void renderMarkdown(JTextPane pane, List<String> lines) {
        StyledDocument doc = pane.getStyledDocument();
        Map<String, Style> styles = createStyles(doc);
        try {
            renderLines(lines, doc, styles);
        } catch (BadLocationException e) {
            // This should not happen in normal operation
            throw new RuntimeException("Error rendering markdown", e);
        }
        pane.setCaretPosition(pane.getDocument().getLength());
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
        StyleConstants.setFontFamily(defaultStyle, "Georgia");
        StyleConstants.setFontSize(defaultStyle, 14);
        StyleConstants.setForeground(defaultStyle, Color.DARK_GRAY);
        styles.put("default", defaultStyle);

        Style tsStyle = doc.addStyle("timestamp", defaultStyle);
        StyleConstants.setFontSize(tsStyle, 16);
        StyleConstants.setBold(tsStyle, true);
        StyleConstants.setForeground(tsStyle, Color.BLACK);
        styles.put("timestamp", tsStyle);

        Style sepStyle = doc.addStyle("sep", defaultStyle);
        StyleConstants.setFontSize(sepStyle, 10);
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

        return styles;
    }

    private static final String TS_REGEX = "^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?$";

    private static void renderLines(List<String> lines, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        Style defaultStyle = styles.get("default");
        Style tsStyle = styles.get("timestamp");
        Style sepStyle = styles.get("sep");

        for (String line : lines) {
            if (line.matches(TS_REGEX)) {
                doc.insertString(doc.getLength(), line + "\n", tsStyle);
            } else if (line.trim().isEmpty()) {
                doc.insertString(doc.getLength(), "\n", sepStyle);
            } else if (line.startsWith("- ")) {
                String text = "• " + line.substring(2);
                Style listStyle = styles.get("list");
                appendLineWithInlineLinks(doc, text, listStyle);
                doc.insertString(doc.getLength(), "\n", listStyle);
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
                for (int i = 0; i < headingStarts.size() - 1; i++) {
                    int start = headingStarts.get(i);
                    int end = headingStarts.get(i + 1);
                    String part = line.substring(start, end);
                    Style partStyle = defaultStyle;
                    String text = part;
                    String marker = null;
                    int maxLen = 0;
                    if (part.startsWith("### ")) {
                        marker = "### ";
                        maxLen = 4;
                    }
                    if (part.startsWith("## ") && 3 > maxLen) {
                        marker = "## ";
                        maxLen = 3;
                    }
                    if (part.startsWith("# ") && 2 > maxLen) {
                        marker = "# ";
                        maxLen = 2;
                    }
                    if (marker != null) {
                        partStyle = switch (marker) {
                            case "### " -> styles.get("h3");
                            case "## " -> styles.get("h2");
                            case "# " -> styles.get("h1");
                            default -> defaultStyle;
                        };
                        text = part.substring(marker.length());
                    }
                    appendLineWithInlineLinks(doc, text, partStyle);
                    doc.insertString(doc.getLength(), "\n", partStyle);
                }
            }
        }
    }

    private static void appendLineWithInlineLinks(StyledDocument doc, String line, Style baseStyle) throws BadLocationException {
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

        // Sort by start position
        elements.sort(Comparator.comparingInt(TextElement::start));

        // Insert
        int last = 0;
        int lastEnd = 0;
        for (TextElement elem : elements) {
            if (elem.type.equals("link")) {
                if (elem.start >= lastEnd && elem.start > last) {
                    String before = line.substring(last, elem.start - 1);
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