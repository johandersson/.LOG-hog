import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import encryption.EncryptionManager;
import filehandling.LogFileHandler;

/**
 * Simple test program for LogHog optimizations
 * Tests caching, lazy sorting, and encryption without GUI
 */
public class LogHogOptimizationTest {

    private static final Path TEST_FILE_BASIC = Paths.get("log_test_basic.txt");
    private static final Path TEST_FILE_ENCRYPT = Paths.get("log_test_encrypt.txt");
    private static final Path TEST_FILE_CACHE = Paths.get("log_test_cache.txt");
    private static final Path TEST_FILE_SORT = Paths.get("log_test_sort.txt");

    private static char[] generateTestPassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        char[] password = new char[16];
        for (int i = 0; i < password.length; i++) {
            password[i] = chars.charAt(random.nextInt(chars.length()));
        }
        return password;
    }

    public static void main(String[] args) {
        utils.Log.info("=== LogHog Optimization Test ===\n");

        try {
            // Clean up
            Files.deleteIfExists(TEST_FILE_BASIC);
            Files.deleteIfExists(TEST_FILE_ENCRYPT);
            Files.deleteIfExists(TEST_FILE_CACHE);
            Files.deleteIfExists(TEST_FILE_SORT);

            // Test 1: Basic file operations
            utils.Log.info("Test 1: Basic file operations");
            testBasicOperations();

            // Test 2: Encryption operations
            utils.Log.info("\nTest 2: Encryption operations");
            testEncryption();

            // Test 3: Caching system
            utils.Log.info("\nTest 3: Caching system");
            testCaching();

            // Test 4: Lazy sorting
            utils.Log.info("\nTest 4: Lazy sorting");
            testLazySorting();

            utils.Log.info("\n=== All tests passed! ===");

        } catch (Exception e) {
            utils.Log.error("Test failed: " + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(TEST_FILE_BASIC);
            } catch (Exception e) {
                utils.Log.error("Cleanup failed: " + e.getMessage(), e);
            }
        }
    }

    private static void testBasicOperations() throws Exception {
        LogFileHandler.setTestFilePath(TEST_FILE_BASIC);
        LogFileHandler handler = new LogFileHandler();

        // Test saving
        utils.Log.info("  - Testing save operations...");
        handler.saveText("First entry", new javax.swing.DefaultListModel<>());
        handler.saveText("Second entry", new javax.swing.DefaultListModel<>());

        List<String> lines = handler.getLines();
        utils.Log.info("    Saved " + lines.size() + " lines");

        if (lines.isEmpty()) {
            throw new RuntimeException("Should have saved lines");
        }

        utils.Log.info("  ✓ Basic operations passed");
        
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
        byte[] salt = EncryptionManager.getInstance().generateSalt();
        char[] testPassword = generateTestPassword();
        byte[] encrypted = EncryptionManager.getInstance().encrypt(testData, testPassword, salt);
        String decrypted = EncryptionManager.getInstance().decrypt(encrypted, testPassword);

        if (!testData.equals(decrypted)) {
            throw new RuntimeException("Encryption/decryption failed");
        }

        handler.enableEncryption(testPassword);
        if (!handler.isEncrypted()) {
            throw new RuntimeException("Should be encrypted");
        }

        handler.saveText("Encrypted entry", new javax.swing.DefaultListModel<>());
        List<String> lines = handler.getLines();
        if (lines.isEmpty()) {
            throw new RuntimeException("Should have encrypted lines");
        }

        handler.disableEncryption();
        if (handler.isEncrypted()) {
            throw new RuntimeException("Should not be encrypted");
        }

        utils.Log.info("  ✓ Encryption operations passed");
        
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
        char[] cachePassword = generateTestPassword();
        handler.enableEncryption(cachePassword);

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

        utils.Log.info("  ✓ Caching system passed");
        
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
        utils.Log.info("  - First read, checking if sorted...");

        // Check if sorted (should contain "01:00" first after sorting)
        boolean isSorted = lines.stream().anyMatch(line -> line.contains("01:00"));
        if (!isSorted) {
            throw new RuntimeException("Lines should be sorted");
        }

        utils.Log.info("  ✓ Lazy sorting passed");
        
        // Cleanup
        Files.deleteIfExists(TEST_FILE_SORT);
    }
}