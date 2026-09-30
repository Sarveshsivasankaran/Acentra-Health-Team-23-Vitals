package com.cache;

import com.cache.eviction.EvictionPolicy;
import com.cache.eviction.LRUEvictionPolicy;
import com.cache.model.CacheEntry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LRUEvictionTest {

    @Test
    public void testLRUEviction() {
        EvictionPolicy policy = new LRUEvictionPolicy();
        
        CacheEntry e1 = new CacheEntry("A", "val", 0, 1000, 0, 0, 1);
        CacheEntry e2 = new CacheEntry("B", "val", 0, 1000, 0, 0, 2);
        CacheEntry e3 = new CacheEntry("C", "val", 0, 1000, 0, 0, 3);
        
        policy.onPut(e1);
        policy.onPut(e2);
        policy.onPut(e3);
        
        String victim = policy.evict();
        assertEquals("A", victim, "Should evict entry with smallest lastAccessSeq (A)");
        
        policy.onPut(e1);
        
        // A accessed again, should move to tail
        e1.recordAccess(0, 4);
        policy.onGet(e1);
        
        victim = policy.evict();
        assertEquals("B", victim, "Should evict entry with smallest lastAccessSeq (B)");
    }
    
    @Test
    public void testEmptyAndNull() {
        EvictionPolicy policy = new LRUEvictionPolicy();
        assertNull(policy.evict());
    }
}
