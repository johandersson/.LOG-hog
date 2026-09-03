package encryption;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

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
        for (int length = 1; length < 28; length++) {
            byte[] shortData = new byte[length];
            EncryptionException exception = assertThrows(EncryptionException.class, () ->
                encryptionManager.decrypt(shortData, "password".toCharArray()));
            assertNotNull(exception.getMessage(), "Length " + length + " should report an error message");
            assertFalse(exception.getMessage().isBlank(), "Length " + length + " should report an error message");
        }
    }

    @Test
    void testDecryptWithCorruptedData() throws Exception {
        String testData = "Test data for corruption";
        byte[] salt = encryptionManager.generateSalt();
        char[] password = "password".toCharArray();
        byte[] encrypted = encryptionManager.encrypt(testData, password, salt);

        for (int i = 0; i < encrypted.length; i++) {
            byte[] corrupted = encrypted.clone();
            corrupted[i] = (byte) ~corrupted[i];
            assertThrows(EncryptionException.class, () ->
                encryptionManager.decrypt(corrupted, "wrongpassword".toCharArray()));
        }
    }

    @Test
    void testDecryptWithInvalidUTF8() throws Exception {
        String testData = "Valid UTF-8 data";
        byte[] salt = encryptionManager.generateSalt();
        char[] password = "password".toCharArray();
        byte[] encrypted = encryptionManager.encrypt(testData, password, salt);
        String decrypted = encryptionManager.decrypt(encrypted, password);
        assertEquals(testData, decrypted);
    }

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
    void testEncryptWithNullPasswordPreservesOriginalIntent() throws Exception {
        byte[] salt = encryptionManager.generateSalt();
        assertThrows(EncryptionException.class, () ->
            encryptionManager.encrypt("data", (char[]) null, salt));
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

        EncryptionException exception = assertThrows(EncryptionException.class, () ->
            encryptionManager.decrypt(encrypted, "wrongpassword".toCharArray()));
        assertTrue(exception.getMessage().contains("password") || exception.getMessage().contains("incorrect"));
    }

    @Test
    void testDataIntegrity() throws Exception {
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
