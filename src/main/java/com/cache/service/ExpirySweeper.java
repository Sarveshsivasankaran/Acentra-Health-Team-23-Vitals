package com.cache.service;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpirySweeper {
    private static final Logger logger = LoggerFactory.getLogger(ExpirySweeper.class);
    private final CacheManager cacheManager;
    private final ScheduledExecutorService executor;

    public ExpirySweeper(CacheManager cacheManager, long intervalMs) {
        this.cacheManager = cacheManager;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ExpirySweeper-Thread");
            t.setDaemon(true);
            return t;
        });
        
        this.executor.scheduleAtFixedRate(this::sweep, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        logger.info("ExpirySweeper started with interval {} ms", intervalMs);
    }

    private void sweep() {
        try {
            int removed = cacheManager.removeExpired();
            if (removed > 0) {
                logger.debug("ExpirySweeper removed {} expired entries", removed);
            }
        } catch (Exception e) {
            logger.error("Error during expiry sweep", e);
        }
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping ExpirySweeper...");
        executor.shutdownNow();
    }
}
