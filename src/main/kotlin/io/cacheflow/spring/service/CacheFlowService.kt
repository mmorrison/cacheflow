package io.cacheflow.spring.service

/** Service interface for CacheFlow operations. */
interface CacheFlowService {
    /**
     * Retrieves a value from the cache.
     *
     * @param key The cache key
     * @return The cached value or null if not found
     */
    fun get(key: String): Any?

    /**
     * Stores a value in the cache.
     *
     * @param key The cache key
     * @param value The value to cache
     * @param ttl Time to live in seconds
     * @param tags Tags associated with this cache entry
     */
    fun put(
        key: String,
        value: Any,
        ttl: Long = 3_600,
        tags: Set<String> = emptySet(),
    )

    /**
     * Evicts a specific cache entry.
     *
     * @param key The cache key to evict
     */
    fun evict(key: String)

    /** Evicts all cache entries. */
    fun evictAll()

    /**
     * Evicts cache entries by tags.
     *
     * @param tags The tags to match for eviction
     */
    fun evictByTags(vararg tags: String)

    /**
     * Evicts a specific cache entry from local storage only.
     *
     * @param key The cache key to evict
     * @return The evicted entry if it existed
     */
    fun evictLocal(key: String): Any?

    /**
     * Evicts cache entries by tags from the local cache only.
     * Used for distributed cache coordination.
     *
     * @param tags The tags to match for eviction
     */
    fun evictLocalByTags(vararg tags: String)

    /**
     * Gets the current cache size.
     *
     * @return The number of entries in the cache
     */
    fun size(): Long

    /**
     * Gets all cache keys.
     *
     * @return Set of all cache keys
     */
    fun keys(): Set<String>

    /**
     * Evicts all cache entries from the local cache only.
     * Used for distributed cache coordination.
     */
    fun evictLocalAll()
}
