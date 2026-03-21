package utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight logging wrapper used by non-library code.
 */
public final class Log {
    private static final Logger logger = Logger.getLogger("LogHog");

    private Log() {}

    public static void info(String msg) {
        logger.log(Level.INFO, msg);
    }

    public static void warn(String msg) {
        logger.log(Level.WARNING, msg);
    }

    public static void error(String msg) {
        logger.log(Level.SEVERE, msg);
    }

    public static void error(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }

    public static void debug(String msg) {
        logger.log(Level.FINE, msg);
    }
}
