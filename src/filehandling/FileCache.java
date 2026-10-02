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

package filehandling;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages caching for log file operations.
 * Handles both line-level cache (for encrypted files) and parsed entry cache.
 */
public class FileCache {
    private long generation;
    private long securityGeneration;
    public synchronized long securityGeneration() { return securityGeneration; }
    public synchronized boolean isSecurityCurrent(long token) { return token == securityGeneration; }
    public synchronized long generation() { return generation; }
    public synchronized boolean updateCachedLinesIfCurrent(List<String> lines, long token) {
        if (generation != token) return false;
        updateCachedLines(lines);
        return true;
    }
    // Cache management for encrypted files
    private List<String> cachedLines = new ArrayList<>();
    private List<List<String>> cachedEntries;
    private long cachedEntriesLastModified;
    
    // Write-back cache for performance
    private boolean isDirty;
    private List<String> pendingLines;
    private long lastWriteTime;
    private static final long WRITE_DELAY_MS = 2000; // 2 second delay before auto-flush
    
    /**
     * Gets cached lines (for encrypted files).
     */
    public synchronized List<String> getCachedLines() {
        return new ArrayList<>(cachedLines);
    }
    
    /**
     * Updates the cached lines.
     */
    public synchronized void updateCachedLines(List<String> lines) {
        this.cachedLines = new ArrayList<>(lines);
    }
    
    /**
     * Clears the cached lines.
     */
    public synchronized void clearCachedLines() {
        generation++;
        this.cachedLines.clear();
    }
    
    /**
     * Gets cached parsed entries.
     */
    public synchronized List<List<String>> getCachedEntries() {
        return copyEntries(cachedEntries);
    }
    
    /**
     * Sets cached parsed entries with timestamp.
     */
    public synchronized void setCachedEntries(List<List<String>> entries, long lastModified) {
        this.cachedEntries = copyEntries(entries);
        this.cachedEntriesLastModified = lastModified;
    }

    private static List<List<String>> copyEntries(List<List<String>> entries) {
        if (entries == null) return null;
        List<List<String>> copy = new ArrayList<>(entries.size());
        for (List<String> entry : entries) copy.add(entry == null ? null : new ArrayList<>(entry));
        return copy;
    }
    
    /**
     * Gets the last modified timestamp of cached entries.
     */
    public synchronized long getCachedEntriesLastModified() {
        return cachedEntriesLastModified;
    }
    
    /**
     * Invalidates the entry cache.
     */
    public synchronized void invalidateEntryCache() {
        this.cachedEntries = null;
        this.cachedEntriesLastModified = 0;
    }
    
    /**
     * Invalidates all caches.
     */
    public synchronized void invalidateCaches() {
        invalidateEntryCache();
        clearCachedLines();
    }

    /**
     * Drops cached references and invalidates in-flight publications on lock.
     * This does not guarantee physical erasure of immutable strings in the JVM.
     */
    public synchronized void secureClear() {
        generation++;
        securityGeneration++;
        cachedLines = new ArrayList<>();
        cachedEntries = null;
        cachedEntriesLastModified = 0;
        
        pendingLines = null;
        isDirty = false;
        lastWriteTime = 0L;
    }

    /**
     * Sets pending lines for write-back cache.
     */
    public synchronized void setPendingLines(List<String> lines) {
        this.pendingLines = lines == null ? null : new ArrayList<>(lines);
        this.isDirty = true;
        this.lastWriteTime = System.currentTimeMillis();
    }
    
    /**
     * Gets pending lines.
     */
    public synchronized List<String> getPendingLines() {
        return pendingLines == null ? null : new ArrayList<>(pendingLines);
    }
    
    /**
     * Clears pending writes.
     */
    public synchronized void clearPendingWrites() {
        this.pendingLines = null;
        this.isDirty = false;
        this.lastWriteTime = 0;
    }
    
    /**
     * Checks if there are pending writes.
     */
    public synchronized boolean hasPendingWrites() {
        return isDirty && pendingLines != null;
    }
    
    /**
     * Checks if write delay has elapsed.
     */
    public synchronized boolean isWriteDelayElapsed() {
        return isDirty && (System.currentTimeMillis() - lastWriteTime) >= WRITE_DELAY_MS;
    }
}
