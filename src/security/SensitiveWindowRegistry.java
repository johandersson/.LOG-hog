package security;

import java.util.ArrayList;
import java.util.List;

/** Tracks sensitive continuations and explicit cleanup, not authentication windows. */
public final class SensitiveWindowRegistry {
    private long generation;
    private final List<Runnable> cleanup = new ArrayList<>();

    public synchronized long generation() { return generation; }
    public synchronized boolean isCurrent(long token) { return generation == token; }

    public synchronized AutoCloseable register(Runnable action) {
        cleanup.add(action);
        return () -> { synchronized (this) { cleanup.remove(action); } };
    }

    public void invalidate() {
        List<Runnable> actions;
        synchronized (this) {
            generation++;
            actions = new ArrayList<>(cleanup);
            cleanup.clear();
        }
        for (Runnable action : actions) {
            try { action.run(); } catch (RuntimeException ignored) { }
        }
    }
}
