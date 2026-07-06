package security;

/**
 * Central password policy for encryption setup/update flows.
 */
public final class PasswordPolicy {
    private static final int MIN_LENGTH = 12;
    private static final int PASSPHRASE_LENGTH = 20;

    private PasswordPolicy() {}

    public static String validateForNewEncryption(char[] password) {
        if (password == null || password.length == 0) {
            return "Password is required.";
        }

        if (password.length < MIN_LENGTH) {
            return "Use at least " + MIN_LENGTH + " characters (or a longer passphrase).";
        }

        int classes = 0;
        if (containsLower(password)) classes++;
        if (containsUpper(password)) classes++;
        if (containsDigit(password)) classes++;
        if (containsSymbol(password)) classes++;

        // Keep usability: long passphrases are accepted even with fewer character classes.
        if (password.length >= PASSPHRASE_LENGTH) {
            return null;
        }

        if (classes < 3) {
            return "Use at least 3 of: lowercase, uppercase, numbers, symbols; or use a 20+ char passphrase.";
        }

        if (hasLongRun(password, 4)) {
            return "Avoid repeated characters (4+ in a row).";
        }

        return null;
    }

    private static boolean containsLower(char[] pwd) {
        for (char c : pwd) if (Character.isLowerCase(c)) return true;
        return false;
    }

    private static boolean containsUpper(char[] pwd) {
        for (char c : pwd) if (Character.isUpperCase(c)) return true;
        return false;
    }

    private static boolean containsDigit(char[] pwd) {
        for (char c : pwd) if (Character.isDigit(c)) return true;
        return false;
    }

    private static boolean containsSymbol(char[] pwd) {
        for (char c : pwd) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) return true;
        }
        return false;
    }

    private static boolean hasLongRun(char[] pwd, int threshold) {
        int run = 1;
        for (int i = 1; i < pwd.length; i++) {
            if (pwd[i] == pwd[i - 1]) {
                run++;
                if (run >= threshold) return true;
            } else {
                run = 1;
            }
        }
        return false;
    }
}