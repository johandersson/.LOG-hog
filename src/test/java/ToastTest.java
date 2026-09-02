package test;

import utils.Toast;

/**
 * Simple test for LogHog Toast utility class functionality
 */
public class ToastTest {

    public static void main(String[] args) {
        testsupport.TestLog.out("=== Toast Test Suite ===\n");

        // Test 1: Test that Toast can be created without throwing exceptions
        testsupport.TestLog.out("Test 1: Testing toast creation...");
        try {
            Toast.showToast(null, "Test message");
            testsupport.TestLog.out("✓ Toast creation successful");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Toast creation failed: " + e.getMessage());
            return;
        }

        // Test 2: Test Toast with a specific message
        testsupport.TestLog.out("Test 2: Testing toast with message...");
        try {
            Toast.showToast(null, "Hello from pure Java test!");
            testsupport.TestLog.out("✓ Toast with message successful");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Toast with message failed: " + e.getMessage());
            return;
        }

        // Test 3: Test Toast with empty message
        testsupport.TestLog.out("Test 3: Testing toast with empty message...");
        try {
            Toast.showToast(null, "");
            testsupport.TestLog.out("✓ Toast with empty message successful");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Toast with empty message failed: " + e.getMessage());
            return;
        }

        // Test 4: Test Toast with null message (should handle gracefully)
        testsupport.TestLog.out("Test 4: Testing toast with null message...");
        try {
            Toast.showToast(null, null);
            testsupport.TestLog.out("✓ Toast with null message handled gracefully");
        } catch (Exception e) {
            testsupport.TestLog.out("✗ FAIL: Toast with null message failed: " + e.getMessage());
            return;
        }

        testsupport.TestLog.out("\n=== All Toast tests completed successfully! ===");
    }
}