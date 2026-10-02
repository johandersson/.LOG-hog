package utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Prevents superseded or invalidated asynchronous requests from publishing results.
 */
public final class AsyncRequestGate {
    private final AtomicLong generation = new AtomicLong();

    public long start() {
        return generation.incrementAndGet();
    }

    public void invalidate() {
        generation.incrementAndGet();
    }

    public boolean isCurrent(long request) {
        return request == generation.get();
    }
}
