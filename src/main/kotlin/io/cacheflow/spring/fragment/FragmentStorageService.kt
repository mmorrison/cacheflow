package io.cacheflow.spring.fragment

/**
 * Service interface for basic fragment storage operations in Russian Doll caching.
 *
 * This interface handles the core CRUD operations for fragment caching including storing,
 * retrieving, and invalidating individual fragments.
 */
interface FragmentStorageService {
    /**
     * Caches a fragment with the given key and TTL.
     *
     * @param key The fragment cache key
     * @param fragment The fragment content to cache
     * @param ttl Time to live in seconds
     */
    fun cacheFragment(
        key: String,
        fragment: String,
        ttl: Long,
    )

    /**
     * Retrieves a fragment from the cache.
     *
     * @param key The fragment cache key
     * @return The cached fragment or null if not found
     */
    fun getFragment(key: String): String?

    /**
     * Invalidates a specific fragment.
     *
     * @param key The fragment key to invalidate
     */
    fun invalidateFragment(key: String)

    /**
     * Checks if a fragment exists in the cache.
     *
     * @param key The fragment key to check
     * @return true if the fragment exists, false otherwise
     */
    fun hasFragment(key: String): Boolean
}
