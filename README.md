<div align="center">
  <img src="assets/acentra.png" alt="Acentra Health" width="150" style="margin: 0 20px; vertical-align: middle;"/>
  <img src="assets/rajalakshmi.png" alt="Rajalakshmi Engineering College" width="300" style="margin: 0 20px; vertical-align: middle;"/>
  <img src="assets/hackforge.png" alt="HACKFORGE.ai" width="150" style="margin: 0 20px; vertical-align: middle;"/>
</div>

<br/>

<h1 align="center">CacheFusion</h1>
<h3 align="center">Adaptive Hybrid Java Cache with Live React Dashboard</h3>

A high-performance, Log-Structured Merge (LSM) inspired Java Custom Cache Library. It implements a multi-tiered memory architecture to optimize lookup latency while drastically reducing object overhead for cold data. 

Includes selectable eviction policies (LRU/LFU), per-entry TTL, and a live React.js frontend metrics dashboard for real-time visualization.

## 🚀 Minimum Requirements Fulfilled

| Requirement | Implementation Detail | Verified By |
|---|---|---|
| **In-memory cache** | `CacheManager` coordinating Hot, Warm, and Cold tiers | Unit tests + Live Demo |
| **LRU eviction strategy** | `LRUEvictionPolicy` (O(1) Custom Doubly Linked List) | `LRUEvictionTest` |
| **LFU eviction strategy** | `LFUEvictionPolicy` (O(1) Frequency Buckets) | `LFUEvictionTest` |
| **Selectable eviction policy** | `POST /api/cache/policy` + React UI Dropdown | `PolicySwitchTest` |
| **Per-entry TTL** | `CacheEntry.expiryTime` | `TTLTest` |
| **TTL independent of eviction**| TTL uses Lazy Expiration on `GET` and Tombstoning | `TTLTest` |
| **Thread-safe concurrent access**| Lock-free `ConcurrentHashMap` & `ConcurrentSkipListMap` | `ConcurrencyTest` (50 threads) |
| **Frontend metrics panel** | React + Vite frontend (`/frontend`) with dynamic polling | Live Dashboard |
| **Display hit / miss rate** | `CacheMetrics` streamed to React-ChartJS | `MetricsTest` |
| **Show metrics vs access pattern**| `/demo` endpoints simulating synthetic dynamic loads | Live Dashboard |

---

## 🏗️ Architecture: The 3 Tiers

CacheFusion abandons the traditional monolithic HashMap cache in favor of a hybrid memory hierarchy, inspired by modern LSM Trees.

### 1. Hot Cache (Tier 1: O(1) Latency)
*   **Data Structure:** `ConcurrentHashMap`
*   **Purpose:** Houses the most frequently accessed keys. Data is strictly promoted to this layer only after crossing a hit threshold, ensuring pure O(1) lookup speeds for active data.

### 2. Warm Cache / MemTable (Tier 2: O(log n) Writes)
*   **Data Structure:** `ConcurrentSkipListMap`
*   **Purpose:** Absorbs all incoming `PUT` requests lock-free. Data is kept naturally sorted without the overhead of rehashing massive hash maps.

### 3. Cold Cache / Immutable Runs (Tier 3: Extreme Memory Density)
*   **Data Structure:** Primitive `CacheEntry[]` arrays paired with **Bloom Filters**.
*   **Purpose:** When the MemTable fills up, it flushes to an Immutable Run. Arrays lack object-pointer overhead, allowing the cache to store millions of cold items with a fraction of the RAM footprint.
*   **Read Optimization (Bloom Filters):** Every Immutable Run generates a mathematically optimized bit-array Bloom Filter (1% false-positive rate). When a `GET` request queries the cold tier, it checks the Bloom Filter first. If the key is absent, it skips the binary search entirely, resulting in lightning-fast $O(1)$ negative lookups.

---

## ♻️ Deletion & Compaction

*   **Logical Deletion (Tombstones):** When you `DELETE` a key, we insert a Tombstone marker instead of blocking readers to physically erase it.
*   **Pairwise Compaction:** A background `CompactionManager` merges older Immutable Runs periodically (Merge Sort style). During this merge, it discards old versions of keys and permanently drops Tombstoned data, freeing up Java Heap space efficiently.

---

## 💻 How to Run

### Technologies Used
*   **Backend:** Java 17+, Maven, Spring Boot 3.x
*   **Frontend:** React, Vite, Tailwind CSS, Chart.js

### 1. Start the Backend
```bash
# In the root directory
mvn clean test
mvn spring-boot:run
```
*The backend API runs on `http://localhost:8080`*

### 2. Start the Frontend Dashboard
```bash
# In a new terminal, navigate to the frontend directory
cd frontend
npm install
npm run dev
```
*The React Dashboard will open on `http://localhost:5173`*

---

## 📡 API Reference & cURL Examples

### Single-Key Operations
*   **GET:** `curl -X GET http://localhost:8080/api/cache/entries/K1`
*   **PUT (with TTL):** `curl -X PUT "http://localhost:8080/api/cache/entries/K1?ttl=60" -d "Value1"`
*   **DELETE:** `curl -X DELETE http://localhost:8080/api/cache/entries/K1`

### Cache Management
*   **LIST All (View Tiers):** `curl -X GET http://localhost:8080/api/cache/entries`
*   **CLEAR:** `curl -X DELETE http://localhost:8080/api/cache/entries`
*   **GET Metrics:** `curl -X GET http://localhost:8080/api/cache/metrics`
*   **SWITCH Policy (LRU/LFU):** `curl -X POST "http://localhost:8080/api/cache/policy?policy=LFU"`
*   **CHANGE Global Capacity:** `curl -X POST "http://localhost:8080/api/cache/capacity?value=100"`

### Demonstrations
*   **Run Sample Load (Respects Capacity):** `curl -X POST http://localhost:8080/api/cache/demo`
*   **Force Eviction:** `curl -X POST http://localhost:8080/api/cache/demo/eviction`
*   **Test TTL Expiration:** `curl -X POST http://localhost:8080/api/cache/demo/ttl`

---

## 📚 Complete System Documentation
A full deep-dive into the algorithms, use cases, and Mermaid flowcharts of the GET/PUT pipelines can be found in the included `CacheFusion_Documentation.md` file.
