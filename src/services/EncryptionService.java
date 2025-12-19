package services;

import encryption.Encryptor;

/**
 * Service interface for encryption operations
 */
public interface EncryptionService {
    void enableEncryption(char[] password) throws Exception;
    void disableEncryption() throws Exception;
    boolean isEncrypted();
    char[] getPassword();
    byte[] getSalt();
    Encryptor getEncryptor();
}