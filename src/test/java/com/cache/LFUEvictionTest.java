package com.cache;

import com.cache.eviction.EvictionPolicy;
import com.cache.eviction.LFUEvictionPolicy;
import com.cache.model.CacheEntry;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LFUEvictionTest {

    @Test
    public void testLFUEviction() {
        EvictionPolicy policy = new LFUEvictionPolicy();
        
        CacheEntry e1 = new CacheEntry("A", "val", 0, 1000, 3, 0, 5); // freq 3
        CacheEntry e2 = new CacheEntry("B", "val", 0, 1000, 1, 0, 4); // freq 1
        CacheEntry e3 = new CacheEntry("C", "val", 0, 1000, 2, 0, 6); // freq 2
        
        String victim = policy.evict(Arrays.asList(e1, e2, e3));
        assertEquals("B", victim, "Should evict entry with smallest accessCount (B)");
    }
    
    @Test
    public void testLFUTieBreak() {
        EvictionPolicy policy = new LFUEvictionPolicy();
        
        CacheEntry e1 = new CacheEntry("A", "val", 0, 1000, 2, 0, 10);
        CacheEntry e2 = new CacheEntry("B", "val", 0, 1000, 1, 0, 5);
        CacheEntry e3 = new CacheEntry("C", "val", 0, 1000, 1, 0, 8);
        
        String victim = policy.evict(Arrays.asList(e1, e2, e3));
        assertEquals("B", victim, "Should break tie using smallest lastAccessSeq (B)");
    }
}
