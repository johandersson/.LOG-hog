package utils;

import java.util.function.Supplier;

/**
 * Centralized best-effort execution helpers to avoid repeated local try/catch
 * blocks for non-critical operations.
 */
public final class SafeExecution {

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private SafeExecution() {
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public static void run(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {
            // Intentionally ignored for best-effort operations.
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public static <T> T getOrDefault(ThrowingSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public static boolean testOrFalse(ThrowingSupplier<Boolean> supplier) {
        try {
            return Boolean.TRUE.equals(supplier.get());
        } catch (Exception ignored) {
            return false;
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public static void runWithLog(ThrowingRunnable runnable, Supplier<String> messageSupplier) {
        try {
            runnable.run();
        } catch (Exception e) {
            Log.warn(messageSupplier);
            Log.debug(() -> String.valueOf(e.getMessage()));
        }
    }
}
