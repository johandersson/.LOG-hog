package filehandling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import encryption.FileEncryptionManager;
import main.BackupManager;

/**
 * Encapsulates asynchronous save and flush operations so UI code can delegate.
 */
public class AsyncSaver {
    private final Path filePath;
    private final FileEncryptionManager encryptionManager;
    private final EntryEditor entryEditor;
    private final FileCache cache;
    private BackupManager backupManager;
    public void setBackupManager(BackupManager backupManager) {
        this.backupManager = backupManager;
    }

    public AsyncSaver(Path filePath, FileEncryptionManager encryptionManager, EntryEditor entryEditor, FileCache cache, BackupManager backupManager) {
        this.filePath = filePath;
        this.encryptionManager = encryptionManager;
        this.entryEditor = entryEditor;
        this.cache = cache;
        this.backupManager = backupManager;
    }

    public void saveTextAsync(String text, javax.swing.DefaultListModel<String> listModel, Runnable onComplete) {
        saveTextAsync(text, listModel, null, onComplete);
    }

    /**
     * Saves an entry on a background thread while a progress dialog is shown.
     * <p>
     * The dialog stays open until {@code postSaveWork} has finished too, so slow
     * follow-up work (such as reparsing a very large log file to rebuild the entry
     * list) keeps giving the user feedback instead of freezing the UI silently.
     *
     * @param text the entry text to save
     * @param listModel the list model backing the entry list (unused here, kept for API compatibility)
     * @param postSaveWork optional work run on the background thread after the save, may be null
     * @param onComplete optional callback run on the EDT once everything is done, may be null
     */
    public void saveTextAsync(String text, javax.swing.DefaultListModel<String> listModel,
                              Runnable postSaveWork, Runnable onComplete) {
        if (text == null || text.isBlank()) return;
        Thread t = new Thread(() -> {
            gui.BackgroundProgress progress = gui.BackgroundProgress.show("Saving", "Saving entry...");

            try {
                try {
                    entryEditor.setBackupManager(backupManager);
                    entryEditor.createAndSaveEntry(text);
                    cache.invalidateEntryCache();
                } catch (Exception e) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        filehandling.DialogHandler.showErrorDialog("<html><b>💾 Save Failed</b><br><br>Unable to save your log entry.</html>");
                    });
                }

                if (postSaveWork != null) {
                    progress.setStatus("Updating entry list...");
                    postSaveWork.run();
                }
            } finally {
                progress.close();
            }

            // Signal completion - caller handles list refresh for proper occurrence counting
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (onComplete != null) onComplete.run();
            });
        }, "loghog-save-thread");
        t.setDaemon(false);
        t.start();
    }

    /**
     * Runs a potentially slow file operation on a background thread while showing
     * the same progress dialog used for loading large files, so the UI stays
     * responsive and the user can see that work is in progress.
     *
     * @param title dialog title
     * @param status initial status message
     * @param backgroundWork the work to run off the EDT
     * @param onComplete optional callback run on the EDT afterwards, may be null
     */
    public void runWithProgressAsync(String title, String status, Runnable backgroundWork, Runnable onComplete) {
        if (backgroundWork == null) {
            if (onComplete != null) javax.swing.SwingUtilities.invokeLater(onComplete);
            return;
        }
        Thread t = new Thread(() -> {
            gui.BackgroundProgress progress = gui.BackgroundProgress.show(title, status);
            try {
                backgroundWork.run();
            } finally {
                progress.close();
            }
            if (onComplete != null) javax.swing.SwingUtilities.invokeLater(onComplete);
        }, "loghog-progress-task");
        t.setDaemon(false);
        t.start();
    }

    public void flushPendingWritesAsync(Runnable onComplete) {
        if (!cache.hasPendingWrites()) {
            if (onComplete != null) javax.swing.SwingUtilities.invokeLater(onComplete);
            return;
        }

        Thread t2 = new Thread(() -> {
            gui.BackgroundProgress progress = gui.BackgroundProgress.show("Saving", "Saving file...");

            try {
                List<String> pendingLines = cache.getPendingLines();
                    if (encryptionManager.isEncrypted()) {
                        if (backupManager != null) backupManager.createNumberedBackup();
                        if (encryptionManager != null) {
                            encryptionManager.encryptFileFromLines(pendingLines);
                        }
                        // Keep the in-memory hydration cache and incremental journal in sync
                        // with the authoritative content we just wrote, otherwise the next
                        // read could fall back to stale pre-write content.
                        entryEditor.syncAfterFullEncryptedWrite(pendingLines);
                } else {
                    if (backupManager != null) backupManager.createNumberedBackup();
                    Files.write(filePath, pendingLines);
                    try { encryption.CryptoUtils.setOwnerOnlyPermissions(filePath); } catch (Exception ignored) {}
                }
                cache.clearPendingWrites();
                } catch (Exception e) {
                javax.swing.SwingUtilities.invokeLater(() -> filehandling.DialogHandler.showErrorDialog("<html><b>💾 Write Failed</b><br><br>Unable to save changes to disk.</html>"));
            } finally {
                progress.close();
            }

            if (onComplete != null) javax.swing.SwingUtilities.invokeLater(onComplete);
        }, "loghog-flush-thread");
        t2.setDaemon(false);
        t2.start();
    }
}
