package filehandling;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StalePublicationRegressionTest {
    @TempDir Path directory;

    @Test void queuedMissingFileFilterClearsCannotEraseNewSessionModel() throws Exception {
        var handler = new LogFileHandler(directory.resolve("missing.txt"), encryption.EncryptionManager.getInstance());
        var loader = handler.getEntryLoader();
        var model = new DefaultListModel<String>();
        SwingUtilities.invokeAndWait(() -> {
            loader.loadFilteredEntries(model, 2026, 10);
            loader.loadFilteredEntriesByYear(model, 2026);
            loader.invalidateCaches();
            model.addElement("fresh session");
        });
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(List.of("fresh session"), java.util.Collections.list(model.elements()));
    }

    @Test void queuedFullListPublicationCannotReplaceNewSessionModel() throws Exception {
        Path file = directory.resolve("model.txt");
        Files.writeString(file, "placeholder");
        var handler = new LogFileHandler(file, encryption.EncryptionManager.getInstance()) {
            @Override public List<String> getLines() {
                return List.of("13:00 2026-09-11", "private entry");
            }
        };
        var loader = handler.getEntryLoader();
        var model = new DefaultListModel<String>();
        SwingUtilities.invokeAndWait(() -> {
            try { loader.loadLogEntries(model); } catch (Exception e) { throw new AssertionError(e); }
            loader.invalidateCaches();
            model.addElement("fresh session");
        });
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(List.of("fresh session"), java.util.Collections.list(model.elements()));
    }

    @Test void queuedNoWorkCompletionsCannotResumeAfterCacheInvalidation() throws Exception {
        var cache = new FileCache();
        var manager = new encryption.FileEncryptionManager(directory.resolve("save.txt"),
            encryption.EncryptionManager.getInstance());
        var saver = new AsyncSaver(directory.resolve("save.txt"), manager,
            new EntryEditor(directory.resolve("save.txt"), manager, cache), cache, null);
        AtomicInteger callbacks = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            saver.runWithProgressAsync("Saving", "Saving", null, callbacks::incrementAndGet);
            saver.flushPendingWritesAsync(callbacks::incrementAndGet);
            cache.secureClear();
        });
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(0, callbacks.get());
        saver.runWithProgressAsync("Saving", "Saving", null, callbacks::incrementAndGet);
        saver.flushPendingWritesAsync(callbacks::incrementAndGet);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(2, callbacks.get(), "fresh completions must remain usable");
    }

    @Test void postSavePublicationAlsoHonorsItsCallingSession() throws Exception {
        Path file = directory.resolve("post-save.txt");
        Files.writeString(file, "placeholder");
        var handler = new LogFileHandler(file, encryption.EncryptionManager.getInstance()) {
            @Override public List<String> getLines() {
                return List.of("13:00 2026-09-11", "private entry");
            }
        };
        var allowed = new java.util.concurrent.atomic.AtomicBoolean(true);
        var model = new DefaultListModel<String>();
        SwingUtilities.invokeAndWait(() -> {
            try { handler.getEntryLoader().loadLogEntries(model, allowed::get); }
            catch (Exception e) { throw new AssertionError(e); }
            allowed.set(false);
            model.addElement("fresh session");
        });
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(List.of("fresh session"), java.util.Collections.list(model.elements()));
    }

    @Test void staleSaveCannotRunPostSaveWorkOrCompletion() throws Exception {
        var cache = new FileCache();
        Path file = directory.resolve("async.txt");
        var manager = new encryption.FileEncryptionManager(file, encryption.EncryptionManager.getInstance());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Thread> worker = new AtomicReference<>();
        var entryEditor = new EntryEditor(file, manager, cache) {
            @Override String createAndSaveEntry(String text, boolean encrypted, long session) throws Exception {
                worker.set(Thread.currentThread());
                entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return "13:00 2026-09-11";
            }
        };
        var saver = new AsyncSaver(file, manager, entryEditor, cache, null);
        AtomicInteger postSave = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        try {
            saver.saveTextAsync("private entry", new DefaultListModel<>(), postSave::incrementAndGet,
                completed::incrementAndGet);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            cache.secureClear();
            release.countDown();
            worker.get().join(5000);
            assertFalse(worker.get().isAlive());
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(0, postSave.get());
            assertEquals(0, completed.get());
        } finally { release.countDown(); }
    }
}
