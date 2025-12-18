package clipboard;

import java.awt.Component;

/**
 * Interface for clipboard operations to enable polymorphism and LSP compliance.
 */
public interface ClipboardHandler {
    void copySecureTextToClipboard(String text, Component parent);
    void copySecureTextToClipboard(String text, Component parent, String successMessage);
}