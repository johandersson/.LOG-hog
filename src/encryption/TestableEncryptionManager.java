package encryption;

/**
 * Testable implementation of Encryptor that is not a singleton
 */
public class TestableEncryptionManager implements Encryptor {
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
    public String decrypt(byte[] data, char[] password) throws EncryptionException {
        return delegate.decrypt(data, password);
    }

    @Override
    public String decryptWithFallback(byte[] data, char[] password, byte[] salt) throws EncryptionException {
        return delegate.decryptWithFallback(data, password, salt);
    }

    public javax.crypto.SecretKey deriveKeyLegacy(char[] password, byte[] salt) throws EncryptionException {
        return delegate.deriveKeyLegacy(password, salt);
    }
}