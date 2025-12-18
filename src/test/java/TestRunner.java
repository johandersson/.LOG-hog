import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple test runner for the new tests added to FileHandlingTest
 */
public class TestRunner {

    public static void main(String[] args) throws Exception {
        // Use temp file for testing
        Path testFile = Files.createTempFile("loghog_test_runner", ".txt");

        try {
            System.out.println("Running extended FileHandlingTest methods...");

            // We can't easily run JUnit tests directly, but we can test the logic
            testTimestampHandlingLogic(testFile);
            testLoadEntryEdgeCases(testFile);

            System.out.println("SUCCESS: All extended tests passed");

        } finally {
            Files.deleteIfExists(testFile);
        }
    }

    private static void testTimestampHandlingLogic(Path testFile) throws Exception {
        // Test the timestamp logic that was key to the fix
        filehandling.LogFileHandler logFileHandler = new filehandling.LogFileHandler();
        filehandling.LogFileHandler.setTestFilePath(testFile);

        try {
            // Test getRawTimestamp logic
            String displayTs1 = "14:30 2025-12-18";
            String rawTs1 = logFileHandler.getRawTimestamp(displayTs1);
            if (!displayTs1.equals(rawTs1)) {
                throw new RuntimeException("FAIL: Basic timestamp handling failed");
            }

            String displayTs2 = "14:30 2025-12-18 (1)";
            String rawTs2 = logFileHandler.getRawTimestamp(displayTs2);
            if (!"14:30 2025-12-18".equals(rawTs2)) {
                throw new RuntimeException("FAIL: Suffix stripping failed: " + rawTs2);
            }

            String displayTs3 = "14:30 2025-12-18 (5)";
            String rawTs3 = logFileHandler.getRawTimestamp(displayTs3);
            if (!"14:30 2025-12-18".equals(rawTs3)) {
                throw new RuntimeException("FAIL: Multi-digit suffix stripping failed: " + rawTs3);
            }

            System.out.println("✓ Timestamp handling logic verified");
        } finally {
            logFileHandler.clearSensitiveData();
        }
    }

    private static void testLoadEntryEdgeCases(Path testFile) throws Exception {
        filehandling.LogFileHandler logFileHandler = new filehandling.LogFileHandler();
        filehandling.EntryLoader entryLoader = new filehandling.EntryLoader(logFileHandler);
        filehandling.LogFileHandler.setTestFilePath(testFile);

        try {
            // Test null/empty handling
            String result1 = entryLoader.loadEntry(null);
            if (!"".equals(result1)) {
                throw new RuntimeException("FAIL: Null timestamp should return empty string");
            }

            String result2 = entryLoader.loadEntry("");
            if (!"".equals(result2)) {
                throw new RuntimeException("FAIL: Empty timestamp should return empty string");
            }

            String result3 = entryLoader.loadEntry("   ");
            if (!"".equals(result3)) {
                throw new RuntimeException("FAIL: Whitespace timestamp should return empty string");
            }

            System.out.println("✓ LoadEntry edge cases verified");
        } finally {
            logFileHandler.clearSensitiveData();
        }
    }
}