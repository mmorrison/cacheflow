package io.cacheflow.spring.management

import io.cacheflow.spring.service.CacheFlowService
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
import org.springframework.stereotype.Component

/** Management endpoint for CacheFlow operations. */
@Component
@Endpoint(id = "cacheflow")
class CacheFlowManagementEndpoint(private val cacheService: CacheFlowService) {

    @ReadOperation
    fun getCacheInfo(): Map<String, Any> {
        return mapOf("size" to cacheService.size(), "keys" to cacheService.keys())
    }

    @WriteOperation
    fun evictByPattern(@Selector pattern: String): Map<String, Any> {
        // Simple pattern matching - in a real implementation, you'd use regex
        val keys = cacheService.keys().filter { it.contains(pattern) }
        keys.forEach { cacheService.evict(it) }

        return mapOf("evicted" to keys.size, "pattern" to pattern)
    }

    @WriteOperation
    fun evictByTags(@Selector tags: String): Map<String, Any> {
        val tagArray = tags.split(",").map { it.trim() }.toTypedArray()
        cacheService.evictByTags(*tagArray)

        return mapOf("evicted" to "all", "tags" to tagArray)
    }

    @WriteOperation
    fun evictAll(): Map<String, Any> {
        cacheService.evictAll()
        return mapOf("evicted" to "all")
    }
}
