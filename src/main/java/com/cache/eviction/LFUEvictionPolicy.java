package com.cache.eviction;

import com.cache.model.CacheEntry;
import java.util.Collection;

public class LFUEvictionPolicy implements EvictionPolicy {

    @Override
    public String evict(Collection<CacheEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        CacheEntry victim = null;
        for (CacheEntry entry : entries) {
            if (victim == null) {
                victim = entry;
            } else if (entry.getAccessCount() < victim.getAccessCount()) {
                victim = entry;
            } else if (entry.getAccessCount() == victim.getAccessCount()) {
                if (entry.getLastAccessSeq() < victim.getLastAccessSeq()) {
                    victim = entry;
                }
            }
        }

        return victim != null ? victim.getKey() : null;
    }
}
