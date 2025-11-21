package clipboard;

import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;

public class ClipboardManager {
    public static void copyTextToClipboard(String text, Component parent) {
        if (text == null || text.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(parent, "Text is empty.", "Copy Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            clipboard.setContents(selection, selection);
            JOptionPane.showMessageDialog(parent, "Text copied to clipboard.", "Copied", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException ise) {
            JOptionPane.showMessageDialog(parent, "Unable to access clipboard right now. Try again.", "Clipboard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void copyLogEntryToClipboard(String timestamp, String content, Component parent) {
        String toCopy = timestamp + "\n\n" + content;
        copyTextToClipboard(toCopy, parent);
    }
}