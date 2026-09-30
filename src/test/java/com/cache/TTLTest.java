package com.cache;

import com.cache.model.CacheResponse;
import com.cache.model.EvictionPolicyType;
import com.cache.service.CacheManager;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TTLTest {

    @Test
    public void testTTLExpiration() {
        AtomicLong fakeClock = new AtomicLong(1000);
        CacheManager cache = new CacheManager(3, EvictionPolicyType.LRU, fakeClock::get);
        
        cache.put("A", "val1", 2); // expires at 3000
        assertEquals("HIT", cache.get("A").getStatus());
        
        fakeClock.set(3001);
        CacheResponse res = cache.get("A");
        assertEquals("MISS", res.getStatus());
        assertEquals("EXPIRED", res.getReason());
        
        // Expiration is counted as a miss and an expiration
        assertEquals(1, cache.getMetrics().getExpirations());
        assertEquals(1, cache.getMetrics().getMisses());
    }

    @Test
    public void testExpiredPurgedBeforeEviction() {
        AtomicLong fakeClock = new AtomicLong(1000);
        CacheManager cache = new CacheManager(2, EvictionPolicyType.LRU, fakeClock::get);
        
        cache.put("A", "val1", 1); // expires at 2000
        cache.put("B", "val2", 10); // expires at 11000
        
        fakeClock.set(2001);
        // At this point A is expired, but CacheFusion cleans it lazily.
        cache.get("A"); // Triggers lazy expiration and metrics
        
        assertEquals(1, cache.getMetrics().getExpirations());
        assertEquals(1, cache.getSize()); // logical size decreased
    }
}
