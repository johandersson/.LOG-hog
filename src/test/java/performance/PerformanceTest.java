package performance;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import encryption.EncryptionException;
import encryption.EncryptionManager;
import filehandling.LogFileHandler;

import javax.swing.DefaultListModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Performance and load tests for LogHog components
 * Tests system behavior under stress and with large data sets
 */
public class PerformanceTest {

    private EncryptionManager encryptionManager;
    private LogFileHandler logFileHandler;
    private DefaultListModel<String> listModel;

    @BeforeEach
    void setup() {
        encryptionManager = EncryptionManager.getInstance();
        logFileHandler = new LogFileHandler();
        listModel = new DefaultListModel<>();
    }

    @Test
    void testEncryptionPerformance() throws EncryptionException {
        System.out.println("🧪 Testing encryption performance...");

        String testData = "This is a test message for performance evaluation. " +
                         "It contains enough content to measure encryption speed.";
        char[] password = "performanceTestPassword123!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        assertDoesNotThrow(() -> {
            // Measure encryption time for a few operations (each includes PBKDF2 key derivation)
            long startTime = System.nanoTime();

            for (int i = 0; i < 3; i++) {
                byte[] encrypted = encryptionManager.encrypt(testData + i, password, salt);
                assertNotNull(encrypted, "Encryption should succeed");
            }

            long endTime = System.nanoTime();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            System.out.println("Encrypted 3 messages in " + durationMs + "ms");
            assertTrue(durationMs < 60000, "Encryption should complete within reasonable time");
        });

        System.out.println("✅ Encryption performance is acceptable");
    }

    @Test
    void testDecryptionPerformance() throws EncryptionException {
        System.out.println("🧪 Testing decryption performance...");

        String testData = "Performance test data for decryption speed measurement.";
        char[] password = "performanceTestPassword123!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        assertDoesNotThrow(() -> {
            // Encrypt one message then decrypt it
            byte[] encrypted = encryptionManager.encrypt(testData, password, salt);

            long startTime = System.nanoTime();
            String decrypted = encryptionManager.decrypt(encrypted, password);
            long endTime = System.nanoTime();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

            assertEquals(testData, decrypted, "Decryption should be correct");
            System.out.println("Decrypted 1 message in " + durationMs + "ms (includes PBKDF2)");
        });

        System.out.println("✅ Decryption performance is acceptable");
    }

    @Test
    void testLargeDataEncryption() throws EncryptionException {
        System.out.println("🧪 Testing large data encryption...");

        // Create a large data set (1MB)
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            largeData.append("This is line ").append(i).append(" of large test data for encryption. ");
        }
        String largeString = largeData.toString();

        char[] password = "largeDataTestPassword!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        assertDoesNotThrow(() -> {
            long startTime = System.nanoTime();
            byte[] encrypted = encryptionManager.encrypt(largeString, password, salt);
            long encryptTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            assertNotNull(encrypted, "Large data encryption should succeed");
            assertTrue(encrypted.length > largeString.length(), "Encrypted data should be larger");

            long startDecryptTime = System.nanoTime();
            String decrypted = encryptionManager.decrypt(encrypted, password);
            long decryptTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startDecryptTime);

            assertEquals(largeString, decrypted, "Large data decryption should be correct");

            System.out.println("Large data (1MB) encrypted in " + encryptTime + "ms, decrypted in " + decryptTime + "ms");
        });

        System.out.println("✅ Large data encryption/decryption works correctly");
    }

    @Test
    void testConcurrentEncryptionOperations() throws EncryptionException {
        System.out.println("🧪 Testing concurrent encryption operations...");

        String testData = "Concurrent encryption test data.";
        char[] password = "concurrentTestPassword!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        assertDoesNotThrow(() -> {
            // Run 3 threads × 1 iteration (each encrypt+decrypt includes PBKDF2)
            Thread[] threads = new Thread[3];
            Exception[] exceptions = new Exception[threads.length];

            for (int i = 0; i < threads.length; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    try {
                        String data = testData + threadId;
                        byte[] encrypted = encryptionManager.encrypt(data, password, salt);
                        String decrypted = encryptionManager.decrypt(encrypted, password);
                        assertEquals(data, decrypted, "Concurrent operation should be correct");
                    } catch (Exception e) {
                        exceptions[threadId] = e;
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join(10000); // 10 second timeout
            }

            // Check for exceptions
            for (Exception e : exceptions) {
                if (e != null) {
                    fail("Concurrent encryption failed: " + e.getMessage());
                }
            }
        });

        System.out.println("✅ Concurrent encryption operations work correctly");
    }

    @Test
    void testFileHandlingWithManyEntries() {
        System.out.println("🧪 Testing file handling with many entries...");

        assertDoesNotThrow(() -> {
            long startTime = System.nanoTime();

            // Add many entries
            for (int i = 0; i < 100; i++) {
                logFileHandler.saveText("Performance test entry number " + i, listModel);
            }

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            assertEquals(100, listModel.getSize(), "Should have 100 entries");
            System.out.println("Added 100 entries in " + durationMs + "ms");

            // Verify all entries have proper timestamps
            for (int i = 0; i < listModel.getSize(); i++) {
                String timestamp = listModel.getElementAt(i);
                assertTrue(timestamp.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}"),
                          "Entry " + i + " should have proper timestamp");
            }
        });

        System.out.println("✅ File handling with many entries works correctly");
    }

    @Test
    void testMemoryEfficiency() {
        System.out.println("🧪 Testing memory efficiency...");

        // Test that operations don't cause excessive memory usage
        // This is a basic test - in a real scenario you'd use a profiler

        Runtime runtime = Runtime.getRuntime();

        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        assertDoesNotThrow(() -> {
            // Perform many operations
            char[] password = "memoryTestPassword!".toCharArray();
            byte[] salt = encryptionManager.generateSalt();

            for (int i = 0; i < 3; i++) {
                String data = "Memory efficiency test data " + i;
                byte[] encrypted = encryptionManager.encrypt(data, password, salt);
                String decrypted = encryptionManager.decrypt(encrypted, password);
                assertEquals(data, decrypted);
            }

            // Force garbage collection
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException e) {}

            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;

            System.out.println("Memory increase: " + (memoryIncrease / 1024) + "KB");
            // Allow some memory increase but not excessive
            assertTrue(memoryIncrease < 10 * 1024 * 1024, "Memory increase should be reasonable (< 10MB)");
        });

        System.out.println("✅ Memory efficiency is acceptable");
    }

    @Test
    void testKeyDerivationPerformance() throws EncryptionException {
        System.out.println("🧪 Testing key derivation performance...");

        char[] password = "keyDerivationPerformanceTestPassword123!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        assertDoesNotThrow(() -> {
            long startTime = System.nanoTime();

            // Derive key multiple times
            for (int i = 0; i < 10; i++) {
                var key = encryptionManager.deriveKey(password, salt);
                assertNotNull(key, "Key derivation should succeed");
            }

            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            System.out.println("Derived 10 keys in " + durationMs + "ms");
            assertTrue(durationMs < 120000, "Key derivation should complete within reasonable time");
        });

        System.out.println("✅ Key derivation performance is acceptable");
    }

    @Test
    void testSystemStabilityUnderLoad() {
        System.out.println("🧪 Testing system stability under load...");

        assertDoesNotThrow(() -> {
            // Simulate heavy usage pattern
            char[] password = "stabilityTestPassword!".toCharArray();
            byte[] salt = encryptionManager.generateSalt();

            // 2 cycles × 2 items (each encrypt+decrypt call includes PBKDF2)
            for (int cycle = 0; cycle < 2; cycle++) {
                for (int i = 0; i < 2; i++) {
                    String data = "Stability test data cycle " + cycle + " item " + i;
                    byte[] encrypted = encryptionManager.encrypt(data, password, salt);
                    String decrypted = encryptionManager.decrypt(encrypted, password);
                    assertEquals(data, decrypted);
                }

                // File operations
                for (int i = 0; i < 10; i++) {
                    logFileHandler.saveText("Stability test entry " + cycle + "-" + i, listModel);
                }
            }

            assertTrue(listModel.getSize() > 0, "Should have entries after stability test");
        });

        System.out.println("✅ System stability under load verified");
    }
}