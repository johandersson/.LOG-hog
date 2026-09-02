package filehandling;

/**
 * Exception thrown for log file handling errors.
 */
public class LogFileException extends Exception {
    private static final long serialVersionUID = 1L;
    public LogFileException(String message) {
        super(message);
    }

    public LogFileException(String message, Throwable cause) {
        super(message, cause);
    }
}