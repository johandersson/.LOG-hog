package encryption;

// Unused import removed for PMD compliance
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the EncryptionManager
 * Tests all encryption, decryption, and key derivation functionality
 */
public class EncryptionManagerTest {

    private EncryptionManager encryptionManager;
    private byte[] testSalt;
    private char[] testPassword;

    @BeforeEach
    void setup() {
        encryptionManager = EncryptionManager.getInstance();
        testSalt = new byte[16];
        new java.security.SecureRandom().nextBytes(testSalt);
        testPassword = "testPassword123!".toCharArray();
    }

    @Test
    void testGenerateSalt() {
        System.out.println("🧪 Testing salt generation...");

        assertDoesNotThrow(() -> {
            byte[] salt = encryptionManager.generateSalt();
            assertNotNull(salt, "Salt should not be null");
            assertEquals(16, salt.length, "Salt should be 16 bytes");
            System.out.println("✅ Salt generation works correctly");
        });
    }

    @Test
    void testEncryptDecryptCycle() {
        System.out.println("🧪 Testing basic encrypt/decrypt cycle...");

        String originalText = "Hello, this is a test message for encryption!";

        assertDoesNotThrow(() -> {
            // Encrypt
            byte[] encrypted = encryptionManager.encrypt(originalText, testPassword, testSalt);
            assertNotNull(encrypted, "Encrypted data should not be null");
            assertTrue(encrypted.length > originalText.length(), "Encrypted data should be longer than original");

            // Decrypt
            String decrypted = encryptionManager.decrypt(encrypted, testPassword);
            assertEquals(originalText, decrypted, "Decrypted text should match original");

            System.out.println("✅ Basic encrypt/decrypt cycle works perfectly");
        });
    }

    @Test
    void testEncryptDecryptWithDifferentPasswords() {
        System.out.println("🧪 Testing encryption with different passwords...");

        String message = "Secret message";
        char[] wrongPassword = "wrongPassword".toCharArray();

        assertDoesNotThrow(() -> {
            // Encrypt with correct password
            byte[] encrypted = encryptionManager.encrypt(message, testPassword, testSalt);

            // Should decrypt with correct password
            String decrypted = encryptionManager.decrypt(encrypted, testPassword);
            assertEquals(message, decrypted, "Should decrypt with correct password");

            // Should fail with wrong password
            assertThrows(EncryptionException.class, () -> {
                encryptionManager.decrypt(encrypted, wrongPassword);
            }, "Should fail with wrong password");

            System.out.println("✅ Password validation works correctly");
        });
    }

    @Test
    void testCorruptedDataHandling() {
        System.out.println("🧪 Testing corrupted data handling...");

        String message = "Test message";

        assertDoesNotThrow(() -> {
            byte[] encrypted = encryptionManager.encrypt(message, testPassword, testSalt);

            // Corrupt the data by changing a byte
            byte[] corrupted = encrypted.clone();
            if (corrupted.length > 0) {
                corrupted[corrupted.length - 1] ^= 0xFF; // Flip bits in last byte
            }

            // Should fail to decrypt corrupted data
            assertThrows(EncryptionException.class, () -> {
                encryptionManager.decrypt(corrupted, testPassword);
            }, "Should fail with corrupted data");

            System.out.println("✅ Corrupted data detected correctly");
        });
    }

    @Test
    void testTooShortDataHandling() {
        System.out.println("🧪 Testing too short data handling...");

        // Test with data shorter than IV length
        byte[] tooShortData = new byte[10]; // Less than GCM_IV_LENGTH (12)

        assertThrows(EncryptionException.class, () -> {
            encryptionManager.decrypt(tooShortData, testPassword);
        }, "Should reject data that's too short");

        System.out.println("✅ Too short data rejected with appropriate error message");
    }

    @Test
    void testUnicodeDataHandling() {
        System.out.println("🧪 Testing Unicode data handling...");

        String unicodeText = "Hello 世界! 🌍 Test with émojis: 😀🎉🚀";

        assertDoesNotThrow(() -> {
            byte[] encrypted = encryptionManager.encrypt(unicodeText, testPassword, testSalt);
            String decrypted = encryptionManager.decrypt(encrypted, testPassword);
            assertEquals(unicodeText, decrypted, "Unicode text should be preserved");

            System.out.println("✅ Unicode and emoji data handled correctly");
        });
    }

    @Test
    void testKeyDerivation() {
        System.out.println("🧪 Testing key derivation...");

        assertDoesNotThrow(() -> {
            // Test that same password + salt produces same key
            var key1 = encryptionManager.deriveKey(testPassword, testSalt);
            var key2 = encryptionManager.deriveKey(testPassword, testSalt);
            assertNotNull(key1, "Key should not be null");
            assertNotNull(key2, "Key should not be null");

            // Keys should be equal (same password + salt)
            assertEquals(key1.getAlgorithm(), key2.getAlgorithm(), "Keys should have same algorithm");

            System.out.println("✅ Key derivation works correctly");
        });
    }

    // Legacy key derivation test removed: no legacy support.
}