package com.cache.model;

public class CacheEntry {
    private final String key;
    private volatile String value;
    private final long createdAt;
    private volatile long expiryTime;
    private volatile long accessCount;
    private volatile long lastAccessTime;
    private volatile long lastAccessSeq;

    public CacheEntry(String key, String value, long createdAt, long expiryTime, long accessCount, long lastAccessTime, long lastAccessSeq) {
        this.key = key;
        this.value = value;
        this.createdAt = createdAt;
        this.expiryTime = expiryTime;
        this.accessCount = accessCount;
        this.lastAccessTime = lastAccessTime;
        this.lastAccessSeq = lastAccessSeq;
    }

    public boolean isExpired(long now) {
        return now >= expiryTime;
    }

    public void recordAccess(long now, long seq) {
        this.accessCount++;
        this.lastAccessTime = now;
        this.lastAccessSeq = seq;
    }

    public long remainingTtlMillis(long now) {
        return Math.max(0, expiryTime - now);
    }

    public long remainingTtlSeconds(long now) {
        long remainingMs = remainingTtlMillis(now);
        if (remainingMs <= 0) {
            return 0;
        }
        return (remainingMs + 999) / 1000;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(long accessCount) {
        this.accessCount = accessCount;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public long getLastAccessSeq() {
        return lastAccessSeq;
    }

    public void setLastAccessSeq(long lastAccessSeq) {
        this.lastAccessSeq = lastAccessSeq;
    }
}
