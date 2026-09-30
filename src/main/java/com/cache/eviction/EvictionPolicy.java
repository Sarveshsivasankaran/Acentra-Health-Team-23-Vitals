package com.cache.eviction;

import com.cache.model.CacheEntry;
import java.util.Collection;

public interface EvictionPolicy {
    /**
     * Returns the key of the entry to evict, or null if there are no entries.
     */
    String evict(Collection<CacheEntry> entries);
}
