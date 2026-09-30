package com.cache.controller;

import com.cache.model.OperationLog;
import com.cache.service.DemoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cache/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @PostMapping
    public Map<String, Object> sampleDemo() {
        return demoService.runSampleDemo();
    }

    @PostMapping("/eviction")
    public Map<String, Object> evictionDemo() {
        return demoService.runEvictionDemo();
    }

    @PostMapping("/ttl")
    public List<OperationLog> ttlDemo() {
        return demoService.runTtlDemo();
    }

    @PostMapping("/compare")
    public Map<String, Object> compareDemo(@RequestParam(defaultValue = "random") String pattern) {
        return demoService.runCompareDemo(pattern);
    }
}
