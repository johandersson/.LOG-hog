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
        if (text == null || text.isBlank()) return;
        new Thread(() -> {
            final gui.LoadingProgressDialog[] holder = new gui.LoadingProgressDialog[1];
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    holder[0] = new gui.LoadingProgressDialog(null, "Saving");
                    holder[0].setStatus("Saving file...");
                    holder[0].setIndeterminate(true);
                    holder[0].show();
                });
            } catch (Exception e) {
                // ignore
            }

            String ts = null;
            try {
                entryEditor.setBackupManager(backupManager);
                ts = entryEditor.createAndSaveEntry(text);
                cache.invalidateEntryCache();
            } catch (Exception e) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    filehandling.DialogHandler.showErrorDialog("<html><b>💾 Save Failed</b><br><br>Unable to save your log entry.</html>");
                });
            } finally {
                gui.LoadingProgressDialog progress = holder[0];
                if (progress != null) {
                    try { progress.close(); } catch (Exception ignore) {}
                }
            }

            if (ts != null) {
                final String added = ts;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    listModel.addElement(added);
                    // sort not accessible; caller should manage
                    if (onComplete != null) onComplete.run();
                });
            } else {
                if (onComplete != null) javax.swing.SwingUtilities.invokeLater(onComplete);
            }
        }, "loghog-save-thread").start();
    }

    public void flushPendingWritesAsync(Runnable onComplete) {
        if (!cache.hasPendingWrites()) {
            if (onComplete != null) javax.swing.SwingUtilities.invokeLater(onComplete);
            return;
        }

        new Thread(() -> {
            final gui.LoadingProgressDialog[] holder = new gui.LoadingProgressDialog[1];
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    holder[0] = new gui.LoadingProgressDialog(null, "Saving");
                    holder[0].setStatus("Saving file...");
                    holder[0].setIndeterminate(true);
                    holder[0].show();
                });
            } catch (Exception e) {
                // ignore
            }

            try {
                List<String> pendingLines = cache.getPendingLines();
                    if (encryptionManager.isEncrypted()) {
                        cache.updateCachedLines(pendingLines);
                        if (backupManager != null) backupManager.createNumberedBackup();
                        if (encryptionManager != null) {
                            encryptionManager.encryptFileFromLines(cache.getCachedLines());
                        }
                } else {
                    if (backupManager != null) backupManager.createNumberedBackup();
                    Files.write(filePath, pendingLines);
                }
                cache.clearPendingWrites();
                } catch (Exception e) {
                javax.swing.SwingUtilities.invokeLater(() -> filehandling.DialogHandler.showErrorDialog("<html><b>💾 Write Failed</b><br><br>Unable to save changes to disk.</html>"));
            } finally {
                gui.LoadingProgressDialog progress = holder[0];
                if (progress != null) {
                    try { progress.close(); } catch (Exception ignore) {}
                }
            }

            if (onComplete != null) javax.swing.SwingUtilities.invokeLater(onComplete);
        }, "loghog-flush-thread").start();
    }
}
