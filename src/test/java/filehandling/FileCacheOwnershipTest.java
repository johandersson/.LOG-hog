package filehandling;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class FileCacheOwnershipTest {
    @Test void cachedEntriesSetterOwnsNestedLists() {
        FileCache cache = new FileCache();
        List<String> entry = new ArrayList<>(List.of("private entry"));
        List<List<String>> entries = new ArrayList<>(List.of(entry));
        cache.setCachedEntries(entries, 42L);
        entry.set(0, "caller replacement");
        entries.clear();
        assertEquals(List.of(List.of("private entry")), cache.getCachedEntries());
        assertEquals(42L, cache.getCachedEntriesLastModified());
    }

    @Test void cachedEntriesGetterReturnsIndependentNestedSnapshot() {
        FileCache cache = new FileCache();
        cache.setCachedEntries(new ArrayList<>(List.of(new ArrayList<>(List.of("private entry")))), 42L);
        List<List<String>> snapshot = cache.getCachedEntries();
        snapshot.get(0).set(0, "snapshot replacement");
        snapshot.clear();
        assertEquals(List.of(List.of("private entry")), cache.getCachedEntries());
    }

    @Test void pendingLinesSetterAndGetterDoNotShareMutableLists() {
        FileCache cache = new FileCache();
        List<String> caller = new ArrayList<>(List.of("private pending entry"));
        cache.setPendingLines(caller);
        caller.clear();
        assertEquals(List.of("private pending entry"), cache.getPendingLines());
        List<String> snapshot = cache.getPendingLines();
        snapshot.clear();
        assertEquals(List.of("private pending entry"), cache.getPendingLines());
        assertTrue(cache.hasPendingWrites());
    }

    @Test void secureClearAcceptsImmutableInputsWithoutMutatingCallerOwnership() {
        FileCache cache = new FileCache();
        List<List<String>> entries = List.of(List.of("private entry"));
        List<String> pending = List.of("private pending entry");
        cache.setCachedEntries(entries, 42L);
        cache.setPendingLines(pending);
        cache.updateCachedLines(List.of("private line"));
        long generation = cache.generation();
        long session = cache.securityGeneration();
        assertDoesNotThrow(cache::secureClear);
        assertNull(cache.getCachedEntries());
        assertNull(cache.getPendingLines());
        assertTrue(cache.getCachedLines().isEmpty());
        assertFalse(cache.hasPendingWrites());
        assertEquals(0L, cache.getCachedEntriesLastModified());
        assertFalse(cache.isSecurityCurrent(session));
        assertFalse(cache.updateCachedLinesIfCurrent(List.of("stale private line"), generation));
        assertEquals(List.of(List.of("private entry")), entries);
        assertEquals(List.of("private pending entry"), pending);
    }
}
