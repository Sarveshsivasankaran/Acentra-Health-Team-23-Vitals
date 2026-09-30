package com.cache.service;

import com.cache.eviction.EvictionPolicy;
import com.cache.eviction.PolicyFactory;
import com.cache.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Service;

public class CacheManager {
    private final ConcurrentHashMap<String, CacheEntry> store = new ConcurrentHashMap<>();
    private volatile int capacity;
    private volatile EvictionPolicyType policyType;
    private EvictionPolicy policy;
    private final CacheMetrics metrics = new CacheMetrics();
    private final LongSupplier clock;
    private final AtomicLong seqCounter = new AtomicLong(0);
    private final ReentrantLock lock = new ReentrantLock();

    public CacheManager(int capacity, EvictionPolicyType defaultPolicy, LongSupplier clock) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be >= 1");
        }
        this.capacity = capacity;
        this.policyType = defaultPolicy;
        this.policy = PolicyFactory.create(defaultPolicy);
        this.clock = clock != null ? clock : System::currentTimeMillis;
    }
    
    public CacheManager(int capacity, EvictionPolicyType defaultPolicy) {
        this(capacity, defaultPolicy, System::currentTimeMillis);
    }

    public CacheResponse get(String key) {
        long now = clock.getAsLong();
        lock.lock();
        try {
            CacheEntry entry = store.get(key);
            CacheResponse response = new CacheResponse();
            response.setKey(key);
            
            if (entry == null) {
                metrics.miss();
                response.setStatus("MISS");
                response.setReason("NOT_FOUND");
                return response;
            }
            
            if (entry.isExpired(now)) {
                store.remove(key);
                metrics.expiration();
                metrics.miss();
                response.setStatus("MISS");
                response.setReason("EXPIRED");
                return response;
            }
            
            // Valid hit
            entry.recordAccess(now, seqCounter.incrementAndGet());
            metrics.hit();
            response.setStatus("HIT");
            response.setValue(entry.getValue());
            return response;
        } finally {
            lock.unlock();
        }
    }

    public PutResult put(String key, String value, long ttlSeconds) {
        long now = clock.getAsLong();
        long expiryTime = now + (ttlSeconds * 1000L);
        
        lock.lock();
        try {
            CacheEntry existing = store.get(key);
            if (existing != null) {
                // Key exists: replace value and TTL, reset accessCount, stamp seq. No eviction.
                existing.setValue(value);
                existing.setExpiryTime(expiryTime);
                existing.setAccessCount(0);
                existing.recordAccess(now, seqCounter.incrementAndGet());
                return new PutResult(true, null);
            }
            
            // New key
            String victimKey = null;
            if (store.size() >= capacity) {
                // First purge all expired
                removeExpiredUnderLock(now);
                
                // If still at or over capacity, evict one
                if (store.size() >= capacity) {
                    victimKey = policy.evict(store.values());
                    if (victimKey != null) {
                        store.remove(victimKey);
                        metrics.eviction();
                    }
                }
            }
            
            CacheEntry newEntry = new CacheEntry(key, value, now, expiryTime, 0, now, seqCounter.incrementAndGet());
            store.put(key, newEntry);
            
            return new PutResult(true, victimKey);
        } finally {
            lock.unlock();
        }
    }
    
    public boolean delete(String key) {
        lock.lock();
        try {
            return store.remove(key) != null;
        } finally {
            lock.unlock();
        }
    }
    
    public void clear() {
        lock.lock();
        try {
            store.clear();
        } finally {
            lock.unlock();
        }
    }
    
    public void setPolicy(EvictionPolicyType type) {
        lock.lock();
        try {
            this.policyType = type;
            this.policy = PolicyFactory.create(type);
        } finally {
            lock.unlock();
        }
    }
    
    public void setCapacity(int newCapacity) {
        if (newCapacity < 1) {
            throw new IllegalArgumentException("Capacity must be >= 1");
        }
        long now = clock.getAsLong();
        lock.lock();
        try {
            this.capacity = newCapacity;
            if (store.size() > capacity) {
                removeExpiredUnderLock(now);
                while (store.size() > capacity) {
                    String victimKey = policy.evict(store.values());
                    if (victimKey != null) {
                        store.remove(victimKey);
                        metrics.eviction();
                    } else {
                        break; // nothing left to evict?
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    public List<EntryView> listEntries() {
        long now = clock.getAsLong();
        List<EntryView> result = new ArrayList<>();
        // Read-only scan; locking optional but safe to do under lock for absolute point-in-time consistency
        lock.lock();
        try {
            for (CacheEntry entry : store.values()) {
                String status = entry.isExpired(now) ? "EXPIRED" : "LIVE";
                long remainingSec = entry.remainingTtlSeconds(now);
                result.add(new EntryView(
                    entry.getKey(), 
                    entry.getValue(), 
                    remainingSec, 
                    entry.getAccessCount(), 
                    entry.getLastAccessTime(), 
                    status
                ));
            }
        } finally {
            lock.unlock();
        }
        return result;
    }
    
    public int removeExpired() {
        long now = clock.getAsLong();
        lock.lock();
        try {
            return removeExpiredUnderLock(now);
        } finally {
            lock.unlock();
        }
    }
    
    private int removeExpiredUnderLock(long now) {
        int removedCount = 0;
        // avoid Iterator concurrent modifications by making a list or using map.entrySet().removeIf
        List<String> toRemove = new ArrayList<>();
        for (CacheEntry entry : store.values()) {
            if (entry.isExpired(now)) {
                toRemove.add(entry.getKey());
            }
        }
        for (String k : toRemove) {
            store.remove(k);
            metrics.expiration();
            removedCount++;
        }
        return removedCount;
    }

    public CacheMetrics snapshotMetrics() {
        // Return a fresh copy or the same metrics object for JSON serialisation
        // The prompt says snapshotMetrics() returns hits, misses, etc.
        // We will just map it in the controller, but exposing getters here.
        return this.metrics;
    }
    
    public int getSize() {
        return store.size();
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
