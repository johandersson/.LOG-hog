package gui;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Comprehensive tests for PasswordGenerator
 * Tests password and passphrase generation with various parameters and edge cases
 */
public class PasswordGeneratorTest {

    @Test
    void testGeneratePasswordBasic() {
        testsupport.TestLog.out("🧪 Testing basic password generation...");

        String password = PasswordGenerator.generatePassword(12);

        // Verify length
        assertEquals(12, password.length(), "Password should be correct length");

        // Verify contains valid characters
        String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
        for (char c : password.toCharArray()) {
            assertTrue(validChars.contains(String.valueOf(c)),
                      "Password should only contain valid characters: " + c);
        }

        testsupport.TestLog.out("✅ Basic password generation works correctly");
    }

    @Test
    void testGeneratePasswordDifferentLengths() {
        testsupport.TestLog.out("🧪 Testing password generation with different lengths...");

        // Test various lengths
        int[] lengths = {1, 8, 16, 32, 64, 128};

        for (int length : lengths) {
            String password = PasswordGenerator.generatePassword(length);
            assertEquals(length, password.length(),
                        "Password should be length " + length);
        }

        testsupport.TestLog.out("✅ Password generation with different lengths works correctly");
    }

    @Test
    void testGeneratePasswordEdgeCases() {
        testsupport.TestLog.out("🧪 Testing password generation edge cases...");

        // Test minimum length
        String minPassword = PasswordGenerator.generatePassword(1);
        assertEquals(1, minPassword.length(), "Should handle minimum length");

        // Test large length
        String largePassword = PasswordGenerator.generatePassword(1000);
        assertEquals(1000, largePassword.length(), "Should handle large lengths");

        testsupport.TestLog.out("✅ Password generation edge cases handled correctly");
    }

    @Test
    void testGeneratePasswordUniqueness() {
        testsupport.TestLog.out("🧪 Testing password uniqueness...");

        Set<String> passwords = new HashSet<>();

        // Generate multiple passwords and check they're mostly unique
        for (int i = 0; i < 100; i++) {
            String password = PasswordGenerator.generatePassword(16);
            passwords.add(password);
        }

        // Should have high uniqueness (allowing for some collisions)
        assertTrue(passwords.size() > 90,
                  "Should generate mostly unique passwords, got " + passwords.size() + " unique out of 100");

        testsupport.TestLog.out("✅ Password uniqueness verified");
    }

    @Test
    void testGeneratePassphraseBasic() {
        testsupport.TestLog.out("🧪 Testing basic passphrase generation...");

        String passphrase = PasswordGenerator.generatePassphrase(3);

        // Should contain words separated by spaces
        assertNotNull(passphrase, "Passphrase should not be null");
        assertTrue(passphrase.length() > 0, "Passphrase should not be empty");

        // Should have correct number of words
        String[] words = passphrase.split(" ");
        assertEquals(3, words.length, "Passphrase should have correct word count");

        // Each word should be non-empty
        for (String word : words) {
            assertTrue(word.length() > 0, "Each word should be non-empty");
        }

        testsupport.TestLog.out("✅ Basic passphrase generation works correctly");
    }

    @Test
    void testGeneratePassphraseDifferentWordCounts() {
        testsupport.TestLog.out("🧪 Testing passphrase generation with different word counts...");

        int[] wordCounts = {1, 2, 4, 6, 8, 12};

        for (int count : wordCounts) {
            String passphrase = PasswordGenerator.generatePassphrase(count);
            String[] words = passphrase.split(" ");
            assertEquals(count, words.length,
                        "Passphrase should have " + count + " words");
        }

        testsupport.TestLog.out("✅ Passphrase generation with different word counts works correctly");
    }

