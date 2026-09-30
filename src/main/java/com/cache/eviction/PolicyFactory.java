package com.cache.eviction;

import com.cache.model.EvictionPolicyType;

public class PolicyFactory {
    
    public static EvictionPolicy create(EvictionPolicyType type) {
        if (type == null) {
            return new LRUEvictionPolicy();
        }
        return switch (type) {
            case LFU -> new LFUEvictionPolicy();
            case LRU -> new LRUEvictionPolicy();
        };
    }
}
