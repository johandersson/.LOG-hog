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
import java.util.regex.Pattern;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Element;

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

    // Pre-compiled pattern for timestamp validation - much faster than String.matches()
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( *\\(\\d+\\))?$");

    public static void renderMarkdown(JTextPane pane, List<String> lines) {
        renderMarkdown(pane, lines, false);
    }
    
    public static void renderMarkdown(JTextPane pane, List<String> lines, boolean scrollToBottom) {
        // Create a new document for rendering (avoids live update overhead)
        javax.swing.text.DefaultStyledDocument newDoc = new javax.swing.text.DefaultStyledDocument();
        Map<String, Style> styles = createStyles(newDoc);
        try {
            List<List<String>> entries = filehandling.LogParser.parseEntriesForFullLog(lines);
            renderEntries(entries, newDoc, styles);
        } catch (BadLocationException e) {
            throw new RuntimeException("Error rendering markdown", e);
        }
        // Swap in the new document (atomic, fast)
        pane.setDocument(newDoc);
        // Set caret position based on scroll preference
        pane.setCaretPosition(scrollToBottom ? newDoc.getLength() : 0);
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
        // Create a new document for rendering (avoids live update overhead)
        javax.swing.text.DefaultStyledDocument newDoc = new javax.swing.text.DefaultStyledDocument();
        Map<String, Style> styles = createStyles(newDoc);
        try {
            renderEntries(entries, newDoc, styles);
        } catch (BadLocationException e) {
            throw new RuntimeException("Error rendering markdown", e);
        }
        // Swap in the new document (atomic, fast)
        pane.setDocument(newDoc);
        // Set caret position based on scroll preference
        pane.setCaretPosition(scrollToBottom ? newDoc.getLength() : 0);
    }

    /**
     * Render markdown directly without parsing into entries (for help/about text).
     * Treats the entire content as a single entry to avoid extra spacing between sections.
     */
    public static void renderMarkdownDirect(JTextPane pane, List<String> lines) {
        String key = "direct:" + computeHash(lines);
        synchronized (CACHE) {
            java.lang.ref.SoftReference<CacheEntry> ref = CACHE.get(key);
            CacheEntry cached = (ref == null) ? null : ref.get();
            if (ref != null && cached == null) CACHE.remove(key);
            if (cached != null) {
                pane.setDocument(cached.doc);
                pane.setCaretPosition(0);
                return;
            }
        }

        DefaultStyledDocument doc = new DefaultStyledDocument();
        Map<String, Style> styles = createStyles(doc);
        try {
            // Filter out extra blank lines to reduce line breaks in help text
            // Be more aggressive - limit to maximum 1 consecutive blank line
            List<String> filteredLines = new ArrayList<>();
            int consecutiveBlanks = 0;
            for (String line : lines) {
                boolean isBlank = line.isBlank();
                if (isBlank) {
                    consecutiveBlanks++;
                    // Allow only 1 consecutive blank line
                    if (consecutiveBlanks <= 1) {
                        filteredLines.add(line);
                    }
                } else {
                    consecutiveBlanks = 0;
                    filteredLines.add(line);
                }
            }
            // Render with compact spacing for help/about text
            renderCompactEntry(filteredLines, new MarkdownRenderingContext(doc, styles));
        } catch (BadLocationException e) {
            throw new RuntimeException("Error rendering markdown", e);
        }

        synchronized (CACHE) {
            CACHE.put(key, new java.lang.ref.SoftReference<>(new CacheEntry(doc)));
        }

        pane.setDocument(doc);
        pane.setCaretPosition(0);
    }

    private static String computeHashFromEntries(List<List<String>> entries) {
        try {
            // For very large lists, computing a full SHA-256 over every line can be expensive
            // and may block the UI. Use a fast fingerprint for large datasets and full hash
            // only for reasonably sized lists.
            if (entries.size() > 256) {
                long count = entries.size();
                String first = entries.isEmpty() || entries.get(0).isEmpty() ? "" : entries.get(0).get(0);
                List<String> lastEntry = entries.get(entries.size() - 1);
                String last = lastEntry.isEmpty() ? "" : lastEntry.get(0);
                long totalLen = 0L;
                for (List<String> e : entries) {
                    for (String s : e) totalLen += (s == null ? 0 : s.length());
                }
                String fingerprint = "fast:" + count + ":" + first + ":" + last + ":" + totalLen;
                return Integer.toHexString(fingerprint.hashCode());
            }

            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            for (List<String> entry : entries) {
                // mark entry boundary
                md.update((byte)1);
                for (String l : entry) {
                    if (l == null) l = "";
                    md.update(l.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    md.update((byte)0);
                }
            }
            return java.util.Base64.getEncoder().encodeToString(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // Entry-level cache: map entry-hash -> list of (text, AttributeSet) segments
    private static final int MAX_ENTRY_CACHE = 1024;
    // Entry cache also stored in SoftReference for memory efficiency
    private static final java.util.Map<String, java.lang.ref.SoftReference<java.util.List<Segment>>> ENTRY_CACHE = new java.util.LinkedHashMap<>() {
        protected boolean removeEldestEntry(java.util.Map.Entry<String, java.lang.ref.SoftReference<java.util.List<Segment>>> eldest) {
            return size() > MAX_ENTRY_CACHE;
        }
    };

    private static record Segment(String text, SimpleAttributeSet attrs) {}

    // Document-level cache for rendered documents (help/about, direct renderings)
    private static final int MAX_DOC_CACHE = 128;
    private static final java.util.Map<String, java.lang.ref.SoftReference<CacheEntry>> CACHE = new java.util.LinkedHashMap<>() {
        protected boolean removeEldestEntry(java.util.Map.Entry<String, java.lang.ref.SoftReference<CacheEntry>> eldest) {
            return size() > MAX_DOC_CACHE;
        }
    };

    private static class CacheEntry {
        final StyledDocument doc;
        CacheEntry(StyledDocument d) { this.doc = d; }
    }

    private static String computeHash(List<String> lines) {
        // Reuse entry-level hashing for a list of lines
        return computeHashForEntry(lines);
    }

    private static String computeHashForEntry(List<String> entry) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update((byte)2); // entry marker
            for (String l : entry) {
                if (l == null) l = "";
                md.update(l.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                md.update((byte)0);
            }
            return java.util.Base64.getEncoder().encodeToString(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static java.util.List<Segment> buildSegmentsForEntry(List<String> entry) throws BadLocationException {
        DefaultStyledDocument temp = new DefaultStyledDocument();
        Map<String, Style> styles = createStyles(temp);
        MarkdownEntryRenderer.renderEntry(entry, new MarkdownRenderingContext(temp, styles));

        int len = temp.getLength();
        java.util.List<Segment> segments = new java.util.ArrayList<>();
        int pos = 0;
        while (pos < len) {
            Element elem = temp.getCharacterElement(pos);
            AttributeSet attrs = elem.getAttributes();
            int start = pos;
            int end = pos + elem.getEndOffset() - elem.getStartOffset();
            if (end > len) end = len;
            String text = temp.getText(start, end - start);
            // copy attributes into SimpleAttributeSet to decouple from temp doc
            SimpleAttributeSet copy = new SimpleAttributeSet(attrs);
            segments.add(new Segment(text, copy));
            pos = end;
        }

        return segments;
    }

    private static void insertSegmentsIntoDoc(StyledDocument target, java.util.List<Segment> segments) throws BadLocationException {
        for (Segment s : segments) {
            target.insertString(target.getLength(), s.text(), s.attrs());
        }
    }

    /**
     * Build a StyledDocument from pre-parsed entries. This method can be called off-EDT
     * and accepts an optional progress consumer (0-100).
     */
    public static StyledDocument buildDocumentFromEntries(List<List<String>> entries, java.util.function.IntConsumer progress) throws BadLocationException {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        Map<String, Style> styles = createStyles(doc);

        // Trim trailing blank lines from entries similar to renderEntries
        List<List<String>> trimmedEntries = new ArrayList<>(entries.size());
        for (List<String> entry : entries) {
            List<String> trimmed = new ArrayList<>(entry);
            while (!trimmed.isEmpty() && trimmed.get(trimmed.size() - 1).isBlank()) {
                trimmed.remove(trimmed.size() - 1);
            }
            trimmedEntries.add(trimmed);
        }

        Style sepStyle = styles.get("sep");
        boolean firstEntry = true;
        int total = Math.max(1, trimmedEntries.size());
        for (int idx = 0; idx < trimmedEntries.size(); idx++) {
            List<String> entry = trimmedEntries.get(idx);
            if (!firstEntry) {
                String separator = filehandling.LogFileFormat.INTERNAL_LINE_SEPARATOR.repeat(filehandling.LogFileFormat.DISPLAY_ENTRY_SEPARATOR_BLANKS);
                doc.insertString(doc.getLength(), separator, sepStyle);
            }
            firstEntry = false;

            if (!entry.isEmpty() && entry.get(0).startsWith("Showing ") && entry.size() > 1 && entry.get(1).contains("Log List view")) {
                // Render info entry directly
                MarkdownRenderingContext context = new MarkdownRenderingContext(doc, styles);
                Style info = styles.get("info");
                for (int i = 0; i < entry.size(); i++) {
                    context.insertString(entry.get(i), info);
                    if (i < entry.size() - 1) context.insertLineSeparator();
                }
                context.insertDoubleLineSeparator();
            } else {
                // Use entry-level cache
                String entryKey = computeHashForEntry(entry);
                java.util.List<Segment> segs = null;
                synchronized (ENTRY_CACHE) {
                    java.lang.ref.SoftReference<java.util.List<Segment>> ref = ENTRY_CACHE.get(entryKey);
                    segs = (ref == null) ? null : ref.get();
                    if (ref != null && segs == null) ENTRY_CACHE.remove(entryKey);
                }
                if (segs != null) {
                    insertSegmentsIntoDoc(doc, segs);
                } else {
                    segs = buildSegmentsForEntry(entry);
                    synchronized (ENTRY_CACHE) {
                        ENTRY_CACHE.put(entryKey, new java.lang.ref.SoftReference<>(segs));
                    }
                    insertSegmentsIntoDoc(doc, segs);
                }
            }

            // Trim trailing newlines after each entry
            while (doc.getLength() > 0) {
                try {
                    if (!"\n".equals(doc.getText(doc.getLength() - 1, 1))) break;
                    doc.remove(doc.getLength() - 1, 1);
                } catch (BadLocationException e) {
                    break;
                }
            }

            if (progress != null) {
                int pct = (int) ((idx + 1) * 100L / total);
                try { progress.accept(pct); } catch (Exception ignored) {}
            }
        }

        return doc;
    }

    /**
     * Render markdown entry with compact spacing (single line breaks instead of double).
     * Used for help/about text to reduce excessive line breaks.
     */
    private static void renderCompactEntry(List<String> entry, MarkdownRenderingContext context) throws BadLocationException {
        boolean inCodeBlock = false;
        List<String> paragraphLines = new ArrayList<>();
        boolean previousWasBlank = false;

        for (int i = 0; i < entry.size(); i++) {
            String line = entry.get(i);

            boolean isTimestamp = (i == 0) && isTimestampLine(line);
            boolean isCodeBlockMarker = "```".equals(line.trim());
            boolean isBlank = line.isBlank();
            boolean isList = line.startsWith("- ");
            boolean isQuote = line.startsWith(">");
            boolean isHeading = isHeadingLine(line);

            // If we have accumulated paragraph lines and this line starts a new block, render the paragraph first
            flushParagraphCompactIfNeeded(isBlank || isList || isQuote || isHeading || isCodeBlockMarker || inCodeBlock, paragraphLines, context);

            if (isCodeBlockMarker) {
                inCodeBlock = !inCodeBlock;
                previousWasBlank = false;
                continue;
            }

            if (inCodeBlock) {
                renderCodeLine(line, context);
                previousWasBlank = false;
            } else if (isTimestamp) {
                renderTimestamp(line, context);
                previousWasBlank = false;
            } else if (isBlank) {
                // For compact rendering, allow single blank lines but skip consecutive ones
                if (!previousWasBlank) {
                    // Add a single line separator for paragraph breaks
                    context.insertLineSeparator();
                }
                previousWasBlank = true;
                continue;
            } else if (isList) {
                i += handleList(i, entry, context);
                previousWasBlank = false;
            } else if (isQuote) {
                i += handleQuote(i, entry, context);
                previousWasBlank = false;
            } else if (isHeading) {
                renderHeading(line, context);
                previousWasBlank = false;
            } else {
                paragraphLines.add(line);
                previousWasBlank = false;
            }
        }

        // Render any remaining paragraph lines
        if (!paragraphLines.isEmpty()) {
            renderParagraphCompact(paragraphLines, context);
        }
    }

    private static void flushParagraphCompactIfNeeded(boolean condition, List<String> paragraphLines, MarkdownRenderingContext context) throws BadLocationException {
        if (condition && !paragraphLines.isEmpty()) {
            renderParagraphCompact(paragraphLines, context);
            paragraphLines.clear();
        }
    }

    private static void renderParagraphCompact(List<String> lines, MarkdownRenderingContext context) throws BadLocationException {
        if (lines.isEmpty()) return;

        // Join lines using the document line separator to preserve paragraph breaks
        String paragraphText = String.join(MarkdownStyle.DOCUMENT_LINE_SEPARATOR, lines);
        // Trim a single leading DOCUMENT_LINE_SEPARATOR if present (can happen after headings)
        if (paragraphText.startsWith(MarkdownStyle.DOCUMENT_LINE_SEPARATOR)) {
            paragraphText = paragraphText.substring(MarkdownStyle.DOCUMENT_LINE_SEPARATOR.length());
        }

        // Check if the paragraph has markdown formatting
        if (MarkdownFormatter.hasMarkdown(paragraphText)) {
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), paragraphText, context.getDefaultStyle(), context.getStyles());
        } else {
            context.insertString(paragraphText, context.getDefaultStyle());
        }

        // For compact rendering, use single line separator instead of double
        context.insertLineSeparator();
    }

    private static void renderTimestamp(String line, MarkdownRenderingContext context) throws BadLocationException {
        context.insertString(line + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, context.getStyle("timestamp"));
    }

    private static void renderHeading(String line, MarkdownRenderingContext context) throws BadLocationException {
        String text = line.startsWith("### ") ? line.substring(4) :
                     line.startsWith("## ") ? line.substring(3) : line.substring(2);
        String styleName = line.startsWith("### ") ? "h3" :
                          line.startsWith("## ") ? "h2" : "h1";
        MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, context.getStyle(styleName), context.getStyles());
        context.insertLineSeparator();
    }

    private static void renderCodeLine(String line, MarkdownRenderingContext context) throws BadLocationException {
        context.insertString(line, context.getStyle("code"));
        context.insertLineSeparator();
    }

    private static int handleList(int i, List<String> entry, MarkdownRenderingContext context) throws BadLocationException {
        List<String> listLines = collectListLines(i, entry);
        renderListBlockCompact(listLines, context);
        return listLines.size() - 1;
    }

    private static int handleQuote(int i, List<String> entry, MarkdownRenderingContext context) throws BadLocationException {
        List<String> quoteLines = collectQuoteLines(i, entry);
        renderBlockquoteCompact(quoteLines, context);
        return quoteLines.size() - 1;
    }

    private static List<String> collectListLines(int startIndex, List<String> entry) {
        List<String> listLines = new ArrayList<>();
        for (int j = startIndex; j < entry.size(); j++) {
            String line = entry.get(j);
            if (line.startsWith("- ")) {
                listLines.add(line);
            } else if (!line.isBlank()) {
                break;
            }
        }
        return listLines;
    }

    private static List<String> collectQuoteLines(int startIndex, List<String> entry) {
        List<String> quoteLines = new ArrayList<>();
        for (int j = startIndex; j < entry.size(); j++) {
            String line = entry.get(j);
            if (!line.isEmpty() && line.charAt(0) == '>') {
                quoteLines.add(line);
            } else if (!line.isBlank()) {
                break;
            }
        }
        return quoteLines;
    }

    private static void renderListBlockCompact(List<String> listLines, MarkdownRenderingContext context) throws BadLocationException {
        for (int j = 0; j < listLines.size(); j++) {
            String line = listLines.get(j);
            String text = "• " + line.substring(2);
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), text, context.getStyle("list"), context.getStyles());
            if (j < listLines.size() - 1) {
                context.insertLineSeparator();
            } else {
                context.insertLineSeparator(); // Single separator for compact rendering
            }
        }
    }

    private static void renderBlockquoteCompact(List<String> quoteLines, MarkdownRenderingContext context) throws BadLocationException {
        for (int k = 0; k < quoteLines.size(); k++) {
            String line = quoteLines.get(k);
            String quoteText = line.startsWith("> ") ? line.substring(2) : line.substring(1);
            MarkdownFormatter.appendLineWithFormatting(context.getDocument(), quoteText, context.getStyle("quote"), context.getStyles());
            if (k < quoteLines.size() - 1) {
                context.insertLineSeparator();
            } else {
                context.insertLineSeparator(); // Single separator for compact rendering
            }
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

        // Small informational text (used for bottom-of-view notices)
        Style infoStyle = doc.addStyle("info", defaultStyle);
        StyleConstants.setFontFamily(infoStyle, MarkdownStyle.FONT_FAMILY_DEFAULT);
        StyleConstants.setFontSize(infoStyle, Math.max(10, MarkdownStyle.FONT_SIZE_DEFAULT - 2));
        StyleConstants.setItalic(infoStyle, true);
        StyleConstants.setForeground(infoStyle, new java.awt.Color(0x5B5B5B));
        styles.put("info", infoStyle);

        return styles;
    }

    private static void renderEntries(List<List<String>> entries, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        Style sepStyle = styles.get("sep");
        boolean firstEntry = true;
        
        // Trim trailing blank lines from entries - pre-size for efficiency
        List<List<String>> trimmedEntries = new ArrayList<>(entries.size());
        for (List<String> entry : entries) {
            List<String> trimmed = new ArrayList<>(entry);
            while (!trimmed.isEmpty() && trimmed.get(trimmed.size() - 1).isBlank()) {
                trimmed.remove(trimmed.size() - 1);
            }
            // Sanitize each line to remove control characters and dangerous script tags
            List<String> sanitized = new ArrayList<>(trimmed.size());
            for (String line : trimmed) {
                sanitized.add(sanitizeLine(line));
            }
            trimmedEntries.add(sanitized);
        }
        
        for (List<String> entry : trimmedEntries) {
            if (!firstEntry) {
                // Use centralized format rules for display spacing
                // ALWAYS add separator between entries for consistency
                String separator = filehandling.LogFileFormat.INTERNAL_LINE_SEPARATOR.repeat(filehandling.LogFileFormat.DISPLAY_ENTRY_SEPARATOR_BLANKS);
                doc.insertString(doc.getLength(), separator, sepStyle);
            }
            firstEntry = false;
            
            // If this is the autogenerated info entry added when limiting results, render it
            // with a smaller informational style so it doesn't dominate the view.
            if (!entry.isEmpty() && entry.get(0).startsWith("Showing ") && entry.size() > 1 && entry.get(1).contains("Log List view")) {
                renderInfoEntry(entry, doc, styles);
            } else {
                // Try entry-level cache to avoid re-rendering identical entries
                try {
                    String entryKey = computeHashForEntry(entry);
                    java.util.List<Segment> segs = null;
                    synchronized (ENTRY_CACHE) {
                        java.lang.ref.SoftReference<java.util.List<Segment>> ref = ENTRY_CACHE.get(entryKey);
                        segs = (ref == null) ? null : ref.get();
                        if (ref != null && segs == null) {
                            // reclaimed
                            ENTRY_CACHE.remove(entryKey);
                        }
                    }
                    if (segs != null) {
                        insertSegmentsIntoDoc(doc, segs);
                    } else {
                        segs = buildSegmentsForEntry(entry);
                        synchronized (ENTRY_CACHE) {
                            ENTRY_CACHE.put(entryKey, new java.lang.ref.SoftReference<>(segs));
                        }
                        insertSegmentsIntoDoc(doc, segs);
                    }
                } catch (BadLocationException e) {
                    // Fallback to direct rendering if building/inserting segments fails
                    MarkdownEntryRenderer.renderEntry(entry, new MarkdownRenderingContext(doc, styles));
                }
            }
            
            // Trim trailing newlines from the rendered entry to prevent extra spacing between entries
            // Optimized: count trailing newlines first, then remove in one operation
            try {
                int docLen = doc.getLength();
                if (docLen > 0) {
                    // Get up to last 10 chars to find trailing newlines (should be enough)
                    int checkLen = Math.min(10, docLen);
                    String tail = doc.getText(docLen - checkLen, checkLen);
                    int trailingNewlines = 0;
                    for (int i = tail.length() - 1; i >= 0 && tail.charAt(i) == '\n'; i--) {
                        trailingNewlines++;
                    }
                    if (trailingNewlines > 0) {
                        doc.remove(docLen - trailingNewlines, trailingNewlines);
                    }
                }
            } catch (BadLocationException e) {
                // Ignore if can't trim
            }
        }
    }

    private static void renderInfoEntry(List<String> entry, StyledDocument doc, Map<String, Style> styles) throws BadLocationException {
        Style info = styles.get("info");
        MarkdownRenderingContext ctx = new MarkdownRenderingContext(doc, styles);
        for (int i = 0; i < entry.size(); i++) {
            ctx.insertString(entry.get(i), info);
            if (i < entry.size() - 1) ctx.insertLineSeparator();
        }
        // Ensure two separators after info block to match display spacing
        ctx.insertDoubleLineSeparator();
    }

    private static String sanitizeLine(String line) {
        if (line == null) return "";
        String sanitized;
        try {
            // Remove ASCII control chars except tab (\t)
            sanitized = line.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
            // Neutralize any <script occurrences (case-insensitive)
            sanitized = sanitized.replaceAll("(?i)<script", "&lt;script");
        } catch (Exception e) {
            // On any regex issues, fall back to a safe plain string
            StringBuilder sb = new StringBuilder();
            for (char c : line.toCharArray()) {
                if (c >= 0x20 || c == '\t') sb.append(c);
            }
            sanitized = sb.toString();
        }
        return sanitized;
    }
    private static boolean isHeadingLine(String line) {
        return line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ");
    }

    private static boolean isTimestampLine(String line) {
        return TIMESTAMP_PATTERN.matcher(line.trim()).matches();
    }

    /**
     * Invalidate entire caches (both document-level and per-entry).
     */
    public static void invalidateAllCaches() {
        synchronized (CACHE) { CACHE.clear(); }
        synchronized (ENTRY_CACHE) { ENTRY_CACHE.clear(); }
    }

    /**
     * Invalidate cache for a specific pre-parsed full-lines key.
     */
    public static void invalidateFullCacheForLines(List<String> lines) {
        String key = "full:" + computeHash(lines);
        synchronized (CACHE) { CACHE.remove(key); }
    }

    /**
     * Invalidate cache for a specific entry.
     */
    public static void invalidateEntryCache(List<String> entry) {
        String key = computeHashForEntry(entry);
        synchronized (ENTRY_CACHE) { ENTRY_CACHE.remove(key); }
    }
}
