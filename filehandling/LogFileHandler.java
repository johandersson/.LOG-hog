package filehandling;

import encryption.EncryptionManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;
import javax.crypto.*;
import javax.swing.*;

public class LogFileHandler {
    static final Path FILE_PATH = Path.of(System.getProperty("user.home"), "log.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm yyyy-MM-dd", Locale.getDefault());

    private boolean encrypted = false;
    private char[] password;
    private byte[] salt;
    List<String> cachedLines = new ArrayList<>();
    private final EntryLoader entryLoader = new EntryLoader(this);

    public void saveText(String text, DefaultListModel<String> listModel) {
        if (text == null || text.isBlank()) return;

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
                SecretKey key = EncryptionManager.deriveKey(password, salt);
                byte[] encryptedData = EncryptionManager.encrypt(fullText, key);
                Files.write(FILE_PATH, encryptedData);
                cachedLines = new ArrayList<>(Arrays.asList(fullText.split("\n")));
            } else {
                if (Files.exists(FILE_PATH)) {
                    // Inspect last line to avoid creating multiple blank lines between entries.
                    List<String> existing = Files.readAllLines(FILE_PATH);
                    boolean lastLineIsBlank = !existing.isEmpty() && existing.get(existing.size() - 1).trim().isEmpty();
                    String toWrite = lastLineIsBlank ? uniqueTimeStamp + ls + text + ls : ls + uniqueTimeStamp + ls + text + ls;
                    Files.writeString(FILE_PATH, toWrite, java.nio.file.StandardOpenOption.APPEND);
                } else {
                    Files.writeString(FILE_PATH, entry, java.nio.file.StandardOpenOption.CREATE);
                }
            }

            listModel.addElement(uniqueTimeStamp);
            sortListModel(listModel);

