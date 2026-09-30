package com.cache.service;

import com.cache.compaction.CompactionManager;
import com.cache.eviction.EvictionPolicy;
import com.cache.eviction.PolicyFactory;
import com.cache.memtable.SkipListMemTable;
import com.cache.model.*;
import com.cache.storage.ImmutableRun;
import com.cache.storage.RunManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Service;

@Service
public class CacheManager {
    // Hot Cache (L1)
    private final ConcurrentHashMap<String, CacheEntry> hotCache = new ConcurrentHashMap<>();
    
    // Warm Cache (L2)
    private final SkipListMemTable memTable;
    
    // Cold Cache (L3)
    private final RunManager runManager;
    private final CompactionManager compactionManager;

    private volatile int capacity;
    private volatile EvictionPolicyType policyType;
    private EvictionPolicy policy; // Tracks global eviction order for the hybrid cache
    
    private final CacheMetrics metrics = new CacheMetrics();
    private final LongSupplier clock;
    private final AtomicLong seqCounter = new AtomicLong(0);
    private final AtomicInteger logicalSize = new AtomicInteger(0);
    
    private static final int HOT_PROMOTION_THRESHOLD = 3;
    private static final int MAX_RUNS = 4;

    public CacheManager(int capacity, EvictionPolicyType defaultPolicy, LongSupplier clock) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be >= 1");
        }
        this.capacity = capacity;
        this.policyType = defaultPolicy;
        this.policy = PolicyFactory.create(defaultPolicy);
        this.clock = clock != null ? clock : System::currentTimeMillis;
        
        this.runManager = new RunManager();
        this.memTable = new SkipListMemTable(Math.max(2, capacity / 2), this.runManager);
        this.compactionManager = new CompactionManager(this.runManager);
    }
    
    public CacheManager(int capacity, EvictionPolicyType defaultPolicy) {
        this(capacity, defaultPolicy, System::currentTimeMillis);
    }

    public CacheResponse get(String key) {
        long now = clock.getAsLong();
        CacheResponse response = new CacheResponse();
        response.setKey(key);

        // 1. Check Hot Cache
        CacheEntry entry = hotCache.get(key);
        if (entry != null) {
            if (entry.isTombstone()) {
                metrics.miss();
                return missResponse(response, "NOT_FOUND");
            }
            if (entry.isExpired(now)) {
                internalDelete(key, now);
                metrics.expiration();
                metrics.miss();
                return missResponse(response, "EXPIRED");
            }
            entry.recordAccess(now, seqCounter.incrementAndGet());
            policy.onGet(entry);
            metrics.hit();
            response.setStatus("HIT");
            response.setValue(entry.getValue());
            return response;
        }

        // 2. Check MemTable
        entry = memTable.get(key);
        if (entry != null) {
            return processLowerTierHit(entry, now, response);
        }

        // 3. Check Immutable Runs
        entry = runManager.getNewest(key);
        if (entry != null) {
            return processLowerTierHit(entry, now, response);
        }

        metrics.miss();
        return missResponse(response, "NOT_FOUND");
    }

    private CacheResponse missResponse(CacheResponse response, String reason) {
        response.setStatus("MISS");
        response.setReason(reason);
        return response;
    }

    private CacheResponse processLowerTierHit(CacheEntry entry, long now, CacheResponse response) {
        if (entry.isTombstone()) {
            metrics.miss();
            return missResponse(response, "NOT_FOUND");
        }
        if (entry.isExpired(now)) {
            internalDelete(entry.getKey(), now);
            metrics.expiration();
            metrics.miss();
            return missResponse(response, "EXPIRED");
        }
        
        entry.recordAccess(now, seqCounter.incrementAndGet());
        policy.onGet(entry);
        metrics.hit();
        response.setStatus("HIT");
        response.setValue(entry.getValue());

        if (entry.getAccessCount() >= HOT_PROMOTION_THRESHOLD) {
            promoteToHot(entry);
        }
        
        return response;
    }

    private synchronized void promoteToHot(CacheEntry entry) {
        if (!hotCache.containsKey(entry.getKey())) {
            int hotCapacity = Math.max(1, capacity / 3);
            if (hotCache.size() >= hotCapacity) {
                // Find a key to demote from hot cache. 
                // Since our global policy tracks everything, we just remove a random or oldest from hot.
                String demoteKey = hotCache.keySet().iterator().next();
                CacheEntry victim = hotCache.remove(demoteKey);
                if (victim != null) {
                    memTable.put(victim.getKey(), victim);
                }
            }
            hotCache.put(entry.getKey(), entry);
        }
    }

    public synchronized PutResult put(String key, String value, long ttlSeconds) {
        long now = clock.getAsLong();
        long expiryTime = now + (ttlSeconds * 1000L);
        CacheEntry newEntry = new CacheEntry(key, value, now, expiryTime, 0, now, seqCounter.incrementAndGet());
        
        CacheEntry existingHot = hotCache.get(key);
        boolean isNew = true;
        
        if (existingHot != null) {
            newEntry.setAccessCount(existingHot.getAccessCount());
            hotCache.put(key, newEntry);
            policy.onRemove(existingHot);
            policy.onPut(newEntry);
            isNew = false;
        } else {
            CacheEntry memExisting = memTable.get(key);
            if (memExisting != null) {
                if (!memExisting.isTombstone()) {
                    newEntry.setAccessCount(memExisting.getAccessCount());
                    policy.onRemove(memExisting);
                    isNew = false;
                }
            } else {
                CacheEntry runExisting = runManager.getNewest(key);
                if (runExisting != null && !runExisting.isTombstone()) {
                    newEntry.setAccessCount(runExisting.getAccessCount());
                    policy.onRemove(runExisting);
                    isNew = false;
                }
            }
            
            memTable.put(key, newEntry);
            policy.onPut(newEntry);
            if (memTable.isOverThreshold()) {
                memTable.flush(now);
                compactionManager.compactIfNeeded(MAX_RUNS);
            }
        }
        
        if (isNew) {
            logicalSize.incrementAndGet();
        }
        
        String victimKey = null;
        while (logicalSize.get() > capacity) {
            victimKey = policy.evict();
            if (victimKey != null) {
                if (internalDelete(victimKey, now)) {
                    metrics.eviction();
                }
            } else {
                break;
            }
        }
        
        return new PutResult(true, victimKey);
    }
    
    private synchronized boolean internalDelete(String key, long now) {
        boolean wasLive = false;
        CacheEntry hot = hotCache.remove(key);
        if (hot != null) {
            wasLive = true;
        } else {
            CacheEntry mem = memTable.get(key);
            if (mem != null && !mem.isTombstone()) {
                wasLive = true;
            } else if (mem == null) {
                CacheEntry run = runManager.getNewest(key);
                if (run != null && !run.isTombstone()) {
                    wasLive = true;
                }
            }
        }
        
        CacheEntry tombstone = CacheEntry.tombstone(key, now);
        tombstone.setVersion(now);
        memTable.put(key, tombstone);
        
        if (wasLive) {
            logicalSize.decrementAndGet();
            policy.onRemove(tombstone);
        }
        return wasLive;
    }
    
    public synchronized boolean delete(String key) {
        long now = clock.getAsLong();
        boolean found = internalDelete(key, now);
        
        if (memTable.isOverThreshold()) {
            memTable.flush(now);
            compactionManager.compactIfNeeded(MAX_RUNS);
        }
        
        return found;
    }
    
    public synchronized void clear() {
        hotCache.clear();
        policy.clear();
        memTable.clear();
        runManager.clear();
        logicalSize.set(0);
    }
    
    public synchronized void setPolicy(EvictionPolicyType type) {
        this.policyType = type;
        this.policy = PolicyFactory.create(type);
        
        // Repopulate the new policy with all currently live entries
        for (EntryView view : listEntries()) {
            if ("LIVE".equals(view.getStatus())) {
                CacheEntry dummy = new CacheEntry(view.getKey(), view.getValue(), 0, 0, view.getAccessCount(), view.getLastAccessTime(), 0);
                this.policy.onPut(dummy);
            }
        }
    }
    
    public synchronized void setCapacity(int newCapacity) {
        if (newCapacity < 1) {
            throw new IllegalArgumentException("Capacity must be >= 1");
        }
        this.capacity = newCapacity;
        long now = clock.getAsLong();
        while (logicalSize.get() > capacity) {
            String victimKey = policy.evict();
            if (victimKey != null) {
                if (internalDelete(victimKey, now)) {
                    metrics.eviction();
                }
            } else {
                break;
            }
        }
    }
    
    public List<EntryView> listEntries() {
        long now = clock.getAsLong();
        java.util.Map<String, EntryView> latestEntries = new java.util.HashMap<>();

        List<ImmutableRun> runs = runManager.getRuns();
        for (int i = runs.size() - 1; i >= 0; i--) {
            ImmutableRun run = runs.get(i);
            for (CacheEntry entry : run.getEntries()) {
                addEntryViewToMap(latestEntries, entry, now, "L3 (Cold)");
            }
        }

        for (CacheEntry entry : memTable.getEntries()) {
            addEntryViewToMap(latestEntries, entry, now, "L2 (Warm)");
        }

        for (CacheEntry entry : hotCache.values()) {
            addEntryViewToMap(latestEntries, entry, now, "L1 (Hot)");
        }

        return new ArrayList<>(latestEntries.values());
    }
    
    private void addEntryViewToMap(java.util.Map<String, EntryView> map, CacheEntry entry, long now, String tier) {
        if (entry.isTombstone()) {
            map.remove(entry.getKey());
            return;
        }
        String status = entry.isExpired(now) ? "EXPIRED" : "LIVE";
        long remainingSec = entry.remainingTtlSeconds(now);
        map.put(entry.getKey(), new EntryView(
            entry.getKey(), 
            entry.getValue(), 
            remainingSec, 
            entry.getAccessCount(), 
            entry.getLastAccessTime(), 
            status,
            tier
        ));
    }
    
    public int removeExpired() {
        long now = clock.getAsLong();
        int removed = 0;
        List<String> toRemove = new ArrayList<>();
        
        for (EntryView view : listEntries()) {
            if ("EXPIRED".equals(view.getStatus())) {
                toRemove.add(view.getKey());
            }
        }
        
        for (String k : toRemove) {
            if (internalDelete(k, now)) {
                metrics.expiration();
                removed++;
            }
        }
        return removed;
    }

    public CacheMetrics snapshotMetrics() {
        return this.metrics;
    }
    
    public int getSize() {
        return logicalSize.get();
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public EvictionPolicyType getPolicyType() {
        return policyType;
    }
    
    public CacheMetrics getMetrics() {
        return metrics;
    }
}
