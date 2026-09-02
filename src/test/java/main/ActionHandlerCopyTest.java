package main;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import encryption.EncryptionException;
import encryption.Encryptor;
import filehandling.LogFileHandler;

public class ActionHandlerCopyTest {
    public static void main(String[] args) throws Exception {
        copyUsesExactDisplayTimestamp();
    }

    private static void copyUsesExactDisplayTimestamp() throws Exception {
        StubLogFileHandler handler = new StubLogFileHandler();
        ActionHandler actionHandler = new ActionHandler(null, handler, new JList<>(), new DefaultListModel<>());
        String selectedTimestamp = "12:00 2026-09-02 (1)";

        String copiedText = actionHandler.buildSelectedEntryClipboardText(selectedTimestamp);

        if (!selectedTimestamp.equals(handler.requestedTimestamp)) {
            throw new AssertionError("Expected exact selected timestamp, got " + handler.requestedTimestamp);
        }
        String expected = selectedTimestamp + "\n\nsecond duplicate entry";
        if (!expected.equals(copiedText)) {
            throw new AssertionError("Unexpected copied text: " + copiedText);
        }
    }

    private static final class StubLogFileHandler extends LogFileHandler {
        private String requestedTimestamp;

        private StubLogFileHandler() throws Exception {
            super(createTempFile(), new NoopEncryptor());
        }

        @Override
        public String loadEntry(String timeStamp) {
            requestedTimestamp = timeStamp;
            return "second duplicate entry";
        }

        private static Path createTempFile() throws Exception {
            Path path = Files.createTempFile("loghog-action-copy-", ".txt");
            path.toFile().deleteOnExit();
            return path;
        }
    }

    private static final class NoopEncryptor implements Encryptor {
        @Override
        public byte[] generateSalt() throws EncryptionException {
            return new byte[0];
        }

        @Override
        public javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) throws EncryptionException {
            return null;
        }

        @Override
        public byte[] encrypt(String data, char[] password, byte[] salt) throws EncryptionException {
            return new byte[0];
        }

        @Override
        public String decrypt(byte[] data, char... password) throws EncryptionException {
            return "";
        }
    }
}
