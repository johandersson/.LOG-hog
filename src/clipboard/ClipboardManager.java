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

package clipboard;

import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import utils.Toast;

public class ClipboardManager {
    public static void copyTextToClipboard(String text, Component parent) {
        copyTextToClipboard(text, parent, "Text copied to clipboard.");
    }

    public static void copyTextToClipboard(String text, Component parent, String successMessage) {
        if (text == null || text.isEmpty()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(parent, "Text is empty.", "Copy Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            clipboard.setContents(selection, selection);
            Component toastParent = parent;
            Window window = SwingUtilities.getWindowAncestor(parent);
            if (window != null) {
                toastParent = window;
            }
            Toast.showToast(toastParent, successMessage);
        } catch (IllegalStateException ise) {
            JOptionPane.showMessageDialog(parent, "Unable to access clipboard right now. Try again.", "Clipboard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void copyLogEntryToClipboard(String timestamp, String content, Component parent) {
        String toCopy = timestamp + "\n\n" + content;
        copyTextToClipboard(toCopy, parent);
    }
}
