package encryption;

public interface Encryptor {
    byte[] generateSalt() throws EncryptionException;
    javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) throws EncryptionException;
    byte[] encrypt(String data, char[] password, byte[] salt) throws EncryptionException;
    String decrypt(byte[] data, char[] password) throws EncryptionException;
    String decryptWithFallback(byte[] data, char[] password, byte[] salt) throws EncryptionException;
}