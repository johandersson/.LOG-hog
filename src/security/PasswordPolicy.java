package security;

/**
 * Central password policy for encryption setup/update flows.
 */
public final class PasswordPolicy {
    private static final int MIN_LENGTH = 20;

    private PasswordPolicy() {}

    public static String validateForNewEncryption(char[] password) {
        if (password == null || password.length == 0) {
            return "Password is required.";
        }

        return password.length < MIN_LENGTH
            ? "Use at least " + MIN_LENGTH + " characters."
            : null;
    }
}