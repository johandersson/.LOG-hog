/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package filehandling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.DefaultListModel;

import encryption.EncryptionManager;
import encryption.Encryptor;
import encryption.FileEncryptionManager;
import main.BackupManager;
import utils.DateHandler;

public class LogFileHandler implements LogFileOperations {
    private static final Path DEFAULT_FILE_PATH = Path.of(System.getProperty("user.home"), "log.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);

    // Maximum file size to load (50MB) - prevents memory exhaustion
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    // Maximum entries in a collection - prevents DoS
    private static final int MAX_COLLECTION_SIZE = 100000;

    // For testing only - deprecated, use constructor instead
    @Deprecated
    public static void setTestFilePath(Path testPath) {
        // This method is deprecated. Use constructor injection instead.
        // Keeping for backward compatibility but this violates immutability.
        throw new UnsupportedOperationException("Use constructor with custom path instead");
    }

    private final Path filePath;
    private final Encryptor encryptor;
    private FileEncryptionManager encryptionManager;
    private BackupManager backupManager; // Optional backup manager
    private boolean encrypted = false;
    private byte[] salt;
    private String backupDirectory = "";
    
    private final FileCache cache = new FileCache();
    private EntryLoader entryLoader;
    private EntryEditor entryEditor;

    // Default constructor for backward compatibility
    public LogFileHandler() {
        this(DEFAULT_FILE_PATH, EncryptionManager.getInstance());
    }

    // Constructor for testing with dependencies
    public LogFileHandler(Path filePath, Encryptor encryptor) {
        this.filePath = filePath;
        this.encryptor = encryptor;
        this.encryptionManager = new FileEncryptionManager(filePath, encryptor);
        this.entryLoader = new EntryLoader(this, encryptor);
        this.entryEditor = new EntryEditor(filePath, encryptionManager, cache);
    }

    public static String removeSecureMarker(String text) {
        if (text == null) return null;
        // Remove the secure clipboard marker if present
        String marker = "[LOGHOG_SECURE_CONTENT]";
        int markerIndex = text.indexOf(marker);
        if (markerIndex == 0) {
            int pipeIndex = text.indexOf('|');
            if (pipeIndex > marker.length()) {
                return text.substring(pipeIndex + 1);
            }
        }
        return text;
    }

    @Override
    public void saveText(String text, DefaultListModel<String> listModel) {
        if (text == null || text.isBlank()) return;

        text = removeSecureMarker(text);

        String timeStamp = FORMATTER.format(LocalDateTime.now());
        int count = getDuplicateCount(timeStamp);
        String uniqueTimeStamp = entryEditor.createUniqueTimestamp(count);

        try {
            entryEditor.setBackupManager(backupManager);
            entryEditor.saveEntry(text, uniqueTimeStamp, encrypted);

            listModel.addElement(uniqueTimeStamp);
            sortListModel(listModel);

            cache.invalidateEntryCache();
        } catch (java.nio.file.AccessDeniedException e) {
            showErrorDialog("<html><b>💾 Save Failed - Access Denied</b><br><br>" +
                "The log file is <b>read-only</b> or you don't have write permissions.<br><br>" +
                "<b>Solutions:</b><br>" +
                "• Right-click the file → Properties → Uncheck 'Read-only'<br>" +
                "• Check file permissions in your system<br>" +
                "• Try running as administrator<br><br>" +
                "<i>File: " + filePath.getFileName() + "</i></html>");
        } catch (java.nio.file.NoSuchFileException e) {
            // File was deleted - offer to recreate
            if (handleMissingLogFile()) {
                // File created/restored, try save again
                try {
                    entryEditor.saveEntry(text, uniqueTimeStamp, encrypted);
                    listModel.addElement(uniqueTimeStamp);
                    sortListModel(listModel);
                    cache.invalidateEntryCache();
                } catch (Exception ex) {
                    // Security: Don't expose internal error details
                    showErrorDialog("<html><b>💾 Save Failed</b><br><br>Unable to save log entry. Please check file permissions.</html>");
                }
            }
        } catch (java.io.IOException e) {
            String errorMsg = "<html><b>💾 Save Failed - I/O Error</b><br><br>";
            if (e.getMessage() != null && e.getMessage().contains("being used by another process")) {
                errorMsg += "The file is <b>locked by another program</b>.<br><br>" +
                    "<b>Solutions:</b><br>" +
                    "• Close any programs that might be using the file<br>" +
                    "• Check if the file is open in a text editor<br>" +
                    "• Restart the application if issue persists</html>";
            } else {
                // Security: Don't expose internal error details
                errorMsg += "Unable to write to the log file.<br><br>" +
                    "<i>Tip: Ensure the file is not read-only or in use by another program.</i></html>";
            }
            showErrorDialogWithRecovery(errorMsg, "Save Error");
        } catch (Exception e) {
            showErrorDialog("<html><b>💾 Save Failed</b><br><br>Unable to save your log entry.<br>Please check your input and try again.<br><br><i>Tip: Ensure the file is not read-only or in use by another program.</i></html>");
        }
    }

    //sort and normalize file
    private void sortAndNormalizeFile() throws Exception {
        if (!Files.exists(filePath)) return;

        List<String> lines;
        if (encryptionManager.isEncrypted()) {
            lines = new ArrayList<>(getLines());
        } else {
            lines = Files.readAllLines(filePath);
        }
        
        entryEditor.setBackupManager(backupManager);
        entryEditor.writeLines(lines, encrypted);
    }

    public void updateEntry(String timeStamp, String newText) {
        if (newText.isBlank() || !Files.exists(filePath)) return;

        try {
            List<String> lines;
            if (encrypted) {
                lines = new ArrayList<>(getLines());
            } else {
                lines = Files.readAllLines(filePath);
            }
            
            List<String> updatedLines = entryEditor.updateEntry(timeStamp, newText, lines);

            // Use write-back cache for performance
            cache.setPendingLines(updatedLines);
            
            // Invalidate caches immediately
            entryLoader.invalidateCaches();
            
            // Note: Actual write happens in flushPendingWrites() called by UI or on explicit flush
        } catch (Exception e) {
            showErrorDialog("<html><b>✏️ Update Failed</b><br><br>Unable to update the log entry.<br>Please try again.<br><br><i>Tip: Ensure the entry exists and the file is writable.</i></html>");
        }
    }
    
    /**
     * Flush pending writes to disk immediately.
     * Called when user switches views, locks file, or after timeout.
     */
    public void flushPendingWrites() {
        if (!cache.hasPendingWrites()) return;
        
        try {
            List<String> pendingLines = cache.getPendingLines();
            if (encryptionManager.isEncrypted()) {
                cache.updateCachedLines(pendingLines);
                String fullText = String.join(LogFileFormat.INTERNAL_LINE_SEPARATOR, cache.getCachedLines());
                // Create numbered backup before encryption
                if (backupManager != null) {
                    backupManager.createNumberedBackup();
                }
                encryptionManager.encryptFile(fullText);
            } else {
                // Create numbered backup before writing
                if (backupManager != null) {
                    backupManager.createNumberedBackup();
                }
                Files.write(filePath, pendingLines);
            }
            
            cache.clearPendingWrites();
        } catch (Exception e) {
            // Security: Don't expose internal error details
            showErrorDialog("<html><b>💾 Write Failed</b><br><br>Unable to save changes to disk.<br>Please check file permissions and disk space.</html>");
        }
    }
    
    /**
     * Check if there are unsaved changes.
     */
    public boolean hasPendingWrites() {
        return cache.hasPendingWrites();
    }

    public void changeTimestamp(String oldTimestamp, String newTimestamp, DefaultListModel<String> listModel) {
        if (!Files.exists(filePath)) return;

        try {
            List<String> lines;
            if (encrypted) {
                lines = new ArrayList<>(getLines());
            } else {
                lines = Files.readAllLines(filePath);
            }
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().equals(oldTimestamp.trim())) {
                    lines.set(i, newTimestamp);
                    break;
                }
            }

            if (encryptionManager.isEncrypted()) {
                cache.updateCachedLines(lines);
                String fullText = String.join(LogFileFormat.INTERNAL_LINE_SEPARATOR, cache.getCachedLines());
                // Create numbered backup before encryption
                if (backupManager != null) {
                    backupManager.createNumberedBackup();
                }
                encryptionManager.encryptFile(fullText);
            } else {
                // Create numbered backup before writing
                if (backupManager != null) {
                    backupManager.createNumberedBackup();
                }
                Files.write(filePath, lines);
            }
            
            // Update the list model
            int index = listModel.indexOf(oldTimestamp);
            if (index != -1) {
                listModel.remove(index);
                listModel.addElement(newTimestamp);
                sortListModel(listModel);
            }
        } catch (Exception e) {
            showErrorDialog("<html><b>⏰ Timestamp Change Failed</b><br><br>Unable to change the timestamp.<br>Please try again.<br><br><i>Tip: Ensure the new timestamp is unique and valid.</i></html>");
        }
    }

    // delete certain log entry
    private void deleteLogEntry(String timeStamp, DefaultListModel<String> listModel) {
        deleteLogEntries(List.of(timeStamp), listModel);
    }

    /**
     * Delete multiple log entries in a single file operation.
     * Much more efficient than calling deleteEntry() multiple times.
     * 
     * @param timestamps List of timestamps to delete
     * @param listModel Optional list model to update
     */
    public void deleteLogEntries(List<String> timestamps, DefaultListModel<String> listModel) {
        if (!Files.exists(filePath) || timestamps == null || timestamps.isEmpty()) return;

        try {
            List<String> lines;
            if (encrypted) {
                lines = new ArrayList<>(getLines());
            } else {
                lines = Files.readAllLines(filePath);
            }
            
            List<String> updatedLines = entryEditor.deleteEntries(timestamps, lines);

            entryEditor.setBackupManager(backupManager);
            entryEditor.writeLines(updatedLines, encrypted);
            
            // Invalidate caches after deletion
            entryLoader.invalidateCaches();
            
            // Update list model if provided
            if (listModel != null) {
                for (String ts : timestamps) {
                    listModel.removeElement(ts);
                }
            }
        } catch (Exception e) {
            showErrorDialog("<html><b>🗑️ Delete Failed</b><br><br>Unable to delete the log entries.<br>Please try again.<br><br><i>Tip: Ensure the entries exist and the file is not locked.</i></html>");
        }
    }

    public int getDuplicateCount(String timeStamp) {
        if (!Files.exists(filePath)) return 0;

        try {
            // Use EntryLoader's cache if available for O(1) lookup
            // This avoids reading the entire file on every save
            if (entryLoader != null) {
                int count = 0;
                // Check the in-memory list model first if available (fastest)
                // Otherwise fall back to counting from lines
                List<String> lines = getLines();
                for (String line : lines) {
                    if (line.trim().startsWith(timeStamp)) {
                        count++;
                    }
                }
                return count;
            }
            
            // Fallback: direct file read (shouldn't happen in practice)
            List<String> lines = getLines();
            return (int) lines.stream()
                .filter(line -> line.startsWith(timeStamp))
                .count();
        } catch (Exception e) {
            showErrorDialog("<html><b>🔍 Duplicate Check Failed</b><br><br>Unable to check for duplicate timestamps.<br>Proceeding with save.<br><br><i>Tip: This may result in duplicate entries.</i></html>");
            return 0;
        }
    }

    public String getDisplayTimestamp(String rawTs) {
        return rawTs;
    }    
    
    public List<String> getLines() throws Exception {
        // Security: Check file size before loading to prevent memory exhaustion DoS
        if (Files.exists(filePath)) {
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_FILE_SIZE) {
                throw new IllegalStateException("File exceeds maximum size limit");
            }
        }
        
        if (encryptionManager.isEncrypted()) {
            List<String> cachedLines = cache.getCachedLines();
            if (cachedLines.isEmpty()) {
                String decrypted = encryptionManager.decryptFile();
                cachedLines = new ArrayList<>(Arrays.asList(decrypted.split("\r?\n", -1)));
                cache.updateCachedLines(cachedLines);
            }
            return cachedLines;
        } else {
            List<String> lines = Files.readAllLines(filePath);
            // Keep .LOG in unencrypted files for Notepad compatibility
            return lines;
        }
    }

    @Override
    public void enableEncryption(char[] pwd) throws Exception {
        // Safety check: don't re-encrypt if already encrypted
        if (encrypted && encryptionManager.isEncrypted()) {
            throw new IllegalStateException("File is already encrypted. Use setEncryption() to set credentials.");
        }
        
        this.salt = encryptor.generateSalt();
        List<String> lines = Files.readAllLines(filePath);
        // Preserve .LOG header in encrypted files (don't remove it)
        String fullText = String.join(LogFileFormat.INTERNAL_LINE_SEPARATOR, lines);
        // Ensure .LOG header is present
        if (!fullText.startsWith(".LOG")) {
            fullText = ".LOG\n\n" + fullText;
        }
        // Save encrypted to backup first
        Path backupPath = getBackupPath(filePath.getFileName().toString() + ".bak");
        Files.write(backupPath, Files.readAllBytes(filePath));
        // Then encrypt and save
        encryptionManager.setEncryption(pwd, this.salt);
        encryptionManager.encryptFile(fullText);
        cache.updateCachedLines(new ArrayList<>(Arrays.asList(fullText.split("\r?\n", -1))));
        encrypted = true;
    }

    private void invalidateEntryCache() {
        cache.invalidateEntryCache();
        // Also invalidate EntryLoader's caches
        if (entryLoader != null) {
            entryLoader.invalidateCaches();
        }
    }
    
    /**
     * Invalidates all entry caches to force reload on next access.
     * Public method for external cache invalidation.
     */
    public void invalidateCaches() {
        invalidateEntryCache();
    }

    public List<List<String>> getParsedEntries() throws Exception {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        long currentModified = Files.getLastModifiedTime(filePath).toMillis();
        if (cache.getCachedEntries() == null || currentModified > cache.getCachedEntriesLastModified()) {
            List<String> lines = getLines();
            List<List<String>> entries = LogParser.parseAllEntries(lines);
            cache.setCachedEntries(entries, currentModified);
        }

        return cache.getCachedEntries();
    }

    public void enableEncryption() throws Exception {

        String plainContent = Files.readString(filePath);

        // Ensure .LOG header is present
        if (!plainContent.startsWith(".LOG")) {
            plainContent = ".LOG\n\n" + plainContent;
        }

        // Save plain text to backup first
        Path backupPath = getBackupPath(filePath.getFileName().toString() + ".bak");
        Files.writeString(backupPath, plainContent);

        // Encrypt and write the content
        encryptionManager.encryptFile(plainContent);

        // Set encryption state
        encrypted = true;
        this.salt = encryptionManager.getSalt().clone();

        // Clear any cached data since encryption state changed
        cache.invalidateCaches();
    }

    @Override
    public void disableEncryption() throws Exception {
        if (!encryptionManager.isEncrypted()) {
            throw new IllegalStateException("File is not encrypted");
        }
        
        // Read and decrypt the current file
        byte[] data = Files.readAllBytes(filePath);
        String decrypted = encryptor.decryptWithFallback(data, encryptionManager.getPassword(), encryptionManager.getSalt());
        
        // Save decrypted to backup first (as encrypted bytes)
        Path backupPath = getBackupPath(filePath.getFileName().toString() + ".bak");
        Files.write(backupPath, data);
        
        // Ensure .LOG header is present (for backward compatibility with old encrypted files)
        String contentWithHeader = decrypted;
        if (!contentWithHeader.startsWith(".LOG")) {
            contentWithHeader = ".LOG\n\n" + contentWithHeader;
        }
        
        // Write decrypted content as plain text (using writeString to preserve encoding)
        Files.writeString(filePath, contentWithHeader);
        
        // Clear encryption state
        encrypted = false;
        this.salt = null;
        encryptionManager.disableEncryption();
    }

    private void sortListModel(DefaultListModel<String> listModel) {
        List<String> sortedEntries = Collections.list(listModel.elements()).stream()
                .sorted((a, b) -> {
                    try {
                        return DateHandler.parseTimestamp(b).compareTo(DateHandler.parseTimestamp(a));
                    } catch (Exception e) {
                        return 0; // keep original order if parsing fails
                    }
                })
                .toList();

        listModel.clear();
        sortedEntries.forEach(listModel::addElement);
    }

    @Override
    public void loadLogEntries(DefaultListModel<String> listModel) throws Exception {
        entryLoader.loadLogEntries(listModel);
    }

    // load only entries matching year and month (1..12)
    public void loadFilteredEntries(DefaultListModel<String> listModel, int year, int month) {
        entryLoader.loadFilteredEntries(listModel, year, month);
    }

    public void loadFilteredEntriesByYear(DefaultListModel<String> listModel, int year) {
        entryLoader.loadFilteredEntriesByYear(listModel, year);
    }

    // produce a filtered DefaultListModel from an existing model
    public DefaultListModel<String> filterModelByYearMonth(DefaultListModel<String> sourceModel, int year, int month) {
        return entryLoader.filterModelByYearMonth(sourceModel, year, month);
    }

    @Override
    public String loadEntry(String timeStamp) {
        return entryLoader.loadEntry(timeStamp);
    }

    public void setEncryption(char[] pwd, byte[] slt) throws Exception {
        // Just set credentials - don't re-encrypt if already encrypted
        encryptionManager.setEncryption(pwd, slt);
        this.salt = slt;
        this.encrypted = true;
        // Clear cache to force re-read with new credentials
        cache.clearCachedLines();
        cache.invalidateEntryCache();
    }

    @Override
    public boolean isEncrypted() {
        return encrypted;
    }

    @Override
    public char[] getPassword() {
        return encryptionManager.getPassword();
    }

    @Override
    public byte[] getSalt() {
        return encryptionManager.getSalt();
    }

    @Override
    public Path getFilePath() {
        return filePath;
    }

    @Override
    public void deleteEntry(String timestamp) throws Exception {
        deleteLogEntry(timestamp, null);
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory != null ? backupDirectory : "";
    }
    
    public void setBackupManager(BackupManager backupManager) {
        this.backupManager = backupManager;
    }
    
    public BackupManager getBackupManager() {
        return backupManager;
    }
    
    public FileEncryptionManager getEncryptionManager() {
        return encryptionManager;
    }
    
    /**
     * Updates the cached lines (used for encrypted files).
     * Provides memory-efficient write operation.
     */
    public void updateCachedLines(List<String> lines) {
        if (lines == null) {
            throw new IllegalArgumentException("Lines cannot be null");
        }
        cache.updateCachedLines(lines);
    }
    
    /**
     * Clears the cached lines to force a fresh read from disk.
     * Useful after encrypting/modifying the file.
     */
    public void clearCachedLines() {
        cache.clearCachedLines();
    }
    
    /**
     * Clears pending writes without flushing them.
     * Used after external file modifications (like formatting) to prevent
     * stale pending writes from overwriting the new content.
     */
    public void clearPendingWrites() {
        cache.clearPendingWrites();
    }
    
    private Path getBackupPath(String filename) {
        if (backupDirectory != null && !backupDirectory.isEmpty()) {
            Path dir = Paths.get(backupDirectory);
            // Validate that the backup directory is within allowed paths
            if (!isValidFilePath(dir)) {
                // Fall back to sibling if backup directory is not valid
                return filePath.resolveSibling(filename);
            }
            try {
                Files.createDirectories(dir);
            } catch (Exception e) {
                // If can't create, fall back to sibling
                return filePath.resolveSibling(filename);
            }
            return dir.resolve(filename);
        } else {
            return filePath.resolveSibling(filename);
        }
    }



    public String getRawTimestamp(String displayTimestamp) {
        return displayTimestamp.replaceAll(" \\([0-9]+\\)$", "");
    }

    public void deleteEntry(String selectedItem, DefaultListModel<String> listModel) {
        if (selectedItem != null && !selectedItem.isBlank()) {
            deleteLogEntry(selectedItem, listModel);
        } else {
            showErrorDialog("<html><b>⚠️ No Selection</b><br><br>Please select a log entry to delete.<br><br><i>Tip: Click on an entry in the list to select it.</i></html>");
        }
    }

    public List<String> getRecentLogEntries(int i) {
        return entryLoader.getRecentLogEntries(i);
    }

    public void clearSensitiveData() {
        encrypted = false;
        if (salt != null) {
            Arrays.fill(salt, (byte) 0);
            salt = null;
        }
        encryptionManager.clearSensitiveData();
        cache.invalidateCaches();
        cache.clearPendingWrites();
        // Clear all EntryLoader caches (timestamps, parsed entries, content cache)
        if (entryLoader != null) {
            entryLoader.invalidateCaches();
        }
    }

    public void showErrorDialog(String message) {
        DialogHandler.showErrorDialog(message);
    }
    
    /**
     * Shows error dialog with recovery options including backup restore.
     */
    public void showErrorDialogWithRecovery(String message, String title) {
        DialogHandler.showErrorDialogWithRecovery(message, title, this::showBackupRestoreDialog);
    }
    
    /**
     * Shows dialog when log file is missing, offering to create new or restore from backup.
     */
    public boolean handleMissingLogFile() {
        return DialogHandler.handleMissingLogFile(filePath, this::invalidateEntryCache);
    }
    
    /**
     * Shows backup restore dialog and allows user to select a backup file.
     */
    public boolean showBackupRestoreDialog() {
        return DialogHandler.showBackupRestoreDialog(filePath, this::invalidateEntryCache);
    }

    private boolean isValidFilePath(Path path) {
        try {
            // Convert to absolute path and normalize
            Path absolutePath = path.toAbsolutePath().normalize();

            // Get user home directory
            String userHome = System.getProperty("user.home");
            Path userHomePath = Path.of(userHome).toAbsolutePath().normalize();

            // Get current working directory
            String cwd = System.getProperty("user.dir");
            Path cwdPath = Path.of(cwd).toAbsolutePath().normalize();

            // Allow paths within user home or current working directory
            return absolutePath.startsWith(userHomePath) || absolutePath.startsWith(cwdPath);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Encryptor getEncryptor() {
        return encryptor;
    }
}
