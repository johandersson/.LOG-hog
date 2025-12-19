package services;

import java.nio.file.Files;

import filehandling.LogFileOperations;

/**
 * Implementation of FileService using LogFileOperations
 */
public class FileServiceImpl implements FileService {

    private final LogFileOperations logFileOperations;

    public FileServiceImpl(LogFileOperations logFileOperations) {
        this.logFileOperations = logFileOperations;
    }

    @Override
    public void saveText(String text) throws Exception {
        // This would need to be implemented based on the LogFileOperations interface
        // For now, we'll delegate to a method that could be added to LogFileOperations
        throw new UnsupportedOperationException("saveText not yet implemented in service layer");
    }

    @Override
    public String loadText() throws Exception {
        // This would load the full file content
        throw new UnsupportedOperationException("loadText not yet implemented in service layer");
    }

    @Override
    public boolean fileExists() {
        return Files.exists(logFileOperations.getFilePath());
    }

    @Override
    public long getFileSize() {
        try {
            return Files.size(logFileOperations.getFilePath());
        } catch (Exception e) {
            return 0;
        }
    }
}