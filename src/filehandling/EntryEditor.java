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

import gui.InputLimits;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import encryption.FileEncryptionManager;
import main.BackupManager;

/**
 * Handles CRUD operations for log entries.
 * Extracted from LogFileHandler to separate entry editing logic.
 */
public class EntryEditor {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);
    
    private final Path filePath;
    private final FileEncryptionManager encryptionManager;
    private final FileCache cache;
    private BackupManager backupManager;
    
    public EntryEditor(Path filePath, FileEncryptionManager encryptionManager, FileCache cache) {
        this.filePath = filePath;
        this.encryptionManager = encryptionManager;
        this.cache = cache;
    }
    
    public void setBackupManager(BackupManager backupManager) {
        this.backupManager = backupManager;
    }
    
    /**
     * Saves a new log entry with timestamp.
     */
    public void saveEntry(String text, String uniqueTimeStamp, boolean encrypted) throws Exception {
        if (text == null || text.isBlank()) return;
        
        String ls = System.lineSeparator();
        String entry = LogFileFormat.createEntry(uniqueTimeStamp, text);

        if (encrypted) {
            List<String> cachedLines = cache.getCachedLines();
            cachedLines.addAll(Arrays.asList(entry.split("\r?\n", -1)));
            // Restore fullText assignment for encrypted block
            StringBuilder fullTextBuilder = new StringBuilder();
            for (String line : cachedLines) {
                fullTextBuilder.append(line).append(LogFileFormat.INTERNAL_LINE_SEPARATOR);
            }
            String fullText = fullTextBuilder.toString();
            // Ensure .LOG header is present for encrypted files
            if (!fullText.startsWith(".LOG")) {
                fullText = ".LOG" + LogFileFormat.INTERNAL_LINE_SEPARATOR + LogFileFormat.INTERNAL_LINE_SEPARATOR + fullText;
                cachedLines = new ArrayList<>(Arrays.asList(fullText.split("\r?\n", -1)));
            }
            // Normalize spacing to prevent accumulation of blank lines
            List<String> normalized = LogFileFormat.normalizeSpacing(cachedLines);
            cache.updateCachedLines(normalized);
            encryptionManager.encryptFileFromLines(cache.getCachedLines());
        } else {
            // Normalize content lines: remove trailing blank lines from user-supplied text
            List<String> contentLines = Arrays.asList(text.split("\r?\n", -1));
            // Remove trailing blank lines
            while (!contentLines.isEmpty() && contentLines.get(contentLines.size() - 1).isBlank()) {
                contentLines = contentLines.subList(0, contentLines.size() - 1);
            }
            String normalizedContent = String.join(LogFileFormat.LINE_SEPARATOR, contentLines);
            String entryToAppend = LogFileFormat.createEntry(uniqueTimeStamp, normalizedContent);

            if (Files.exists(filePath)) {
                if (backupManager != null) {
                    backupManager.createNumberedBackup();
                }
                java.nio.file.Files.write(filePath, entryToAppend.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
            } else {
                // For new files, ensure .LOG header exists for Notepad compatibility
                String contentWithHeader = ".LOG" + ls + ls + entryToAppend;
                java.nio.file.Files.write(filePath, contentWithHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE);
            }
        }
    }
    
    /**
     * Updates an existing log entry.
     */
    public List<String> updateEntry(String timeStamp, String newText, List<String> lines) {
        if (newText == null || newText.isBlank()) return lines;

        // Enforce maximum entry length on updates as well (avoid reassigning parameter)
        String updatedText = newText;
        try {
            if (updatedText.length() > InputLimits.ENTRY_MAX_CHARS) {
                updatedText = updatedText.substring(0, InputLimits.ENTRY_MAX_CHARS);
                if (!updatedText.endsWith(System.lineSeparator())) updatedText = updatedText.concat(System.lineSeparator());
                updatedText = updatedText.concat("[TRUNCATED]").concat(System.lineSeparator());
            }
        } catch (Exception ignore) {
        }
        
        List<String> updatedLines = new ArrayList<>();
        boolean inTargetEntry = false;

        for (String line : lines) {
            if (line.trim().equals(timeStamp.trim())) {
                inTargetEntry = true;
                updatedLines.add(line); // keep the timestamp line
                // Use Arrays.asList instead of tight loop
                updatedLines.addAll(Arrays.asList(updatedText.split("\r?\n", -1)));
                continue;
            }

            if (inTargetEntry) {
                // stop skipping when we hit the next timestamp line
                if (utils.DateHandler.isTimestamp(line)) {
                    inTargetEntry = false;
                    updatedLines.add(line); // add the next timestamp line
                }
                // skip old body lines
            } else {
                updatedLines.add(line);
            }
        }

        return updatedLines;
        // (Fixed: Only one implementation should exist. The above block is the correct one, so remove the duplicate below.)
    }
    
    /**
     * Deletes multiple log entries in a single operation.
     */
    public List<String> deleteEntries(List<String> timestamps, List<String> lines) {
        if (timestamps == null || timestamps.isEmpty()) return lines;
        
        // Convert to set for O(1) lookup
        Set<String> timestampsToDelete = new HashSet<>();
        for (String ts : timestamps) {
            timestampsToDelete.add(ts.trim());
        }
        
        // Remove all matching entries in one pass
        List<String> updatedLines = new ArrayList<>();
        boolean inDeletedEntry = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Check if this is a timestamp we want to delete
            if (timestampsToDelete.contains(trimmed)) {
                inDeletedEntry = true;
                continue; // Skip timestamp line
            }
            
            // Check if we hit the next timestamp (end of deleted entry)
            if (inDeletedEntry && utils.DateHandler.isTimestamp(trimmed)) {
                inDeletedEntry = false;
                updatedLines.add(line);
                continue;
            }
            
            // Skip lines that are part of a deleted entry
            if (inDeletedEntry) {
                continue;
            }
            
            updatedLines.add(line);
        }
        
        return updatedLines;
    }
    
    /**
     * Changes a timestamp to a new value.
     */
    public List<String> changeTimestamp(String oldTimestamp, String newTimestamp, List<String> lines) {
        List<String> updatedLines = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().equals(oldTimestamp.trim())) {
                updatedLines.add(newTimestamp);
                continue;
            } else {
                updatedLines.add(line);
            }
        }
        
        return updatedLines;
    }
    
    /**
     * Creates a unique timestamp, adding duplicate counter if needed.
     */
    public String createUniqueTimestamp(int duplicateCount) {
        String timeStamp = FORMATTER.format(LocalDateTime.now());
        return duplicateCount > 0 ? timeStamp + " (" + duplicateCount + ")" : timeStamp;
    }

    /**
     * Helper that creates a unique timestamp and writes the entry to disk.
     * Returns the generated unique timestamp or null on failure.
     */
    public String createAndSaveEntry(String text) throws Exception {
        if (text == null || text.isBlank()) return null;

        // Enforce maximum entry length to avoid extremely large entries (avoid reassigning parameter)
        String inputText = text;
        try {
            if (inputText.length() > InputLimits.ENTRY_MAX_CHARS) {
                StringBuilder sb = new StringBuilder(inputText.substring(0, InputLimits.ENTRY_MAX_CHARS));
                if (!inputText.endsWith(System.lineSeparator())) sb.append(System.lineSeparator());
                sb.append("[TRUNCATED]").append(System.lineSeparator());
                inputText = sb.toString();
            }
        } catch (Exception ignore) {
        }

        String timeStamp = FORMATTER.format(LocalDateTime.now());
        int count = 0;

        // Determine duplicate count using cache or file read
        if (Files.exists(filePath)) {
            List<String> existingLines = encryptionManager.isEncrypted() ? cache.getCachedLines() : Files.readAllLines(filePath);
            if (existingLines != null) {
                count = (int) existingLines.stream().filter(line -> line.trim().startsWith(timeStamp)).count();
            }
        }

        String unique = createUniqueTimestamp(count);
        // Use StringBuilder for string appends if needed in future logic
        saveEntry(inputText, unique, encryptionManager.isEncrypted());
        return unique;
    }
    
    /**
     * Writes lines to file with backup and normalization.
     */
    public void writeLines(List<String> lines, boolean encrypted) throws Exception {
        // Sort and normalize
        List<String> sortedLines = EntrySorter.sortEntriesByTimestamp(lines);
        List<String> normalized = LogFileFormat.normalizeSpacing(sortedLines);

        if (encrypted) {
            cache.updateCachedLines(normalized);
            if (backupManager != null) {
                backupManager.createNumberedBackup();
            }
            encryptionManager.encryptFileFromLines(cache.getCachedLines());
        } else {
            if (backupManager != null) {
                backupManager.createNumberedBackup();
            }
            Files.write(filePath, normalized);
        }
    }
}
