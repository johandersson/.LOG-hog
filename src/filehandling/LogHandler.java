package filehandling;

import javax.swing.DefaultListModel;

/**
 * Interface for log file handling operations to enable polymorphism and LSP compliance.
 */
public interface LogHandler {
    void saveText(String text, DefaultListModel<String> listModel) throws Exception;
    String loadEntry(String timeStamp) throws Exception;
    void enableEncryption(char[] pwd) throws Exception;
    void disableEncryption() throws Exception;
    boolean isEncrypted();
    String getDisplayTimestamp(String rawTs);
    int getDuplicateCount(String timeStamp);
}