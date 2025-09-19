package io.cacheflow.spring.service

/** Service interface for CacheFlow operations. */
interface CacheFlowService {
    fun get(key: String): Any?
    fun put(key: String, value: Any, ttl: Long = 3600)
    fun evict(key: String)
    fun evictAll()
    fun evictByTags(vararg tags: String)
    fun size(): Long
    fun keys(): Set<String>
}
