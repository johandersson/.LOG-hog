package services;

import java.nio.file.Path;

import encryption.EncryptionManager;
import encryption.Encryptor;
import filehandling.LogFileHandler;
import filehandling.LogFileOperations;

/**
 * Factory for creating service instances with proper dependency injection
 */
public class ServiceFactory {

    private static ServiceFactory instance;
    private final Encryptor encryptor;

    private ServiceFactory() {
        this.encryptor = EncryptionManager.getInstance();
    }

    private ServiceFactory(Encryptor encryptor) {
        this.encryptor = encryptor;
    }

    public static ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }

    // For testing with custom encryptor
    public static ServiceFactory createWithEncryptor(Encryptor encryptor) {
        return new ServiceFactory(encryptor);
    }

    public FileService createFileService(Path filePath) {
        LogFileOperations logFileOps = new LogFileHandler(filePath, encryptor);
        return new FileServiceImpl(logFileOps);
    }

    public EncryptionService createEncryptionService(Path filePath) {
        LogFileOperations logFileOps = new LogFileHandler(filePath, encryptor);
        return new EncryptionServiceImpl(logFileOps);
    }

    public LogEntryService createLogEntryService(Path filePath) {
        LogFileOperations logFileOps = new LogFileHandler(filePath, encryptor);
        return new LogEntryServiceImpl(logFileOps);
    }

    // Combined service for backward compatibility
    public LogFileOperations createLogFileOperations(Path filePath) {
        return new LogFileHandler(filePath, encryptor);
    }
}