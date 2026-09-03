package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import security.AppPathPolicy;

/**
 * Lightweight logging wrapper used by non-library code.
 * Adds lazy-evaluation helpers to avoid expensive string concatenation when
 * the log level is disabled (helps satisfy PMD guardlogstatement checks).
 *
 * <p>In addition to the default console handler, a best-effort {@link FileHandler}
 * is attached so that messages (in particular {@link #error} calls used to report
 * otherwise-uncaught failures, such as a broken look and feel) are still recorded
 * when the application is launched without an attached console (e.g. via
 * {@code javaw} on Windows), where anything written to stdout/stderr is silently
 * discarded and would otherwise appear to "not log anything".</p>
 */
public final class Log {
    private static final Logger logger = Logger.getLogger("LogHog");
    private static final String LOG_FILE_NAME = "loghog.log";

    static {
        attachFileHandler();
    }

    private Log() {}

    private static void attachFileHandler() {
        try {
            Path dir = AppPathPolicy.appDataDirectory();
            Files.createDirectories(dir);
            Path logFile = dir.resolve(LOG_FILE_NAME);
            FileHandler fileHandler = new FileHandler(logFile.toString(), 1_000_000, 3, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
        } catch (IOException | SecurityException ignored) {
            // Best effort: fall back to whatever handlers (e.g. console) are already
            // configured rather than failing logging setup itself.
        }
    }

    public static void info(String msg) {
        logger.log(Level.INFO, msg);
    }

    public static void info(Supplier<String> msgSupplier) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, msgSupplier.get());
        }
    }

    public static void warn(String msg) {
        logger.log(Level.WARNING, msg);
    }

    public static void warn(Supplier<String> msgSupplier) {
        if (logger.isLoggable(Level.WARNING)) {
            logger.log(Level.WARNING, msgSupplier.get());
        }
    }

    public static void error(String msg) {
        logger.log(Level.SEVERE, msg);
    }

    public static void error(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }

    public static void error(Supplier<String> msgSupplier, Throwable t) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.log(Level.SEVERE, msgSupplier.get(), t);
        }
    }

    public static void debug(String msg) {
        logger.log(Level.FINE, msg);
    }

    public static void debug(Supplier<String> msgSupplier) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, msgSupplier.get());
        }
    }
}
