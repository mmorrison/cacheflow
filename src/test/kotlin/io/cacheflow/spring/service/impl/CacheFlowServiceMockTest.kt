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
import java.util.concurrent.TimeUnit

class CacheFlowServiceMockTest {

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, Any>

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

        // Setup Metrics Mocks
        `when`(meterRegistry.counter("cacheflow.local.hits")).thenReturn(localHitCounter)
        `when`(meterRegistry.counter("cacheflow.local.misses")).thenReturn(localMissCounter)
        `when`(meterRegistry.counter("cacheflow.redis.hits")).thenReturn(redisHitCounter)
        `when`(meterRegistry.counter("cacheflow.redis.misses")).thenReturn(redisMissCounter)
        `when`(meterRegistry.counter("cacheflow.puts")).thenReturn(putCounter)
        `when`(meterRegistry.counter("cacheflow.evictions")).thenReturn(evictCounter)

        // Setup Edge Mocks
        // Note: Since these launch coroutines, we might not see the result immediately in unit tests without some waiting/concurrency handling,
        // but verify calls should work if we ensure the scope is active or we spy/verify the call happened.
        // However, CacheFlowServiceImpl launches a new coroutine on a private scope. 
        // We can verify the service call was made.
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
        val redisKey = "test-prefix:key1"
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
        val redisKey = "test-prefix:key1"
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
        val redisKey = "test-prefix:missing"

        `when`(valueOperations.get(redisKey)).thenReturn(null)

        val result = cacheService.get(key)
        assertNull(result)

        verify(redisMissCounter, times(1)).increment()
    }

    @Test
    fun `put should write to local and Redis`() {
        val key = "key1"
        val redisKey = "test-prefix:key1"
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
        val redisKey = "test-prefix:key1"

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
        val redisKeyPattern = "test-prefix:*"
        val keys = setOf("test-prefix:k1", "test-prefix:k2")
        `when`(redisTemplate.keys(redisKeyPattern)).thenReturn(keys)

        cacheService.evictAll()

        verify(redisTemplate).keys(redisKeyPattern)
        verify(redisTemplate).delete(keys)
        
        Thread.sleep(100)
        verify(edgeCacheService).purgeAll()
        verify(evictCounter, times(1)).increment()
    }
    
    @Test
    fun `evictByTags should trigger edge tag purge`() {
        val tags = arrayOf("tag1", "tag2")
        
        cacheService.evictByTags(*tags)
        
        Thread.sleep(100)
        verify(edgeCacheService).purgeByTag("tag1")
        verify(edgeCacheService).purgeByTag("tag2")
        // Note: evictByTags implementation in Service doesn't explicitly increment evictCounter currently based on code reading,
        // it calls cache.clear() but evictAll increments counter. evictByTags does NOT call evictAll().
        // Let's check the code: 
        // override fun evictByTags(vararg tags: String) { ... cache.clear() } 
        // It does NOT increment eviction counter in the code I wrote. 
        // So no verification of counter here unless I add it.
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
