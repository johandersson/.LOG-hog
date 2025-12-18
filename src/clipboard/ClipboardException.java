package clipboard;

/**
 * Exception thrown for clipboard operation errors.
 */
public class ClipboardException extends Exception {
    public ClipboardException(String message) {
        super(message);
    }

    public ClipboardException(String message, Throwable cause) {
        super(message, cause);
    }
}