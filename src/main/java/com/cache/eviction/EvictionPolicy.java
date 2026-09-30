package com.cache.eviction;

import com.cache.model.CacheEntry;
import java.util.Collection;

public interface EvictionPolicy {
    
    /** Called when a new entry is added or an existing entry is fully replaced */
    void onPut(CacheEntry entry);
    
    /** Called when an entry is accessed (hit) */
    void onGet(CacheEntry entry);
    
    /** Called when an entry is removed manually or via expiration */
    void onRemove(CacheEntry entry);
    
    /**
     * Returns the key of the entry to evict, and removes it from the policy tracking.
     * Returns null if there are no entries.
     */
    String evict();
    
    /** Clears all tracking data */
    void clear();

    /** Legacy method, default to null as policies are now stateful O(1) */
    default String evict(Collection<CacheEntry> entries) {
        return evict();
    }
}
