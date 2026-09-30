package com.cache.config;

import com.cache.model.EvictionPolicyType;
import com.cache.service.CacheManager;
import com.cache.service.ExpirySweeper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Value("${cache.capacity:5}")
    private int capacity;

    @Value("${cache.default-policy:LRU}")
    private EvictionPolicyType defaultPolicy;

    @Value("${cache.sweep-interval-ms:5000}")
    private long sweepIntervalMs;

    @Bean
    public CacheManager cacheManager() {
        return new CacheManager(capacity, defaultPolicy);
    }

    @Bean
    public ExpirySweeper expirySweeper(CacheManager cacheManager) {
        return new ExpirySweeper(cacheManager, sweepIntervalMs);
    }
}
