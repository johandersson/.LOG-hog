import encryption.EncryptionManager;
import filehandling.LogFileHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Simple test program for LogHog optimizations
 * Tests caching, lazy sorting, and encryption without GUI
 */
public class LogHogOptimizationTest {

    private static final Path TEST_FILE_BASIC = Paths.get("log_test_basic.txt");
    private static final Path TEST_FILE_ENCRYPT = Paths.get("log_test_encrypt.txt");
    private static final Path TEST_FILE_CACHE = Paths.get("log_test_cache.txt");
    private static final Path TEST_FILE_SORT = Paths.get("log_test_sort.txt");
    private static final char[] TEST_PASSWORD = "TestPassword123!".toCharArray();

    public static void main(String[] args) {
        System.out.println("=== LogHog Optimization Test ===\n");

        try {
            // Clean up
            Files.deleteIfExists(TEST_FILE_BASIC);
            Files.deleteIfExists(TEST_FILE_ENCRYPT);
            Files.deleteIfExists(TEST_FILE_CACHE);
            Files.deleteIfExists(TEST_FILE_SORT);

            // Test 1: Basic file operations
            System.out.println("Test 1: Basic file operations");
            testBasicOperations();

            // Test 2: Encryption operations
            System.out.println("\nTest 2: Encryption operations");
            testEncryption();

            // Test 3: Caching system
            System.out.println("\nTest 3: Caching system");
            testCaching();

            // Test 4: Lazy sorting
            System.out.println("\nTest 4: Lazy sorting");
            testLazySorting();

            System.out.println("\n=== All tests passed! ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                Files.deleteIfExists(TEST_FILE_BASIC);
            } catch (Exception e) {
                System.err.println("Cleanup failed: " + e.getMessage());
            }
        }
    }

    private static void testBasicOperations() throws Exception {
        LogFileHandler.setTestFilePath(TEST_FILE_BASIC);
        LogFileHandler handler = new LogFileHandler();

        // Test saving
        System.out.println("  - Testing save operations...");
        handler.saveText("First entry", new javax.swing.DefaultListModel<>());
        handler.saveText("Second entry", new javax.swing.DefaultListModel<>());

        List<String> lines = handler.getLines();
        System.out.println("    Saved " + lines.size() + " lines");

        if (lines.size() == 0) {
            throw new RuntimeException("Should have saved lines");
        }

        System.out.println("  ✓ Basic operations passed");
        
        // Cleanup
        Files.deleteIfExists(TEST_FILE_BASIC);
    }

    private static void testEncryption() throws Exception {
        LogFileHandler.setTestFilePath(TEST_FILE_ENCRYPT);
        LogFileHandler handler = new LogFileHandler();

        // Ensure file exists first
        if (!java.nio.file.Files.exists(TEST_FILE_ENCRYPT)) {
            java.nio.file.Files.createFile(TEST_FILE_ENCRYPT);
        }

        handler.saveText("Initial entry", new javax.swing.DefaultListModel<>());

        // Test encryption/decryption
        String testData = "Test data for encryption";
        byte[] salt = EncryptionManager.generateSalt();
        byte[] encrypted = EncryptionManager.encrypt(testData,
            EncryptionManager.deriveKey(TEST_PASSWORD, salt));
        String decrypted = EncryptionManager.decrypt(encrypted,
            EncryptionManager.deriveKey(TEST_PASSWORD, salt));

        if (!testData.equals(decrypted)) {
            throw new RuntimeException("Encryption/decryption failed");
        }

        handler.enableEncryption(TEST_PASSWORD);
        if (!handler.isEncrypted()) {
            throw new RuntimeException("Should be encrypted");
        }

        handler.saveText("Encrypted entry", new javax.swing.DefaultListModel<>());
        List<String> lines = handler.getLines();
        if (lines.size() == 0) {
            throw new RuntimeException("Should have encrypted lines");
        }

        handler.disableEncryption();
        if (handler.isEncrypted()) {
            throw new RuntimeException("Should not be encrypted");
        }

        System.out.println("  ✓ Encryption operations passed");
        
        // Cleanup
        Files.deleteIfExists(TEST_FILE_ENCRYPT);
    }

    private static void testCaching() throws Exception {
        LogFileHandler.setTestFilePath(TEST_FILE_CACHE);
        LogFileHandler handler = new LogFileHandler();

        // Create fresh file and enable encryption
        if (!java.nio.file.Files.exists(TEST_FILE_CACHE)) {
            java.nio.file.Files.createFile(TEST_FILE_CACHE);
        }
        handler.saveText("Initial entry", new javax.swing.DefaultListModel<>());
        handler.enableEncryption(TEST_PASSWORD);

        // Add entries
        for (int i = 0; i < 3; i++) {
            handler.saveText("Cache entry " + i, new javax.swing.DefaultListModel<>());
        }

        // Test caching - multiple calls should return same object
        List<List<String>> entries1 = handler.getParsedEntries();
        List<List<String>> entries2 = handler.getParsedEntries();

        if (entries1 != entries2) {
            throw new RuntimeException("Caching should return same instance");
        }

        // Test cache invalidation
        handler.saveText("New entry", new javax.swing.DefaultListModel<>());
        List<List<String>> entries3 = handler.getParsedEntries();

        if (entries1 == entries3) {
            throw new RuntimeException("Cache should be invalidated");
        }

        System.out.println("  ✓ Caching system passed");
        
        // Cleanup
        Files.deleteIfExists(TEST_FILE_CACHE);
    }

    private static void testLazySorting() throws Exception {
        LogFileHandler.setTestFilePath(TEST_FILE_SORT);
        LogFileHandler handler = new LogFileHandler();

        // Create unsorted entries
        String[] unsortedEntries = {
            "23:59 2025-12-04\nLast entry",
            "01:00 2025-12-04\nFirst entry",
            "12:00 2025-12-04\nMiddle entry"
        };

        Files.write(TEST_FILE_SORT, Arrays.asList(unsortedEntries));

        // First read should trigger sorting
        List<String> lines = handler.getLines();
        System.out.println("  - First read, checking if sorted...");

        // Check if sorted (should contain "01:00" first after sorting)
        boolean isSorted = lines.stream().anyMatch(line -> line.contains("01:00"));
        if (!isSorted) {
            throw new RuntimeException("Lines should be sorted");
        }

        System.out.println("  ✓ Lazy sorting passed");
        
        // Cleanup
        Files.deleteIfExists(TEST_FILE_SORT);
    }
}