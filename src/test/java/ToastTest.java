package test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import utils.Toast;

/**
 * First JUnit test for LogHog application
 * Tests the Toast utility class functionality
 */
public class ToastTest {

    @Test
    public void testToastCreation() {
        // Test that Toast can be created without throwing exceptions
        assertDoesNotThrow(() -> {
            Toast.showToast(null, "Test message");
        });
    }

    @Test
    public void testToastWithMessage() {
        // Test Toast with a specific message
        assertDoesNotThrow(() -> {
            Toast.showToast(null, "Hello from JUnit test!");
        });
    }

    @Test
    public void testToastWithEmptyMessage() {
        // Test Toast with empty message
        assertDoesNotThrow(() -> {
            Toast.showToast(null, "");
        });
    }

    @Test
    public void testToastWithNullMessage() {
        // Test Toast with null message (should handle gracefully)
        assertDoesNotThrow(() -> {
            Toast.showToast(null, null);
        });
    }
}