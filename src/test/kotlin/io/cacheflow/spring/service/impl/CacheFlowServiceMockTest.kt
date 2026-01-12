package io.cacheflow.spring.service.impl

import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.edge.EdgeCacheResult
import io.cacheflow.spring.edge.EdgeCacheOperation
import io.cacheflow.spring.edge.service.EdgeCacheIntegrationService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.SetOperations
import java.util.concurrent.TimeUnit

class CacheFlowServiceMockTest {

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, Any>
    
    @Mock
    private lateinit var setOperations: SetOperations<String, Any>

    @Mock
    private lateinit var edgeCacheService: EdgeCacheIntegrationService

    @Mock
    private lateinit var meterRegistry: MeterRegistry

    @Mock
    private lateinit var localHitCounter: Counter
    @Mock
    private lateinit var localMissCounter: Counter
    @Mock
    private lateinit var redisHitCounter: Counter
    @Mock
    private lateinit var redisMissCounter: Counter
    @Mock
    private lateinit var putCounter: Counter
    @Mock
    private lateinit var evictCounter: Counter

    private lateinit var cacheService: CacheFlowServiceImpl
    private lateinit var properties: CacheFlowProperties

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Setup Properties
        properties = CacheFlowProperties(
            storage = CacheFlowProperties.StorageType.REDIS,
            enabled = true,
            defaultTtl = 3600,
            baseUrl = "https://api.example.com",
            redis = CacheFlowProperties.RedisProperties(keyPrefix = "test-prefix:")
        )

        // Setup Redis Mocks
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(redisTemplate.opsForSet()).thenReturn(setOperations)

        // Setup Metrics Mocks
        `when`(meterRegistry.counter("cacheflow.local.hits")).thenReturn(localHitCounter)
        `when`(meterRegistry.counter("cacheflow.local.misses")).thenReturn(localMissCounter)
        `when`(meterRegistry.counter("cacheflow.redis.hits")).thenReturn(redisHitCounter)
        `when`(meterRegistry.counter("cacheflow.redis.misses")).thenReturn(redisMissCounter)
        `when`(meterRegistry.counter("cacheflow.puts")).thenReturn(putCounter)
        `when`(meterRegistry.counter("cacheflow.evictions")).thenReturn(evictCounter)

        // Setup Edge Mocks
        `when`(edgeCacheService.purgeCacheKey(anyString(), anyString())).thenReturn(
            flowOf(EdgeCacheResult.success("test", EdgeCacheOperation.PURGE_URL))
        )
        `when`(edgeCacheService.purgeAll()).thenReturn(
            flowOf(EdgeCacheResult.success("test", EdgeCacheOperation.PURGE_ALL))
        )
        `when`(edgeCacheService.purgeByTag(anyString())).thenReturn(
             flowOf(EdgeCacheResult.success("test", EdgeCacheOperation.PURGE_TAG))
        )

