package com.cache.compaction;

import com.cache.model.CacheEntry;
import com.cache.storage.ImmutableRun;
import com.cache.storage.RunManager;

import java.util.ArrayList;
import java.util.List;

public class CompactionManager {

    private final RunManager runManager;

    public CompactionManager(RunManager runManager) {
        this.runManager = runManager;
    }

    public void compactIfNeeded(int maxRuns) {
        // If we have more than maxRuns, we merge the oldest two runs.
        List<ImmutableRun> currentRuns = runManager.getRuns();
        if (currentRuns.size() > maxRuns) {
            // Newest are at index 0, oldest at end.
            ImmutableRun run1 = currentRuns.get(currentRuns.size() - 2);
            ImmutableRun run2 = currentRuns.get(currentRuns.size() - 1);
            
            ImmutableRun merged = merge(run1, run2);
            
            List<ImmutableRun> oldRuns = new ArrayList<>();
            oldRuns.add(run1);
            oldRuns.add(run2);
            
            runManager.replaceRuns(oldRuns, merged);
        }
    }

    private ImmutableRun merge(ImmutableRun r1, ImmutableRun r2) {
        CacheEntry[] e1 = r1.getEntries();
        CacheEntry[] e2 = r2.getEntries();
        
        List<CacheEntry> mergedList = new ArrayList<>();
        int i = 0, j = 0;
        
        while (i < e1.length && j < e2.length) {
            int cmp = e1[i].getKey().compareTo(e2[j].getKey());
            if (cmp < 0) {
                if (!e1[i].isTombstone()) mergedList.add(e1[i]);
                i++;
            } else if (cmp > 0) {
                if (!e2[j].isTombstone()) mergedList.add(e2[j]);
                j++;
            } else {
                // Keys are equal. Newest version wins.
                CacheEntry newest = e1[i].getVersion() > e2[j].getVersion() ? e1[i] : e2[j];
                if (!newest.isTombstone()) mergedList.add(newest);
                i++;
                j++;
            }
        }
        
        while (i < e1.length) {
            if (!e1[i].isTombstone()) mergedList.add(e1[i]);
            i++;
        }
        
        while (j < e2.length) {
            if (!e2[j].isTombstone()) mergedList.add(e2[j]);
            j++;
        }
        
        // Determine merged creation time (e.g., newest of the two)
        long createdAt = Math.max(r1.getCreatedAt(), r2.getCreatedAt());
        
        // Pass a new RunId (typically higher to denote it's newer, or maybe just next seq)
        return new ImmutableRun(runManager.generateRunId(), mergedList, createdAt);
    }
}
