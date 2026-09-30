package com.cache.storage;

import com.cache.model.CacheEntry;
import java.util.List;

public class ImmutableRun {
    private final CacheEntry[] entries;
    private final long runId;
    private final long createdAt;
    private final BloomFilter bloomFilter;

    public ImmutableRun(long runId, List<CacheEntry> sortedEntries, long createdAt) {
        this.runId = runId;
        this.createdAt = createdAt;
        this.entries = sortedEntries.toArray(new CacheEntry[0]);
        
        // Initialize Bloom Filter with expected size and 1% false positive rate
        this.bloomFilter = new BloomFilter(this.entries.length, 0.01);
        for (CacheEntry entry : this.entries) {
            this.bloomFilter.add(entry.getKey());
        }
    }

    public CacheEntry get(String key) {
        // Fast-path: O(1) rejection if the key is definitely not in this run
        if (!bloomFilter.mightContain(key)) {
            return null;
        }

        // Standard O(log N) binary search
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
