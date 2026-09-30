package com.cache.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationLog {
    private String op;
    private String key;
    private String result;
    private String evictedKey;
    private Long ttl;

    public OperationLog(String op, String key, String result, String evictedKey, Long ttl) {
        this.op = op;
        this.key = key;
        this.result = result;
        this.evictedKey = evictedKey;
        this.ttl = ttl;
    }

    public String getOp() { return op; }
    public String getKey() { return key; }
    public String getResult() { return result; }
    public String getEvictedKey() { return evictedKey; }
    public Long getTtl() { return ttl; }
}
