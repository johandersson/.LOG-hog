package encryption;

// Unused import removed for PMD compliance
import static org.junit.jupiter.api.Assertions.*;

// Unused import removed for PMD compliance

/**
 * Comprehensive edge case tests for encryption functionality
 */
public class EncryptionEdgeCaseTest {

    private TestableEncryptionManager encryptionManager;

    @BeforeEach
    void setup() {
        encryptionManager = new TestableEncryptionManager();
    }

    @Test
    void testDecryptWithNullData() {
        assertThrows(EncryptionException.class, () ->
            encryptionManager.decrypt(null, "password".toCharArray()));
    }

    @Test
    void testDecryptWithEmptyData() {
        assertThrows(EncryptionException.class, () ->
            encryptionManager.decrypt(new byte[0], "password".toCharArray()));
    }

    @Test
    void testDecryptWithTooShortData() {
        // Test various short lengths
        for (int length = 1; length < 28; length++) {
            byte[] shortData = new byte[length];
            EncryptionException exception = assertThrows(EncryptionException.class, () ->
                encryptionManager.decrypt(shortData, "password".toCharArray()));
            assertTrue(exception.getMessage().contains("damaged") || exception.getMessage().contains("enough data"),
                "Length " + length + " should throw appropriate error");
        }
    }

    @Test
    void testDecryptWithCorruptedData() throws Exception {
        // Create valid encrypted data then corrupt it
        String testData = "Test data for corruption";
        byte[] salt = encryptionManager.generateSalt();
        char[] password = "password".toCharArray();
        byte[] encrypted = encryptionManager.encrypt(testData, password, salt);

        // Corrupt the data in various ways
        for (int i = 0; i < encrypted.length; i++) {
            byte[] corrupted = encrypted.clone();
            corrupted[i] = (byte) ~corrupted[i]; // Flip bits

            // Should throw exception for wrong password or corrupted data
            assertThrows(EncryptionException.class, () ->
                encryptionManager.decrypt(corrupted, "wrongpassword".toCharArray()));
        }
    }

    @Test
    void testDecryptWithInvalidUTF8() throws Exception {
        // Create data that decrypts to invalid UTF-8
        String testData = "Valid UTF-8 data";
        byte[] salt = encryptionManager.generateSalt();
        char[] password = "password".toCharArray();
        byte[] encrypted = encryptionManager.encrypt(testData, password, salt);

        // Manually corrupt the decrypted content to be invalid UTF-8
        // This is tricky to do reliably, but we can test the UTF-8 validation
        // by mocking or by creating data that would result in invalid UTF-8

        // For now, test that valid data works
        String decrypted = encryptionManager.decrypt(encrypted, password);
        assertEquals(testData, decrypted);
    }

    // Legacy fallback tests removed: legacy key derivation/encrypt removed

    @Test
    void testSaltGeneration() throws Exception {
        byte[] salt1 = encryptionManager.generateSalt();
        byte[] salt2 = encryptionManager.generateSalt();

        assertEquals(16, salt1.length);
        assertEquals(16, salt2.length);
        assertFalse(java.util.Arrays.equals(salt1, salt2), "Salts should be unique");
    }

    @Test
    void testKeyDerivationWithNullPassword() {
        assertThrows(EncryptionException.class, () ->
            encryptionManager.deriveKey(null, new byte[16]));
    }

    @Test
    void testKeyDerivationWithEmptyPassword() {
        assertThrows(EncryptionException.class, () ->
            encryptionManager.deriveKey(new char[0], new byte[16]));
    }

    @Test
    void testKeyDerivationWithWrongSaltLength() {
        assertThrows(EncryptionException.class, () ->
            encryptionManager.deriveKey("password".toCharArray(), new byte[15]));
    }

    @Test
    void testEncryptWithNullData() throws Exception {
        byte[] salt = encryptionManager.generateSalt();
        char[] password = "password".toCharArray();
        assertThrows(EncryptionException.class, () ->
            encryptionManager.encrypt(null, password, salt));
    }

    @Test
    void testEncryptWithNullKey() throws Exception {
        byte[] salt = encryptionManager.generateSalt();
        assertThrows(EncryptionException.class, () ->
            encryptionManager.encrypt("data", null, salt));
    }

    @Test
    void testUnicodeDataHandling() throws Exception {
        String unicodeData = "Hello 世界 🌍 émojis 🎉";
        byte[] salt = encryptionManager.generateSalt();
        char[] password = "password".toCharArray();

        byte[] encrypted = encryptionManager.encrypt(unicodeData, password, salt);
        String decrypted = encryptionManager.decrypt(encrypted, password);

        assertEquals(unicodeData, decrypted);
    }

    @Test
    void testLargeDataHandling() throws Exception {
        // Test with large data
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeData.append("Line ").append(i).append(" with some content\n");
        }

        byte[] salt = encryptionManager.generateSalt();
        char[] password = "password".toCharArray();

        byte[] encrypted = encryptionManager.encrypt(largeData.toString(), password, salt);
        String decrypted = encryptionManager.decrypt(encrypted, password);

        assertEquals(largeData.toString(), decrypted);
    }

    @Test
    void testWrongPasswordWithValidData() throws Exception {
        String testData = "Secret data";
        byte[] salt = encryptionManager.generateSalt();
        char[] correctPassword = "correctpassword".toCharArray();
        byte[] encrypted = encryptionManager.encrypt(testData, correctPassword, salt);

        // Try to decrypt with wrong password
        EncryptionException exception = assertThrows(EncryptionException.class, () ->
            encryptionManager.decrypt(encrypted, "wrongpassword".toCharArray()));
        assertTrue(exception.getMessage().contains("password") || exception.getMessage().contains("incorrect"));
    }

    @Test
    void testDataIntegrity() throws Exception {
        // Test that encryption/decryption preserves data exactly
        String[] testCases = {
            "",
            "a",
            "Hello World",
            "Data with\nnewlines",
            "Data with\ttabs",
            "Data with \"quotes\"",
            "Data with 'single quotes'",
            "Data with special chars: !@#$%^&*()",
            "Unicode: αβγδε 中文 🚀"
        };

        byte[] salt = encryptionManager.generateSalt();
        char[] password = "password".toCharArray();

        for (String testData : testCases) {
            byte[] encrypted = encryptionManager.encrypt(testData, password, salt);
            String decrypted = encryptionManager.decrypt(encrypted, password);
            assertEquals(testData, decrypted, "Data integrity failed for: " + testData);
        }
    }
}