package com.cache.model;

public class PutResult {
    private final boolean stored;
    private final String evictedKey;

    public PutResult(boolean stored, String evictedKey) {
        this.stored = stored;
        this.evictedKey = evictedKey;
    }

    public boolean isStored() {
        return stored;
    }

    public String getEvictedKey() {
        return evictedKey;
    }
}
