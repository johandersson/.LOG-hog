package filehandling;

/**
 * Exception thrown for log file handling errors.
 */
public class LogFileException extends Exception {
    public LogFileException(String message) {
        super(message);
    }

    public LogFileException(String message, Throwable cause) {
        super(message, cause);
    }
}