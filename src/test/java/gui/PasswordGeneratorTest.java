package gui;

import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordGeneratorTest {

    @Test
    void testGeneratePasswordBasic() {
        String password = PasswordGenerator.generatePassword(12);
        String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
        assertEquals(12, password.length());
        for (char c : password.toCharArray()) {
            assertTrue(validChars.contains(String.valueOf(c)));
        }
    }

    @Test
    void testGeneratePasswordDifferentLengths() {
        int[] lengths = {1, 8, 16, 32, 64, 128};
        for (int length : lengths) {
            assertEquals(length, PasswordGenerator.generatePassword(length).length());
        }
    }

    @Test
    void testGeneratePasswordBounds() {
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generatePassword(0));
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generatePassword(1001));
    }

    @Test
    void testGeneratePasswordUniqueness() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            passwords.add(PasswordGenerator.generatePassword(16));
        }
        assertTrue(passwords.size() > 90);
    }

    @Test
    void testGeneratePassphraseBasic() {
        String passphrase = PasswordGenerator.generatePassphrase(3);
        String[] words = passphrase.split(" ");
        assertEquals(3, words.length);
        for (String word : words) {
            assertFalse(word.isBlank());
        }
    }

    @Test
    void testGeneratePassphraseDifferentWordCounts() {
        int[] wordCounts = {1, 2, 4, 6, 8, 12};
        for (int count : wordCounts) {
            assertEquals(count, PasswordGenerator.generatePassphrase(count).split(" ").length);
        }
    }

    @Test
    void testGeneratePassphraseBounds() {
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generatePassphrase(0));
        assertThrows(IllegalArgumentException.class, () -> PasswordGenerator.generatePassphrase(21));
    }

    @Test
    void testGeneratePassphraseUniqueness() {
        Set<String> passphrases = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            passphrases.add(PasswordGenerator.generatePassphrase(4));
        }
        assertTrue(passphrases.size() > 20);
    }

    @Test
    void testPasswordVsPassphraseCharacteristics() {
        String password = PasswordGenerator.generatePassword(16);
        String passphrase = PasswordGenerator.generatePassphrase(4);
        assertFalse(password.contains(" "));
        assertTrue(passphrase.contains(" "));
    }

    @Test
    void testRepeatedGenerationConsistency() {
        for (int i = 0; i < 10; i++) {
            String password = PasswordGenerator.generatePassword(12);
            String passphrase = PasswordGenerator.generatePassphrase(3);
            assertNotNull(password);
            assertNotNull(passphrase);
            assertEquals(12, password.length());
            assertEquals(3, passphrase.split(" ").length);
        }
    }

    @Test
    void testWordListLoading() {
        String passphrase = PasswordGenerator.generatePassphrase(2);
        String passphrase2 = PasswordGenerator.generatePassphrase(2);
        assertNotNull(passphrase);
        assertNotNull(passphrase2);
        assertEquals(2, passphrase.split(" ").length);
        assertEquals(2, passphrase2.split(" ").length);
    }
}
