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

// Unused import removed for PMD compliance
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Handles inline markdown formatting (bold, italic, links, code, etc.).
 * Extracted from MarkdownRenderer to improve modularity.
 */
public class MarkdownFormatter {
    
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]*)`");
    private static final Pattern RED_PATTERN = Pattern.compile("<span style=\"color:red\">(.*?)</span>", Pattern.DOTALL);
    private static final Pattern HAS_MARKDOWN_PATTERN = Pattern.compile("[\\[*`<#>-]");
    
    /**
     * Represents a formatted text element with its position and styling.
     */
    record FormattedElement(int start, int end, String type, String text, String href) {}
    
    /**
     * Checks if a line contains any markdown syntax.
     */
    public static boolean hasMarkdown(String line) {
        return HAS_MARKDOWN_PATTERN.matcher(line).find();
    }
    
    /**
     * Appends a line with inline formatting to the document.
     * 
     * @param doc The styled document
     * @param line The line of text to format
     * @param baseStyle The base style to apply
     * @param styles Map of available styles
     * @throws BadLocationException If insertion fails
     */
    public static void appendLineWithFormatting(StyledDocument doc, String line, Style baseStyle, 
                                                java.util.Map<String, Style> styles) throws BadLocationException {
        // Early exit optimization: if line has no markdown syntax, insert as plain text
        if (!hasMarkdown(line)) {
            // Ensure extremely long unbroken words get break opportunities so JTextPane can wrap them
            String safe = insertBreaksForLongWords(line, 30);
            doc.insertString(doc.getLength(), safe, baseStyle);
            return;
        }
        
        List<FormattedElement> elements = findFormattedElements(line);
        
        if (elements.isEmpty()) {
            doc.insertString(doc.getLength(), line, baseStyle);
            return;
        }
        
        insertFormattedElements(doc, line, elements, baseStyle);
    }
    
    /**
     * Finds all formatted elements in a line of text.
     */
    private static List<FormattedElement> findFormattedElements(String line) {
        List<FormattedElement> elements = new ArrayList<>(10);
        
        // Find bold
        Matcher boldMatcher = BOLD_PATTERN.matcher(line);
        while (boldMatcher.find()) {
            elements.add(new FormattedElement(boldMatcher.start(), boldMatcher.end(), 
                "bold", boldMatcher.group(1), null));
        }
        
        // Find italic
        Matcher italicMatcher = ITALIC_PATTERN.matcher(line);
        while (italicMatcher.find()) {
            elements.add(new FormattedElement(italicMatcher.start(), italicMatcher.end(), 
                "italic", italicMatcher.group(1), null));
        }
        
        // Find links
        Matcher linkMatcher = LINK_PATTERN.matcher(line);
        while (linkMatcher.find()) {
            String display = linkMatcher.group(1);
            String target = linkMatcher.group(2);
            elements.add(new FormattedElement(linkMatcher.start(), linkMatcher.end(), 
                "link", display, target));
        }
        
        // Find inline code
        Matcher codeMatcher = INLINE_CODE_PATTERN.matcher(line);
        while (codeMatcher.find()) {
            elements.add(new FormattedElement(codeMatcher.start(), codeMatcher.end(), 
                "inlineCode", codeMatcher.group(1), null));
        }
        
        // Find red text spans
        Matcher redMatcher = RED_PATTERN.matcher(line);
        while (redMatcher.find()) {
            elements.add(new FormattedElement(redMatcher.start(), redMatcher.end(), 
                "red", redMatcher.group(1), null));
        }
        
        // Sort by start position
        elements.sort(Comparator.comparingInt(FormattedElement::start));
        
        return elements;
    }
    
    /**
     * Inserts formatted elements into the document.
     */
    private static void insertFormattedElements(StyledDocument doc, String line, 
                                               List<FormattedElement> elements, 
                                               Style baseStyle) throws BadLocationException {
        int last = 0;
        int lastEnd = 0;
        
        for (FormattedElement elem : elements) {
            // Insert plain text before this element
            if (elem.start >= lastEnd && elem.start > last) {
                String before = line.substring(last, elem.start);
                before = insertBreaksForLongWords(before, 30);
                doc.insertString(doc.getLength(), before, baseStyle);
            }
            
            if (elem.start >= lastEnd) {
                SimpleAttributeSet style = createStyleForElement(elem, baseStyle);
                String textToInsert = elem.text;
                // Don't insert breaks inside inline code elements
                if (!"inlineCode".equals(elem.type)) {
                    textToInsert = insertBreaksForLongWords(textToInsert, 30);
                }
                doc.insertString(doc.getLength(), textToInsert, style);
                last = elem.end;
                lastEnd = elem.end;
            }
        }
        
        // Insert remaining text
        if (last < line.length()) {
            String after = line.substring(last);
            after = insertBreaksForLongWords(after, 30);
            doc.insertString(doc.getLength(), after, baseStyle);
        }
    }

    /**
     * Insert zero-width space break opportunities into very long unbroken tokens.
     * This helps Swing's text wrapping when encountering long words without natural break points.
     */
    public static String insertBreaksForLongWords(String s, int maxChunk) {
        if (s == null || s.length() <= maxChunk) return s;
        StringBuilder out = new StringBuilder(s.length() + s.length() / maxChunk + 4);
        int run = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c);
            if (Character.isWhitespace(c)) {
                run = 0;
            } else {
                run++;
                if (run >= maxChunk) {
                    // Insert zero-width space as a soft break
                    out.append('\u200B');
                    run = 0;
                }
            }
        }
        return out.toString();
    }
    
    /**
     * Creates an appropriate style for the given formatted element.
     */
    private static SimpleAttributeSet createStyleForElement(FormattedElement elem, Style baseStyle) {
        SimpleAttributeSet attrs = new SimpleAttributeSet(baseStyle);
        
        switch (elem.type) {
            case "link":
                StyleConstants.setForeground(attrs, MarkdownStyle.COLOR_LINK);
                StyleConstants.setUnderline(attrs, true);
                attrs.addAttribute("href", elem.href);
                break;
            case "bold":
                StyleConstants.setBold(attrs, true);
                break;
            case "italic":
                StyleConstants.setItalic(attrs, true);
                break;
            case "inlineCode":
                StyleConstants.setFontFamily(attrs, MarkdownStyle.FONT_FAMILY_CODE);
                StyleConstants.setBackground(attrs, MarkdownStyle.COLOR_INLINE_CODE_BG);
                break;
            case "red":
                StyleConstants.setForeground(attrs, MarkdownStyle.COLOR_RED_TEXT);
                break;
            default:
                // No additional styling for unknown types
                break;
        }
        
        return attrs;
    }
}
