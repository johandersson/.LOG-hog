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

package gui;

import java.awt.Toolkit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import gui.DialogHelper;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

public class HighlightableTextPane extends JTextPane {
    private final Highlighter.HighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(java.awt.Color.YELLOW);

    public HighlightableTextPane() {
        super();
    }

    public boolean highlightText(String query) {
        return highlightText(query, false, false) > 0;
    }

    public int highlightText(String query, boolean wholeWord, boolean caseSensitive) {
        var highlighter = getHighlighter();
        highlighter.removeAllHighlights();

        if (query == null || query.isBlank()) {
            return 0;
        }

        String text = getDocumentText();
        if (text == null) {
            return 0;
        }

        int matchCount = performSearch(text, query, wholeWord, caseSensitive, highlighter);

        if (matchCount > 0) {
            scrollToFirstHighlight(highlighter);
        }

        return matchCount;
    }

    private String getDocumentText() {
        var doc = getDocument();
        var len = doc.getLength();
        try {
            return doc.getText(0, len);
        } catch (BadLocationException e) {
            DialogHelper.showError(this, "Error", "Error accessing document");
            return null;
        }
    }

    private int performSearch(String text, String query, boolean wholeWord, boolean caseSensitive, Highlighter highlighter) {
        final int MAX_HIGHLIGHTS = 10000;
        if (!wholeWord) {
            return performSimpleSearch(text, query, caseSensitive, highlighter, MAX_HIGHLIGHTS);
        } else {
            return performRegexSearch(text, query, caseSensitive, highlighter, MAX_HIGHLIGHTS);
        }
    }

    private int performSimpleSearch(String text, String query, boolean caseSensitive, Highlighter highlighter, int maxHighlights) {
        String searchText = caseSensitive ? text : text.toLowerCase();
        String searchQuery = caseSensitive ? query : query.toLowerCase();
        int queryLen = searchQuery.length();
        int matchCount = 0;
        int index = 0;

        try {
            while ((index = searchText.indexOf(searchQuery, index)) != -1) {
                highlighter.addHighlight(index, index + queryLen, highlightPainter);
                matchCount++;
                index += queryLen;

                if (matchCount >= maxHighlights) {
                    break;
                }
            }
        } catch (BadLocationException ex) {
            // ignore individual highlight failures
        }

        return matchCount;
    }

    private int performRegexSearch(String text, String query, boolean caseSensitive, Highlighter highlighter, int maxHighlights) {
        String regex = "\\b" + Pattern.quote(query) + "\\b";
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern = Pattern.compile(regex, flags);
        Matcher matcher = pattern.matcher(text);
        int matchCount = 0;

        try {
            while (matcher.find()) {
                highlighter.addHighlight(matcher.start(), matcher.end(), highlightPainter);
                matchCount++;

                if (matchCount >= maxHighlights) {
                    break;
                }
            }
        } catch (BadLocationException ex) {
            // ignore individual highlight failures
        }

        return matchCount;
    }

    private void scrollToFirstHighlight(Highlighter highlighter) {
        try {
            var highlights = highlighter.getHighlights();
            if (highlights.length > 0) {
                var firstIndex = highlights[0].getStartOffset();
                var rect = modelToView2D(firstIndex).getBounds();
                if (rect != null) {
                    rect.height = Math.max(rect.height, 20);
                    scrollRectToVisible(rect);
                    setCaretPosition(firstIndex);
                }
            }
        } catch (BadLocationException ex) {
            // ignore scrolling failure
        }
    }

    public void navigateHighlights(boolean next) {
        var highlighter = getHighlighter();
        var highlights = highlighter.getHighlights();
        if (highlights.length == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        var caretPos = getCaretPosition();
        var targetIndex = -1;

        if (next) {
            for (var i = 0; i < highlights.length; i++) {
                if (highlights[i].getStartOffset() > caretPos) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1) targetIndex = 0;
        } else {
            for (var i = highlights.length - 1; i >= 0; i--) {
                if (highlights[i].getEndOffset() < caretPos) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1) targetIndex = highlights.length - 1;
        }

        if (targetIndex != -1) {
            var h = highlights[targetIndex];
            var start = h.getStartOffset();
            try {
                var rect = modelToView2D(start).getBounds();
                rect.height = Math.max(rect.height, 20);
                scrollRectToVisible(rect);
                setCaretPosition(start);
            } catch (BadLocationException ex) {
                // ignore
            }
        }
    }

    public void clearHighlights() {
        getHighlighter().removeAllHighlights();
    }
}
