package main;

import java.util.function.IntConsumer;

final class AuthenticationAttemptPolicy {
    static final int MAX_ATTEMPTS = 3;

    private final IntConsumer exitHandler;
    private int attempts;

    AuthenticationAttemptPolicy(IntConsumer exitHandler) {
        this.exitHandler = exitHandler;
    }

    boolean recordFailure(boolean exitOnExhaustion, Runnable onExhausted) {
        attempts++;
        if (attempts < MAX_ATTEMPTS) {
            return false;
        }

        onExhausted.run();
        if (exitOnExhaustion) {
            exitHandler.accept(1);
        }
        return true;
    }

    int getAttempts() {
        return attempts;
    }

    int getRemainingAttempts() {
        return MAX_ATTEMPTS - attempts;
    }
}
