package filehandling;

/*
 * Copyright (C) 2026 Johan Andersson
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
import utils.SafeExecution;
import utils.DateHandler;

public class LogFileHandler implements LogFileOperations {
        // Listeners for UI components to refresh when file caches change
        private final java.util.List<Runnable> cacheInvalidationListeners = new java.util.ArrayList<>();
    private static final Path DEFAULT_FILE_PATH = Path.of(System.getProperty("user.home"), "log.txt");
    private static final String LOG_HEADER = ".LOG";
    private static final String BACKUP_EXT_ENCRYPTED = ".bak.enc";
    private static final String ENCRYPTED_ONLY_MSG = "Encrypted-only mode is enforced by security policy.";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);

    // Use centralized resource limits to prevent memory exhaustion and DoS
    private static final long MAX_FILE_SIZE = ResourceLimits.MAX_FILE_SIZE;

    // For testing only - deprecated, use constructor instead
    @Deprecated
    public static void setTestFilePath(Path testPath) {
        // This method is deprecated. Use constructor injection instead.
        // Intentionally unsupported in production flow.
        throw new UnsupportedOperationException("Use constructor with custom path instead");
    }

    private final Path filePath;
    private final Encryptor encryptor;
    private FileEncryptionManager encryptionManager;
    private BackupManager backupManager; // Optional backup manager
    private boolean encrypted;
    private byte[] salt;
    private String backupDirectory = "";
    
    private final FileCache cache = new FileCache();
    // Listeners for UI components to refresh when file caches change
    private EntryLoader entryLoader;
    private EntryEditor entryEditor;
    private final AsyncSaver asyncSaver;

    // Default constructor used by current app and tests.
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
        this.asyncSaver = new AsyncSaver(filePath, encryptionManager, entryEditor, cache, backupManager);
    }

    public static String removeSecureMarker(String text) {
        if (text == null) return null;
        // Remove the secure clipboard marker if present
        final String marker = "[LOGHOG_SECURE_CONTENT]|";
        if (text.startsWith(marker)) {
            return text.substring(marker.length());
        }
        return text;
    }

    /**
     * Reads all lines from a file, attempting UTF-8 first and falling back to ISO-8859-1
     * if the file contains bytes that are not valid UTF-8.
     * 
     * @param path the file path to read
     * @return list of lines from the file
     * @throws java.io.IOException if an I/O error occurs
     */
    public static List<String> readAllLinesSafe(Path path) throws java.io.IOException {
        long size = -1;
        try {
            size = Files.size(path);
        } catch (java.io.IOException ignored) {
        }
        if (size > 0 && size > ResourceLimits.MAX_FILE_SIZE) {
            throw new java.io.IOException("File too large to read into memory: " + size + " bytes");
        }
        try {
            return Files.readAllLines(path);
        } catch (java.nio.charset.MalformedInputException e) {
            // File contains bytes invalid for UTF-8; fall back to ISO-8859-1
            return Files.readAllLines(path, java.nio.charset.StandardCharsets.ISO_8859_1);
        }
    }

    public static List<String> readAllLinesSafe(Path path, java.nio.charset.Charset cs) throws java.io.IOException {
        long size = -1;
        try {
            size = Files.size(path);
        } catch (java.io.IOException ignored) {
        }
        if (size > 0 && size > ResourceLimits.MAX_FILE_SIZE) {
            throw new java.io.IOException("File too large to read into memory: " + size + " bytes");
        }
        return Files.readAllLines(path, cs);
    }

    @Override
    public void saveText(String text, DefaultListModel<String> listModel) {
        if (text == null || text.isBlank()) return;

        // Default synchronous save path (keeps existing behavior)
        String uniqueTimeStamp = null;
        try {
            uniqueTimeStamp = saveTextInternal(text);

            // Invalidate caches and reload list to properly count occurrences
            // for display suffixes (e.g., "14:30 2026-04-01 (1)")
            invalidateEntryCache();
            if (uniqueTimeStamp != null) {
                entryLoader.loadLogEntries(listModel);
            }
            // Notify UI (FullLog, etc.) that parsed/full-log caches should be invalidated
            notifyCacheInvalidationListeners();
            writeDebug(new StringBuilder("saveText: success (ts=").append(uniqueTimeStamp).append(')').toString());
        } catch (java.nio.file.AccessDeniedException e) {
            writeDebug(new StringBuilder("saveText: access denied - ").append(e.getMessage()).toString());
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
                    String ts = saveTextInternal(text);
                    // Invalidate caches and reload list for proper occurrence counting
                    invalidateEntryCache();
                    if (ts != null) {
                        entryLoader.loadLogEntries(listModel);
                    }
                    notifyCacheInvalidationListeners();
                    writeDebug("saveText: success after recreate (ts=" + uniqueTimeStamp + ")");
                } catch (Exception ex) {
                    // Security: Don't expose internal error details
                    showErrorDialog("<html><b>💾 Save Failed</b><br><br>Unable to save log entry. Please check file permissions.</html>");
                }
            }
        } catch (java.io.IOException e) {
            String errorMsg;
            if (e.getMessage() != null && e.getMessage().contains("being used by another process")) {
                errorMsg = "<html><b>💾 Save Failed - I/O Error</b><br><br>The file is <b>locked by another program</b>.<br><br><b>Solutions:</b><br>• Close any programs that might be using the file<br>• Check if the file is open in a text editor<br>• Restart the application if issue persists</html>";
            } else {
                // Security: Don't expose internal error details
                errorMsg = "<html><b>💾 Save Failed - I/O Error</b><br><br>Unable to write to the log file.<br><br><i>Tip: Ensure the file is not read-only or in use by another program.</i></html>";
            }
            writeDebug(new StringBuilder("saveText: io error - ").append(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()).toString());
            showErrorDialogWithRecovery(errorMsg, "Save Error");
        } catch (Exception e) {
            writeDebug(new StringBuilder("saveText: unknown error - ").append(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()).toString());
            showErrorDialog("<html><b>💾 Save Failed</b><br><br>Unable to save your log entry.<br>Please check your input and try again.<br><br><i>Tip: Ensure the file is not read-only or in use by another program.</i></html>");
        }
    }

    /**
     * Performs the actual I/O for saving text and returns the generated timestamp.
     * This method does not touch Swing components and is safe to run off the EDT.
     */
    private String saveTextInternal(String text) throws Exception {
        if (text == null || text.isBlank()) return null;

        String processedText = removeSecureMarker(text);

        String timeStamp = FORMATTER.format(LocalDateTime.now());
        int count = getDuplicateCount(timeStamp);
        String uniqueTimeStamp = entryEditor.createUniqueTimestamp(count);

        entryEditor.setBackupManager(backupManager);
        entryEditor.saveEntry(processedText, uniqueTimeStamp, encrypted);

        return uniqueTimeStamp;
    }

    /**
     * Asynchronous save helper: runs save on a background thread and updates the model on EDT.
     */
    public void saveTextAsync(String text, DefaultListModel<String> listModel, Runnable onComplete) {
        asyncSaver.saveTextAsync(text, listModel, () -> {
            // Reload list to properly count occurrences for display suffixes.
            // Runs on the save thread (not the EDT) so large files do not freeze the UI.
            try {
                invalidateEntryCache();
                entryLoader.loadLogEntries(listModel);
            } catch (Exception e) {
                // Fall back to just invalidation on error
                writeDebug("saveTextAsync: reload failed - " + e.getMessage());
            }
        }, () -> {
            // Keep Full Log and other cache-aware views in sync after async saves.
            notifyCacheInvalidationListeners();
            if (onComplete != null) onComplete.run();
        });
        // PMD: Avoid unused private methods such as 'sortAndNormalizeFile()'.
        // Method removed as it is not used.
        // Removed call to entryEditor.writeLines(lines, encrypted); as 'lines' is undefined here.
    }

    /**
     * Updates an entry identified by display timestamp (which may include occurrence suffix).
     * @param displayTimestamp the timestamp as shown in UI (e.g., "14:30 2026-04-01" or "14:30 2026-04-01 (1)")
     * @param newText the new content
     */
    public void updateEntry(String displayTimestamp, String newText) {
        if (newText.isBlank() || !Files.exists(filePath)) return;

        try {
            // Parse display timestamp to get raw timestamp and occurrence index
            String rawTs = getRawTimestamp(displayTimestamp);
            int occurrence = parseOccurrenceIndex(displayTimestamp);
            
            List<String> lines;
            lines = new ArrayList<>(getLines());

            if (!containsTimestampOccurrence(lines, rawTs, occurrence)) {
                showErrorDialog("<html><b>✏️ Update Failed</b><br><br>Unable to locate the selected log entry.<br>Please reload and try again.</html>");
                return;
            }
            
            List<String> updatedLines = entryEditor.updateEntry(rawTs, occurrence, newText, lines);

            // Use write-back cache for performance
            cache.invalidateEntryCache();
            // Also set pending lines so write-back will flush to disk
            cache.setPendingLines(updatedLines);

            // Notify UI that parsed/full-log caches should be invalidated or refreshed
            notifyCacheInvalidationListeners();
            
            // Invalidate EntryLoader caches (timestamps, parsed entries, content cache)
            if (entryLoader != null) {
                entryLoader.invalidateCaches();
            }
            
            // Note: Actual write happens in flushPendingWrites() called by UI or on explicit flush
        } catch (Exception e) {
            showErrorDialog("<html><b>✏️ Update Failed</b><br><br>Unable to update the log entry.<br>Please try again.<br><br><i>Tip: Ensure the entry exists and the file is writable.</i></html>");
        }
    }

    /**
     * Updates an entry on a background thread while showing a progress dialog.
     * Rewriting a very large log file can take a while, so this keeps the UI
     * responsive instead of freezing it until the save completes.
     *
     * @param displayTimestamp the timestamp as shown in UI
     * @param newText the new content
     * @param onComplete callback run on the EDT once the update has been applied
     */
    public void updateEntryAsync(String displayTimestamp, String newText, Runnable onComplete) {
        asyncSaver.runWithProgressAsync("Saving", "Saving entry...",
            () -> updateEntry(displayTimestamp, newText), onComplete);
    }

    private boolean containsTimestampOccurrence(List<String> lines, String rawTimestamp, int occurrence) {
        if (lines == null || rawTimestamp == null) {
            return false;
        }

        int currentOccurrence = 0;
        for (String line : lines) {
            String normalizedLineTs = normalizeTimestampForMatching(line);
            if (normalizedLineTs.equals(rawTimestamp.trim())) {
                if (currentOccurrence == occurrence) {
                    return true;
                }
                currentOccurrence++;
            }
        }

        return false;
    }

    private String normalizeTimestampForMatching(String ts) {
        if (ts == null) {
            return "";
        }

        String normalized = ts.trim();
        normalized = normalized.replaceAll("^\\d+\\|(\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2})(.*)$", "$1$2");
        normalized = normalized.replaceAll(" \\(\\d+\\)$", "");
        return normalized;
    }
    
    /**
     * Parses occurrence index from display timestamp.
     * @param displayTimestamp e.g., "14:30 2026-04-01 (2)" returns 2, "14:30 2026-04-01" returns 0
     */
    private int parseOccurrenceIndex(String displayTimestamp) {
        if (displayTimestamp == null) return 0;
        var matcher = java.util.regex.Pattern.compile(" \\((\\d+)\\)$").matcher(displayTimestamp.trim());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }
    
    /**
     * Flush pending writes to disk immediately.
     * Called when user switches views, locks file, or after timeout.
     */
    public void flushPendingWrites() {
        if (!cache.hasPendingWrites()) return;
        
        try {
            List<String> pendingLines = cache.getPendingLines();
            if (!encryptionManager.isEncrypted()) {
                throw new IllegalStateException(ENCRYPTED_ONLY_MSG);
            }

            // Create numbered backup before encryption
            if (backupManager != null) {
                backupManager.createNumberedBackup();
            }
            encryptionManager.encryptFileFromLines(pendingLines);
            // Keep the in-memory hydration cache and incremental journal in sync with
            // the authoritative content we just wrote, otherwise the next read could
            // fall back to stale pre-write content.
            entryEditor.syncAfterFullEncryptedWrite(pendingLines);
            
            cache.clearPendingWrites();
            // Notify UI that pending writes were flushed to disk and caches may need refresh
            notifyCacheInvalidationListeners();
        } catch (Exception e) {
            // Security: Don't expose internal error details
            showErrorDialog("<html><b>💾 Write Failed</b><br><br>Unable to save changes to disk.<br>Please check file permissions and disk space.</html>");
        }
    }

    /**
     * Async version of flushPendingWrites - shows a progress dialog and runs write off-EDT.
     */
    public void flushPendingWritesAsync(Runnable onComplete) {
        asyncSaver.flushPendingWritesAsync(() -> {
            // On successful flush, pending writes are cleared and views can safely refresh
            // from disk. Skip notification if write failed and pending writes remain.
            if (!cache.hasPendingWrites()) {
                notifyCacheInvalidationListeners();
            }
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
    
    /**
     * Check if there are unsaved changes.
     */
    public boolean hasPendingWrites() {
        return cache.hasPendingWrites();
    }

    public void changeTimestamp(String displayTimestamp, String newTimestamp, DefaultListModel<String> listModel) {
        if (!Files.exists(filePath)) return;

        try {
            // Parse display timestamp to get raw timestamp and occurrence
            String rawOldTs = getRawTimestamp(displayTimestamp);
            int targetOccurrence = parseOccurrenceIndex(displayTimestamp);
            
            List<String> lines;
            lines = new ArrayList<>(getLines());
            
            // Find and replace the correct occurrence
            int currentOccurrence = 0;
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                // Strip any old suffix from file for matching
                String lineRawTs = trimmed.replaceAll(" \\(\\d+\\)$", "");
                
                if (lineRawTs.equals(rawOldTs) || trimmed.equals(rawOldTs)) {
                    if (currentOccurrence == targetOccurrence) {
                        lines.set(i, newTimestamp);
                        found = true;
                        break;
                    }
                    currentOccurrence++;
                }
            }
            
            if (!found) return;

            if (!encryptionManager.isEncrypted()) {
                throw new IllegalStateException(ENCRYPTED_ONLY_MSG);
            }

            // Create numbered backup before encryption
            if (backupManager != null) {
                backupManager.createNumberedBackup();
            }
            encryptionManager.encryptFileFromLines(lines);
            // Keep the in-memory hydration cache and incremental journal in sync with
            // the authoritative content we just wrote, otherwise the next read could
            // fall back to stale pre-write content.
            entryEditor.syncAfterFullEncryptedWrite(lines);
            
            // Invalidate caches and reload list for proper display suffix regeneration
            invalidateEntryCache();
            notifyCacheInvalidationListeners();
            try {
                entryLoader.loadLogEntries(listModel);
            } catch (Exception e) {
                // Fallback: just update the model directly
                int index = listModel.indexOf(displayTimestamp);
                if (index != -1) {
                    listModel.remove(index);
                    listModel.addElement(newTimestamp);
                    sortListModel(listModel);
                }
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
            lines = new ArrayList<>(getLines());
            
            List<String> updatedLines = entryEditor.deleteEntries(timestamps, lines);

            entryEditor.setBackupManager(backupManager);
            entryEditor.writeLines(updatedLines, encrypted);
            
            // Invalidate caches after deletion
            entryLoader.invalidateCaches();
            // Notify UI to invalidate parsed/full-log caches
            notifyCacheInvalidationListeners();
            
            // Update list model if provided
            if (listModel != null) {
                // Rebuild the model in a single O(n) pass instead of calling
                // removeElement() per timestamp, which is O(n) per call (linear
                // search + shift) and O(n * k) overall for a batch of k deletions.
                java.util.Set<String> toRemove = new java.util.HashSet<>(timestamps);
                int size = listModel.getSize();
                List<String> remaining = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    String el = listModel.getElementAt(i);
                    if (!toRemove.contains(el)) {
                        remaining.add(el);
                    }
                }
                listModel.removeAllElements();
                for (String el : remaining) {
                    listModel.addElement(el);
                }
                // Re-number "(n)" occurrence suffixes for any remaining entries that share a
                // raw timestamp with a deleted entry. Without this, stale suffixes (e.g. "(2)")
                // can remain in the list after an earlier occurrence (e.g. "(1)") was deleted,
                // causing the display label to no longer match the entry's position in the
                // freshly-rebuilt content cache. Selecting such a stale label falls back to an
                // ambiguous "match by raw timestamp" lookup, which can display the wrong entry.
                renumberDuplicateTimestampSuffixes(listModel);
            }
        } catch (Exception e) {
            showErrorDialog("<html><b>🗑️ Delete Failed</b><br><br>Unable to delete the log entries.<br>Please try again.<br><br><i>Tip: Ensure the entries exist and the file is not locked.</i></html>");
        }
    }

    /**
     * Recomputes the "(n)" occurrence suffixes for entries in the list model that share the
     * same raw timestamp, based on their current relative order in the model. This keeps the
     * displayed suffixes consistent with the ones the content cache assigns when it is rebuilt,
     * which is essential after a batch delete removes some but not all occurrences of a
     * duplicated timestamp.
     */
    private void renumberDuplicateTimestampSuffixes(DefaultListModel<String> listModel) {
        java.util.Map<String, Integer> occurrenceCount = new java.util.HashMap<>();
        for (int i = 0; i < listModel.getSize(); i++) {
            String display = listModel.getElementAt(i);
            String rawTs = getRawTimestamp(display);
            int occurrence = occurrenceCount.getOrDefault(rawTs, 0);
            occurrenceCount.put(rawTs, occurrence + 1);
            String correctDisplay = occurrence > 0 ? rawTs + " (" + occurrence + ")" : rawTs;
            if (!correctDisplay.equals(display)) {
                listModel.set(i, correctDisplay);
            }
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
                String shortTitle = "File Too Large";
                String longMessage = "The log file is larger than the allowed limit (" + (MAX_FILE_SIZE / (1024 * 1024)) + " MB).\n\n" +
                    "Loading very large files can cause the application to run out of memory.";
                DialogHandler.showLimitExceeded(shortTitle, longMessage);
                throw new IllegalStateException("File exceeds maximum size limit");
            }
        }
        // If there are pending writes (write-back cache), return them immediately
        if (cache.hasPendingWrites()) {
            List<String> pending = cache.getPendingLines();
            if (pending != null) {
                return new ArrayList<>(pending);
            }
        }
        
        if (!encryptionManager.isEncrypted()) {
            throw new IllegalStateException(ENCRYPTED_ONLY_MSG);
        }

        return entryEditor.getMergedEncryptedWorkingLines();
    }

    /**
     * Streaming plaintext access is intentionally disabled in encrypted-only mode.
     */
    public java.util.stream.Stream<String> getLinesStreamed() throws Exception {
        throw new UnsupportedOperationException("Streaming plaintext access is disabled in encrypted-only mode.");
    }

    @Override
    public void enableEncryption(char[] pwd) throws Exception {
        // Safety check: don't re-encrypt if already encrypted
        if (encrypted && encryptionManager.isEncrypted()) {
            throw new IllegalStateException("File is already encrypted. Use setEncryption() to set credentials.");
        }
        
        this.salt = encryptor.generateSalt();
        List<String> lines;
        if (Files.exists(filePath)) {
            lines = readAllLinesSafe(filePath);
        } else {
            lines = new ArrayList<>();
        }
        if (lines.isEmpty() || !LOG_HEADER.equalsIgnoreCase(lines.get(0).trim())) {
            List<String> withHeader = new ArrayList<>();
            withHeader.add(LOG_HEADER);
            withHeader.add("");
            withHeader.addAll(lines);
            lines = withHeader;
        }
        // Do NOT write an unencrypted plaintext backup. Instead set up encryption
        // and write the encrypted file, then create an encrypted backup copy.
        encryptionManager.setEncryption(pwd, this.salt);
        encryptionManager.encryptFileFromLines(lines);
        // Create an encrypted backup copy to preserve previous state without leaving plaintext on disk
        try {
            Path backupPathEnc = getBackupPath(filePath.getFileName().toString() + BACKUP_EXT_ENCRYPTED);
            Files.copy(filePath, backupPathEnc, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
            // Don't fail encryption if backup copy can't be created
        }
        cache.clearCachedLines();
        encrypted = true;
        // Notify UI that encryption state changed and cached parsed data should be invalidated
        notifyCacheInvalidationListeners();
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

    /**
     * Register a listener that is invoked when parsed/full-log caches should be invalidated.
     */
    public void addCacheInvalidationListener(Runnable r) {
        if (r == null) return;
        synchronized (cacheInvalidationListeners) {
            cacheInvalidationListeners.add(r);
        }
    }

    /**
     * Remove a previously registered cache invalidation listener.
     */
    public void removeCacheInvalidationListener(Runnable r) {
        if (r == null) return;
        synchronized (cacheInvalidationListeners) {
            cacheInvalidationListeners.remove(r);
        }
    }

    /**
     * Notify registered listeners that caches were invalidated.
     * Runs listeners on the EDT to keep UI-safe.
     */
    private void notifyCacheInvalidationListeners() {
        java.util.List<Runnable> copy;
        synchronized (cacheInvalidationListeners) {
            if (cacheInvalidationListeners.isEmpty()) return;
            copy = new java.util.ArrayList<>(cacheInvalidationListeners);
        }
        for (Runnable r : copy) {
            try {
                if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                    r.run();
                } else {
                    javax.swing.SwingUtilities.invokeLater(r);
                }
            } catch (RuntimeException ignore) {
                writeDebug("notifyCacheInvalidationListeners: listener failure");
            }
        }
    }

    /**
     * Lightweight debug logging used during development.
     */
    private void writeDebug(String msg) {
        // Method intentionally left blank for production. Parameter required for interface compatibility.
        // PMD: Avoid unused method parameters such as 'msg'.
        // No-op.
        @SuppressWarnings("unused")
        String unused = msg;
    }

    public List<List<String>> getParsedEntries() throws Exception {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        return LogParser.parseAllEntries(getLines());
    }

    public void enableEncryption() throws Exception {
        throw new UnsupportedOperationException("Use enableEncryption(char[] pwd) in encrypted-only mode.");
    }

    @Override
    public void disableEncryption() throws Exception {
        throw new UnsupportedOperationException("Decryption is disabled by encrypted-only security policy.");
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
        // Defensive copy: clone the provided salt to avoid retaining caller-owned array
        byte[] saltClone = slt != null ? slt.clone() : null;
        encryptionManager.setEncryption(pwd, saltClone);
        this.salt = saltClone;
        this.encrypted = true;
        // Clear cache to force re-read with new credentials
        cache.clearCachedLines();
        cache.invalidateEntryCache();
        // Notify listeners that parsed/full-log cache should be invalidated
        notifyCacheInvalidationListeners();
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

    /**
     * Returns the distinct years present in the log file, newest-first.
     * Delegates to EntryLoader and limits the number of years returned.
     */
    public java.util.List<Integer> getAvailableYears(int maxYears) {
        try {
            if (entryLoader != null) {
                return entryLoader.getAvailableYears(maxYears);
            }
        } catch (Exception e) {
            // Ignore and fall back to current year
        }
        return java.util.List.of(java.time.LocalDate.now().getYear());
    }
    
    public void setBackupManager(BackupManager backupManager) {
        this.backupManager = backupManager;
        if (asyncSaver != null) {
            asyncSaver.setBackupManager(backupManager);
        }
    }
    
    public BackupManager getBackupManager() {
        return backupManager;
    }
    
    public FileEncryptionManager getEncryptionManager() {
        return encryptionManager;
    }

    /**
     * Expose EntryLoader for read-only operations that compute results off-EDT.
     */
    public EntryLoader getEntryLoader() {
        return entryLoader;
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
     * Securely clears all cached data by overwriting before clearing.
     * Called when locking the file to prevent memory forensics.
     */
    public void secureClearCache() {
        cache.secureClear();
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
            } catch (java.io.IOException | SecurityException e) {
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

    public void compactEncryptedJournal() throws Exception {
        if (!encrypted) {
            return;
        }
        entryEditor.compactEncryptedJournal();
        notifyCacheInvalidationListeners();
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
        // Notify listeners that sensitive data cleared and caches should be invalidated
        notifyCacheInvalidationListeners();
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
