package main;

import services.*;
import filehandling.LogFileOperations;

import java.nio.file.Path;

/**
 * Main application coordinator that manages service dependencies
 * and coordinates between different application components.
 */
public class Application {

    private final ServiceFactory serviceFactory;
    private final Path logFilePath;

    // Services
    private FileService fileService;
    private EncryptionService encryptionService;
    private LogEntryService logEntryService;

    public Application(Path logFilePath) {
        this.logFilePath = logFilePath;
        this.serviceFactory = ServiceFactory.getInstance();
        initializeServices();
    }

    // For testing with custom encryptor
    public Application(Path logFilePath, encryption.Encryptor encryptor) {
        this.logFilePath = logFilePath;
        this.serviceFactory = ServiceFactory.createWithEncryptor(encryptor);
        initializeServices();
    }

    private void initializeServices() {
        this.fileService = serviceFactory.createFileService(logFilePath);
        this.encryptionService = serviceFactory.createEncryptionService(logFilePath);
        this.logEntryService = serviceFactory.createLogEntryService(logFilePath);
    }

    // Service getters for components that need them
    public FileService getFileService() {
        return fileService;
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public LogEntryService getLogEntryService() {
        return logEntryService;
    }

    public LogFileOperations getLogFileOperations() {
        return serviceFactory.createLogFileOperations(logFilePath);
    }

    public Path getLogFilePath() {
        return logFilePath;
    }
}