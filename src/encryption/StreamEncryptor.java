package encryption;

import java.io.InputStream;
import java.io.OutputStream;
import utils.ProgressCallback;

/**
 * Optional interface for encryptors that support streaming encryption with progress reporting.
 */
public interface StreamEncryptor extends Encryptor {
    void encryptStream(InputStream in, OutputStream out, char[] password, byte[] salt, ProgressCallback progress) throws EncryptionException;
}
