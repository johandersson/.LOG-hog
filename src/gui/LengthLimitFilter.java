package gui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class LengthLimitFilter extends DocumentFilter {
    private final int max;

    public LengthLimitFilter(int max) {
        this.max = max;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (string == null) return;
        int allowed = max - fb.getDocument().getLength();
        if (allowed <= 0) return;
        String toInsert = string.length() > allowed ? string.substring(0, allowed) : string;
        super.insertString(fb, offset, toInsert, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (text == null) return;
        int currentLen = fb.getDocument().getLength();
        int allowed = max - (currentLen - length);
        if (allowed <= 0) return;
        String toReplace = text.length() > allowed ? text.substring(0, allowed) : text;
        super.replace(fb, offset, length, toReplace, attrs);
    }
}
