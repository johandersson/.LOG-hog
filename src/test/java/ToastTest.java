package test;

import utils.Toast;

/**
 * Simple test for LogHog Toast utility class functionality
 */
public class ToastTest {

    public static void main(String[] args) {
        System.out.println("=== Toast Test Suite ===\n");

        // Test 1: Test that Toast can be created without throwing exceptions
        System.out.println("Test 1: Testing toast creation...");
        try {
            Toast.showToast(null, "Test message");
            System.out.println("✓ Toast creation successful");
        } catch (Exception e) {
            System.out.println("✗ FAIL: Toast creation failed: " + e.getMessage());
            return;
        }

        // Test 2: Test Toast with a specific message
        System.out.println("Test 2: Testing toast with message...");
        try {
            Toast.showToast(null, "Hello from pure Java test!");
            System.out.println("✓ Toast with message successful");
        } catch (Exception e) {
            System.out.println("✗ FAIL: Toast with message failed: " + e.getMessage());
            return;
        }

        // Test 3: Test Toast with empty message
        System.out.println("Test 3: Testing toast with empty message...");
        try {
            Toast.showToast(null, "");
            System.out.println("✓ Toast with empty message successful");
        } catch (Exception e) {
            System.out.println("✗ FAIL: Toast with empty message failed: " + e.getMessage());
            return;
        }

        // Test 4: Test Toast with null message (should handle gracefully)
        System.out.println("Test 4: Testing toast with null message...");
        try {
            Toast.showToast(null, null);
            System.out.println("✓ Toast with null message handled gracefully");
        } catch (Exception e) {
            System.out.println("✗ FAIL: Toast with null message failed: " + e.getMessage());
            return;
        }

        System.out.println("\n=== All Toast tests completed successfully! ===");
    }
}