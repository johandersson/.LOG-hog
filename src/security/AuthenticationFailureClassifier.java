package security;

import java.util.Locale;

/**
 * Classifies whether a thrown error likely represents authentication/decryption failure.
 */
public final class AuthenticationFailureClassifier {
    private AuthenticationFailureClassifier() {}

    public static boolean isLikelyAuthenticationFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (isCryptoAuthException(current)) {
                return true;
            }

            String msg = current.getMessage();
            if (msg != null && containsAuthMarker(msg.toLowerCase(Locale.ROOT))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isCryptoAuthException(Throwable t) {
        return t instanceof javax.crypto.AEADBadTagException
            || t instanceof javax.crypto.BadPaddingException
            || t instanceof javax.crypto.IllegalBlockSizeException
            || t instanceof java.nio.charset.MalformedInputException
            || t instanceof encryption.EncryptionException;
    }

    private static boolean containsAuthMarker(String lowerMessage) {
        return lowerMessage.contains("tag mismatch")
            || lowerMessage.contains("bad tag")
            || lowerMessage.contains("integrity check failed")
            || lowerMessage.contains("mac check failed")
            || lowerMessage.contains("decryption failed")
            || lowerMessage.contains("unable to open your file")
            || lowerMessage.contains("your password might be incorrect")
            || lowerMessage.contains("malformedinput")
            || lowerMessage.contains("input length");
    }
}