package com.cache.service;

import com.cache.model.CacheResponse;
import com.cache.model.EvictionPolicyType;
import com.cache.model.OperationLog;
import com.cache.model.PutResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class DemoService {

    private final CacheManager cacheManager;

    public DemoService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Map<String, Object> runSampleDemo() {
        cacheManager.setCapacity(3);
        cacheManager.clear();
        cacheManager.getMetrics().reset();

        List<OperationLog> logs = new ArrayList<>();
        logs.add(doPut("A", "valA", 60));
        logs.add(doPut("B", "valB", 60));
        logs.add(doPut("C", "valC", 60));
        logs.add(doGet("A"));
        logs.add(doGet("A"));
        logs.add(doGet("B"));
        logs.add(doGet("A"));
        logs.add(doGet("C"));
        logs.add(doGet("X"));
        logs.add(doGet("X"));
        logs.add(doPut("D", "valD", 60));
        logs.add(doGet("A"));
        logs.add(doGet("B"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Set capacity 3, cleared cache, reset metrics.");
        result.put("logs", logs);
        return result;
    }

    public Map<String, Object> runEvictionDemo() {
        cacheManager.setCapacity(3);
        cacheManager.clear();
        cacheManager.getMetrics().reset();

        List<OperationLog> logs = new ArrayList<>();
        logs.add(doPut("A", "valA", 60));
        logs.add(doPut("B", "valB", 60));
        logs.add(doPut("C", "valC", 60));
        logs.add(doGet("A"));
        logs.add(doGet("A"));
        logs.add(doGet("A"));
        logs.add(doGet("B"));
        logs.add(doGet("C"));
        logs.add(doPut("D", "valD", 60));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Set capacity 3, cleared cache, reset metrics.");
        result.put("logs", logs);
        return result;
    }

    public List<OperationLog> runTtlDemo() {
        List<OperationLog> logs = new ArrayList<>();
        logs.add(doPut("temp", "ttl-val", 3));
        logs.add(doGet("temp"));
        return logs;
    }

    public Map<String, Object> runCompareDemo(String pattern) {
        CacheManager lruCache = new CacheManager(10, EvictionPolicyType.LRU);
        CacheManager lfuCache = new CacheManager(10, EvictionPolicyType.LFU);

        int totalRequests = 1000;
        int keySpace = 50;
        Random random = new Random(42);

        for (int i = 0; i < totalRequests; i++) {
            int r;
            if ("zipf".equalsIgnoreCase(pattern)) {
                r = generateZipf(random, keySpace);
            } else if ("scan".equalsIgnoreCase(pattern)) {
                r = i % 15;
            } else {
                r = random.nextInt(keySpace);
            }

            String key = "K" + r;
            String val = "V" + r;

            simulateAccess(lruCache, key, val);
            simulateAccess(lfuCache, key, val);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pattern", pattern);
        result.put("LRU", extractCompareMetrics(lruCache));
        result.put("LFU", extractCompareMetrics(lfuCache));
        return result;
    }

    private void simulateAccess(CacheManager cm, String key, String val) {
        CacheResponse getRes = cm.get(key);
        if ("MISS".equals(getRes.getStatus())) {
            cm.put(key, val, 60);
        }
    }

    private int generateZipf(Random random, int max) {
        // Simple approximation
        double sum = 0.0;
        for (int i = 1; i <= max; i++) sum += 1.0 / i;
        double rand = random.nextDouble() * sum;
        double cumul = 0.0;
        for (int i = 1; i <= max; i++) {
            cumul += 1.0 / i;
            if (rand <= cumul) return i - 1;
        }
        return max - 1;
    }

    private Map<String, Object> extractCompareMetrics(CacheManager cm) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hits", cm.getMetrics().getHits());
        m.put("misses", cm.getMetrics().getMisses());
        m.put("hitRate", cm.getMetrics().getHitRate());
        m.put("evictions", cm.getMetrics().getEvictions());
        return m;
    }

    private OperationLog doPut(String key, String val, long ttl) {
        PutResult pr = cacheManager.put(key, val, ttl);
        return new OperationLog("PUT", key, "STORED", pr.getEvictedKey(), ttl);
    }

    private OperationLog doGet(String key) {
        CacheResponse cr = cacheManager.get(key);
        return new OperationLog("GET", key, cr.getStatus(), null, null);
    }
}
