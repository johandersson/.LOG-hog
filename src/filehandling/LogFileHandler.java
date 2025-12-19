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
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

import encryption.EncryptionManager;
import encryption.FileEncryptionManager;
import utils.DateHandler;

public class LogFileHandler {
    static Path FILE_PATH = Path.of(System.getProperty("user.home"), "log.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.ROOT);

    // For testing only
    public static void setTestFilePath(Path testPath) {
        FILE_PATH = testPath;
    }

    private FileEncryptionManager encryptionManager = new FileEncryptionManager(FILE_PATH);
    private boolean encrypted = false;
    private byte[] salt;
    private String backupDirectory = "";
    List<String> cachedLines = new ArrayList<>();
    private List<List<String>> cachedEntries = null;
    private long cachedEntriesLastModified = 0;
    private final EntryLoader entryLoader = new EntryLoader(this);

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

    public void saveText(String text, DefaultListModel<String> listModel) {
        if (text == null || text.isBlank()) return;

        text = removeSecureMarker(text);

        String timeStamp = FORMATTER.format(LocalDateTime.now());
        int count = getDuplicateCount(timeStamp);
        String uniqueTimeStamp = count > 0 ? timeStamp + " (" + count + ")" : timeStamp;

        String ls = System.lineSeparator();
        // Entry ends with exactly one blank line for correct grouping
        String entry = uniqueTimeStamp + ls + text + ls;

        try {
            if (encrypted) {
                cachedLines.addAll(Arrays.asList(entry.split("\n", -1)));
                String fullText = String.join("\n", cachedLines);
                
                // Ensure .LOG header is present for encrypted files
                if (!fullText.startsWith(".LOG")) {
                    fullText = ".LOG\n\n" + fullText;
                    cachedLines = new ArrayList<>(Arrays.asList(fullText.split("\n")));
                }
                
                encryptionManager.encryptFile(fullText);
                cachedLines = new ArrayList<>(Arrays.asList(fullText.split("\n")));
            } else {
                if (Files.exists(FILE_PATH)) {
                    // Inspect last line to avoid creating multiple blank lines between entries.
                    List<String> existing = Files.readAllLines(FILE_PATH);
                    String toWrite = uniqueTimeStamp + ls + text + ls;
                    Files.writeString(FILE_PATH, toWrite, java.nio.file.StandardOpenOption.APPEND);
                } else {
                    // For new files, ensure .LOG header exists for Notepad compatibility
                    String contentWithHeader = ".LOG" + ls + ls + entry;
                    Files.writeString(FILE_PATH, contentWithHeader, java.nio.file.StandardOpenOption.CREATE);
                }
            }

            listModel.addElement(uniqueTimeStamp);
            sortListModel(listModel);

            invalidateEntryCache();
        } catch (Exception e) {
            showErrorDialog("<html><b>💾 Save Failed</b><br><br>Unable to save your log entry.<br>Please check your input and try again.<br><br><i>Tip: Ensure the file is not read-only or in use by another program.</i></html>");
        }
    }

    //sort and normalize file
    private void sortAndNormalizeFile() throws Exception {
        if (!Files.exists(FILE_PATH)) return;

        List<String> lines;
        if (encryptionManager.isEncrypted()) {
            lines = new ArrayList<>(getLines());
        } else {
            lines = Files.readAllLines(FILE_PATH);
        }
        
        // Sort entries by timestamp
        List<String> sortedLines = sortEntriesByTimestamp(lines);
        
        // Normalize spacing
        List<String> normalized = getNormalized(sortedLines);

        if (encryptionManager.isEncrypted()) {
            cachedLines = new ArrayList<>(normalized);
            String fullText = String.join("\n", cachedLines);
            encryptionManager.encryptFile(fullText);
        } else {
            Files.write(FILE_PATH, normalized);
        }
    }

    public void updateEntry(String timeStamp, String newText) {
        if (newText.isBlank() || !Files.exists(FILE_PATH)) return;

        try {
            List<String> lines;
            if (encrypted) {
                lines = new ArrayList<>(getLines());
            } else {
                lines = Files.readAllLines(FILE_PATH);
            }
            List<String> updatedLines = new ArrayList<>();
            boolean inTargetEntry = false;

            for (String line : lines) {
                if (line.trim().equals(timeStamp.trim())) {
                    inTargetEntry = true;
                    updatedLines.add(line); // keep the timestamp line
                    updatedLines.add(newText); // add the new text
                    continue;
                }

                if (inTargetEntry) {
                    // stop skipping when we hit the next timestamp line
                    if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                        inTargetEntry = false;
                        updatedLines.add(line); // add the next timestamp line
                    }
                    // skip old body lines
                } else {
                    updatedLines.add(line);
                }
            }

            if (encryptionManager.isEncrypted()) {
                cachedLines = new ArrayList<>(updatedLines);
                String fullText = String.join("\n", cachedLines);
                encryptionManager.encryptFile(fullText);
            } else {
                Files.write(FILE_PATH, updatedLines);
            }
        } catch (Exception e) {
            showErrorDialog("<html><b>✏️ Update Failed</b><br><br>Unable to update the log entry.<br>Please try again.<br><br><i>Tip: Ensure the entry exists and the file is writable.</i></html>");
        }
    }

