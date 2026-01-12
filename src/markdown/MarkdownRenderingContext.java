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

import java.util.Map;

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

/**
 * Context object for markdown rendering operations.
 * Encapsulates the StyledDocument and style map to reduce method parameters
 * and improve readability and reusability.
 */
public class MarkdownRenderingContext {
    private final StyledDocument doc;
    private final Map<String, Style> styles;

    public MarkdownRenderingContext(StyledDocument doc, Map<String, Style> styles) {
        this.doc = doc;
        this.styles = styles;
    }

    public StyledDocument getDocument() {
        return doc;
    }

    public Style getStyle(String name) {
        return styles.get(name);
    }

    public Style getDefaultStyle() {
        return getStyle("default");
    }

    public Style getTimestampStyle() {
        return getStyle("timestamp");
    }

    public Style getListStyle() {
        return getStyle("list");
    }

    public Style getQuoteStyle() {
        return getStyle("quote");
    }

    public Style getQuoteBorderStyle() {
        return getStyle("quoteBorder");
    }

    public Style getCodeStyle() {
        return getStyle("code");
    }

    public Style getH1Style() {
        return getStyle("h1");
    }

    public Style getH2Style() {
        return getStyle("h2");
    }

    public Style getH3Style() {
        return getStyle("h3");
    }

    public void insertString(String text, Style style) throws BadLocationException {
        doc.insertString(doc.getLength(), text, style);
    }

    public void insertString(String text, String styleName) throws BadLocationException {
        insertString(text, getStyle(styleName));
    }

    public void insertLineSeparator() throws BadLocationException {
        insertString(MarkdownStyle.DOCUMENT_LINE_SEPARATOR, getDefaultStyle());
    }

    public void insertDoubleLineSeparator() throws BadLocationException {
        insertString(MarkdownStyle.DOCUMENT_LINE_SEPARATOR + MarkdownStyle.DOCUMENT_LINE_SEPARATOR, getDefaultStyle());
    }

    public Map<String, Style> getStyles() {
        return styles;
    }
}