package io.cacheflow.spring.service.impl

import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.edge.service.EdgeCacheIntegrationService
import io.cacheflow.spring.service.CacheFlowService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Implementation of CacheFlowService supporting Local -> Redis -> Edge layering. */
@Service
class CacheFlowServiceImpl(
    private val properties: CacheFlowProperties,
    private val redisTemplate: RedisTemplate<String, Any>? = null,
    private val edgeCacheService: EdgeCacheIntegrationService? = null,
    private val meterRegistry: MeterRegistry? = null,
    private val redisCacheInvalidator: io.cacheflow.spring.messaging.RedisCacheInvalidator? = null,
) : CacheFlowService {
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val localTagIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val logger = LoggerFactory.getLogger(CacheFlowServiceImpl::class.java)
    private val millisecondsPerSecond = 1_000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Metrics
    private val hits = meterRegistry?.counter("cacheflow.hits")
    private val misses = meterRegistry?.counter("cacheflow.misses")
    private val puts = meterRegistry?.counter("cacheflow.puts")
    private val evictions = meterRegistry?.counter("cacheflow.evictions")

    private val localHits: Counter? = meterRegistry?.counter("cacheflow.local.hits")
    private val localMisses: Counter? = meterRegistry?.counter("cacheflow.local.misses")
    private val redisHits: Counter? = meterRegistry?.counter("cacheflow.redis.hits")
    private val redisMisses: Counter? = meterRegistry?.counter("cacheflow.redis.misses")

    private val sizeGauge =
        meterRegistry?.gauge(
            "cacheflow.size",
            cache,
        ) { it.size.toDouble() }

    private val isRedisEnabled = properties.storage == CacheFlowProperties.StorageType.REDIS && redisTemplate != null

    override fun get(key: String): Any? {
        // 1. Check Local Cache
        val localEntry = cache[key]
        if (localEntry != null) {
            if (!isExpired(localEntry)) {
                logger.debug("Local cache hit for key: {}", key)
                localHits?.increment()
                return localEntry.value
            }
            evict(key) // Explicitly evict to clean up indexes
        }
        localMisses?.increment()

        // 2. Check Redis Cache
        if (isRedisEnabled) {
            return try {
                val redisValue = redisTemplate?.opsForValue()?.get(getRedisKey(key))
                if (redisValue != null) {
                    logger.debug("Redis cache hit for key: {}", key)
                    redisHits?.increment()
                    // Populate local cache (L1) from Redis (L2)
                    // Note: Tags are lost if we don't store them in L2 as well.
                    // In a full implementation, we might store metadata in a separate Redis key.
                    // For now, we populate local without tags on Redis hit.
                    putLocal(key, redisValue, properties.defaultTtl, emptySet())
                    redisValue
                } else {
                    redisMisses?.increment()
                    null
                }
            } catch (e: Exception) {
                logger.error("Error retrieving from Redis", e)
                redisMisses?.increment()
                null
            }
        }

        return null
    }

    private fun isExpired(entry: CacheEntry): Boolean = System.currentTimeMillis() > entry.expiresAt

    override fun put(
        key: String,
        value: Any,
        ttl: Long,
        tags: Set<String>,
    ) {
        puts?.increment()
        // 1. Put Local
        putLocal(key, value, ttl, tags)

        // 2. Put Redis
        if (isRedisEnabled) {
            try {
                val redisKey = getRedisKey(key)
                redisTemplate?.opsForValue()?.set(redisKey, value, ttl, TimeUnit.SECONDS)

                // Index tags in Redis
                tags.forEach { tag ->
                    redisTemplate?.opsForSet()?.add(getRedisTagKey(tag), key)
                }
            } catch (e: Exception) {
                logger.error("Error writing to Redis", e)
            }
        }
    }

    private fun putLocal(
        key: String,
        value: Any,
        ttl: Long,
        tags: Set<String>,
    ) {
        val expiresAt = System.currentTimeMillis() + ttl * millisecondsPerSecond
        cache[key] = CacheEntry(value, expiresAt, tags)

        // Update local tag index
        tags.forEach { tag ->
            localTagIndex.computeIfAbsent(tag) { ConcurrentHashMap.newKeySet() }.add(key)
        }
    }

    override fun evict(key: String) {
        evictions?.increment()

        // 1. Evict Local and clean up index
        val entry = evictLocal(key) as? CacheEntry

        // 2. Evict Redis
        if (isRedisEnabled) {
            try {
                val redisKey = getRedisKey(key)
                redisTemplate?.delete(redisKey)

                // Clean up tag index in Redis
                entry?.tags?.forEach { tag ->
                    redisTemplate?.opsForSet()?.remove(getRedisTagKey(tag), key)
                }

                // 3. Publish Invalidation Message
                redisCacheInvalidator?.publish(io.cacheflow.spring.messaging.InvalidationType.EVICT, keys = setOf(key))
            } catch (e: Exception) {
                logger.error("Error evicting from Redis", e)
            }
        }

        // 3. Evict Edge
        if (edgeCacheService != null) {
            scope.launch {
                try {
                    edgeCacheService.purgeCacheKey(properties.baseUrl, key).collect { result ->
                        if (!result.success) {
                            logger.warn(
                                "Failed to purge edge cache for key {}: {}",
                                key,
                                result.error?.message ?: "Unknown error",
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error purging edge cache", e)
                }
            }
        }
    }

    override fun evictAll() {
        evictions?.increment()
        cache.clear()
        localTagIndex.clear()

        // 2. Redis Eviction
        if (isRedisEnabled) {
            try {
                // Delete all cache data keys
                val dataKeys = redisTemplate?.keys(getRedisKey("*"))
                if (!dataKeys.isNullOrEmpty()) {
                    redisTemplate?.delete(dataKeys)
                }

                // Delete all tag index keys
                val tagKeys = redisTemplate?.keys(getRedisTagKey("*"))
                if (!tagKeys.isNullOrEmpty()) {
                    redisTemplate?.delete(tagKeys)
                }

                // 3. Publish Invalidation Message
                redisCacheInvalidator?.publish(io.cacheflow.spring.messaging.InvalidationType.EVICT_ALL)
            } catch (e: Exception) {
                logger.error("Error clearing Redis cache", e)
            }
        }

        if (edgeCacheService != null) {
            scope.launch {
                try {
                    edgeCacheService.purgeAll().collect {}
                } catch (e: Exception) {
                    logger.error("Error purging all from edge cache", e)
                }
            }
        }
    }

    override fun evictByTags(vararg tags: String) {
        evictions?.increment()

        tags.forEach { tag ->
            // 1. Local Eviction
            evictLocalByTags(tag)

            // 2. Redis Eviction
            if (isRedisEnabled) {
                try {
                    val tagKey = getRedisTagKey(tag)
                    val keys = redisTemplate?.opsForSet()?.members(tagKey)
                    if (!keys.isNullOrEmpty()) {
                        // Delete actual data keys
                        val redisKeys = keys.map { getRedisKey(it as String) }
                        redisTemplate?.delete(redisKeys)

                        // Remove tag key
                        redisTemplate?.delete(tagKey)
                    }

                    // 3. Publish Invalidation Message
                    redisCacheInvalidator?.publish(io.cacheflow.spring.messaging.InvalidationType.EVICT_BY_TAGS, tags = setOf(tag))
                } catch (e: Exception) {
                    logger.error("Error evicting by tag from Redis", e)
                }
            }

            // 3. Edge Eviction
            if (edgeCacheService != null) {
                scope.launch {
                    try {
                        edgeCacheService.purgeByTag(tag).collect {}
                    } catch (e: Exception) {
                        logger.error("Error purging tag $tag from edge cache", e)
                    }
                }
            }
        }
    }

    override fun evictLocal(key: String): Any? {
        val entry = cache.remove(key)
        entry?.tags?.forEach { tag ->
            localTagIndex[tag]?.remove(key)
            if (localTagIndex[tag]?.isEmpty() == true) {
                localTagIndex.remove(tag)
            }
        }
        return entry
    }

    override fun evictLocalByTags(vararg tags: String) {
        tags.forEach { tag ->
            localTagIndex.remove(tag)?.forEach { key ->
                cache.remove(key)
            }
        }
    }

    override fun evictLocalAll() {
        cache.clear()
        localTagIndex.clear()
    }

    override fun size(): Long = cache.size.toLong()

    override fun keys(): Set<String> = cache.keys.toSet()

    private fun getRedisKey(key: String): String = properties.redis.keyPrefix + "data:" + key

    private fun getRedisTagKey(tag: String): String = properties.redis.keyPrefix + "tag:" + tag

    private data class CacheEntry(
        val value: Any,
        val expiresAt: Long,
        val tags: Set<String> = emptySet(),
    )
}
