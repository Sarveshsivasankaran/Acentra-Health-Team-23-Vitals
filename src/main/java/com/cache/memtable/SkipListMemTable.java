package com.cache.memtable;

import com.cache.model.CacheEntry;
import com.cache.storage.ImmutableRun;
import com.cache.storage.RunManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SkipListMemTable {
    private volatile ConcurrentSkipListMap<String, CacheEntry> activeMap;
    private final AtomicInteger size;
    private final int flushThreshold;
    private final RunManager runManager;
    private final ReadWriteLock flushLock = new ReentrantReadWriteLock();

    public SkipListMemTable(int flushThreshold, RunManager runManager) {
        this.activeMap = new ConcurrentSkipListMap<>();
        this.size = new AtomicInteger(0);
        this.flushThreshold = flushThreshold;
        this.runManager = runManager;
    }

    public CacheEntry put(String key, CacheEntry entry) {
        flushLock.readLock().lock();
        try {
            CacheEntry old = activeMap.put(key, entry);
            if (old == null) {
                int currentSize = size.incrementAndGet();
                if (currentSize >= flushThreshold) {
                    // Trigger flush asynchronously or synchronously. 
                    // For prototype, we can trigger it inline or we return a flag for the manager to flush.
                    // Let's do it inline with a tryLock to avoid blocking all writers if another thread is flushing.
                }
            }
            return old;
        } finally {
            flushLock.readLock().unlock();
        }
    }

    public CacheEntry get(String key) {
        // Lock-free read from ConcurrentSkipListMap
        return activeMap.get(key);
    }

    public void remove(String key) {
        flushLock.readLock().lock();
        try {
            CacheEntry removed = activeMap.remove(key);
            if (removed != null) {
                size.decrementAndGet();
            }
        } finally {
            flushLock.readLock().unlock();
        }
    }

    public boolean isOverThreshold() {
        return size.get() >= flushThreshold;
    }

    public void flush(long now) {
        flushLock.writeLock().lock();
        try {
            if (size.get() == 0) return;
            
            // Freeze the current map by swapping it out
            ConcurrentSkipListMap<String, CacheEntry> frozen = this.activeMap;
            this.activeMap = new ConcurrentSkipListMap<>();
            this.size.set(0);
            
            // Convert frozen to ImmutableRun (it's already sorted by key!)
            List<CacheEntry> sortedEntries = new ArrayList<>(frozen.values());
            
            ImmutableRun run = new ImmutableRun(runManager.generateRunId(), sortedEntries, now);
            runManager.addRun(run);
        } finally {
            flushLock.writeLock().unlock();
        }
    }

    public int size() {
        return size.get();
    }
    
    public void clear() {
        flushLock.writeLock().lock();
        try {
            activeMap.clear();
            size.set(0);
        } finally {
            flushLock.writeLock().unlock();
        }
    }
}
