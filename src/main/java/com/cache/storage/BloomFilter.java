package com.cache.storage;

import java.util.BitSet;

public class BloomFilter {
    private final BitSet bitSet;
    private final int size;
    private final int numHashFunctions;

    /**
     * Creates a Bloom filter optimized for the expected number of elements.
     * @param expectedElements The number of elements expected to be inserted.
     * @param falsePositiveRate The desired false positive probability (e.g., 0.01 for 1%).
     */
    public BloomFilter(int expectedElements, double falsePositiveRate) {
        if (expectedElements <= 0) expectedElements = 1;
        
        // Optimal size of bit array: m = -(n * ln(p)) / (ln(2)^2)
        this.size = (int) Math.ceil(-(expectedElements * Math.log(falsePositiveRate)) / Math.pow(Math.log(2), 2));
        this.bitSet = new BitSet(this.size);
        
        // Optimal number of hash functions: k = (m / n) * ln(2)
        this.numHashFunctions = (int) Math.ceil((this.size / (double) expectedElements) * Math.log(2));
    }

    public void add(String key) {
        int[] hashes = getHashes(key);
        for (int hash : hashes) {
            bitSet.set(Math.abs(hash % size));
        }
    }

    public boolean mightContain(String key) {
        int[] hashes = getHashes(key);
        for (int hash : hashes) {
            if (!bitSet.get(Math.abs(hash % size))) {
                return false; // Definitely not present
            }
        }
        return true; // Might be present
    }

    // Generate multiple hashes using MurmurHash or similar.
    // For simplicity and zero dependencies, we use standard String hashcode combined with primes.
    private int[] getHashes(String key) {
        int[] hashes = new int[numHashFunctions];
        int hash1 = key.hashCode();
        int hash2 = hash1 ^ (hash1 >>> 16); // basic bit mixing
        
        // Use double-hashing technique to generate k hash values
        for (int i = 0; i < numHashFunctions; i++) {
            hashes[i] = hash1 + (i * hash2);
        }
        return hashes;
    }
}
