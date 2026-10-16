package security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
    @Test
    void rejectsPasswordsShorterThanTwentyCharacters() {
        assertNotNull(PasswordPolicy.validateForNewEncryption("Abcd1234!xyz".toCharArray()));
        assertNotNull(PasswordPolicy.validateForNewEncryption("Abcdefghij1!xyz1234".toCharArray()));
    }

    @Test
    void acceptsTwentyCharacterPassphrases() {
        assertNull(PasswordPolicy.validateForNewEncryption("correct horse battery".toCharArray()));
    }
}
