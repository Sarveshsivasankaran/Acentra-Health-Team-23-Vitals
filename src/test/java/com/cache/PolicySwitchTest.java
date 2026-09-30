package com.cache;

import com.cache.model.EvictionPolicyType;
import com.cache.service.CacheManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PolicySwitchTest {

    @Test
    public void testPolicySwitch() {
        // CacheFusion routes to MemTable first, then Hot Cache.
        // We will just test that the policy type is updated properly.
        CacheManager cache = new CacheManager(10, EvictionPolicyType.LRU);
        
        cache.setPolicy(EvictionPolicyType.LFU);
        assertEquals(EvictionPolicyType.LFU, cache.getPolicyType());
        
        // Put some entries
        cache.put("A", "val", 60);
        cache.put("B", "val", 60);
        
        // Hit A to promote / record access
        cache.get("A");
        cache.get("A");
        cache.get("A"); // Promoted to hot cache (threshold = 3)
        
        assertEquals("HIT", cache.get("A").getStatus());
    }
}
