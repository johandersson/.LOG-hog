package security;

import java.util.Properties;

/**
 * Persistent authentication lockout tracker backed by app settings.
 */
public final class PersistentAuthLockout {
    private static final String KEY_FAILED = "authFailedAttempts";
    private static final String KEY_LOCKED_UNTIL = "authLockedUntilEpochMs";
    private static final int MAX_FAILED_ATTEMPTS = 4;
    private static final long LOCKOUT_MS = 10 * 60 * 1000L;

    private PersistentAuthLockout() {}

    public static long getRemainingLockoutMillis(Properties settings) {
        long now = System.currentTimeMillis();
        long lockedUntil = getLong(settings, KEY_LOCKED_UNTIL, 0L);
        return Math.max(0L, lockedUntil - now);
    }

    public static void registerFailure(Properties settings) {
        int failed = getInt(settings, KEY_FAILED, 0) + 1;
        if (failed >= MAX_FAILED_ATTEMPTS) {
            settings.setProperty(KEY_LOCKED_UNTIL, Long.toString(System.currentTimeMillis() + LOCKOUT_MS));
            settings.setProperty(KEY_FAILED, "0");
            return;
        }
        settings.setProperty(KEY_FAILED, Integer.toString(failed));
    }

    public static void clear(Properties settings) {
        settings.setProperty(KEY_FAILED, "0");
        settings.setProperty(KEY_LOCKED_UNTIL, "0");
    }

    private static int getInt(Properties settings, String key, int defaultValue) {
        try {
            return Integer.parseInt(settings.getProperty(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static long getLong(Properties settings, String key, long defaultValue) {
        try {
            return Long.parseLong(settings.getProperty(key, Long.toString(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}