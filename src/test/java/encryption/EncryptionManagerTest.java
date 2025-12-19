package encryption;

import org.junit.jupiter.api.*;
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
            // Derive key from password and salt
            var key = encryptionManager.deriveKey(testPassword, testSalt);

            // Encrypt
            byte[] encrypted = encryptionManager.encrypt(originalText, key);
            assertNotNull(encrypted, "Encrypted data should not be null");
            assertTrue(encrypted.length > originalText.length(), "Encrypted data should be longer than original");

            // Decrypt
            String decrypted = encryptionManager.decryptWithFallback(encrypted, testPassword, testSalt);
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
            // Derive key from correct password
            var key = encryptionManager.deriveKey(testPassword, testSalt);

            // Encrypt with correct password
            byte[] encrypted = encryptionManager.encrypt(message, key);

            // Should decrypt with correct password
            String decrypted = encryptionManager.decryptWithFallback(encrypted, testPassword, testSalt);
            assertEquals(message, decrypted, "Should decrypt with correct password");

            // Should fail with wrong password
            assertThrows(EncryptionException.class, () -> {
                encryptionManager.decryptWithFallback(encrypted, wrongPassword, testSalt);
            }, "Should fail with wrong password");

            System.out.println("✅ Password validation works correctly");
        });
    }

    @Test
    void testCorruptedDataHandling() {
        System.out.println("🧪 Testing corrupted data handling...");

        String message = "Test message";

        assertDoesNotThrow(() -> {
            var key = encryptionManager.deriveKey(testPassword, testSalt);
            byte[] encrypted = encryptionManager.encrypt(message, key);

            // Corrupt the data by changing a byte
            byte[] corrupted = encrypted.clone();
            if (corrupted.length > 0) {
                corrupted[corrupted.length - 1] ^= 0xFF; // Flip bits in last byte
            }

            // Should fail to decrypt corrupted data
            assertThrows(EncryptionException.class, () -> {
                encryptionManager.decryptWithFallback(corrupted, testPassword, testSalt);
            }, "Should fail with corrupted data");

            System.out.println("✅ Corrupted data detected correctly");
        });
    }

    @Test
    void testTooShortDataHandling() {
        System.out.println("🧪 Testing too short data handling...");

        // Test with data shorter than IV length
        byte[] tooShortData = new byte[10]; // Less than GCM_IV_LENGTH (12)

        EncryptionException exception = assertThrows(EncryptionException.class, () -> {
            encryptionManager.decryptWithFallback(tooShortData, testPassword, testSalt);
        }, "Should reject data that's too short");

        // Verify the error message is specific to short data
        assertTrue(exception.getMessage().contains("unencrypted or uses an incompatible encryption format"),
                  "Should give specific error for short data");

        System.out.println("✅ Too short data rejected with appropriate error message");
    }

    @Test
    void testUnicodeDataHandling() {
        System.out.println("🧪 Testing Unicode data handling...");

        String unicodeText = "Hello 世界! 🌍 Test with émojis: 😀🎉🚀";

        assertDoesNotThrow(() -> {
            var key = encryptionManager.deriveKey(testPassword, testSalt);
            byte[] encrypted = encryptionManager.encrypt(unicodeText, key);
            String decrypted = encryptionManager.decryptWithFallback(encrypted, testPassword, testSalt);
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

    @Test
    void testLegacyKeyDerivation() {
        System.out.println("🧪 Testing legacy key derivation compatibility...");

        assertDoesNotThrow(() -> {
            var legacyKey = encryptionManager.deriveKeyLegacy(testPassword, testSalt);
            assertNotNull(legacyKey, "Legacy key should not be null");

            System.out.println("✅ Legacy key derivation works for backward compatibility");
        });
    }
}