package services;

/**
 * Service interface for file operations
 */
public interface FileService {
    void saveText(String text) throws Exception;
    String loadText() throws Exception;
    boolean fileExists();
    long getFileSize();
}