    public void changeTimestamp(String oldTimestamp, String newTimestamp, DefaultListModel<String> listModel) {
        if (!Files.exists(FILE_PATH)) return;

        try {
            List<String> lines;
            if (encrypted) {
                lines = new ArrayList<>(getLines());
            } else {
                lines = Files.readAllLines(FILE_PATH);
            }
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).trim().equals(oldTimestamp.trim())) {
                    lines.set(i, newTimestamp);
                    break;
                }
            }

            if (encryptionManager.isEncrypted()) {
                cachedLines = new ArrayList<>(lines);
                String fullText = String.join("\n", cachedLines);
                encryptionManager.encryptFile(fullText);
            } else {
                Files.write(FILE_PATH, lines);
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
        if (!Files.exists(FILE_PATH)) return;

        try {
            List<String> lines;
            if (encrypted) {
                lines = new ArrayList<>(getLines());
            } else {
                lines = Files.readAllLines(FILE_PATH);
            }
            List<String> updatedLines = getUpdatedLines(timeStamp, lines);

            // Sort entries by timestamp before normalizing
            List<String> sortedLines = sortEntriesByTimestamp(updatedLines);

            // Normalize spacing: ensure at most one blank line between entries
            List<String> normalized = getNormalized(sortedLines);

            if (encryptionManager.isEncrypted()) {
                cachedLines = new ArrayList<>(normalized);
                String fullText = String.join("\n", cachedLines);
                encryptionManager.encryptFile(fullText);
            } else {
                Files.write(FILE_PATH, normalized);
            }
            listModel.removeElement(timeStamp);
        } catch (Exception e) {
            showErrorDialog("<html><b>🗑️ Delete Failed</b><br><br>Unable to delete the log entry.<br>Please try again.<br><br><i>Tip: Ensure the entry exists and the file is not locked.</i></html>");
        }
    }

    private static List<String> getNormalized(List<String> updatedLines) {
        List<String> normalized = new ArrayList<>();
        boolean prevBlank = false;
        for (String l : updatedLines) {
            boolean isBlank = l.trim().isEmpty();
            if (isBlank) {
                if (!prevBlank) {
                    normalized.add(""); // keep single blank line
                    prevBlank = true;
                } // else skip additional blank lines
            } else {
                normalized.add(l);
                prevBlank = false;
            }
        }
        return normalized;
    }

    private static List<String> sortEntriesByTimestamp(List<String> lines) {
        // Check if .LOG header exists in the input
        boolean hasLogHeader = lines.stream().anyMatch(line -> line.trim().equalsIgnoreCase(".LOG"));
        
        List<List<String>> entries = new ArrayList<>();
        List<String> currentEntry = new ArrayList<>();
        java.util.regex.Pattern tsPattern = java.util.regex.Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$", java.util.regex.Pattern.MULTILINE);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase(".LOG")) continue; // Skip .LOG during processing
            if (tsPattern.matcher(trimmed).matches()) {
                if (!currentEntry.isEmpty()) {
                    entries.add(new ArrayList<>(currentEntry));
                    currentEntry.clear();
                }
                currentEntry.add(line);
            } else {
                if (!currentEntry.isEmpty() || !trimmed.isEmpty()) {
                    currentEntry.add(line);
                }
            }
        }
        if (!currentEntry.isEmpty()) {
            entries.add(currentEntry);
        }

        // Separate timestamp entries from non-timestamp entries
        List<List<String>> timestampEntries = new ArrayList<>();
        List<List<String>> nonTimestampEntries = new ArrayList<>();
        for (List<String> entry : entries) {
            if (!entry.isEmpty() && tsPattern.matcher(entry.get(0).trim()).matches()) {
                timestampEntries.add(entry);
            } else {
                nonTimestampEntries.add(entry);
            }
        }

        // Sort timestamp entries by date ascending (oldest first)
        timestampEntries.sort((a, b) -> {
            try {
                LocalDateTime dateA = parseDateForSorting(a.get(0));
                LocalDateTime dateB = parseDateForSorting(b.get(0));
                return dateA.compareTo(dateB);
            } catch (Exception e) {
                return 0; // keep original order if parsing fails
            }
        });

        // Combine: non-timestamp entries first, then sorted timestamp entries
        List<List<String>> sortedEntries = new ArrayList<>();
        sortedEntries.addAll(nonTimestampEntries);
        sortedEntries.addAll(timestampEntries);

        // Flatten back to lines with consistent spacing
        List<String> sortedLines = new ArrayList<>();
        
        // Add .LOG header at the top if it existed in the input
        if (hasLogHeader) {
            sortedLines.add(".LOG");
            sortedLines.add(""); // Blank line after header
        }
        
        for (int i = 0; i < sortedEntries.size(); i++) {
            List<String> entry = sortedEntries.get(i);
            
            // Remove trailing blank lines from entry
            while (!entry.isEmpty() && entry.get(entry.size() - 1).trim().isEmpty()) {
                entry.remove(entry.size() - 1);
            }
            
            sortedLines.addAll(entry);
            
            // Add exactly one blank line after each entry except the last one
            if (i < sortedEntries.size() - 1) {
                sortedLines.add("");
            }
        }

        return sortedLines;
    }

    private static LocalDateTime parseDateForSorting(String timestampLine) {
        String dateStr = timestampLine.trim().replaceAll(" \\(\\d+\\)", "");
        return LocalDateTime.parse(dateStr, FORMATTER);
    }

    private static List<String> getUpdatedLines(String timeStamp, List<String> lines) {
        List<String> updatedLines = new ArrayList<>();
        boolean skipping = false;

        for (String line : lines) {
            // timestamp lines are exact matches (whitespace trimmed)
            if (!skipping && line.trim().equals(timeStamp.trim())) {
                skipping = true; // start skipping this timestamp and its body
                continue;
            }

            if (skipping) {
                // stop skipping when we hit the next timestamp line
                if (line.matches("\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\(\\d+\\))?")) {
                    skipping = false;
                    // This line is the next timestamp; it should be kept
                    updatedLines.add(line);
                } else {
                    // while skipping, simply continue (this drops blank lines and body lines)
                    continue;
                }
            } else {
                updatedLines.add(line);
            }
        }
        return updatedLines;
    }

    public int getDuplicateCount(String timeStamp) {
        if (!Files.exists(FILE_PATH)) return 0;

        try {
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
        if (encryptionManager.isEncrypted()) {
            if (cachedLines == null) {
                String decrypted = encryptionManager.decryptFile();
                cachedLines = Arrays.stream(decrypted.split("\n")).map(String::trim).collect(Collectors.toList());
            }
            return cachedLines;
        } else {
            List<String> lines = Files.readAllLines(FILE_PATH);
            // Keep .LOG in unencrypted files for Notepad compatibility
            return lines.stream().map(String::trim).collect(Collectors.toList());
        }
    }

    public void enableEncryption(char[] pwd) throws Exception {
        this.salt = EncryptionManager.getInstance().generateSalt();
        List<String> lines = Files.readAllLines(FILE_PATH);
        // Preserve .LOG header in encrypted files (don't remove it)
        String fullText = String.join("\n", lines);
        // Ensure .LOG header is present
        if (!fullText.startsWith(".LOG")) {
            fullText = ".LOG\n\n" + fullText;
        }
        // Save encrypted to backup first
        Path backupPath = getBackupPath(FILE_PATH.getFileName().toString() + ".bak");
        Files.write(backupPath, Files.readAllBytes(FILE_PATH));
        // Then encrypt and save
        encryptionManager.setEncryption(pwd, this.salt);
        encryptionManager.encryptFile(fullText);
        cachedLines = new ArrayList<>(Arrays.asList(fullText.split("\n")));
        encrypted = true;
    }

    private void invalidateEntryCache() {
        cachedEntries = null;
        cachedEntriesLastModified = 0;
    }

    public List<List<String>> getParsedEntries() throws Exception {
        if (!Files.exists(FILE_PATH)) {
            return new ArrayList<>();
        }

        long currentModified = Files.getLastModifiedTime(FILE_PATH).toMillis();
        if (cachedEntries == null || currentModified > cachedEntriesLastModified) {
            List<String> lines = getLines();
            cachedEntries = parseEntriesFromLines(lines);
            cachedEntriesLastModified = currentModified;
        }

        return cachedEntries;
    }

    private List<List<String>> parseEntriesFromLines(List<String> lines) {
        List<List<String>> entries = new ArrayList<>();
        List<String> currentEntry = new ArrayList<>();
        java.util.regex.Pattern tsPattern = java.util.regex.Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$", java.util.regex.Pattern.MULTILINE);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase(".LOG")) continue;
            if (tsPattern.matcher(trimmed).matches()) {
                if (!currentEntry.isEmpty()) {
                    entries.add(new ArrayList<>(currentEntry));
                    currentEntry.clear();
                }
                currentEntry.add(line);
            } else if (!currentEntry.isEmpty()) {
                currentEntry.add(line);
            }
        }
        if (!currentEntry.isEmpty()) {
            entries.add(currentEntry);
        }
        return entries;
    }

    public void enableEncryption() throws Exception {

        String plainContent = Files.readString(FILE_PATH);

        // Ensure .LOG header is present
        if (!plainContent.startsWith(".LOG")) {
            plainContent = ".LOG\n\n" + plainContent;
        }

        // Save plain text to backup first
        Path backupPath = getBackupPath(FILE_PATH.getFileName().toString() + ".bak");
        Files.writeString(backupPath, plainContent);

        // Encrypt and write the content
        encryptionManager.encryptFile(plainContent);

        // Set encryption state
        encrypted = true;
        this.salt = encryptionManager.getSalt().clone();

        // Clear any cached data since encryption state changed
        if (cachedLines != null) {
            cachedLines.clear();
            cachedLines = null;
        }
        if (cachedEntries != null) {
            cachedEntries.clear();
            cachedEntries = null;
            cachedEntriesLastModified = 0;
        }
    }

    public void disableEncryption() throws Exception {
        if (!encryptionManager.isEncrypted()) {
            throw new IllegalStateException("File is not encrypted");
        }
        
        // Read and decrypt the current file
        byte[] data = Files.readAllBytes(FILE_PATH);
        String decrypted = EncryptionManager.getInstance().decryptWithFallback(data, encryptionManager.getPassword(), encryptionManager.getSalt());
        
        // Save decrypted to backup first (as encrypted bytes)
        Path backupPath = getBackupPath(FILE_PATH.getFileName().toString() + ".bak");
        Files.write(backupPath, data);
        
        // Ensure .LOG header is present (for backward compatibility with old encrypted files)
        String contentWithHeader = decrypted;
        if (!contentWithHeader.startsWith(".LOG")) {
            contentWithHeader = ".LOG\n\n" + contentWithHeader;
        }
        
        // Write decrypted content as plain text (using writeString to preserve encoding)
        Files.writeString(FILE_PATH, contentWithHeader);
        
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

    public void loadLogEntries(DefaultListModel<String> listModel) throws Exception {
        entryLoader.loadLogEntries(listModel);
    }

    // load only entries matching year and month (1..12)
    public void loadFilteredEntries(DefaultListModel<String> listModel, int year, int month) {
        entryLoader.loadFilteredEntries(listModel, year, month);
    }

    // produce a filtered DefaultListModel from an existing model
    public DefaultListModel<String> filterModelByYearMonth(DefaultListModel<String> sourceModel, int year, int month) {
        return entryLoader.filterModelByYearMonth(sourceModel, year, month);
    }

    public String loadEntry(String timeStamp) {
        return entryLoader.loadEntry(timeStamp);
    }

    public void setEncryption(char[] pwd, byte[] slt) throws Exception {
        // Set credentials first
        encryptionManager.setEncryption(pwd, slt);
        
        // Then encrypt the existing file content
        enableEncryption();
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public char[] getPassword() {
        return encryptionManager.getPassword();
    }

    public byte[] getSalt() {
        return encryptionManager.getSalt();
    }

    public Path getFilePath() {
        return FILE_PATH;
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory != null ? backupDirectory : "";
    }

    private Path getBackupPath(String filename) {
        if (backupDirectory != null && !backupDirectory.isEmpty()) {
            Path dir = Paths.get(backupDirectory);
            // Validate that the backup directory is within allowed paths
            if (!isValidFilePath(dir)) {
                // Fall back to sibling if backup directory is not valid
                return FILE_PATH.resolveSibling(filename);
            }
            try {
                Files.createDirectories(dir);
            } catch (Exception e) {
                // If can't create, fall back to sibling
                return FILE_PATH.resolveSibling(filename);
            }
            return dir.resolve(filename);
        } else {
            return FILE_PATH.resolveSibling(filename);
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
        if (cachedLines != null) {
            cachedLines.clear();
            cachedLines = null;
        }
        if (cachedEntries != null) {
            cachedEntries.clear();
            cachedEntries = null;
            cachedEntriesLastModified = 0;
        }
    }

    public void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
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
}
