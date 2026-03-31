package encryption;

public interface Encryptor {
    byte[] generateSalt() throws EncryptionException;
    javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) throws EncryptionException;
    byte[] encrypt(String data, char[] password, byte[] salt) throws EncryptionException;
    String decrypt(byte[] data, char... password) throws EncryptionException;
    String decryptWithFallback(byte[] data, char... passwordAndSalt) throws EncryptionException;
    /**
     * Stream-based decryption: reads encrypted data from the provided InputStream and returns
     * the decrypted contents as a String. Implementations should avoid reading the entire
     * encrypted file into memory before decrypting.
     */
    String decryptStream(java.io.InputStream in, char... passwordAndSalt) throws EncryptionException;
}