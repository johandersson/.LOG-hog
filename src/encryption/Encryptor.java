package encryption;

/**
 * Interface for encryption operations to enable polymorphism and LSP compliance.
 */
public interface Encryptor {
    byte[] generateSalt() throws EncryptionException;
    javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) throws EncryptionException;
    byte[] encrypt(String data, javax.crypto.SecretKey key) throws EncryptionException;
    String decryptWithFallback(byte[] data, char[] password, byte[] salt) throws EncryptionException;
}