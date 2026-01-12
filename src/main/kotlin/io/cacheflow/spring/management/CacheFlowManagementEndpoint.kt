package io.cacheflow.spring.management

import io.cacheflow.spring.service.CacheFlowService
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
import org.springframework.stereotype.Component

private const val EVICTED_KEY = "evicted"

/** Management endpoint for CacheFlow operations. */
@Component
@Endpoint(id = "cacheflow")
class CacheFlowManagementEndpoint(
    private val cacheService: CacheFlowService,
) {
    /**
     * Gets cache information.
     *
     * @return Map containing cache size and keys
     */

    @ReadOperation
    fun getCacheInfo() = mapOf("size" to cacheService.size(), "keys" to cacheService.keys())

    /**
     * Evicts cache entries by pattern.
     *
     * @param pattern The pattern to match against cache keys
     * @return Map containing eviction results
     */

    @WriteOperation
    fun evictByPattern(
        @Selector pattern: String,
    ): Map<String, Any> {
        // Simple pattern matching - in a real implementation, you'd use regex
        val keys = cacheService.keys().filter { it.contains(pattern) }
        keys.forEach { cacheService.evict(it) }
        return mapOf(EVICTED_KEY to keys.size, "pattern" to pattern)
    }

    /**
     * Evicts cache entries by tags.
     *
     * @param tags Comma-separated list of tags
     * @return Map containing eviction results
     */

    @WriteOperation
    fun evictByTags(
        @Selector tags: String,
    ): Map<String, Any> {
        val tagArray = tags.split(",").map { it.trim() }.toTypedArray()
        cacheService.evictByTags(*tagArray)
        return mapOf(EVICTED_KEY to "all", "tags" to tagArray)
    }

    /**
     * Evicts all cache entries.
     *
     * @return Map containing eviction results
     */

    @WriteOperation
    fun evictAll() = mapOf(EVICTED_KEY to "all").also { cacheService.evictAll() }
}
