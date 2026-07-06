package filehandling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import encryption.FileEncryptionManager;

final class EncryptedIncrementalJournal {
    private static final long COMPACT_THRESHOLD_BYTES = 64L * 1024L;
    private final Path baseFilePath;
    private final FileEncryptionManager baseManager;
    private final Path journalPath;

    EncryptedIncrementalJournal(Path baseFilePath, FileEncryptionManager baseManager) {
        this.baseFilePath = baseFilePath;
        this.baseManager = baseManager;
        this.journalPath = baseFilePath.resolveSibling(baseFilePath.getFileName().toString() + ".journal.enc");
    }

    List<String> readMergedLines() throws Exception {
        List<String> merged = new ArrayList<>();
        merged.addAll(readSnapshotLines());
        merged.addAll(readJournalLines());
        return merged;
    }

    void appendEntryLines(List<String> entryLines) throws Exception {
        if (entryLines == null || entryLines.isEmpty()) {
            return;
        }

        List<String> journalLines = readJournalLines();
        journalLines.addAll(entryLines);
        baseManager.duplicateFor(journalPath).encryptFileFromLines(journalLines);
        compactIfNeeded();
    }

    void compactNow() throws Exception {
        compactIntoSnapshot();
    }

    void clear() {
        try {
            Files.deleteIfExists(journalPath);
        } catch (Exception ignored) {
        }
    }

    private List<String> readSnapshotLines() throws Exception {
        if (!Files.exists(baseFilePath) || Files.size(baseFilePath) == 0L) {
            return new ArrayList<>();
        }
        return stripHeader(baseManager.decryptFileToLines());
    }

    private List<String> readJournalLines() throws Exception {
        if (!Files.exists(journalPath) || Files.size(journalPath) == 0L) {
            return new ArrayList<>();
        }
        return stripHeader(baseManager.duplicateFor(journalPath).decryptFileToLines());
    }

    private void compactIfNeeded() throws Exception {
        if (!Files.exists(journalPath)) {
            return;
        }
        if (Files.size(journalPath) < COMPACT_THRESHOLD_BYTES) {
            return;
        }
        compactIntoSnapshot();
    }

    private void compactIntoSnapshot() throws Exception {
        List<String> merged = readSnapshotLines();
        merged.addAll(readJournalLines());
        if (merged.isEmpty()) {
            return;
        }

        baseManager.encryptFileFromLines(merged);
        clear();
    }

    private List<String> stripHeader(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }

        int start = 0;
        if (".LOG".equalsIgnoreCase(lines.get(0).trim())) {
            start = 1;
            if (lines.size() > 1 && lines.get(1).isBlank()) {
                start = 2;
            }
        }
        return new ArrayList<>(lines.subList(start, lines.size()));
    }
}