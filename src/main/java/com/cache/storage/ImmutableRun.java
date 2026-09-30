package com.cache.storage;

import com.cache.model.CacheEntry;
import java.util.List;

public class ImmutableRun {
    private final CacheEntry[] entries;
    private final long runId;
    private final long createdAt;

    public ImmutableRun(long runId, List<CacheEntry> sortedEntries, long createdAt) {
        this.runId = runId;
        this.createdAt = createdAt;
        this.entries = sortedEntries.toArray(new CacheEntry[0]);
    }

    public CacheEntry get(String key) {
        int left = 0;
        int right = entries.length - 1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            int cmp = entries[mid].getKey().compareTo(key);
            if (cmp == 0) {
                return entries[mid];
            } else if (cmp < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return null;
    }

    public CacheEntry[] getEntries() {
        return entries;
    }

    public int size() {
        return entries.length;
    }

    public long getRunId() {
        return runId;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
