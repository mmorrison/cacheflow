package io.cacheflow.spring.fragment

/**
 * Service interface for fragment management operations in Russian Doll caching.
 *
 * This interface handles bulk operations, statistics, and administrative functions for fragment
 * caching.
 */
interface FragmentManagementService {

    /**
     * Invalidates all fragments with the given tag.
     *
     * @param tag The tag to match for invalidation
     */
    fun invalidateFragmentsByTag(tag: String)

    /** Invalidates all fragments. */
    fun invalidateAllFragments()

    /**
     * Gets the number of cached fragments.
     *
     * @return The number of cached fragments
     */
    fun getFragmentCount(): Long

    /**
     * Gets all fragment keys.
     *
     * @return Set of all fragment keys
     */
    fun getFragmentKeys(): Set<String>
}
