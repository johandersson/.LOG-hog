package utils;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AsyncRequestGateTest {
    @Test
    void onlyNewestRequestCanApplyResults() {
        var gate = new AsyncRequestGate();
        long first = gate.start();
        long second = gate.start();
        assertFalse(gate.isCurrent(first));
        assertTrue(gate.isCurrent(second));
    }

    @Test
    void invalidatedRequestsStayInvalidWhenNewWorkStarts() {
        var gate = new AsyncRequestGate();
        long beforeLock = gate.start();
        gate.invalidate();
        assertFalse(gate.isCurrent(beforeLock));
        long afterUnlock = gate.start();
        assertFalse(gate.isCurrent(beforeLock));
        assertTrue(gate.isCurrent(afterUnlock));
    }
}
