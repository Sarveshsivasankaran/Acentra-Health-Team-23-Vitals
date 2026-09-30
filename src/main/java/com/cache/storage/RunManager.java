package com.cache.storage;

import com.cache.model.CacheEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class RunManager {
    // Newest runs are typically added to the end.
    // CopyOnWriteArrayList allows lock-free reads while compactions/flushes replace/add runs.
    private final CopyOnWriteArrayList<ImmutableRun> runs = new CopyOnWriteArrayList<>();
    private final AtomicLong runIdGenerator = new AtomicLong(1);

    public void addRun(ImmutableRun run) {
        // Add to front so that newest runs are at index 0 for faster lookup of newest version
        runs.add(0, run);
    }

    public void replaceRuns(List<ImmutableRun> oldRuns, ImmutableRun newCompactedRun) {
        synchronized (runs) {
            runs.removeAll(oldRuns);
            if (newCompactedRun != null) {
                // Determine correct insertion point (mostly end if it's the oldest data, 
                // but simpler to just append or maintain timestamp order).
                // Let's insert based on runId (descending) to keep newest first.
                runs.add(newCompactedRun);
                runs.sort((r1, r2) -> Long.compare(r2.getRunId(), r1.getRunId()));
            }
        }
    }

    public CacheEntry getNewest(String key) {
        // Since newest are at the beginning (index 0), first match is the newest version.
        for (ImmutableRun run : runs) {
            CacheEntry entry = run.get(key);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    public List<ImmutableRun> getRuns() {
        return Collections.unmodifiableList(runs);
    }

    public int getRunCount() {
        return runs.size();
    }
    
    public long generateRunId() {
        return runIdGenerator.incrementAndGet();
    }
    
    public void clear() {
        runs.clear();
    }
}
