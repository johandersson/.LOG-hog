package clipboard;

/**
 * Exception thrown for clipboard operation errors.
 */
public class ClipboardException extends Exception {
    private static final long serialVersionUID = 1L;
    public ClipboardException(String message) {
        super(message);
    }

    public ClipboardException(String message, Throwable cause) {
        super(message, cause);
    }
}