package performance;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import encryption.EncryptionException;
import encryption.EncryptionManager;
import filehandling.LogFileHandler;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class PerformanceTest {

    private EncryptionManager encryptionManager;
    private LogFileHandler logFileHandler;
    private DefaultListModel<String> listModel;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        encryptionManager = EncryptionManager.getInstance();
        logFileHandler = new LogFileHandler(tempDir.resolve("performance-log.txt"), encryptionManager);
        listModel = new DefaultListModel<>();
        try {
            logFileHandler.enableEncryption("performance-password".toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void cleanup() {
        logFileHandler.clearSensitiveData();
    }

    @Test
    void testEncryptionPerformance() throws EncryptionException {
        String testData = "This is a test message for performance evaluation. It contains enough content to measure encryption speed.";
        char[] password = "performanceTestPassword123!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        long startTime = System.nanoTime();
        for (int i = 0; i < 3; i++) {
            byte[] encrypted = encryptionManager.encrypt(testData + i, password, salt);
            assertNotNull(encrypted);
        }
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        assertTrue(durationMs < 60000);
    }

    @Test
    void testDecryptionPerformance() throws EncryptionException {
        String testData = "Performance test data for decryption speed measurement.";
        char[] password = "performanceTestPassword123!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        byte[] encrypted = encryptionManager.encrypt(testData, password, salt);
        long startTime = System.nanoTime();
        String decrypted = encryptionManager.decrypt(encrypted, password);
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertEquals(testData, decrypted);
        assertTrue(durationMs < 60000);
    }

    @Test
    void testLargeDataEncryption() throws EncryptionException {
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            largeData.append("This is line ").append(i).append(" of large test data for encryption. ");
        }
        String largeString = largeData.toString();
        char[] password = "largeDataTestPassword!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        byte[] encrypted = encryptionManager.encrypt(largeString, password, salt);
        String decrypted = encryptionManager.decrypt(encrypted, password);
        assertEquals(largeString, decrypted);
    }

    @Test
    void testConcurrentEncryptionOperations() throws EncryptionException {
        String testData = "Concurrent encryption test data.";
        char[] password = "concurrentTestPassword!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();
        Thread[] threads = new Thread[3];
        Exception[] exceptions = new Exception[threads.length];

        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    String data = testData + threadId;
                    byte[] encrypted = encryptionManager.encrypt(data, password, salt);
                    String decrypted = encryptionManager.decrypt(encrypted, password);
                    assertEquals(data, decrypted);
                } catch (Exception e) {
                    exceptions[threadId] = e;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting for worker threads");
            }
        }

        for (Exception e : exceptions) {
            if (e != null) {
                fail("Concurrent encryption failed: " + e.getMessage());
            }
        }
    }

    @Test
    void testFileHandlingWithManyEntries() throws Exception {
        for (int i = 0; i < 100; i++) {
            logFileHandler.saveText("Performance test entry number " + i, listModel);
        }
        flushEdt();
        assertEquals(100, listModel.getSize());
    }

    @Test
    void testMemoryEfficiency() throws EncryptionException {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        char[] password = "memoryTestPassword!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();

        for (int i = 0; i < 3; i++) {
            String data = "Memory efficiency test data " + i;
            byte[] encrypted = encryptionManager.encrypt(data, password, salt);
            String decrypted = encryptionManager.decrypt(encrypted, password);
            assertEquals(data, decrypted);
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        assertTrue(memoryIncrease < 128L * 1024L * 1024L, "Memory increase should stay within a generous bound");
    }

    @Test
    void testKeyDerivationPerformance() throws EncryptionException {
        char[] password = "keyDerivationPerformanceTestPassword123!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();
        long startTime = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            assertNotNull(encryptionManager.deriveKey(password, salt));
        }
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        assertTrue(durationMs < 120000);
    }

    @Test
    void testSystemStabilityUnderLoad() throws Exception {
        char[] password = "stabilityTestPassword!".toCharArray();
        byte[] salt = encryptionManager.generateSalt();
        for (int i = 0; i < 10; i++) {
            String entry = "stability-entry-" + i;
            byte[] encrypted = encryptionManager.encrypt(entry, password, salt);
            assertEquals(entry, encryptionManager.decrypt(encrypted, password));
            logFileHandler.saveText(entry, listModel);
        }
        flushEdt();
        assertTrue(listModel.getSize() > 0);
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }
}
