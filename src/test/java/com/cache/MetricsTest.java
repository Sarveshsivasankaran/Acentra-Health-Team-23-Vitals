package com.cache;

import com.cache.model.EvictionPolicyType;
import com.cache.service.CacheManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetricsTest {

    @Test
    public void testMetrics() {
        CacheManager cache = new CacheManager(3, EvictionPolicyType.LRU);
        
        cache.put("A", "val1", 60);
        cache.get("A"); // hit
        cache.get("B"); // miss
        cache.get("A"); // hit
        
        assertEquals(2, cache.getMetrics().getHits());
        assertEquals(1, cache.getMetrics().getMisses());
        assertEquals(66.7, cache.getMetrics().getHitRate(), 0.01);
        assertEquals(33.3, cache.getMetrics().getMissRate(), 0.01);
        
        cache.getMetrics().reset();
        assertEquals(0, cache.getMetrics().getHits());
        assertEquals(0.0, cache.getMetrics().getHitRate());
    }
    
    @Test
    public void testZeroRequests() {
        CacheManager cache = new CacheManager(3, EvictionPolicyType.LRU);
        assertEquals(0.0, cache.getMetrics().getHitRate());
        assertEquals(0.0, cache.getMetrics().getMissRate());
    }
}
