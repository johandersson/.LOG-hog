package filehandling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.DefaultListModel;

class DeleteEntriesSuffixRegressionTest {

    @TempDir
    Path tempDir;

    @Test
    void deletingMiddleDuplicateRenumbersRemainingSuffixes() throws Exception {
        Path tempFile = tempDir.resolve("dup-suffix-regression.txt");
        Files.deleteIfExists(tempFile);
        LogFileHandler handler = new LogFileHandler(tempFile, new NoOpEncryptor());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("10:00 2025-01-01");
        listModel.addElement("10:00 2025-01-01 (1)");
        listModel.addElement("10:00 2025-01-01 (2)");
        listModel.removeElement("10:00 2025-01-01 (1)");

        Method renumber = LogFileHandler.class.getDeclaredMethod(
            "renumberDuplicateTimestampSuffixes", javax.swing.DefaultListModel.class);
        renumber.setAccessible(true);
        renumber.invoke(handler, listModel);

        assertEquals(2, listModel.getSize());
        assertEquals(List.of("10:00 2025-01-01", "10:00 2025-01-01 (1)"),
            List.of(listModel.getElementAt(0), listModel.getElementAt(1)));
    }

    private static class NoOpEncryptor implements encryption.Encryptor {
        @Override
        public byte[] generateSalt() {
            return new byte[16];
        }

        @Override
        public javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) {
            return new javax.crypto.spec.SecretKeySpec(new byte[16], "AES");
        }

        @Override
        public byte[] encrypt(String data, char[] password, byte[] salt) {
            return data == null ? new byte[0] : data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override
        public String decrypt(byte[] data, char... password) {
            return data == null ? "" : new String(data, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
