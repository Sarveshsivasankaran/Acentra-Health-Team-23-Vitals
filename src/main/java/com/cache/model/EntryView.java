package com.cache.model;

public class EntryView {
    private final String key;
    private final String value;
    private final long remainingTtlSeconds;
    private final long accessCount;
    private final long lastAccessTime;
    private final String status; // "LIVE" or "EXPIRED"
    private final String tier;

    public EntryView(String key, String value, long remainingTtlSeconds, long accessCount, long lastAccessTime, String status, String tier) {
        this.key = key;
        this.value = value;
        this.remainingTtlSeconds = remainingTtlSeconds;
        this.accessCount = accessCount;
        this.lastAccessTime = lastAccessTime;
        this.status = status;
        this.tier = tier;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public long getRemainingTtlSeconds() {
        return remainingTtlSeconds;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public String getStatus() {
        return status;
    }

    public String getTier() {
        return tier;
    }
}
