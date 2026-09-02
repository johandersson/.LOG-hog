package testsupport;

import utils.Log;

/**
 * Centralized test logging adapter to avoid direct System.out/err usage.
 */
public final class TestLog {

    private TestLog() {
    }

    public static void out(Object message) {
        Log.info(() -> String.valueOf(message));
    }

    public static void err(Object message) {
        Log.error(() -> String.valueOf(message), null);
    }
}
