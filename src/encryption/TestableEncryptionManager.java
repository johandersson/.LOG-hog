package encryption;

/**
 * Testable implementation of Encryptor that is not a singleton
 */
public class TestableEncryptionManager implements SessionKeyEncryptor {
    private final EncryptionManager delegate = new EncryptionManager();

    @Override
    public byte[] generateSalt() throws EncryptionException {
        return delegate.generateSalt();
    }

    @Override
    public javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) throws EncryptionException {
        return delegate.deriveKey(password, salt);
    }

    @Override
    public byte[] encrypt(String data, char[] password, byte[] salt) throws EncryptionException {
        return delegate.encrypt(data, password, salt);
    }

    @Override
    public void encryptStream(java.io.InputStream in, java.io.OutputStream out, char[] password, byte[] salt, utils.ProgressCallback progress) throws EncryptionException {
        delegate.encryptStream(in, out, password, salt, progress);
    }

    @Override
    public String decrypt(byte[] data, char... password) throws EncryptionException {
        return delegate.decrypt(data, password);
    }

    @Override
    public byte[] encrypt(String data, javax.crypto.SecretKey sessionKey, byte[] salt) throws EncryptionException {
        return delegate.encrypt(data, sessionKey, salt);
    }

    @Override
    public String decrypt(byte[] data, javax.crypto.SecretKey sessionKey) throws EncryptionException {
        return delegate.decrypt(data, sessionKey);
    }

    @Override
    public void encryptStream(java.io.InputStream in, java.io.OutputStream out, javax.crypto.SecretKey sessionKey, byte[] salt, utils.ProgressCallback progress) throws EncryptionException {
        delegate.encryptStream(in, out, sessionKey, salt, progress);
    }

    @Override
    public java.io.InputStream openDecryptedStream(java.io.InputStream encryptedIn, javax.crypto.SecretKey sessionKey, utils.ProgressCallback progress) throws EncryptionException {
        return delegate.openDecryptedStream(encryptedIn, sessionKey, progress);
    }

    @Override
    public java.io.InputStream openDecryptedStream(java.io.InputStream encryptedIn, char[] password, byte[] salt, utils.ProgressCallback progress) throws EncryptionException {
        return delegate.openDecryptedStream(encryptedIn, password, salt, progress);
    }
}