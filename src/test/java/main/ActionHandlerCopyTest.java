package main;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import encryption.EncryptionException;
import encryption.Encryptor;
import filehandling.LogFileHandler;

public class ActionHandlerCopyTest {
    @TempDir
    Path tempDir;

    @Test
    void copyUsesExactDisplayTimestamp() throws Exception {
        StubLogFileHandler handler = new StubLogFileHandler(tempDir.resolve("action-handler-copy.txt"));
        ActionHandler actionHandler = new ActionHandler(null, handler, new JList<>(), new DefaultListModel<>());
        String selectedTimestamp = "12:00 2026-09-02 (1)";

        String copiedText = actionHandler.buildSelectedEntryClipboardText(selectedTimestamp);

        assertEquals(selectedTimestamp, handler.requestedTimestamp, "Expected exact selected timestamp to be used");
        assertEquals(selectedTimestamp + "\n\nsecond duplicate entry", copiedText);
    }

    private static final class StubLogFileHandler extends LogFileHandler {
        private String requestedTimestamp;

        private StubLogFileHandler(Path path) throws Exception {
            super(path, new NoopEncryptor());
        }

        @Override
        public String loadEntry(String timeStamp) {
            requestedTimestamp = timeStamp;
            return "second duplicate entry";
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
