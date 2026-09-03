package filehandling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import encryption.Encryptor;
import encryption.FileEncryptionManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class EntryEditorLimitsTest {

    @TempDir
    Path tempDir;

    @Test
    void extremelyLongEntriesAreTruncatedWithMarker() throws Exception {
        Path tmp = tempDir.resolve("entry-editor-limits.txt");
        Encryptor stub = new Encryptor() {
            public byte[] generateSalt() { return new byte[0]; }
            public javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) { return null; }
            public byte[] encrypt(String data, char[] password, byte[] salt) { return new byte[0]; }
            public String decrypt(byte[] data, char... password) { return ""; }
            public String decryptStream(java.io.InputStream in, char... passwordAndSalt) { return ""; }
        };
        FileEncryptionManager fem = new FileEncryptionManager(tmp, stub);
        FileCache cache = new FileCache();
        EntryEditor editor = new EntryEditor(tmp, fem, cache);

        StringBuilder sb = new StringBuilder(70000);
        for (int i = 0; i < 70000; i++) {
            sb.append('A');
        }

        String ts = editor.createAndSaveEntry(sb.toString());
        assertNotNull(ts, "Entry should be created");

        List<String> lines = Files.readAllLines(tmp);
        assertTrue(lines.stream().anyMatch(line -> line.contains("[TRUNCATED]")),
            "Truncation marker should be written to disk");
    }
}
