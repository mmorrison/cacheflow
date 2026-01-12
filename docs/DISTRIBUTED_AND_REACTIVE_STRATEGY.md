# Distributed & Reactive CacheFlow Strategy

> **Goal:** Elevate CacheFlow to Level 3 maturity by implementing robust distributed state management, real-time coordination, and operational excellence features.

## 📋 Strategy: "Distributed & Reactive"

We will focus on making the Russian Doll pattern robust in a distributed environment by moving state from local memory to Redis and implementing active communication between instances.

---

### Phase 1: Robust Distributed State (Level 2 Completion)
**Goal:** Ensure dependencies and state persist across restarts and are shared between instances.

#### 1. Redis-Backed Dependency Graph (⚠️ -> ✅)
*   **Problem:** `CacheDependencyTracker` currently uses in-memory `ConcurrentHashMap`. Dependencies are lost on restart and isolated per instance.
*   **Solution:** Refactor `CacheDependencyTracker` to use Redis Sets.
    *   **Data Structure:**
        *   `rd:deps:{cacheKey}` -> Set of `dependencyKeys`
        *   `rd:rev-deps:{dependencyKey}` -> Set of `cacheKeys`
    *   **Implementation:** Inject `StringRedisTemplate` into `CacheDependencyTracker`. Replace `dependencyGraph` and `reverseDependencyGraph` operations with `redisTemplate.opsForSet().add/remove/members`.
    *   **Optimization:** Use `pipelined` execution for batch operations to reduce network latency.
    *   **Maintenance:** Set default expiration (e.g., 24h) on dependency keys to prevent garbage accumulation.

#### 2. Touch Propagation Mechanism (⚠️ -> ✅)
*   **Problem:** `HasUpdatedAt` exists but isn't automatically updated.
*   **Solution:** Implement an Aspect-based approach for flexibility.
    *   **Action:** Create `TouchPropagationAspect` targeting methods annotated with `@CacheFlowUpdate`.
    *   **Logic:** When a child is updated, identify the parent via configuration and update its `updatedAt` field.
    *   **Annotation:** Introduce `@CacheFlowUpdate(parent = "userId")` or similar to link actions to parent entities.

---

### Phase 2: Active Distributed Coordination (Level 3 - Pub/Sub)
**Goal:** Real-time synchronization of Layer 1 (Local) caches across the cluster.

#### 3. Pub/Sub for Invalidation (❌ -> ✅)
*   **Problem:** When Instance A updates Redis, Instance B's local in-memory cache remains stale until TTL expires.
*   **Solution:** Implement Redis Pub/Sub.
    *   **Channel:** `cacheflow:invalidation`
    *   **Message:** JSON payload `{ "type": "EVICT", "keys": ["key1", "key2"], "origin": "instance-id" }`.
    *   **Publisher:** `CacheFlowServiceImpl` publishes a message after any `put` or `evict` operation.
    *   **Subscriber:** A `RedisMessageListenerContainer` bean that listens to the channel. Upon receipt (if `origin != self`), it evicts the keys from the *local* in-memory cache (L1) only.

---

### Phase 3: Operational Excellence (Level 3 - Advanced)
**Goal:** Enhance usability and performance for production readiness.

#### 4. Cache Warming & Preloading (❌ -> ✅)
*   **Problem:** Cold caches lead to latency spikes on startup or after deployments.
*   **Solution:** Add a "Warmer" interface and runner.
    *   **Interface:** `interface CacheWarmer { fun warm(cache: CacheFlowService) }`.
    *   **Runner:** A `CommandLineRunner` that auto-detects all `CacheWarmer` beans and executes them on startup.
    *   **Config:** Add properties `cacheflow.warming.enabled` (default `true`) and `cacheflow.warming.parallelism`.

#### 5. Tag-Based Cache Eviction (❌ -> ✅)
*   **Problem:** `evictByTags()` currently clears the entire local cache (aggressive) and doesn't support tag eviction for Redis. Only Edge cache properly supports tag-based eviction.
*   **Solution:** Implement proper tag tracking for Local and Redis caches.
    *   **Options:**
        *   Add tag metadata to `CacheEntry` and maintain a tag→keys index in both local and Redis storage.
        *   Alternatively, document current behavior as a known limitation and make it configurable.
    *   **Current Workaround:** Local cache calls `cache.clear()` on tag eviction to ensure consistency (safe but aggressive).
    *   **Location:** `CacheFlowServiceImpl.evictByTags()` (line 190)

---

### 📅 Execution Roadmap

#### Week 1: Distributed Core
1.  **Refactor `CacheDependencyTracker`:** Migrate from `ConcurrentHashMap` to `RedisTemplate` sets. (High Priority)
2.  **Add `TouchPropagation`:** Implement `@CacheFlowUpdate` aspect for parent touching.

#### Week 2: Real-time Sync
3.  **Implement Pub/Sub:** Set up Redis Topic, Publisher, and Subscriber to clear L1 caches globally. (High Priority for consistency)

#### Week 3: Polish
4.  **Implement Cache Warming:** Create the warmer interface and runner infrastructure.
5.  **Documentation:** Update docs to explain the distributed architecture and new configurations.
