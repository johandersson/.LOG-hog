package gui;

import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordGeneratorTest {

    @Test
    void testGeneratePasswordBasic() {
        char[] password = PasswordGenerator.generatePasswordChars(12);
        String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
        assertEquals(12, password.length);
        for (char c : password) {
            assertTrue(validChars.contains(String.valueOf(c)));
        }
        encryption.CryptoUtils.zeroize(password);
    }

    @Test
    void testGeneratePasswordDifferentLengths() {
        int[] lengths = {1, 8, 16, 32, 64, 128};
        for (int length : lengths) {
            char[] password = PasswordGenerator.generatePasswordChars(length);
            assertEquals(length, password.length);
            encryption.CryptoUtils.zeroize(password);
        }
    }

    @Test
    void testGeneratePasswordBounds() {
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generatePasswordChars(0));
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generatePasswordChars(1001));
    }

    @Test
    void testGeneratePasswordUniqueness() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            char[] chars = PasswordGenerator.generatePasswordChars(16);
            passwords.add(new String(chars));
            encryption.CryptoUtils.zeroize(chars);
        }
        assertTrue(passwords.size() > 90);
    }

    @Test
    void testGeneratePassphraseBasic() {
        char[] passphraseChars = PasswordGenerator.generatePassphraseChars(3);
        String passphrase = new String(passphraseChars);
        String[] words = passphrase.split(" ");
        assertEquals(3, words.length);
        for (String word : words) {
            assertFalse(word.isBlank());
        }
        encryption.CryptoUtils.zeroize(passphraseChars);
    }

    @Test
    void testGeneratePassphraseDifferentWordCounts() {
        int[] wordCounts = {1, 2, 4, 6, 8, 12};
        for (int count : wordCounts) {
            char[] chars = PasswordGenerator.generatePassphraseChars(count);
            String passphrase = new String(chars);
            assertEquals(count, passphrase.split(" ").length);
            encryption.CryptoUtils.zeroize(chars);
        }
    }

    @Test
    void testGeneratePassphraseBounds() {
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generatePassphraseChars(0));
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generatePassphraseChars(21));
    }

    @Test
    void testGeneratePassphraseUniqueness() {
        Set<String> passphrases = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            char[] chars = PasswordGenerator.generatePassphraseChars(4);
            passphrases.add(new String(chars));
            encryption.CryptoUtils.zeroize(chars);
        }
        assertTrue(passphrases.size() > 20);
    }

    @Test
    void testPasswordVsPassphraseCharacteristics() {
        char[] passwordChars = PasswordGenerator.generatePasswordChars(16);
        char[] passphraseChars = PasswordGenerator.generatePassphraseChars(4);
        
        String password = new String(passwordChars);
        String passphrase = new String(passphraseChars);
        
        assertFalse(password.contains(" "));
        assertTrue(passphrase.contains(" "));
        
        encryption.CryptoUtils.zeroize(passwordChars);
        encryption.CryptoUtils.zeroize(passphraseChars);
    }

    @Test
    void testRepeatedGenerationConsistency() {
        for (int i = 0; i < 10; i++) {
            char[] passwordChars = PasswordGenerator.generatePasswordChars(12);
            char[] passphraseChars = PasswordGenerator.generatePassphraseChars(3);
            
            String password = new String(passwordChars);
            String passphrase = new String(passphraseChars);
            
            assertNotNull(password);
            assertNotNull(passphrase);
            assertEquals(12, password.length());
            assertEquals(3, passphrase.split(" ").length);
            
            encryption.CryptoUtils.zeroize(passwordChars);
            encryption.CryptoUtils.zeroize(passphraseChars);
        }
    }

    @Test
    void testWordListLoading() {
        char[] passphrase1Chars = PasswordGenerator.generatePassphraseChars(2);
        char[] passphrase2Chars = PasswordGenerator.generatePassphraseChars(2);
        
        String passphrase = new String(passphrase1Chars);
        String passphrase2 = new String(passphrase2Chars);
        
        assertNotNull(passphrase);
        assertNotNull(passphrase2);
        assertEquals(2, passphrase.split(" ").length);
        assertEquals(2, passphrase2.split(" ").length);
        
        encryption.CryptoUtils.zeroize(passphrase1Chars);
        encryption.CryptoUtils.zeroize(passphrase2Chars);
    }

    @Test
    void testGeneratePasswordCharsReturnsCharArray() {
        char[] passwordChars = PasswordGenerator.generatePasswordChars(12);
        assertNotNull(passwordChars);
        assertEquals(12, passwordChars.length);
        // Verify we can zeroize it
        encryption.CryptoUtils.zeroize(passwordChars);
        for (char c : passwordChars) {
            assertEquals('\0', c);
        }
    }

    @Test
    void testGeneratePasswordCharsRandomness() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            char[] chars = PasswordGenerator.generatePasswordChars(16);
            passwords.add(new String(chars));
            encryption.CryptoUtils.zeroize(chars);
        }
        assertTrue(passwords.size() > 90);
    }

    @Test
    void testGeneratePassphraseCharsReturnsCharArray() {
        char[] passphraseChars = PasswordGenerator.generatePassphraseChars(3);
        assertNotNull(passphraseChars);
        assertTrue(passphraseChars.length > 0);
        // Verify we can zeroize it
        encryption.CryptoUtils.zeroize(passphraseChars);
        for (char c : passphraseChars) {
            assertEquals('\0', c);
        }
    }

    @Test
    void testGeneratePassphraseCharsRandomness() {
        Set<String> passphrases = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            char[] chars = PasswordGenerator.generatePassphraseChars(4);
            passphrases.add(new String(chars));
            encryption.CryptoUtils.zeroize(chars);
        }
        assertTrue(passphrases.size() > 20);
    }
}
