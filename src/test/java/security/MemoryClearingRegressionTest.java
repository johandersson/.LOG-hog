package security;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import clipboard.SecureClipboardManager;
import encryption.EncryptionManager;
import filehandling.FullLogFileLoader;
import filehandling.LogFileHandler;
import gui.EntryPanel;
import gui.HighlightableTextPane;
import utils.UndoRedoTextArea;

class MemoryClearingRegressionTest {
    @TempDir Path directory;

    @Test void lockedEntryCannotRecoverSecretAfterUnlockByUndo() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EntryPanel panel = new EntryPanel(null);
            panel.getTextArea().setText("private entry");
            panel.setLocked(true);
            panel.setLocked(false);
            finishEdit(panel.getTextArea());
            for (int i = 0; i < 5; i++) {
                panel.getTextArea().getActionMap().get("Undo").actionPerformed(null);
                assertEquals("", panel.getTextArea().getText());
            }
        });
    }

    @Test void undoCannotModifyReadOnlyAreaButStillWorksWhenEditable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UndoRedoTextArea area = new UndoRedoTextArea();
            area.setText("private entry");
            finishEdit(area);
            area.setEditable(false);
            area.getActionMap().get("Undo").actionPerformed(null);
            assertEquals("private entry", area.getText());
            area.setEditable(true);
            area.getActionMap().get("Undo").actionPerformed(null);
            assertEquals("", area.getText());
            area.getActionMap().get("Redo").actionPerformed(null);
            assertEquals("private entry", area.getText());
        });
    }

    @Test void invalidationDuringFullLogParseCannotRepublishPlaintextCache() throws Exception {
        Path file = directory.resolve("log.txt");
        Files.writeString(file, "placeholder");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        LogFileHandler handler = new LogFileHandler(file, EncryptionManager.getInstance()) {
            @Override public boolean isEncrypted() { return true; }
            @Override public List<String> getLines() {
                entered.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                return List.of("13:00 2026-09-11", "private entry");
            }
        };
        FullLogFileLoader loader = new FullLogFileLoader(handler, new HighlightableTextPane());
        var executor = Executors.newSingleThreadExecutor();
        try {
            var work = executor.submit(() -> loader.parseLogFile(file));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            loader.invalidateCache();
            release.countDown();
            try { work.get(5, TimeUnit.SECONDS); } catch (java.util.concurrent.ExecutionException expected) { }
            assertNull(field(loader, "cachedParsedData"));
            assertNotNull(loader.parseLogFile(file), "fresh loads must remain possible");
        } finally { release.countDown(); executor.shutdownNow(); handler.clearSensitiveData(); }
    }

    @Test void staleFullLogWorkerCannotRenderAfterLockThenUnlock() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        Path file = directory.resolve("worker.txt");
        Files.writeString(file, "placeholder");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        LogFileHandler handler = new LogFileHandler(file, EncryptionManager.getInstance()) {
            @Override public boolean isEncrypted() { return true; }
            @Override public List<String> getLines() {
                entered.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                return List.of("13:00 2026-09-11", "private entry");
            }
        };
        var editor = editorWithoutStartup(handler);
        var panel = new gui.FullLogPanel(editor, handler);
        try {
            panel.loadFullLog();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            editor.setLocked(true);
            editor.setLocked(false);
            release.countDown();
            awaitEdt(250);
            assertFalse(panel.getFullLogPane().getText().contains("private entry"));
            panel.loadFullLog();
            for (int i = 0; i < 20 && !panel.getFullLogPane().getText().contains("private entry"); i++) awaitEdt(50);
            assertTrue(panel.getFullLogPane().getText().contains("private entry"), "fresh unlock load must render");
        } finally { release.countDown(); panel.dispose(); handler.clearSensitiveData(); }
    }

    @Test void startupFilteredLoaderCannotContinueAfterLockThenUnlock() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var worker = new java.util.concurrent.atomic.AtomicReference<Thread>();
        var fallbacks = new java.util.concurrent.atomic.AtomicInteger();
        Runnable block = () -> {
            worker.set(Thread.currentThread());
            entered.countDown();
            try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
            catch (InterruptedException e) { throw new AssertionError(e); }
        };
        var handler = new LogFileHandler(directory.resolve("startup.txt"), EncryptionManager.getInstance()) {
            @Override public void loadFilteredEntries(javax.swing.DefaultListModel<String> model, int year, int month) {
                block.run();
            }
            @Override public filehandling.EntryLoader getEntryLoader() {
                return new filehandling.EntryLoader(this) {
                    @Override public List<String> computeTimestampsByYearMonth(int year, int month) {
                        block.run();
                        return List.of();
                    }
                };
            }
            @Override public List<String> getRecentLogEntries(int count) {
                fallbacks.incrementAndGet();
                return List.of();
            }
        };
        var editor = editorWithoutStartup(handler);
        var initializer = new main.UIInitializer(editor, new javax.swing.JTabbedPane(), List.of(),
            new java.util.Properties());
        var progress = new gui.LoadingProgressDialog(null, "Loading");
        var timer = new javax.swing.Timer(100, event -> {});
        try {
            var start = main.UIInitializer.class.getDeclaredMethod("startFilteredEntriesLoader",
                javax.swing.DefaultListModel.class, int.class, int.class, gui.LoadingProgressDialog.class,
                javax.swing.Timer.class);
            start.setAccessible(true);
            SwingUtilities.invokeAndWait(() -> {
                try { start.invoke(initializer, new javax.swing.DefaultListModel<String>(), 2026, 10, progress, timer); }
                catch (Exception e) { throw new AssertionError(e); }
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            editor.setLocked(true);
            editor.setLocked(false);
            release.countDown();
            worker.get().join(5000);
            assertFalse(worker.get().isAlive());
            assertEquals(0, fallbacks.get(), "old loader must not begin fallback work in the new session");
        } finally { release.countDown(); timer.stop(); progress.close(); }
    }

    @Test void invalidatedEntryHydrationCannotRepublishCache() throws Exception {
        Path file = directory.resolve("hydration.txt");
        Files.writeString(file, "placeholder");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var manager = new encryption.FileEncryptionManager(file, EncryptionManager.getInstance()) {
            @Override public List<String> decryptFileToLines() throws Exception {
                entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return List.of("13:00 2026-09-11", "private entry");
            }
        };
        var cache = new filehandling.FileCache();
        var entryEditor = new filehandling.EntryEditor(file, manager, cache);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var work = executor.submit(() -> entryEditor.getMergedEncryptedWorkingLines());
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            cache.secureClear();
            release.countDown();
            assertThrows(java.util.concurrent.ExecutionException.class, () -> work.get(5, TimeUnit.SECONDS));
            assertTrue(cache.getCachedLines().isEmpty());
            assertFalse(entryEditor.getMergedEncryptedWorkingLines().isEmpty());
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test void discardedHistoryPreservesDocumentListenersAndLengthFilter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var area = new UndoRedoTextArea();
            var document = (javax.swing.text.AbstractDocument) area.getDocument();
            var filter = new gui.LengthLimitFilter(10);
            document.setDocumentFilter(filter);
            java.util.concurrent.atomic.AtomicInteger changes = new java.util.concurrent.atomic.AtomicInteger();
            document.addDocumentListener(new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e) { changes.incrementAndGet(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { changes.incrementAndGet(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { changes.incrementAndGet(); }
            });
            area.setText("secret");
            area.clearSensitiveData();
            assertSame(document, area.getDocument());
            assertSame(filter, document.getDocumentFilter());
            int before = changes.get();
            area.append("legitimate");
            assertTrue(changes.get() > before);
            area.getActionMap().get("Undo").actionPerformed(null);
            assertEquals("", area.getText());
            area.getActionMap().get("Undo").actionPerformed(null);
            assertEquals("", area.getText());
        });
    }

    private static void awaitEdt(int delay) throws Exception {
        CountDownLatch tick = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            var timer = new javax.swing.Timer(delay, e -> tick.countDown());
            timer.setRepeats(false);
            timer.start();
        });
        assertTrue(tick.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {});
    }

    @Test void failedKeySetupCannotRetainPartiallyInstalledSessionKey() throws Exception {
        encryption.Encryptor encryptor = new encryption.Encryptor() {
            public byte[] generateSalt() { return new byte[16]; }
            public javax.crypto.SecretKey deriveKey(char[] password, byte[] salt) {
                return new javax.crypto.spec.SecretKeySpec(new byte[32], "AES");
            }
            public byte[] encrypt(String text, char[] password, byte[] salt) { return new byte[0]; }
            public String decrypt(byte[] data, char... password) { return ""; }
        };
        var manager = new encryption.FileEncryptionManager(directory.resolve("log.txt"), encryptor);
        assertThrows(Exception.class, () -> manager.setEncryption("password".toCharArray(), null));
        assertNull(field(manager, "sessionKeyBytes"));
        assertNull(field(manager, "backupHmacKeyBytes"));
        assertFalse(manager.isEncrypted());
    }

    @Test void everyLockedTransitionWipesInstalledKeysEvenDuringStartup() throws Exception {
        var handler = new LogFileHandler(directory.resolve("keys.txt"), new encryption.TestableEncryptionManager());
        handler.setEncryption("password".toCharArray(), new byte[16]);
        main.LogTextEditor editor = editorWithoutStartup(handler);
        try {
            try { editor.setLocked(true); } catch (NullPointerException startupFailure) { }
            assertTrue(editor.isLocked());
            assertNull(field(handler.getEncryptionManager(), "sessionKeyBytes"));
            assertNull(field(handler.getEncryptionManager(), "backupHmacKeyBytes"));
        } finally { handler.clearSensitiveData(); }
    }

    static main.LogTextEditor editorWithoutStartup(LogFileHandler handler) throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        var editor = (main.LogTextEditor) unsafe.allocateInstance(main.LogTextEditor.class);
        for (var setting : java.util.Map.of("lockObject", new Object(),
                "logFileHandler", handler, "listModel", new javax.swing.DefaultListModel<>()).entrySet()) {
            Field f = main.LogTextEditor.class.getDeclaredField(setting.getKey());
            f.setAccessible(true);
            f.set(editor, setting.getValue());
        }
        return editor;
    }

    @Test void sensitiveDialogCleanupClearsSecretsAndInvalidatesContinuation() throws Exception {
        Class<?> registry = Class.forName("security.SensitiveWindowRegistry");
        Object state = registry.getConstructor().newInstance();
        var register = registry.getMethod("register", Runnable.class);
        var invalidate = registry.getMethod("invalidate");
        var generation = registry.getMethod("generation");
        var current = registry.getMethod("isCurrent", long.class);
        javax.swing.JTextField secret = new javax.swing.JTextField("generated secret");
        long token = (long) generation.invoke(state);
        register.invoke(state, (Runnable) () -> secret.setText(""));
        invalidate.invoke(state);
        assertEquals("", secret.getText());
        assertEquals(false, current.invoke(state, token));
        assertEquals(true, current.invoke(state, generation.invoke(state)));
    }

    @Test void failedNativeClearRetainsTrackingUntilSuccessfulRetry() throws Exception {
        Clipboard nativeClipboard = new Clipboard("regression") {
            boolean fail = true;
            @Override public synchronized void setContents(java.awt.datatransfer.Transferable data,
                    java.awt.datatransfer.ClipboardOwner owner) {
                if (fail) { fail = false; throw new IllegalStateException("busy"); }
                super.setContents(data, owner);
            }
        };
        // The first set simulates a transient native failure; populate via the second.
        assertThrows(IllegalStateException.class, () -> nativeClipboard.setContents(new StringSelection("private"), null));
        nativeClipboard.setContents(new StringSelection("private"), null);
        var provider = SecureClipboardManager.class.getDeclaredField("clipboardProvider");
        provider.setAccessible(true);
        Object previous = provider.get(null);
        try {
            provider.set(null, (Supplier<Clipboard>) () -> { throw new IllegalStateException("busy"); });
            track("private");
            SecureClipboardManager.onLock();
            assertNotNull(staticField("lastCopiedDigest"), "failed native access must not forget the secret");
            provider.set(null, (Supplier<Clipboard>) () -> nativeClipboard);
            SecureClipboardManager.clearSecureClipboard();
            assertEquals("", nativeClipboard.getData(DataFlavor.stringFlavor));
            assertNull(staticField("lastCopiedDigest"));
            nativeClipboard.setContents(new StringSelection("replacement"), null);
            track("private");
            SecureClipboardManager.onLock();
            assertEquals("replacement", nativeClipboard.getData(DataFlavor.stringFlavor));
        } finally { provider.set(null, previous); }
    }

    @Test void failedNativeWriteRetriesAutomaticallyEvenWhenAutoClearIsDisabled() throws Exception {
        java.util.concurrent.atomic.AtomicBoolean busy = new java.util.concurrent.atomic.AtomicBoolean();
        Clipboard nativeClipboard = new Clipboard("retry") {
            @Override public synchronized void setContents(java.awt.datatransfer.Transferable value,
                    java.awt.datatransfer.ClipboardOwner owner) {
                if (busy.getAndSet(false)) throw new IllegalStateException("busy");
                super.setContents(value, owner);
            }
        };
        nativeClipboard.setContents(new StringSelection("private"), null);
        Field provider = SecureClipboardManager.class.getDeclaredField("clipboardProvider");
        provider.setAccessible(true);
        Object previous = provider.get(null);
        boolean autoClear = SecureClipboardManager.isAutoClearEnabled();
        try {
            provider.set(null, (Supplier<Clipboard>) () -> nativeClipboard);
            SecureClipboardManager.setAutoClearEnabled(false);
            track("private");
            busy.set(true);
            SecureClipboardManager.onLock();
            assertNotNull(staticField("lastCopiedDigest"));
            for (int i = 0; i < 60 && staticField("lastCopiedDigest") != null; i++) Thread.sleep(50);
            assertNull(staticField("lastCopiedDigest"));
            assertEquals("", nativeClipboard.getData(DataFlavor.stringFlavor));
        } finally {
            SecureClipboardManager.clearSecureClipboard();
            SecureClipboardManager.setAutoClearEnabled(autoClear);
            provider.set(null, previous);
        }
    }

    @Test void nativeSuccessWithoutActualReplacementCannotForgetSecret() throws Exception {
        java.util.concurrent.atomic.AtomicBoolean ignoreWrites = new java.util.concurrent.atomic.AtomicBoolean();
        Clipboard clipboard = new Clipboard("unconfirmed") {
            @Override public synchronized void setContents(java.awt.datatransfer.Transferable value,
                    java.awt.datatransfer.ClipboardOwner owner) {
                if (!ignoreWrites.get()) super.setContents(value, owner);
            }
        };
        clipboard.setContents(new StringSelection("private"), null);
        Field provider = SecureClipboardManager.class.getDeclaredField("clipboardProvider");
        provider.setAccessible(true);
        Object previous = provider.get(null);
        try {
            provider.set(null, (Supplier<Clipboard>) () -> clipboard);
            track("private");
            ignoreWrites.set(true);
            SecureClipboardManager.onLock();
            assertNotNull(staticField("lastCopiedDigest"));
            assertEquals("private", clipboard.getData(DataFlavor.stringFlavor));
            ignoreWrites.set(false);
            SecureClipboardManager.clearSecureClipboard();
            assertNull(staticField("lastCopiedDigest"));
        } finally { ignoreWrites.set(false); SecureClipboardManager.clearSecureClipboard(); provider.set(null, previous); }
    }

    @Test void shutdownFailurePreservesRecoveryMarkerAndRecoveryDoesNotEraseReplacement() throws Exception {
        Field provider = SecureClipboardManager.class.getDeclaredField("clipboardProvider");
        provider.setAccessible(true);
        Object previous = provider.get(null);
        Field markerField = SecureClipboardManager.class.getDeclaredField("CLIPBOARD_MARKER");
        markerField.setAccessible(true);
        Path marker = (Path) markerField.get(null);
        Clipboard clipboard = new Clipboard("recovery");
        try {
            provider.set(null, (Supplier<Clipboard>) () -> { throw new IllegalStateException("busy"); });
            track("private");
            SecureClipboardManager.shutdown();
            assertNotNull(staticField("lastCopiedDigest"));
            assertTrue(Files.exists(marker));
            assertFalse(((java.util.concurrent.ScheduledExecutorService) staticField("scheduler")).isShutdown());
            var task = (java.util.concurrent.ScheduledFuture<?>) staticField("clearTask");
            task.cancel(false);
            setStatic("clearTask", null);
            setStatic("lastCopiedDigest", null);
            setStatic("weOwnClipboard", false);
            clipboard.setContents(new StringSelection("genuine replacement"), null);
            provider.set(null, (Supplier<Clipboard>) () -> clipboard);
            SecureClipboardManager.recoverClipboardAfterCrash();
            assertEquals("genuine replacement", clipboard.getData(DataFlavor.stringFlavor));
            assertFalse(Files.exists(marker));
        } finally {
            SecureClipboardManager.clearSecureClipboard();
            provider.set(null, previous);
        }
    }

    private static void track(String text) throws Exception {
        setStatic("lastCopiedDigest", java.security.MessageDigest.getInstance("SHA-256")
            .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        setStatic("weOwnClipboard", true);
    }
    private static Object staticField(String name) throws Exception {
        Field f = SecureClipboardManager.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }
    private static void setStatic(String name, Object value) throws Exception {
        Field f = SecureClipboardManager.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }
    private static Object field(Object object, String name) throws Exception {
        Field f = object.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(object);
    }
    private static void finishEdit(javax.swing.JTextArea area) {
        try {
            var filter = ((javax.swing.text.AbstractDocument) area.getDocument()).getDocumentFilter();
            var finish = filter.getClass().getDeclaredMethod("endCompoundEdit");
            finish.setAccessible(true);
            finish.invoke(filter);
        } catch (Exception e) { throw new AssertionError(e); }
    }
}
