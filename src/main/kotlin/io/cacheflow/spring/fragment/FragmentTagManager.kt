package io.cacheflow.spring.fragment

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component

/**
 * Manages fragment tags for group-based operations in Russian Doll caching.
 *
 * This service handles the association between fragments and tags, allowing for efficient
 * group-based invalidation and retrieval operations.
 */
@Component
class FragmentTagManager {

    private val fragmentTags = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * Associates a fragment with a tag for group-based operations.
     *
     * @param key The fragment key
     * @param tag The tag to associate with the fragment
     */
    fun addFragmentTag(key: String, tag: String) {
        fragmentTags.computeIfAbsent(tag) { ConcurrentHashMap.newKeySet() }.add(key)
    }

    /**
     * Removes a tag association from a fragment.
     *
     * @param key The fragment key
     * @param tag The tag to remove
     */
    fun removeFragmentTag(key: String, tag: String) {
        fragmentTags[tag]?.remove(key)
        if (fragmentTags[tag]?.isEmpty() == true) {
            fragmentTags.remove(tag)
        }
    }

    /**
     * Gets all fragments associated with a tag.
     *
     * @param tag The tag to get fragments for
     * @return Set of fragment keys
     */
    fun getFragmentsByTag(tag: String): Set<String> = fragmentTags[tag]?.toSet() ?: emptySet()

    /**
     * Gets all tags associated with a fragment.
     *
     * @param key The fragment key
     * @return Set of tags
     */
    fun getFragmentTags(key: String): Set<String> {
        return fragmentTags
                .entries
                .filter { (_, keys) -> keys.contains(key) }
                .map { (tag, _) -> tag }
                .toSet()
    }

    /**
     * Removes a fragment from all tag associations.
     *
     * @param key The fragment key to remove
     */
    fun removeFragmentFromAllTags(key: String) {
        fragmentTags.values.forEach { it.remove(key) }
    }

    /** Clears all tag associations. */
    fun clearAllTags() {
        fragmentTags.clear()
    }

    /**
     * Gets all available tags.
     *
     * @return Set of all tag names
     */
    fun getAllTags(): Set<String> = fragmentTags.keys.toSet()

    /**
     * Gets the number of tags.
     *
     * @return The number of tags
     */
    fun getTagCount(): Int = fragmentTags.size
}
