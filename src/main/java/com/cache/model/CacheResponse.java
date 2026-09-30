package com.cache.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CacheResponse {
    private String key;
    private String value;
    private String status; // "HIT", "MISS", "STORED", "DELETED", etc.
    private String reason; // "NOT_FOUND", "EXPIRED"
    private Long ttl;      // for PUT response
    private String evictedKey;

    public CacheResponse() {}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public String getEvictedKey() {
        return evictedKey;
    }

    public void setEvictedKey(String evictedKey) {
        this.evictedKey = evictedKey;
    }
}
