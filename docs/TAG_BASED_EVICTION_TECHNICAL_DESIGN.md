# Tag-Based Eviction Technical Design

## 📋 Overview
Currently, CacheFlow's tag-based eviction is only fully supported at the Edge layer. The Local (L1) and Redis (L2) layers lack the necessary metadata and indexing to perform efficient tag-based purges, currently resorting to aggressive cache clearing.

## 🛠️ Required Changes

### 1. Metadata Enhancement
The `CacheEntry` needs to store the tags associated with the value at the time of insertion.

```kotlin
data class CacheEntry(
    val value: Any,
    val expiresAt: Long,
    val tags: Set<String> = emptySet() // Added metadata
)
```

### 2. Local Indexing (L1)
To avoid scanning the entire `ConcurrentHashMap` during eviction, we need a reverse index: `Map<Tag, Set<CacheKey>>`.

- **Implementation:** Use `ConcurrentHashMap<String, MutableSet<String>>` for the tag index.
- **Maintenance:** 
    - `put`: Add key to index for each tag.
    - `evict`: Remove key from index.
    - `get`: Clean up index if entry is found to be expired.

### 3. Redis Indexing (L2)
Use Redis Sets to store the relationship between tags and keys.

- **Key Pattern:** `rd:tag:{tagName}` -> Set of cache keys.
- **Operations:**
    - `SADD` on `put`.
    - `SREM` on `evict`.
    - `SMEMBERS` + `DEL` on `evictByTags`.

### 4. Consistency Considerations
- **Orchestration:** When `evictByTags` is called, it must propagate through all three layers (Local Index -> Redis Index -> Edge API).
- **Race Conditions:** Use atomic Redis operations (or Lua scripts) to ensure the tag index stays in sync with the actual data keys.

## 📅 Implementation Steps
1. **Update `CacheFlowServiceImpl`**: Store tags in `CacheEntry` and maintain a local `tagIndex`.
2. **Update Redis Logic**: Implement `SADD` and `SMEMBERS` logic in the service.
3. **Refactor `CacheFlowAspect`**: Extract tags from the `@CacheFlow` annotation and pass them to the `put` method.
4. **Testing**: Add specific tests for partial eviction (e.g., evicting "users" tag should not affect "products" entries).
