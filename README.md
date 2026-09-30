# Custom Cache Library with Live Metrics Panel

A high-performance, thread-safe, in-memory Java Custom Cache Library with selectable eviction policies (LRU/LFU), per-entry TTL, and a live metrics dashboard.

## Features

| Minimum requirement | Where it is implemented | How it is verified |
|---|---|---|
| In-memory cache | `CacheManager` + `ConcurrentHashMap` | Unit tests + Demo endpoints |
| LRU eviction | `LRUEvictionPolicy` | `LRUEvictionTest` |
| LFU eviction | `LFUEvictionPolicy` | `LFUEvictionTest` |
| Selectable eviction policy | `POST /api/cache/policy` + UI dropdown | `PolicySwitchTest` |
| Per-entry TTL | `CacheEntry.expiryTime` | `TTLTest` |
| TTL independent of eviction | TTL lives in `CacheManager`/`CacheEntry`, never in policies | `TTLTest` |
| Thread-safe concurrent GET/PUT | Single ReentrantLock around compound operations | `ConcurrencyTest` |
| Frontend metrics panel | `static/index.html`, `style.css`, `app.js` | Live Dashboard |
| Display cache hit rate & miss rate | Metrics card in UI | `MetricsTest` |
| Show metrics against a sample pattern | RUN SAMPLE, LRU vs LFU test, compare policies | Demo Endpoints |

## Architecture

```text
                      CACHE MANAGER
                           │
     ┌─────────────────────┼─────────────────────┐
     ▼                     ▼                     ▼
  CACHE DATA          TTL LOGIC              METRICS
(ConcurrentHashMap)  (lazy check in GET  (hits, misses,
                      + ExpirySweeper)    evictions, expirations)
                           │
                    EVICTION POLICY  (interface)
                     ┌─────┴─────┐
                     ▼           ▼
                    LRU         LFU
```

## Thread-Safety

The cache guarantees thread safety by wrapping all compound operations (check → evict → insert) in a single `ReentrantLock`. While we use `ConcurrentHashMap` for storage, the lock is what truly ensures atomicity of these multi-step cache operations. `ConcurrentHashMap` ensures safe read-only snapshots (for listing entries) and prevents standard concurrent modification exceptions during sweeps. Atomic counters (`AtomicLong`, `LongAdder`) track metrics and sequential access reliably across multiple threads. 

## Eviction Policies

### LRU (Least Recently Used)
The LRU policy evicts the entry with the oldest `lastAccessSeq` (the smallest monotonic sequence number generated upon access or creation). Using a monotonic sequence guarantees deterministic tie-breaking even when operations happen in the same millisecond. Time complexity for eviction scan is O(n) (deliberate MVP choice to keep it simple, while a LinkedHashMap or Doubly-Linked List could provide O(1)).

### LFU (Least Frequently Used)
The LFU policy evicts the entry with the smallest `accessCount`. In the event of a tie (multiple entries with the same minimum frequency), it breaks the tie by using the oldest `lastAccessSeq` (falling back to LRU). Time complexity for eviction scan is O(n) (could be improved with O(1) frequency buckets in a full implementation).

## TTL (Time To Live)

TTL is handled independently of the eviction policies. Each `CacheEntry` stores an absolute `expiryTime`.
1. **Lazy Expiry:** On every `GET` or `PUT`, the entry is checked against the current time. If it has expired, it is purged.
2. **Background Sweep:** A single-threaded daemon `ExpirySweeper` runs periodically (every 5 seconds) to actively remove expired entries, freeing up memory.
3. **Purge-Before-Evict:** Before electing a victim for eviction when full, the cache purges any newly expired entries.

## How to Run

### Technologies Used
Java 17+, Maven, Spring Boot 3.x, HTML/CSS/Vanilla JS.

1. **Compile and Test:**
   ```bash
   mvn clean test
   ```

2. **Start the Server:**
   ```bash
   mvn spring-boot:run
   ```

3. **View Dashboard:**
   Open a browser to [http://localhost:8080](http://localhost:8080)

## API Reference & cURL Examples

### Single-Key Operations

**GET an entry**
```bash
curl -X GET http://localhost:8080/api/cache/entries/user1
```

**PUT an entry (with TTL)**
```bash
curl -X PUT "http://localhost:8080/api/cache/entries/user1?ttl=30" -H "Content-Type: text/plain" -d "Sarvesh"
```

**DELETE an entry**
```bash
curl -X DELETE http://localhost:8080/api/cache/entries/user1
```

### Cache Management

**LIST all entries**
```bash
curl -X GET http://localhost:8080/api/cache/entries
```

**CLEAR cache**
```bash
curl -X DELETE http://localhost:8080/api/cache/entries
```

**GET metrics**
```bash
curl -X GET http://localhost:8080/api/cache/metrics
```

**RESET metrics**
```bash
curl -X POST http://localhost:8080/api/cache/metrics/reset
```

**SWITCH policy**
```bash
curl -X POST "http://localhost:8080/api/cache/policy?policy=LFU"
```

**CHANGE capacity**
```bash
curl -X POST "http://localhost:8080/api/cache/capacity?value=10"
```

### Demonstrations

**Run Sample Access Pattern**
```bash
curl -X POST http://localhost:8080/api/cache/demo
```

**LRU vs LFU Eviction Test**
```bash
curl -X POST http://localhost:8080/api/cache/demo/eviction
```

**TTL Demo**
```bash
curl -X POST http://localhost:8080/api/cache/demo/ttl
```

**Compare Policies**
```bash
curl -X POST "http://localhost:8080/api/cache/demo/compare?pattern=zipf"
```

## Limitations
* Single-node, purely in-memory cache (no persistence).
* Eviction uses O(n) scanning of the map values. For a production-ready cache, O(1) structures like doubly-linked lists or frequency buckets would be required.
