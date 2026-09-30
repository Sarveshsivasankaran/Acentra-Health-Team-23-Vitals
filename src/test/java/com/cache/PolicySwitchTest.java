package com.cache;

import com.cache.model.EvictionPolicyType;
import com.cache.service.CacheManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PolicySwitchTest {

    @Test
    public void testPolicySwitch() {
        CacheManager cache = new CacheManager(3, EvictionPolicyType.LRU);
        
        // Sequence: PUT A, PUT B, PUT C
        cache.put("A", "val", 60);
        cache.put("B", "val", 60);
        cache.put("C", "val", 60);
        
        // GET A, GET A, GET A
        cache.get("A");
        cache.get("A");
        cache.get("A");
        
        // GET B
        cache.get("B");
        
        // GET C
        cache.get("C");
        
        // At this point under LRU order of recency: A is oldest (last read 1st), B is 2nd, C is newest
        // Access counts: A=3, B=1, C=1
        
        // Switch to LFU
        cache.setPolicy(EvictionPolicyType.LFU);
        assertEquals(EvictionPolicyType.LFU, cache.getPolicyType());
        
        // PUT D
        cache.put("D", "val", 60);
        
        // Under LFU, A=3, B=1, C=1. Tie break B and C -> B has older lastAccessSeq. B should be evicted.
        assertEquals("HIT", cache.get("A").getStatus());
        assertEquals("MISS", cache.get("B").getStatus());
        assertEquals("HIT", cache.get("C").getStatus());
        assertEquals("HIT", cache.get("D").getStatus());
    }
}
