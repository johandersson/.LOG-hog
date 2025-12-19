package services;

import filehandling.LogFileOperations;

import javax.swing.DefaultListModel;

/**
 * Implementation of LogEntryService using LogFileOperations
 */
public class LogEntryServiceImpl implements LogEntryService {

    private final LogFileOperations logFileOperations;

    public LogEntryServiceImpl(LogFileOperations logFileOperations) {
        this.logFileOperations = logFileOperations;
    }

    @Override
    public void saveEntry(String text, DefaultListModel<String> listModel) throws Exception {
        logFileOperations.saveText(text, listModel);
    }

    @Override
    public void loadEntries(DefaultListModel<String> listModel) throws Exception {
        logFileOperations.loadLogEntries(listModel);
    }

    @Override
    public String loadEntry(String timestamp) throws Exception {
        return logFileOperations.loadEntry(timestamp);
    }

    @Override
    public void updateEntry(String timestamp, String newText) throws Exception {
        // This needs to be implemented in LogFileHandler
        // For now, we'll need to cast to LogFileHandler to access updateEntry
        if (logFileOperations instanceof filehandling.LogFileHandler) {
            ((filehandling.LogFileHandler) logFileOperations).updateEntry(timestamp, newText);
        } else {
            throw new UnsupportedOperationException("updateEntry not supported by this implementation");
        }
    }

    @Override
    public void deleteEntry(String timestamp) throws Exception {
        // This needs to be implemented in LogFileHandler
        // For now, we'll need to cast to LogFileHandler to access deleteEntry
        if (logFileOperations instanceof filehandling.LogFileHandler) {
            ((filehandling.LogFileHandler) logFileOperations).deleteEntry(timestamp, null);
        } else {
            throw new UnsupportedOperationException("deleteEntry not supported by this implementation");
        }
    }
}