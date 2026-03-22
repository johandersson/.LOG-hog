package utils;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight logging wrapper used by non-library code.
 * Adds lazy-evaluation helpers to avoid expensive string concatenation when
 * the log level is disabled (helps satisfy PMD guardlogstatement checks).
 */
public final class Log {
    private static final Logger logger = Logger.getLogger("LogHog");

    private Log() {}

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
