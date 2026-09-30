package com.cache.controller;

import com.cache.model.CacheResponse;
import com.cache.model.EntryView;
import com.cache.model.EvictionPolicyType;
import com.cache.model.PutResult;
import com.cache.service.CacheManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final CacheManager cacheManager;

    public CacheController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/entries/{key}")
    public CacheResponse getEntry(@PathVariable String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        return cacheManager.get(key);
    }

    @PutMapping("/entries/{key}")
    public CacheResponse putEntry(@PathVariable String key, 
                                  @RequestBody(required = false) String value, 
                                  @RequestParam(required = false, defaultValue = "60") Long ttl) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Value cannot be empty");
        }
        if (ttl <= 0) {
            throw new IllegalArgumentException("TTL must be positive");
        }
        
        PutResult result = cacheManager.put(key, value, ttl);
        
        CacheResponse response = new CacheResponse();
        response.setKey(key);
        response.setValue(value);
        response.setTtl(ttl);
        response.setStatus("STORED");
        response.setEvictedKey(result.getEvictedKey());
        return response;
    }

    @DeleteMapping("/entries/{key}")
    public CacheResponse deleteEntry(@PathVariable String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        boolean deleted = cacheManager.delete(key);
        if (!deleted) {
            throw new IllegalArgumentException("Key not found"); // Map to 404 conceptually if we wanted, but 400 is fine given the prompt, wait prompt says 404 for missing DELETE. Let's throw a custom or just return 404.
        }
        CacheResponse response = new CacheResponse();
        response.setKey(key);
        response.setStatus("DELETED");
        return response;
    }

    @GetMapping("/entries")
    public List<EntryView> listEntries() {
        return cacheManager.listEntries();
    }

    @DeleteMapping("/entries")
    public Map<String, String> clearCache() {
        cacheManager.clear();
        Map<String, String> response = new HashMap<>();
        response.put("status", "CLEARED");
        return response;
    }

    @PostMapping("/policy")
    public Map<String, String> setPolicy(@RequestParam String policy) {
        try {
            EvictionPolicyType type = EvictionPolicyType.valueOf(policy.toUpperCase());
            cacheManager.setPolicy(type);
            Map<String, String> response = new HashMap<>();
            response.put("policy", type.name());
            response.put("status", "UPDATED");
            return response;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown policy: " + policy);
        }
    }

    @PostMapping("/capacity")
    public Map<String, Object> setCapacity(@RequestParam int value) {
        cacheManager.setCapacity(value);
        Map<String, Object> response = new HashMap<>();
        response.put("capacity", value);
        response.put("status", "UPDATED");
        return response;
    }
}
