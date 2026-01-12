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
) : CacheFlowService {
    private val logger = LoggerFactory.getLogger(CacheFlowServiceImpl::class.java)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val millisecondsPerSecond = 1_000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val localHits: Counter? = meterRegistry?.counter("cacheflow.local.hits")
    private val localMisses: Counter? = meterRegistry?.counter("cacheflow.local.misses")
    private val redisHits: Counter? = meterRegistry?.counter("cacheflow.redis.hits")
    private val redisMisses: Counter? = meterRegistry?.counter("cacheflow.redis.misses")
    private val puts: Counter? = meterRegistry?.counter("cacheflow.puts")
    private val evictions: Counter? = meterRegistry?.counter("cacheflow.evictions")

    private val isRedisEnabled: Boolean
        get() = properties.storage == CacheFlowProperties.StorageType.REDIS && redisTemplate != null

    override fun get(key: String): Any? {
        // 1. Check Local Cache
        val localEntry = cache[key]
        if (localEntry != null) {
            if (!isExpired(localEntry)) {
                logger.debug("Local cache hit for key: {}", key)
                localHits?.increment()
                return localEntry.value
            }
            cache.remove(key)
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
                    val ttl = properties.defaultTtl
                    putLocal(key, redisValue, ttl)
                    redisValue
                } else {
                    redisMisses?.increment()
                    null
                }
            } catch (e: Exception) {
                logger.error("Error retrieving from Redis", e)
                redisMisses?.increment() // Count error as miss
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
    ) {
        puts?.increment()
        // 1. Put Local
        putLocal(key, value, ttl)

        // 2. Put Redis
        if (isRedisEnabled) {
            try {
                redisTemplate?.opsForValue()?.set(getRedisKey(key), value, ttl, TimeUnit.SECONDS)
            } catch (e: Exception) {
                logger.error("Error writing to Redis", e)
            }
        }
    }

    private fun putLocal(
        key: String,
        value: Any,
        ttl: Long,
    ) {
        val expiresAt = System.currentTimeMillis() + ttl * millisecondsPerSecond
        cache[key] = CacheEntry(value, expiresAt)
    }

    override fun evict(key: String) {
        evictions?.increment()
        // 1. Evict Local
        cache.remove(key)

        // 2. Evict Redis
        if (isRedisEnabled) {
            try {
                redisTemplate?.delete(getRedisKey(key))
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
        if (isRedisEnabled) {
            try {
                val keys = redisTemplate?.keys(properties.redis.keyPrefix + "*")
                if (!keys.isNullOrEmpty()) {
                    redisTemplate?.delete(keys)
                }
            } catch (e: Exception) {
                logger.error("Error evicting all from Redis", e)
            }
        }
        
        if (edgeCacheService != null) {
            scope.launch {
                try {
                    edgeCacheService.purgeAll().collect {
                         // consume flow
                    }
                } catch (e: Exception) {
                    logger.error("Error purging all from edge cache", e)
                }
            }
        }
    }

    override fun evictByTags(vararg tags: String) {
        // Local tag support is limited in this simple Map implementation. 
        // Real implementation would need a Tag->Key mapping.
        // For Alpha/Beta, we can skip local tag eviction or clear all if robust implementation is missing.
        // But for Redis and Edge, we can pass it through if supported.
        
        // Edge supports tags
        if (edgeCacheService != null) {
             tags.forEach { tag ->
                scope.launch {
                    try {
                        edgeCacheService.purgeByTag(tag).collect { }
                    } catch (e: Exception) {
                        logger.error("Error purging tag $tag from edge cache", e)
                    }
                }
             }
        }
        
        // For Redis and Local, without tag mapping, we can't easily evict by tag unless we scan values or maintain index.
        // Leaving as TODO or no-op for Local/Redis for now, or just clearing Local to be safe?
        // Clearing local is safe but aggressive.
        cache.clear() 
    }

    override fun size(): Long = cache.size.toLong()

    override fun keys(): Set<String> = cache.keys.toSet()

    private fun getRedisKey(key: String): String = properties.redis.keyPrefix + key

    private data class CacheEntry(
        val value: Any,
        val expiresAt: Long,
    )
}
