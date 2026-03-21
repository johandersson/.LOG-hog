package encryption;

import java.io.InputStream;
import java.io.OutputStream;
import utils.ProgressCallback;

/**
 * Optional interface for encryptors that support streaming encryption with progress reporting.
 */
public interface StreamEncryptor extends Encryptor {
    void encryptStream(InputStream in, OutputStream out, char[] password, byte[] salt, ProgressCallback progress) throws EncryptionException;

    /**
     * Open a stream that yields decrypted plaintext from the given encrypted input stream.
     * Implementations should NOT read the entire stream into memory; callers will read
     * from the returned InputStream and must close it when finished.
     */
    java.io.InputStream openDecryptedStream(InputStream encryptedIn, char[] password, byte[] salt, ProgressCallback progress) throws EncryptionException;
}
