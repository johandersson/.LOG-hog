package services;

import javax.swing.DefaultListModel;

/**
 * Service interface for log entry operations
 */
public interface LogEntryService {
    void saveEntry(String text, DefaultListModel<String> listModel) throws Exception;
    void loadEntries(DefaultListModel<String> listModel) throws Exception;
    String loadEntry(String timestamp) throws Exception;
    void updateEntry(String timestamp, String newText) throws Exception;
    void deleteEntry(String timestamp) throws Exception;
}