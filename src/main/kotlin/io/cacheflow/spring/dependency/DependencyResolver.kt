package io.cacheflow.spring.dependency

/**
 * Interface for managing cache dependencies in Russian Doll caching.
 *
 * This interface provides methods to track dependencies between cache entries and invalidate
 * dependent caches when a dependency changes.
 */
interface DependencyResolver {

    /**
     * Tracks a dependency relationship between a cache key and a dependency key.
     *
     * @param cacheKey The cache key that depends on the dependency
     * @param dependencyKey The key that the cache depends on
     */
    fun trackDependency(cacheKey: String, dependencyKey: String)

    /**
     * Invalidates all caches that depend on the given dependency key.
     *
     * @param dependencyKey The dependency key that has changed
     * @return Set of cache keys that were invalidated
     */
    fun invalidateDependentCaches(dependencyKey: String): Set<String>

    /**
     * Gets all dependencies for a given cache key.
     *
     * @param cacheKey The cache key to get dependencies for
     * @return Set of dependency keys
     */
    fun getDependencies(cacheKey: String): Set<String>

    /**
     * Gets all cache keys that depend on the given dependency key.
     *
     * @param dependencyKey The dependency key
     * @return Set of dependent cache keys
     */
    fun getDependentCaches(dependencyKey: String): Set<String>

    /**
     * Removes a specific dependency relationship.
     *
     * @param cacheKey The cache key
     * @param dependencyKey The dependency key to remove
     */
    fun removeDependency(cacheKey: String, dependencyKey: String)

    /**
     * Clears all dependencies for a cache key.
     *
     * @param cacheKey The cache key to clear dependencies for
     */
    fun clearDependencies(cacheKey: String)

    /**
     * Gets the total number of tracked dependencies.
     *
     * @return Number of dependency relationships
     */
    fun getDependencyCount(): Int
}
