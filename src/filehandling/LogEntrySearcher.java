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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Efficient O(N) search utility for log entries.
 * Works with EntryLoader's cache for fast searching without re-parsing files.
 */
public class LogEntrySearcher {
    
    /**
     * Search result containing timestamp and match info.
     */
    public static class SearchResult {
        private final String timestamp;
        private final int matchCount;
        
        public SearchResult(String timestamp, int matchCount) {
            this.timestamp = timestamp;
            this.matchCount = matchCount;
        }
        
        public String getTimestamp() {
            return timestamp;
        }
        
        public int getMatchCount() {
            return matchCount;
        }
    }
    
    /**
     * Search options for text matching.
     */
    public static class SearchOptions {
        private final String query;
        private final boolean wholeWord;
        private final boolean caseSensitive;
        private final Pattern pattern;
        
        public SearchOptions(String query, boolean wholeWord, boolean caseSensitive) {
            this.query = query;
            this.wholeWord = wholeWord;
            this.caseSensitive = caseSensitive;
            this.pattern = buildPattern(query, wholeWord, caseSensitive);
        }
        
        private static Pattern buildPattern(String query, boolean wholeWord, boolean caseSensitive) {
            if (query == null || query.isEmpty()) {
                return null;
            }
            
            String escaped = Pattern.quote(query);
            String regex = wholeWord ? "\\b" + escaped + "\\b" : escaped;
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            return Pattern.compile(regex, flags);
        }
        
        public String getQuery() {
            return query;
        }
        
        public boolean isWholeWord() {
            return wholeWord;
        }
        
        public boolean isCaseSensitive() {
            return caseSensitive;
        }
        
        public boolean isEmpty() {
            return query == null || query.isEmpty();
        }
        
        /**
         * Check if the text matches this search.
         * @param text the text to search in
         * @return number of matches found
         */
        public int countMatches(String text) {
            if (pattern == null || text == null) {
                return 0;
            }
            Matcher matcher = pattern.matcher(text);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            return count;
        }
        
        /**
         * Check if the text contains at least one match.
         * @param text the text to search in
         * @return true if at least one match found
         */
        public boolean hasMatch(String text) {
            if (pattern == null || text == null) {
                return false;
            }
            return pattern.matcher(text).find();
        }
    }
    
    private LogEntrySearcher() {
        // Utility class
    }
    
    /**
     * Search through entries using the provided content accessor.
     * This is O(N) where N is the number of entries in the input list.
     * 
     * @param timestamps list of timestamps to search through
     * @param contentAccessor function to get content for a timestamp
     * @param options search options
     * @return list of matching search results
     */
    public static List<SearchResult> search(
            List<String> timestamps,
            java.util.function.Function<String, String> contentAccessor,
            SearchOptions options) {
        
        if (options.isEmpty()) {
            // Return all entries if no search query
            List<SearchResult> results = new ArrayList<>(timestamps.size());
            for (String ts : timestamps) {
                results.add(new SearchResult(ts, 0));
            }
            return results;
        }
        
        List<SearchResult> results = new ArrayList<>();
        
        for (String timestamp : timestamps) {
            // Search in timestamp itself
            int tsMatches = options.countMatches(timestamp);
            
            // Search in content
            String content = contentAccessor.apply(timestamp);
            int contentMatches = options.countMatches(content);
            
            int totalMatches = tsMatches + contentMatches;
            if (totalMatches > 0) {
                results.add(new SearchResult(timestamp, totalMatches));
            }
        }
        
        return results;
    }
    
    /**
     * Search through entries with date filtering applied first.
     * This is O(N) where N is the number of entries in the cache.
     * 
     * @param timestamps list of timestamps (already date-filtered)
     * @param contentAccessor function to get content for a timestamp
     * @param options search options
     * @return list of matching timestamps (order preserved)
     */
    public static List<String> searchTimestamps(
            List<String> timestamps,
            java.util.function.Function<String, String> contentAccessor,
            SearchOptions options) {
        
        if (options.isEmpty()) {
            return new ArrayList<>(timestamps);
        }
        
        List<String> results = new ArrayList<>();
        
        for (String timestamp : timestamps) {
            // Search in timestamp
            if (options.hasMatch(timestamp)) {
                results.add(timestamp);
                continue;
            }
            
            // Search in content
            String content = contentAccessor.apply(timestamp);
            if (options.hasMatch(content)) {
                results.add(timestamp);
            }
        }
        
        return results;
    }
}
