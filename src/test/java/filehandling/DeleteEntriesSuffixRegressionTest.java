package filehandling;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.DefaultListModel;

/**
 * Regression test for a bug where deleting a non-first occurrence of a
 * duplicated timestamp left stale "(n)" suffixes in the list model, causing
 * the wrong entry's content to be displayed (or a false "file may be
 * corrupted" style error) after a batch delete.
 */
public class DeleteEntriesSuffixRegressionTest {
    public static void main(String[] args) throws Exception {
        deletingMiddleDuplicateRenumbersRemainingSuffixes();
        System.out.println("DeleteEntriesSuffixRegressionTest passed");
    }

    private static void deletingMiddleDuplicateRenumbersRemainingSuffixes() throws Exception {
        Path tempFile = Files.createTempFile("loghog_dup_suffix_test", ".txt");
        Files.deleteIfExists(tempFile);
        try {
            LogFileHandler handler = new LogFileHandler(tempFile, new NoOpEncryptor());

            DefaultListModel<String> listModel = new DefaultListModel<>();
            // Simulate the state after loading 3 entries sharing the same timestamp:
            // occurrence 0 has no suffix, occurrence 1 and 2 use "(1)" and "(2)".
            listModel.addElement("10:00 2025-01-01");
            listModel.addElement("10:00 2025-01-01 (1)");
            listModel.addElement("10:00 2025-01-01 (2)");

            // Simulate deleting the middle occurrence "(1)", as deleteLogEntries() would
            // right before it calls the renumbering helper.
            listModel.removeElement("10:00 2025-01-01 (1)");

            Method renumber = LogFileHandler.class.getDeclaredMethod(
                "renumberDuplicateTimestampSuffixes", DefaultListModel.class);
            renumber.setAccessible(true);
            renumber.invoke(handler, listModel);

            List<String> expected = List.of("10:00 2025-01-01", "10:00 2025-01-01 (1)");
            List<String> actual = List.of(
                listModel.getElementAt(0),
                listModel.getElementAt(1)
            );

            if (listModel.getSize() != 2) {
                throw new AssertionError("Expected 2 remaining entries, got " + listModel.getSize());
            }
            if (!expected.equals(actual)) {
                throw new AssertionError("Expected suffixes to be renumbered to " + expected
                    + " but got " + actual + ". Stale suffixes cause wrong-entry lookups after delete.");
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /** Minimal Encryptor stub; encryption is never exercised by this test. */
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
