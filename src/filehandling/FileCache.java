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
     * Securely clears all cached data by overwriting content before clearing.
     * Should be called when locking the file to prevent memory forensics.
     */
    public void secureClear() {
        // Overwrite cached lines content before clearing
        for (int i = 0; i < cachedLines.size(); i++) {
            cachedLines.set(i, null);
        }
        cachedLines.clear();
        
        // Overwrite cached entries content before clearing
        if (cachedEntries != null) {
            for (List<String> entry : cachedEntries) {
                if (entry != null) {
                    for (int i = 0; i < entry.size(); i++) {
                        entry.set(i, null);
                    }
                    entry.clear();
                }
            }
            cachedEntries.clear();
            cachedEntries = null;
        }
        cachedEntriesLastModified = 0;
        
        // Clear pending lines
        if (pendingLines != null) {
            for (int i = 0; i < pendingLines.size(); i++) {
                pendingLines.set(i, null);
            }
            pendingLines.clear();
            pendingLines = null;
        }
        isDirty = false;
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