            // Normalize the entire file to ensure consistent blank lines
            normalizeFile();
        } catch (Exception e) {
            showErrorDialog("Error saving text: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    //normalize file
    private void normalizeFile() throws Exception {
        if (!Files.exists(FILE_PATH)) return;

        List<String> lines;
        if (encrypted) {
            lines = new ArrayList<>(getLines());
        } else {
            lines = Files.readAllLines(FILE_PATH);
        }
        List<String> normalized = getNormalized(lines);

        if (encrypted) {
            cachedLines = new ArrayList<>(normalized);
            String fullText = String.join("\n", cachedLines);
            SecretKey key = EncryptionManager.deriveKey(password, salt);
            byte[] encryptedData = EncryptionManager.encrypt(fullText, key);
            Files.write(FILE_PATH, encryptedData);
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
                    updatedLines.add(""); // ensure a blank line after the entry
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

            if (encrypted) {
                cachedLines = new ArrayList<>(updatedLines);
                String fullText = String.join("\n", cachedLines);
                SecretKey key = EncryptionManager.deriveKey(password, salt);
                byte[] encryptedData = EncryptionManager.encrypt(fullText, key);
                Files.write(FILE_PATH, encryptedData);
            } else {
                Files.write(FILE_PATH, updatedLines);
            }
        } catch (Exception e) {
            showErrorDialog("Error updating log entry: " + e.getMessage());
        }
    }

    public void changeTimestamp(String oldTimestamp, String newTimestamp) {
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

            if (encrypted) {
                cachedLines = new ArrayList<>(lines);
                String fullText = String.join("\n", cachedLines);
                SecretKey key = EncryptionManager.deriveKey(password, salt);
                byte[] encryptedData = EncryptionManager.encrypt(fullText, key);
                Files.write(FILE_PATH, encryptedData);
            } else {
                Files.write(FILE_PATH, lines);
            }
            normalizeFile();
        } catch (Exception e) {
            showErrorDialog("Error changing timestamp: " + e.getMessage());
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

            if (encrypted) {
                cachedLines = new ArrayList<>(normalized);
                String fullText = String.join("\n", cachedLines);
                SecretKey key = EncryptionManager.deriveKey(password, salt);
                byte[] encryptedData = EncryptionManager.encrypt(fullText, key);
                Files.write(FILE_PATH, encryptedData);
            } else {
                Files.write(FILE_PATH, normalized);
            }
            listModel.removeElement(timeStamp);
        } catch (Exception e) {
            showErrorDialog("Error deleting log entry: " + e.getMessage());
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
        List<List<String>> entries = new ArrayList<>();
        List<String> currentEntry = new ArrayList<>();
        java.util.regex.Pattern tsPattern = java.util.regex.Pattern.compile("^\\d{2}:\\d{2} \\d{4}-\\d{2}-\\d{2}( \\([0-9]+\\))?$", java.util.regex.Pattern.MULTILINE);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equalsIgnoreCase(".LOG")) continue;
            if (tsPattern.matcher(line).matches()) {
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
            if (!entry.isEmpty() && tsPattern.matcher(entry.get(0)).matches()) {
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

        // Flatten back to lines
        List<String> sortedLines = new ArrayList<>();
        for (List<String> entry : sortedEntries) {
            sortedLines.addAll(entry);
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

    private int getDuplicateCount(String timeStamp) {
        if (!Files.exists(FILE_PATH)) return 0;

        try {
            List<String> lines = getLines();
            return (int) lines.stream()
                .filter(line -> line.startsWith(timeStamp))
                .count();
        } catch (Exception e) {
            showErrorDialog("Error checking duplicates: " + e.getMessage());
            return 0;
        }
    }    public List<String> getLines() throws Exception {
        if (encrypted) {
            if (cachedLines == null) {
                byte[] data = Files.readAllBytes(FILE_PATH);
                SecretKey key = EncryptionManager.deriveKey(password, salt);
                String decrypted = EncryptionManager.decrypt(data, key);
                cachedLines = Arrays.stream(decrypted.split("\n")).map(String::trim).collect(Collectors.toList());
            }
            return cachedLines;
        } else {
            List<String> lines = Files.readAllLines(FILE_PATH);
            if (!lines.isEmpty() && lines.get(0).trim().equals(".LOG")) {
                lines.remove(0);
            }
            return lines.stream().map(String::trim).collect(Collectors.toList());
        }
    }

    public void enableEncryption(char[] pwd) throws Exception {
        this.salt = EncryptionManager.generateSalt();
        List<String> lines = Files.readAllLines(FILE_PATH);
        if (!lines.isEmpty() && lines.get(0).trim().equals(".LOG")) {
            lines.remove(0);
        }
        String fullText = String.join("\n", lines);
        SecretKey key = EncryptionManager.deriveKey(pwd, this.salt);
        byte[] encrypted = EncryptionManager.encrypt(fullText, key);
        // Save encrypted to backup first
        Path backupPath = FILE_PATH.resolveSibling(FILE_PATH.getFileName().toString() + ".bak");
        Files.write(backupPath, encrypted);
        // Then save to main file
        Files.write(FILE_PATH, encrypted);
        setEncryption(pwd, this.salt);
        cachedLines = new ArrayList<>(lines);
    }

    private void sortListModel(DefaultListModel<String> listModel) {
        List<String> sortedEntries = Collections.list(listModel.elements()).stream()
                .sorted((a, b) -> {
                    try {
                        return entryLoader.parseDate(b).compareTo(entryLoader.parseDate(a));
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

    public void setEncryption(char[] pwd, byte[] slt) {
        // Clear old sensitive data before setting new
        if (password != null) {
            Arrays.fill(password, '\0');
        }
        if (salt != null) {
            Arrays.fill(salt, (byte) 0);
        }
        if (cachedLines != null) {
            cachedLines.clear();
            cachedLines = null;
        }
        this.password = pwd.clone();
        this.salt = slt.clone();
        this.encrypted = true;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public char[] getPassword() {
        return password;
    }

    public byte[] getSalt() {
        return salt;
    }

    public Path getFilePath() {
        return FILE_PATH;
    }



    public void deleteEntry(String selectedItem, DefaultListModel<String> listModel) {
        if (selectedItem != null && !selectedItem.isBlank()) {
            deleteLogEntry(selectedItem, listModel);
        } else {
            showErrorDialog("No entry selected for deletion.");
        }
    }

    public List<String> getRecentLogEntries(int i) {
        return entryLoader.getRecentLogEntries(i);
    }

    public void clearSensitiveData() {
        if (password != null) {
            Arrays.fill(password, '\0');
            password = null;
        }
        if (salt != null) {
            Arrays.fill(salt, (byte) 0);
            salt = null;
        }
        if (cachedLines != null) {
            cachedLines.clear();
            cachedLines = null;
        }
        encrypted = false;
    }

    public void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}