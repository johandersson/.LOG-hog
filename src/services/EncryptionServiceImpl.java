package services;

import encryption.Encryptor;
import filehandling.LogFileOperations;

/**
 * Implementation of EncryptionService using LogFileOperations
 */
public class EncryptionServiceImpl implements EncryptionService {

    private final LogFileOperations logFileOperations;

    public EncryptionServiceImpl(LogFileOperations logFileOperations) {
        this.logFileOperations = logFileOperations;
    }

    @Override
    public void enableEncryption(char[] password) throws Exception {
        logFileOperations.enableEncryption(password);
    }

    @Override
    public void disableEncryption() throws Exception {
        logFileOperations.disableEncryption();
    }

    @Override
    public boolean isEncrypted() {
        return logFileOperations.isEncrypted();
    }

    @Override
    public char[] getPassword() {
        return logFileOperations.getPassword();
    }

    @Override
    public byte[] getSalt() {
        return logFileOperations.getSalt();
    }

    @Override
    public Encryptor getEncryptor() {
        return logFileOperations.getEncryptor();
    }
}