package gui;

import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.*;
import javax.swing.text.*;

public class HighlightableTextPane extends JTextPane {
    private final Highlighter.HighlightPainter highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(java.awt.Color.YELLOW);

    public HighlightableTextPane() {
        super();
    }

    public boolean highlightText(String query) {
        Highlighter highlighter = getHighlighter();
        highlighter.removeAllHighlights();

        if (query == null || query.isBlank()) {
            return false;
        }

        Document doc = getDocument();
        int len = doc.getLength();
        String text;
        try {
            text = doc.getText(0, len);
        } catch (BadLocationException e) {
            JOptionPane.showMessageDialog(this, "Error accessing document", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String lower = text.toLowerCase();
        String qLower = query.toLowerCase();

        int index = lower.indexOf(qLower);
        if (index == -1) {
            return false;
        }

        int start = 0;
        try {
            while ((start = lower.indexOf(qLower, start)) >= 0) {
                int end = start + qLower.length();
                highlighter.addHighlight(start, end, highlightPainter);
                start = end;
            }
        } catch (BadLocationException ex) {
            // ignore individual highlight failures
        }

        try {
            Rectangle rect = modelToView(index);
            if (rect != null) {
                rect.height = Math.max(rect.height, 20);
                scrollRectToVisible(rect);
                setCaretPosition(index);
            }
        } catch (BadLocationException ex) {
            // ignore scrolling failure
        }
        return true;
    }

    public void navigateHighlights(boolean next) {
        Highlighter highlighter = getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        if (highlights.length == 0) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        int caretPos = getCaretPosition();
        int targetIndex = -1;

        if (next) {
            for (int i = 0; i < highlights.length; i++) {
                if (highlights[i].getStartOffset() > caretPos) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1) targetIndex = 0;
        } else {
            for (int i = highlights.length - 1; i >= 0; i--) {
                if (highlights[i].getEndOffset() < caretPos) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1) targetIndex = highlights.length - 1;
        }

        if (targetIndex != -1) {
            Highlighter.Highlight h = highlights[targetIndex];
            int start = h.getStartOffset();
            try {
                Rectangle rect = modelToView2D(start).getBounds();
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