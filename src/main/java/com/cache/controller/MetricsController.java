package com.cache.controller;

import com.cache.service.CacheManager;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache/metrics")
public class MetricsController {

    private final CacheManager cacheManager;

    public MetricsController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping
    public Map<String, Object> getMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        var cm = cacheManager.getMetrics();
        m.put("hits", cm.getHits());
        m.put("misses", cm.getMisses());
        m.put("hitRate", cm.getHitRate());
        m.put("missRate", cm.getMissRate());
        m.put("evictions", cm.getEvictions());
        m.put("expirations", cm.getExpirations());
        m.put("size", cacheManager.getSize());
        m.put("capacity", cacheManager.getCapacity());
        m.put("policy", cacheManager.getPolicyType().name());
        return m;
    }

    @PostMapping("/reset")
    public Map<String, String> resetMetrics() {
        cacheManager.getMetrics().reset();
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "RESET");
        return response;
    }
}
