package io.cacheflow.spring.service.impl

import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.service.CacheFlowService
import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Service

/** Simple in-memory implementation of CacheFlowService. */
@Service
class CacheFlowServiceImpl(private val properties: CacheFlowProperties) : CacheFlowService {

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override fun get(key: String): Any? {
        val entry = cache[key] ?: return null

        // Check if expired
        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(key)
            return null
        }

        return entry.value
    }

    override fun put(key: String, value: Any, ttl: Long) {
        val expiresAt = System.currentTimeMillis() + (ttl * 1000)
        cache[key] = CacheEntry(value, expiresAt)
    }

    override fun evict(key: String) {
        cache.remove(key)
    }

    override fun evictAll() {
        cache.clear()
    }

    override fun evictByTags(vararg tags: String) {
        // Simple implementation - in a real implementation, you'd track tags
        // For now, we'll just clear all entries
        cache.clear()
    }

    override fun size(): Long = cache.size.toLong()

    override fun keys(): Set<String> = cache.keys.toSet()

    private data class CacheEntry(val value: Any, val expiresAt: Long)
}
