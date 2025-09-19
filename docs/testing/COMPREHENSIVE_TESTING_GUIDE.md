# Comprehensive Testing Guide

This guide provides thorough and maintainable testing strategies for the CacheFlow with edge caching functionality.

## Table of Contents

- [Testing Strategy](#testing-strategy)
- [Unit Testing](#unit-testing)
- [Integration Testing](#integration-testing)
- [Performance Testing](#performance-testing)
- [Test Utilities](#test-utilities)
- [Best Practices](#best-practices)

## Testing Strategy

### Test Pyramid

```
    ┌─────────────────┐
    │   E2E Tests     │  ← Few, high-level, slow
    │   (5-10%)       │
    ├─────────────────┤
    │ Integration     │  ← Some, medium-level, medium speed
    │ Tests (20-30%)  │
    ├─────────────────┤
    │   Unit Tests    │  ← Many, low-level, fast
    │   (60-70%)      │
    └─────────────────┘
```

### Test Categories

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **Performance Tests**: Test under load and stress
4. **End-to-End Tests**: Test complete user workflows

## Unit Testing

### Core Cache Service Testing

```kotlin
@ExtendWith(MockitoExtension::class)
class RussianDollCacheServiceTest {

    @Mock
    private lateinit var localCache: CacheStorage
    @Mock
    private lateinit var redisCache: CacheStorage
    @Mock
    private lateinit var edgeCacheService: EdgeCacheIntegrationService
    @Mock
    private lateinit var properties: RussianDollCacheProperties

    @InjectMocks
    private lateinit var cacheService: RussianDollCacheServiceImpl

    @Test
    fun `should get from local cache when available`() = runTest {
        // Given
        val key = "test-key"
        val expectedValue = "test-value"
        val cacheEntry = CacheEntry(
            value = expectedValue,
            ttl = 3600,
            createdAt = System.currentTimeMillis()
        )

        `when`(localCache.get(key)).thenReturn(cacheEntry)

        // When
        val result = cacheService.get(key)

        // Then
        assertEquals(expectedValue, result)
        verify(localCache).get(key)
        verify(redisCache, never()).get(any())
    }

    @Test
    fun `should fallback to Redis when local cache miss`() = runTest {
        // Given
        val key = "test-key"
        val expectedValue = "test-value"
        val cacheEntry = CacheEntry(
            value = expectedValue,
            ttl = 3600,
            createdAt = System.currentTimeMillis()
        )

        `when`(localCache.get(key)).thenReturn(null)
        `when`(redisCache.get(key)).thenReturn(cacheEntry)

        // When
        val result = cacheService.get(key)

        // Then
        assertEquals(expectedValue, result)
        verify(localCache).get(key)
        verify(redisCache).get(key)
        verify(localCache).put(key, cacheEntry) // Should populate local cache
    }

    @Test
    fun `should evict from all caches including edge cache`() = runTest {
        // Given
        val key = "test-key"
        `when`(localCache.evict(key)).thenReturn(true)
        `when`(redisCache.evict(key)).thenReturn(true)
        `when`(properties.cloudflare.enabled).thenReturn(true)
        `when`(properties.cloudflare.purgeOnEvict).thenReturn(true)
        `when`(edgeCacheService.purgeCacheKey(any(), any())).thenReturn(flowOf())

        // When
        cacheService.evict(key)

        // Then
        verify(localCache).evict(key)
        verify(redisCache).evict(key)
        verify(edgeCacheService).purgeCacheKey(any(), eq(key))
    }
}
```

### Edge Cache Integration Service Testing

```kotlin
@ExtendWith(MockitoExtension::class)
class EdgeCacheIntegrationServiceTest {

    @Mock
    private lateinit var edgeCacheManager: EdgeCacheManager

    @InjectMocks
    private lateinit var edgeCacheService: EdgeCacheIntegrationService

    @Test
    fun `should purge URL successfully`() = runTest {
        // Given
        val url = "https://example.com/api/users/123"
        val expectedResult = EdgeCacheResult.success(
            provider = "cloudflare",
            operation = EdgeCacheOperation.PURGE_URL,
            url = url,
            purgedCount = 1
        )

        `when`(edgeCacheManager.purgeUrl(url)).thenReturn(flowOf(expectedResult))

        // When
        val results = edgeCacheService.purgeUrl(url).toList()

        // Then
        assertEquals(1, results.size)
        assertEquals(expectedResult, results[0])
        assertTrue(results[0].success)
        verify(edgeCacheManager).purgeUrl(url)
    }

    @Test
    fun `should handle multiple providers`() = runTest {
        // Given
        val url = "https://example.com/api/users/123"
        val cloudflareResult = EdgeCacheResult.success(
            provider = "cloudflare",
            operation = EdgeCacheOperation.PURGE_URL,
            url = url
        )
        val fastlyResult = EdgeCacheResult.success(
            provider = "fastly",
            operation = EdgeCacheOperation.PURGE_URL,
            url = url
        )

        `when`(edgeCacheManager.purgeUrl(url)).thenReturn(flowOf(cloudflareResult, fastlyResult))

        // When
        val results = edgeCacheService.purgeUrl(url).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { it.success })
        verify(edgeCacheManager).purgeUrl(url)
    }

    @Test
    fun `should handle provider failures gracefully`() = runTest {
        // Given
        val url = "https://example.com/api/users/123"
        val successResult = EdgeCacheResult.success(
            provider = "cloudflare",
            operation = EdgeCacheOperation.PURGE_URL,
            url = url
        )
        val failureResult = EdgeCacheResult.failure(
            provider = "fastly",
            operation = EdgeCacheOperation.PURGE_URL,
            url = url,
            error = RuntimeException("API Error")
        )

        `when`(edgeCacheManager.purgeUrl(url)).thenReturn(flowOf(successResult, failureResult))

        // When
        val results = edgeCacheService.purgeUrl(url).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results.any { it.success })
        assertTrue(results.any { !it.success })
    }
}
```

### Rate Limiter Testing

```kotlin
class EdgeCacheRateLimiterTest {

    @Test
    fun `should allow requests within rate limit`() = runTest {
        // Given
        val rateLimit = RateLimit(requestsPerSecond = 10, burstSize = 20)
        val rateLimiter = EdgeCacheRateLimiter(rateLimit)

        // When & Then
        repeat(10) {
            assertTrue(rateLimiter.tryAcquire())
        }
    }

    @Test
    fun `should reject requests exceeding rate limit`() = runTest {
        // Given
        val rateLimit = RateLimit(requestsPerSecond = 1, burstSize = 2)
        val rateLimiter = EdgeCacheRateLimiter(rateLimit)

        // When
        val results = (1..5).map { rateLimiter.tryAcquire() }

        // Then
        assertTrue(results.take(2).all { it }) // First 2 should succeed
        assertFalse(results.drop(2).any { it }) // Rest should fail
    }

    @Test
    fun `should refill tokens over time`() = runTest {
        // Given
        val rateLimit = RateLimit(requestsPerSecond = 2, burstSize = 2)
        val rateLimiter = EdgeCacheRateLimiter(rateLimit)

        // When
        assertTrue(rateLimiter.tryAcquire())
        assertTrue(rateLimiter.tryAcquire())
        assertFalse(rateLimiter.tryAcquire()) // Should be rate limited

        // Wait for token refill
        delay(600) // 600ms should refill 1 token

        // Then
        assertTrue(rateLimiter.tryAcquire())
    }
}
```

### Circuit Breaker Testing

```kotlin
class EdgeCacheCircuitBreakerTest {

    @Test
    fun `should open circuit after failure threshold`() = runTest {
        // Given
        val config = CircuitBreakerConfig(
            failureThreshold = 3,
            recoveryTimeout = 1000,
            halfOpenMaxCalls = 2
        )
        val circuitBreaker = EdgeCacheCircuitBreaker(config)

        // When
        repeat(3) {
            circuitBreaker.recordFailure()
        }

        // Then
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState())
        assertFalse(circuitBreaker.allowRequest())
    }

    @Test
    fun `should transition to half-open after recovery timeout`() = runTest {
        // Given
        val config = CircuitBreakerConfig(
            failureThreshold = 2,
            recoveryTimeout = 100,
            halfOpenMaxCalls = 1
        )
        val circuitBreaker = EdgeCacheCircuitBreaker(config)

        // Open the circuit
        repeat(2) { circuitBreaker.recordFailure() }
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState())

        // Wait for recovery timeout
        delay(150)

        // When
        val allowed = circuitBreaker.allowRequest()

        // Then
        assertTrue(allowed)
        assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState())
    }
}
```

## Integration Testing

### Spring Boot Integration Tests

```kotlin
@SpringBootTest
@TestPropertySource(properties = [
    "cacheflow.enabled=true",
    "cacheflow.storage=IN_MEMORY",
    "cacheflow.cloudflare.enabled=true",
    "cacheflow.cloudflare.zone-id=test-zone",
    "cacheflow.cloudflare.api-token=test-token"
])
class RussianDollCacheIntegrationTest {

    @Autowired
    private lateinit var cacheService: RussianDollCacheService

    @Autowired
    private lateinit var edgeCacheService: EdgeCacheIntegrationService

    @MockBean
    private lateinit var webClient: WebClient

    @Test
    fun `should cache and evict with edge cache integration`() = runTest {
        // Given
        val key = "test-key"
        val value = "test-value"

        // Mock WebClient responses
        mockWebClientForCloudflare()

        // When
        cacheService.put(key, value, 3600)
        val retrievedValue = cacheService.get(key)

        // Then
        assertEquals(value, retrievedValue)

        // When evicting
        cacheService.evict(key)

        // Then
        val evictedValue = cacheService.get(key)
        assertNull(evictedValue)
    }

    @Test
    fun `should handle edge cache failures gracefully`() = runTest {
        // Given
        val key = "test-key"
        val value = "test-value"

        // Mock WebClient to return error
        mockWebClientForError()

        // When
        cacheService.put(key, value, 3600)
        cacheService.evict(key) // This should not fail even if edge cache fails

        // Then
        val evictedValue = cacheService.get(key)
        assertNull(evictedValue) // Local cache should still be evicted
    }

    private fun mockWebClientForCloudflare() {
        // Implementation for mocking successful Cloudflare responses
    }

    private fun mockWebClientForError() {
        // Implementation for mocking error responses
    }
}
```

## Performance Testing

### Load Testing

```kotlin
@Test
fun `should handle high concurrent load`() = runTest {
    // Given
    val concurrentUsers = 100
    val operationsPerUser = 1000
    val cacheService = createCacheService()

    // When
    val startTime = System.currentTimeMillis()

    val jobs = (1..concurrentUsers).map { userId ->
        async {
            repeat(operationsPerUser) { operationId ->
                val key = "user-$userId-operation-$operationId"
                val value = "value-$userId-$operationId"

                cacheService.put(key, value, 3600)
                cacheService.get(key)
            }
        }
    }

    jobs.awaitAll()

    val endTime = System.currentTimeMillis()
    val totalOperations = concurrentUsers * operationsPerUser * 2 // put + get
    val operationsPerSecond = totalOperations * 1000 / (endTime - startTime)

    // Then
    assertTrue(operationsPerSecond > 1000) // Should handle at least 1000 ops/sec
}
```

## Test Utilities

### Test Data Builders

```kotlin
object CacheTestDataBuilder {

    fun buildUser(id: Long = 1L, name: String = "Test User"): User {
        return User(
            id = id,
            name = name,
            email = "test$id@example.com",
            updatedAt = Instant.now()
        )
    }

    fun buildCacheEntry(
        value: Any = "test-value",
        ttl: Long = 3600,
        tags: Set<String> = setOf("test")
    ): CacheEntry {
        return CacheEntry(
            value = value,
            ttl = ttl,
            createdAt = System.currentTimeMillis(),
            tags = tags
        )
    }

    fun buildEdgeCacheResult(
        provider: String = "test-provider",
        success: Boolean = true,
        url: String = "https://example.com/test"
    ): EdgeCacheResult {
        return if (success) {
            EdgeCacheResult.success(
                provider = provider,
                operation = EdgeCacheOperation.PURGE_URL,
                url = url,
                purgedCount = 1
            )
        } else {
            EdgeCacheResult.failure(
                provider = provider,
                operation = EdgeCacheOperation.PURGE_URL,
                url = url,
                error = RuntimeException("Test error")
            )
        }
    }
}
```

### Test Configuration

```kotlin
@Configuration
@TestConfiguration
class CacheTestConfiguration {

    @Bean
    @Primary
    fun testCacheProperties(): RussianDollCacheProperties {
        return RussianDollCacheProperties(
            enabled = true,
            defaultTtl = 60,
            maxSize = 1000,
            storage = StorageType.IN_MEMORY,
            baseUrl = "https://test.example.com",
            cloudflare = CloudflareProperties(
                enabled = true,
                zoneId = "test-zone-id",
                apiToken = "test-token",
                keyPrefix = "test:",
                defaultTtl = 300,
                autoPurge = true,
                purgeOnEvict = true
            )
        )
    }
}
```

## Best Practices

### 1. Test Organization

```kotlin
// Group related tests in nested classes
@Nested
class CacheEvictionTests {

    @Test
    fun `should evict single key`() { /* ... */ }

    @Test
    fun `should evict by pattern`() { /* ... */ }

    @Test
    fun `should evict by tags`() { /* ... */ }
}
```

### 2. Test Naming

```kotlin
// Use descriptive test names that explain the scenario
@Test
fun `should return cached value when key exists in local cache`() { /* ... */ }

@Test
fun `should fallback to Redis when local cache miss occurs`() { /* ... */ }

@Test
fun `should purge edge cache when local cache is evicted`() { /* ... */ }
```

### 3. Async Testing

```kotlin
// Always use runTest for coroutine-based tests
@Test
fun `should handle async operations`() = runTest {
    // Given
    val cacheService = createCacheService()

    // When
    val result = cacheService.getAsync("test-key")

    // Then
    assertNotNull(result)
}
```

This comprehensive testing guide provides a solid foundation for testing the CacheFlow with edge caching functionality. The tests are maintainable, thorough, and cover all aspects from unit tests to performance scenarios.
