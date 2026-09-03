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
        testsupport.TestLog.out("🧪 Testing salt generation...");

        assertDoesNotThrow(() -> {
            byte[] salt = encryptionManager.generateSalt();
            assertNotNull(salt, "Salt should not be null");
            assertEquals(16, salt.length, "Salt should be 16 bytes");
            testsupport.TestLog.out("✅ Salt generation works correctly");
        });
    }

    @Test
    void testEncryptDecryptCycle() {
        testsupport.TestLog.out("🧪 Testing basic encrypt/decrypt cycle...");

        String originalText = "Hello, this is a test message for encryption!";

        assertDoesNotThrow(() -> {
            byte[] encrypted = encryptionManager.encrypt(originalText, testPassword, testSalt);
            assertNotNull(encrypted, "Encrypted data should not be null");
            assertTrue(encrypted.length > originalText.length(), "Encrypted data should be longer than original");

            String decrypted = encryptionManager.decrypt(encrypted, testPassword);
            assertEquals(originalText, decrypted, "Decrypted text should match original");

            testsupport.TestLog.out("✅ Basic encrypt/decrypt cycle works perfectly");
        });
    }

    @Test
    void testEncryptDecryptWithDifferentPasswords() {
        testsupport.TestLog.out("🧪 Testing encryption with different passwords...");

        String message = "Secret message";
        char[] wrongPassword = "wrongPassword".toCharArray();

        assertDoesNotThrow(() -> {
            byte[] encrypted = encryptionManager.encrypt(message, testPassword, testSalt);
            String decrypted = encryptionManager.decrypt(encrypted, testPassword);
            assertEquals(message, decrypted, "Should decrypt with correct password");

            assertThrows(EncryptionException.class, () -> encryptionManager.decrypt(encrypted, wrongPassword),
                "Should fail with wrong password");

            testsupport.TestLog.out("✅ Password validation works correctly");
        });
    }

    @Test
    void testCorruptedDataHandling() {
        testsupport.TestLog.out("🧪 Testing corrupted data handling...");

        String message = "Test message";

        assertDoesNotThrow(() -> {
            byte[] encrypted = encryptionManager.encrypt(message, testPassword, testSalt);
            byte[] corrupted = encrypted.clone();
            if (corrupted.length > 0) {
                corrupted[corrupted.length - 1] ^= 0xFF;
            }

            assertThrows(EncryptionException.class, () -> encryptionManager.decrypt(corrupted, testPassword),
                "Should fail with corrupted data");

            testsupport.TestLog.out("✅ Corrupted data detected correctly");
        });
    }

    @Test
    void testTooShortDataHandling() {
        testsupport.TestLog.out("🧪 Testing too short data handling...");

        byte[] tooShortData = new byte[10];

        assertThrows(EncryptionException.class, () -> encryptionManager.decrypt(tooShortData, testPassword),
            "Should reject data that's too short");

        testsupport.TestLog.out("✅ Too short data rejected with appropriate error message");
    }

    @Test
    void testUnicodeDataHandling() {
        testsupport.TestLog.out("🧪 Testing Unicode data handling...");

        String originalText = "Hello 世界 🌍 Здравствуй мир! مرحبا بالعالم";

        assertDoesNotThrow(() -> {
            byte[] encrypted = encryptionManager.encrypt(originalText, testPassword, testSalt);
            String decrypted = encryptionManager.decrypt(encrypted, testPassword);
            assertEquals(originalText, decrypted, "Unicode text should be preserved exactly");
            testsupport.TestLog.out("✅ Unicode data handled correctly");
        });
    }

    @Test
    void testEmptyStringEncryption() {
        testsupport.TestLog.out("🧪 Testing empty string encryption...");

        assertDoesNotThrow(() -> {
            byte[] encrypted = encryptionManager.encrypt("", testPassword, testSalt);
            assertNotNull(encrypted, "Empty string should still produce encrypted data");
            String decrypted = encryptionManager.decrypt(encrypted, testPassword);
            assertEquals("", decrypted, "Empty string should decrypt correctly");
            testsupport.TestLog.out("✅ Empty string encryption works correctly");
        });
    }
}
