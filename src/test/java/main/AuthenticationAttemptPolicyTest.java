package main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AuthenticationAttemptPolicyTest {
    @Test
    void startupExitsAfterThreeFailedAttempts() {
        List<Integer> exitStatuses = new ArrayList<>();
        List<String> events = new ArrayList<>();
        AuthenticationAttemptPolicy policy = new AuthenticationAttemptPolicy(status -> {
            exitStatuses.add(status);
            events.add("exit");
        });

        assertFalse(policy.recordFailure(true, () -> events.add("exhausted")));
        assertTrue(exitStatuses.isEmpty());
        assertFalse(policy.recordFailure(true, () -> events.add("exhausted")));
        assertTrue(exitStatuses.isEmpty());
        assertTrue(policy.recordFailure(true, () -> events.add("exhausted")));

        assertEquals(List.of("exhausted", "exit"), events);
        assertEquals(List.of(1), exitStatuses);
        assertEquals(3, policy.getAttempts());
        assertEquals(0, policy.getRemainingAttempts());
    }

    @Test
    void exhaustedStartupShowsMessageBeforeExiting() {
        List<String> events = new ArrayList<>();
        AuthenticationAttemptPolicy policy = new AuthenticationAttemptPolicy(status -> {
            assertEquals(1, status);
            events.add("exit");
        });

        policy.recordFailure(true, () -> {});
        policy.recordFailure(true, () -> {});
        assertTrue(policy.recordFailure(true, () -> events.add("message")));

        assertEquals(List.of("message", "exit"), events);
    }

    @Test
    void exhaustedManualUnlockDoesNotExitApplication() {
        List<Integer> exitStatuses = new ArrayList<>();
        AuthenticationAttemptPolicy policy = new AuthenticationAttemptPolicy(exitStatuses::add);

        policy.recordFailure(false, () -> {});
        policy.recordFailure(false, () -> {});
        assertTrue(policy.recordFailure(false, () -> {}));

        assertTrue(exitStatuses.isEmpty());
    }
}
