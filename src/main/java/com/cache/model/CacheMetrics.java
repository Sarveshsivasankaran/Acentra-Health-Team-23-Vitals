package com.cache.model;

import java.util.concurrent.atomic.LongAdder;

public class CacheMetrics {
    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder evictions = new LongAdder();
    private final LongAdder expirations = new LongAdder();

    public void hit() {
        hits.increment();
    }

    public void miss() {
        misses.increment();
    }

    public void eviction() {
        evictions.increment();
    }

    public void expiration() {
        expirations.increment();
    }

    public void expiration(long count) {
        expirations.add(count);
    }

    public long getHits() {
        return hits.sum();
    }

    public long getMisses() {
        return misses.sum();
    }

    public long getEvictions() {
        return evictions.sum();
    }

    public long getExpirations() {
        return expirations.sum();
    }

    public double getHitRate() {
        long total = getHits() + getMisses();
        if (total == 0) {
            return 0.0;
        }
        double rate = ((double) getHits() / total) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    public double getMissRate() {
        long total = getHits() + getMisses();
        if (total == 0) {
            return 0.0;
        }
        double rate = ((double) getMisses() / total) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    public void reset() {
        hits.reset();
        misses.reset();
        evictions.reset();
        expirations.reset();
    }
}
