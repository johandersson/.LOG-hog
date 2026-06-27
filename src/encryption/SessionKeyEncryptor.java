package encryption;

import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.SecretKey;

import utils.ProgressCallback;

/**
 * Encryptor operations that work with a session-scoped derived key.
 */
public interface SessionKeyEncryptor extends Encryptor, StreamEncryptor {
    byte[] encrypt(String data, SecretKey sessionKey, byte[] salt) throws EncryptionException;
    String decrypt(byte[] data, SecretKey sessionKey) throws EncryptionException;
    void encryptStream(InputStream in, OutputStream out, SecretKey sessionKey, byte[] salt, ProgressCallback progress) throws EncryptionException;
    InputStream openDecryptedStream(InputStream encryptedIn, SecretKey sessionKey, ProgressCallback progress) throws EncryptionException;
}