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

import java.util.ArrayList;
import java.util.List;

/**
 * Manages caching for log file operations.
 * Handles both line-level cache (for encrypted files) and parsed entry cache.
 */
public class FileCache {
    // Cache management for encrypted files
    private List<String> cachedLines = new ArrayList<>();
    private List<List<String>> cachedEntries = null;
    private long cachedEntriesLastModified = 0;
    
    // Write-back cache for performance
    private boolean isDirty = false;
    private List<String> pendingLines = null;
    private long lastWriteTime = 0;
    private static final long WRITE_DELAY_MS = 2000; // 2 second delay before auto-flush
    
    /**
     * Gets cached lines (for encrypted files).
     */
    public List<String> getCachedLines() {
        return new ArrayList<>(cachedLines);
    }
    
    /**
     * Updates the cached lines.
     */
    public void updateCachedLines(List<String> lines) {
        this.cachedLines = new ArrayList<>(lines);
    }
    
    /**
     * Clears the cached lines.
     */
    public void clearCachedLines() {
        this.cachedLines.clear();
    }
    
    /**
     * Gets cached parsed entries.
     */
    public List<List<String>> getCachedEntries() {
        return cachedEntries;
    }
    
    /**
     * Sets cached parsed entries with timestamp.
     */
    public void setCachedEntries(List<List<String>> entries, long lastModified) {
        this.cachedEntries = entries;
        this.cachedEntriesLastModified = lastModified;
    }
    
    /**
     * Gets the last modified timestamp of cached entries.
     */
    public long getCachedEntriesLastModified() {
        return cachedEntriesLastModified;
    }
    
    /**
     * Invalidates the entry cache.
     */
    public void invalidateEntryCache() {
        this.cachedEntries = null;
        this.cachedEntriesLastModified = 0;
    }
    
    /**
     * Invalidates all caches.
     */
    public void invalidateCaches() {
        invalidateEntryCache();
        clearCachedLines();
    }
    
    /**
     * Sets pending lines for write-back cache.
     */
    public void setPendingLines(List<String> lines) {
        this.pendingLines = lines;
        this.isDirty = true;
        this.lastWriteTime = System.currentTimeMillis();
    }
    
    /**
     * Gets pending lines.
     */
    public List<String> getPendingLines() {
        return pendingLines;
    }
    
    /**
     * Clears pending writes.
     */
    public void clearPendingWrites() {
        this.pendingLines = null;
        this.isDirty = false;
        this.lastWriteTime = 0;
    }
    
    /**
     * Checks if there are pending writes.
     */
    public boolean hasPendingWrites() {
        return isDirty && pendingLines != null;
    }
    
    /**
     * Checks if write delay has elapsed.
     */
    public boolean isWriteDelayElapsed() {
        return isDirty && (System.currentTimeMillis() - lastWriteTime) >= WRITE_DELAY_MS;
    }
}
