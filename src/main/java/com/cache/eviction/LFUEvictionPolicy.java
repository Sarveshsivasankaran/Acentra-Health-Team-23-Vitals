package com.cache.eviction;

import com.cache.model.CacheEntry;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * O(1) LFU Eviction Policy using Frequency Buckets.
 * Ties are broken by LRU order (guaranteed by LinkedHashSet).
 * Inspired by Caffeine's focus on O(1) eviction efficiencies.
 */
public class LFUEvictionPolicy implements EvictionPolicy {

    private final Map<Long, LinkedHashSet<String>> freqBuckets = new HashMap<>();
    private final Map<String, Long> keyFreq = new HashMap<>();
    private long minFreq = 0;

    @Override
    public void onPut(CacheEntry entry) {
        long freq = entry.getAccessCount();
        if (keyFreq.containsKey(entry.getKey())) {
            onRemove(entry);
        }
        
        keyFreq.put(entry.getKey(), freq);
        freqBuckets.computeIfAbsent(freq, k -> new LinkedHashSet<>()).add(entry.getKey());
        
        if (freq < minFreq || freqBuckets.get(minFreq) == null || freqBuckets.get(minFreq).isEmpty()) {
            minFreq = freq;
        }
    }

    @Override
    public void onGet(CacheEntry entry) {
        String key = entry.getKey();
        Long oldFreq = keyFreq.get(key);
        
        if (oldFreq != null) {
            LinkedHashSet<String> oldBucket = freqBuckets.get(oldFreq);
            if (oldBucket != null) {
                oldBucket.remove(key);
                if (oldBucket.isEmpty() && minFreq == oldFreq) {
                    minFreq = oldFreq + 1;
                }
            }
        } else {
            oldFreq = entry.getAccessCount() - 1; // Fallback
        }
        
        long newFreq = entry.getAccessCount();
        keyFreq.put(key, newFreq);
        freqBuckets.computeIfAbsent(newFreq, k -> new LinkedHashSet<>()).add(key);
    }

    @Override
    public void onRemove(CacheEntry entry) {
        String key = entry.getKey();
        Long freq = keyFreq.remove(key);
        if (freq != null) {
            LinkedHashSet<String> bucket = freqBuckets.get(freq);
            if (bucket != null) {
                bucket.remove(key);
            }
        }
    }

    @Override
    public String evict() {
        if (keyFreq.isEmpty()) {
            return null;
        }
        
        LinkedHashSet<String> bucket = freqBuckets.get(minFreq);
        while (bucket == null || bucket.isEmpty()) {
            minFreq++;
            bucket = freqBuckets.get(minFreq);
        }
        
        String victimKey = bucket.iterator().next(); // First element (oldest/LRU)
        bucket.remove(victimKey);
        keyFreq.remove(victimKey);
        return victimKey;
    }

    @Override
    public void clear() {
        freqBuckets.clear();
        keyFreq.clear();
        minFreq = 0;
    }
}
