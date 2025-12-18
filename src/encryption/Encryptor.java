package encryption;

/**
 * Interface for encryption operations to enable polymorphism and LSP compliance.
 */
public interface Encryptor {
    byte[] generateSalt() throws Exception;
    javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) throws Exception;
    byte[] encrypt(String data, javax.crypto.SecretKey key) throws Exception;
    String decryptWithFallback(byte[] data, char[] password, byte[] salt) throws Exception;
}