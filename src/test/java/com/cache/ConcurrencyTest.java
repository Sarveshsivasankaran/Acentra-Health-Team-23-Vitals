package com.cache;

import com.cache.model.EvictionPolicyType;
import com.cache.service.CacheManager;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrencyTest {

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        int threadCount = 50;
        int operationsPerThread = 400; // total 20,000 ops
        CacheManager cache = new CacheManager(100, EvictionPolicyType.LRU);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalGets = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "key" + (j % 50); // High contention on 50 keys
                    if (j % 2 == 0) {
                        cache.put(key, "val" + j, 60);
                    } else {
                        cache.get(key);
                        totalGets.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertTrue(cache.getSize() <= 100, "Size should not exceed capacity");
        long reportedGets = cache.getMetrics().getHits() + cache.getMetrics().getMisses();
        assertEquals(totalGets.get(), reportedGets, "Hits + misses should equal exact number of GETs issued");
    }
}
