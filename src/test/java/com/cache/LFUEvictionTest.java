package com.cache;

import com.cache.eviction.EvictionPolicy;
import com.cache.eviction.LFUEvictionPolicy;
import com.cache.model.CacheEntry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LFUEvictionTest {

    @Test
    public void testLFUEviction() {
        EvictionPolicy policy = new LFUEvictionPolicy();
        
        CacheEntry e1 = new CacheEntry("A", "val", 0, 1000, 3, 0, 5); // freq 3
        CacheEntry e2 = new CacheEntry("B", "val", 0, 1000, 1, 0, 4); // freq 1
        CacheEntry e3 = new CacheEntry("C", "val", 0, 1000, 2, 0, 6); // freq 2
        
        policy.onPut(e1);
        policy.onPut(e2);
        policy.onPut(e3);
        
        String victim = policy.evict();
        assertEquals("B", victim, "Should evict entry with smallest accessCount (B)");
    }
    
    @Test
    public void testLFUTieBreak() {
        EvictionPolicy policy = new LFUEvictionPolicy();
        
        CacheEntry e1 = new CacheEntry("A", "val", 0, 1000, 2, 0, 10);
        CacheEntry e2 = new CacheEntry("B", "val", 0, 1000, 1, 0, 5);
        CacheEntry e3 = new CacheEntry("C", "val", 0, 1000, 1, 0, 8);
        
        policy.onPut(e1);
        policy.onPut(e2); // B goes in first (oldest among freq 1)
        policy.onPut(e3); // C goes in next
        
        String victim = policy.evict();
        assertEquals("B", victim, "Should break tie using smallest lastAccessSeq (B)");
    }
    
    @Test
    public void testEmptyAndNull() {
        EvictionPolicy policy = new LFUEvictionPolicy();
        assertNull(policy.evict());
    }
}