        cacheService = CacheFlowServiceImpl(properties, redisTemplate, edgeCacheService, meterRegistry)
    }

    @Test
    fun `get should check local cache first`() {
        // First put to populate local cache
        cacheService.put("key1", "value1", 60)
        verify(putCounter, times(1)).increment() // 1 put

        // Then get
        val result = cacheService.get("key1")
        assertEquals("value1", result)
        
        // Should hit local, not call Redis get
        verify(valueOperations, never()).get(anyString())
        // Verify local hit counter
        verify(localHitCounter, times(1)).increment()
    }

    @Test
    fun `get should check Redis on local miss`() {
        val key = "key1"
        val redisKey = "test-prefix:data:key1"
        val value = "redis-value"

        `when`(valueOperations.get(redisKey)).thenReturn(value)

        val result = cacheService.get(key)
        assertEquals(value, result)

        verify(valueOperations).get(redisKey)
        // Verify redis hit counter was incremented
        verify(redisHitCounter, times(1)).increment() 
        // Also local miss
        verify(localMissCounter, times(1)).increment()
    }

    @Test
    fun `get should populate local cache on Redis hit`() {
        val key = "key1"
        val redisKey = "test-prefix:data:key1"
        val value = "redis-value"

        `when`(valueOperations.get(redisKey)).thenReturn(value)

        // First call - hits Redis
        val result1 = cacheService.get(key)
        assertEquals(value, result1)

        // Second call - should hit local cache
        val result2 = cacheService.get(key)
        assertEquals(value, result2)

        // Redis should only be called once
        verify(valueOperations, times(1)).get(redisKey)
    }

    @Test
    fun `get should return null on Redis miss`() {
        val key = "missing"
        val redisKey = "test-prefix:data:missing"

        `when`(valueOperations.get(redisKey)).thenReturn(null)

        val result = cacheService.get(key)
        assertNull(result)

        verify(redisMissCounter, times(1)).increment()
    }

    @Test
    fun `put should write to local and Redis`() {
        val key = "key1"
        val redisKey = "test-prefix:data:key1"
        val value = "value1"
        val ttl = 60L

        cacheService.put(key, value, ttl)

        // Verify Redis write
        verify(valueOperations).set(eq(redisKey), eq(value), eq(ttl), eq(TimeUnit.SECONDS))
        
        // Verify metric
        verify(putCounter, times(1)).increment()
    }

    @Test
    fun `evict should remove from local, Redis and Edge`() {
        val key = "key1"
        val redisKey = "test-prefix:data:key1"

        // Pre-populate local
        cacheService.put(key, "val", 60)
        
        cacheService.evict(key)

        // Verify Local removed (by checking it's gone)
        // Since we can't inspect private map, we check get() goes to Redis (or returns null if Redis empty)
        `when`(valueOperations.get(redisKey)).thenReturn(null)
        assertNull(cacheService.get(key))

        // Verify Redis delete
        verify(redisTemplate).delete(redisKey)

        // Verify Edge purge - async
        Thread.sleep(100)
        verify(edgeCacheService).purgeCacheKey("https://api.example.com", key)

        verify(evictCounter, times(1)).increment()
    }

    @Test
    fun `evictAll should clear local, Redis and Edge`() {
        val redisDataKeyPattern = "test-prefix:data:*"
        val redisTagKeyPattern = "test-prefix:tag:*"
        
        val dataKeys = setOf("test-prefix:data:k1", "test-prefix:data:k2")
        val tagKeys = setOf("test-prefix:tag:t1")
        
        `when`(redisTemplate.keys(redisDataKeyPattern)).thenReturn(dataKeys)
        `when`(redisTemplate.keys(redisTagKeyPattern)).thenReturn(tagKeys)

        cacheService.evictAll()

        verify(redisTemplate).keys(redisDataKeyPattern)
        verify(redisTemplate).delete(dataKeys)
        verify(redisTemplate).keys(redisTagKeyPattern)
        verify(redisTemplate).delete(tagKeys)
        
        Thread.sleep(100)
        verify(edgeCacheService).purgeAll()
        verify(evictCounter, times(1)).increment()
    }
    
    @Test
    fun `evictByTags should trigger local and Redis tag purge`() {
        val tags = arrayOf("tag1")
        val redisTagKey = "test-prefix:tag:tag1"
        val redisDataKey = "test-prefix:data:key1"
        
        // Setup Redis mock for members
        `when`(setOperations.members(redisTagKey)).thenReturn(setOf("key1"))
        
        cacheService.evictByTags(*tags)
        
        Thread.sleep(100)
        // Verify Redis data key deletion
        verify(redisTemplate).delete(listOf(redisDataKey))
        // Verify Redis tag key deletion
        verify(redisTemplate).delete(redisTagKey)
        
        // Verify Edge purge
        verify(edgeCacheService).purgeByTag("tag1")
        
        verify(evictCounter, times(1)).increment()
    }

    @Test
    fun `evict should clean up tag indexes`() {
        val key = "key1"
        val tags = setOf("tag1")
        val redisTagKey = "test-prefix:tag:tag1"
        
        // Put with tags first to populate internal index
        cacheService.put(key, "value", 60, tags)
        
        // Evict
        cacheService.evict(key)
        
        // Verify Redis SREM
        verify(setOperations).remove(redisTagKey, key)
    }

    @Test
    fun `should handle Redis exceptions gracefully during get`() {
        val key = "key1"
        `when`(valueOperations.get(anyString())).thenThrow(RuntimeException("Redis down"))

        val result = cacheService.get(key)
        assertNull(result)
        
        verify(redisMissCounter, times(1)).increment() // Counts error as miss in current impl
    }

    @Test
    fun `should handle Redis exceptions gracefully during put`() {
        val key = "key1"
        `when`(valueOperations.set(anyString(), any(), anyLong(), any())).thenThrow(RuntimeException("Redis down"))

        // Should not throw
        cacheService.put(key, "val", 60)
    }
}