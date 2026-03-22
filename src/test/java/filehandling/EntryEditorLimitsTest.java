import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import filehandling.EntryEditor;
import filehandling.FileCache;
import encryption.FileEncryptionManager;
import encryption.Encryptor;

/**
 * Quick manual test to verify that extremely long entries are truncated
 * by EntryEditor according to InputLimits.
 */
public class EntryEditorLimitsTest {
    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempFile("loghog-test-", ".txt");
        try {
            // Create a no-op Encryptor stub (won't be used because we don't enable encryption)
            Encryptor stub = new Encryptor() {
                public byte[] generateSalt() { return new byte[0]; }
                public javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) { return null; }
                public byte[] encrypt(String data, char[] password, byte[] salt) { return new byte[0]; }
                public String decrypt(byte[] data, char[] password) { return ""; }
                public String decryptWithFallback(byte[] data, char[] password, byte[] salt) { return ""; }
                public String decryptStream(java.io.InputStream in, char[] password, byte[] salt) { return ""; }
            };
            FileEncryptionManager fem = new FileEncryptionManager(tmp, stub);
            FileCache cache = new FileCache();
            EntryEditor editor = new EntryEditor(tmp, fem, cache);

            // Create a very long input (double the allowed size)
            int largeSize = 70000;
            StringBuilder sb = new StringBuilder(largeSize);
            for (int i = 0; i < largeSize; i++) sb.append('A');
            String longText = sb.toString();

            String ts = editor.createAndSaveEntry(longText);
            if (ts == null) {
                System.err.println("Failed to create entry");
                System.exit(2);
            }

            List<String> lines = Files.readAllLines(tmp);
            boolean foundTruncated = false;
            for (String l : lines) {
                if (l.contains("[TRUNCATED]")) foundTruncated = true;
            }

            if (foundTruncated) {
                System.out.println("PASS: Truncation marker found in file");
                System.exit(0);
            } else {
                System.err.println("FAIL: Truncation marker NOT found in file");
                System.exit(3);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
