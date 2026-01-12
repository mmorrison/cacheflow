package io.cacheflow.spring.service.impl

import io.cacheflow.spring.service.CacheFlowService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/** Simple in-memory implementation of CacheFlowService. */
@Service
class CacheFlowServiceImpl : CacheFlowService {
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val millisecondsPerSecond = 1_000L

    override fun get(key: String): Any? {
        val entry = cache[key] ?: return null

        return if (isExpired(entry)) {
            cache.remove(key)
            null
        } else {
            entry.value
        }
    }

    private fun isExpired(entry: CacheEntry): Boolean = System.currentTimeMillis() > entry.expiresAt

    override fun put(
        key: String,
        value: Any,
        ttl: Long,
    ) {
        val expiresAt = System.currentTimeMillis() + ttl * millisecondsPerSecond
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

    private data class CacheEntry(
        val value: Any,
        val expiresAt: Long,
    )
}
