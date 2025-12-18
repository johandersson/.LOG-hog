package clipboard;

import java.awt.Component;

public interface ClipboardHandler {
    void copySecureTextToClipboard(String text, Component parent);
    void copySecureTextToClipboard(String text, Component parent, String successMessage);
}