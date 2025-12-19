package filehandling;

import encryption.Encryptor;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Interface for log file operations to enable better testing
 */
public interface LogFileOperations {
    void saveText(String text, DefaultListModel<String> listModel) throws Exception;
    void loadLogEntries(DefaultListModel<String> listModel) throws Exception;
    String loadEntry(String timestamp) throws Exception;
    void enableEncryption(char[] password) throws Exception;
    void disableEncryption() throws Exception;
    boolean isEncrypted();
    char[] getPassword();
    byte[] getSalt();
    Path getFilePath();
}