    @Test
    void testGeneratePassphraseWordContent() {
        testsupport.TestLog.out("🧪 Testing passphrase word content...");

        String passphrase = PasswordGenerator.generatePassphrase(5);
        String[] words = passphrase.split(" ");

        // Words should be lowercase (typical for dictionary words)
        for (String word : words) {
            assertTrue(word.equals(word.toLowerCase()),
                      "Words should be lowercase: " + word);
            // Should not contain special characters (basic check)
            assertTrue(word.matches("[a-z]+"),
                      "Words should only contain letters: " + word);
        }

        testsupport.TestLog.out("✅ Passphrase word content validated");
    }

    @Test
    void testGeneratePassphraseUniqueness() {
        testsupport.TestLog.out("🧪 Testing passphrase uniqueness...");

        Set<String> passphrases = new HashSet<>();

        // Generate multiple passphrases
        for (int i = 0; i < 50; i++) {
            String passphrase = PasswordGenerator.generatePassphrase(4);
            passphrases.add(passphrase);
        }

        // Should have reasonable uniqueness
        assertTrue(passphrases.size() > 40,
                  "Should generate mostly unique passphrases, got " + passphrases.size() + " unique out of 50");

        testsupport.TestLog.out("✅ Passphrase uniqueness verified");
    }

    @Test
    void testGeneratePassphraseEdgeCases() {
        testsupport.TestLog.out("🧪 Testing passphrase generation edge cases...");

        // Test single word
        String singleWord = PasswordGenerator.generatePassphrase(1);
        assertFalse(singleWord.contains(" "), "Single word should not contain spaces");

        // Test large word count
        String manyWords = PasswordGenerator.generatePassphrase(20);
        String[] words = manyWords.split(" ");
        assertEquals(20, words.length, "Should handle large word counts");

        testsupport.TestLog.out("✅ Passphrase generation edge cases handled correctly");
    }

    @Test
    void testPasswordVsPassphraseCharacteristics() {
        testsupport.TestLog.out("🧪 Testing password vs passphrase characteristics...");

        String password = PasswordGenerator.generatePassword(16);
        String passphrase = PasswordGenerator.generatePassphrase(4);

        // Password should contain special characters
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if ("!@#$%^&*()_+-=[]{}|;:,.<>?".contains(String.valueOf(c))) {
                hasSpecial = true;
                break;
            }
        }
        assertTrue(hasSpecial, "Password should contain special characters");

        // Passphrase should not contain special characters (basic check)
        assertFalse(passphrase.matches(".*[!@#$%^&*()_+-=\\[\\]{}|;:,.<>?].*"),
                   "Passphrase should not contain special characters");

        // Passphrase should be more readable (contain spaces)
        assertTrue(passphrase.contains(" "), "Passphrase should contain spaces between words");

        testsupport.TestLog.out("✅ Password vs passphrase characteristics validated");
    }

    @Test
    void testRepeatedGenerationConsistency() {
        testsupport.TestLog.out("🧪 Testing repeated generation consistency...");

        // Generate the same type multiple times - should work without issues
        for (int i = 0; i < 10; i++) {
            String password = PasswordGenerator.generatePassword(12);
            String passphrase = PasswordGenerator.generatePassphrase(3);

            assertNotNull(password, "Password should not be null");
            assertNotNull(passphrase, "Passphrase should not be null");
            assertEquals(12, password.length(), "Password should be correct length");
            assertEquals(2, passphrase.split(" ").length, "Passphrase should have correct word count");
        }

        testsupport.TestLog.out("✅ Repeated generation consistency verified");
    }

    @Test
    void testWordListLoading() {
        testsupport.TestLog.out("🧪 Testing word list loading...");

        // Generate a passphrase to trigger word list loading
        String passphrase = PasswordGenerator.generatePassphrase(2);

        // Should work without throwing exceptions
        assertNotNull(passphrase, "Passphrase generation should work after word list loading");

        // Generate another one to ensure word list is cached
        String passphrase2 = PasswordGenerator.generatePassphrase(2);
        assertNotNull(passphrase2, "Second passphrase should also work");

        testsupport.TestLog.out("✅ Word list loading works correctly");
    }
}