# Edge Cache Testing Guide

This guide explains how to test the edge caching functionality in your applications.

> **📚 For comprehensive testing patterns and examples, see the [Comprehensive Testing Guide](COMPREHENSIVE_TESTING_GUIDE.md)**

## Quick Start

This guide covers the essential testing patterns for edge caching. For detailed examples, test utilities, and advanced testing strategies, refer to the comprehensive testing guide.

## Unit Testing

### Testing Edge Cache Integration Service

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
            provider = "test",
            operation = EdgeCacheOperation.PURGE_URL,
            url = url
        )

        `when`(edgeCacheManager.purgeUrl(url)).thenReturn(flowOf(expectedResult))

        // When
        val results = edgeCacheService.purgeUrl(url).toList()

        // Then
        assertEquals(1, results.size)
        assertEquals(expectedResult, results[0])
        verify(edgeCacheManager).purgeUrl(url)
    }

    @Test
    fun `should handle rate limiting`() = runTest {
        // Given
        val rateLimiter = EdgeCacheRateLimiter(RateLimit(1, 1))
        val urls = (1..5).map { "https://example.com/api/users/$it" }

        // When
        val results = urls.map { rateLimiter.tryAcquire() }

        // Then
        assertTrue(results.any { it }) // At least one should succeed
        assertTrue(results.any { !it }) // At least one should be rate limited
    }

    @Test
    fun `should handle circuit breaker`() = runTest {
        // Given
        val circuitBreaker = EdgeCacheCircuitBreaker(
            CircuitBreakerConfig(failureThreshold = 2)
        )

        // When - simulate failures
        repeat(3) {
            try {
                circuitBreaker.execute { throw RuntimeException("Simulated failure") }
            } catch (e: Exception) {
                // Expected
            }
        }

        // Then
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState())
        assertEquals(3, circuitBreaker.getFailureCount())
    }
}
```

### Testing Service Integration

```kotlin
@ExtendWith(MockitoExtension::class)
class UserServiceEdgeCacheTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var edgeCacheService: EdgeCacheIntegrationService

    @InjectMocks
    private lateinit var userService: UserService

    @Test
    fun `should purge edge cache on user update`() = runTest {
        // Given
        val user = User(1L, "John Doe", "john@example.com")
        val updatedUser = user.copy(name = "John Updated")

        `when`(userRepository.save(any())).thenReturn(updatedUser)
        `when`(edgeCacheService.purgeUrl(any())).thenReturn(flowOf(
            EdgeCacheResult.success("test", EdgeCacheOperation.PURGE_URL)
        ))

        // When
        val result = userService.updateUser(user)

        // Then
        assertEquals(updatedUser, result)
        verify(edgeCacheService).purgeUrl("/api/users/1")
    }
}
```

## Integration Testing

### Testing with TestContainers

```kotlin
@SpringBootTest
@Testcontainers
class EdgeCacheIntegrationTest {

    @Container
    static val redis = GenericContainer("redis:7-alpine")
        .withExposedPorts(6379)

    @Container
    static val mockServer = GenericContainer("mockserver/mockserver:5.15.0")
        .withExposedPorts(1080)
        .withCommand("-serverPort", "1080")

    @Test
    fun `should integrate with Cloudflare API`() = runTest {
        // Given
        val mockServerClient = MockServerClient(
            mockServer.host,
            mockServer.getMappedPort(1080)
        )

        mockServerClient
            .`when`(request()
                .withMethod("POST")
                .withPath("/client/v4/zones/test-zone/purge_cache")
                .withHeader("Authorization", "Bearer test-token"))
            .respond(response()
                .withStatusCode(200)
                .withBody("""{"success": true, "result": {"id": "purge-id"}}"""))

        // When
        val results = edgeCacheService.purgeUrl("https://example.com/test").toList()

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.success })
    }
}
```

### Testing Rate Limiting

```kotlin
@Test
fun `should respect rate limits`() = runTest {
    // Given
    val rateLimiter = EdgeCacheRateLimiter(RateLimit(2, 2))
    val urls = (1..10).map { "https://example.com/api/users/$it" }

    // When
    val results = urls.map { url ->
        rateLimiter.tryAcquire()
    }

    // Then
    val successCount = results.count { it }
    assertTrue(successCount <= 2) // Should not exceed burst size
}
```

### Testing Circuit Breaker

```kotlin
@Test
fun `should open circuit breaker on failures`() = runTest {
    // Given
    val circuitBreaker = EdgeCacheCircuitBreaker(
        CircuitBreakerConfig(failureThreshold = 3)
    )

    // When - simulate failures
    repeat(5) {
        try {
            circuitBreaker.execute {
                throw RuntimeException("Service unavailable")
            }
        } catch (e: Exception) {
            // Expected
        }
    }

    // Then
    assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState())

    // Verify circuit breaker blocks new requests
    assertThrows<CircuitBreakerOpenException> {
        runBlocking {
            circuitBreaker.execute { "should not execute" }
        }
    }
}
```

## End-to-End Testing

### Testing Management Endpoints

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "cacheflow.cloudflare.enabled=true",
    "cacheflow.cloudflare.zone-id=test-zone",
    "cacheflow.cloudflare.api-token=test-token"
])
class EdgeCacheManagementEndpointTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should get health status`() {
        // When
        val response = restTemplate.getForEntity(
            "/actuator/edgecache",
            Map::class.java
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.containsKey("providers"))
    }

    @Test
    fun `should purge URL via endpoint`() {
        // When
        val response = restTemplate.postForEntity(
            "/actuator/edgecache/purge/https://example.com/test",
            null,
            Map::class.java
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.containsKey("results"))
    }
}
```

### Testing Error Scenarios

```kotlin
@Test
fun `should handle API failures gracefully`() = runTest {
    // Given
    val mockWebClient = WebClient.builder()
        .baseUrl("https://api.cloudflare.com")
        .build()

    val cloudflareProvider = CloudflareEdgeCacheProvider(
        webClient = mockWebClient,
        zoneId = "test-zone",
        apiToken = "invalid-token"
    )

    // When
    val result = cloudflareProvider.purgeUrl("https://example.com/test")

    // Then
    assertFalse(result.success)
    assertNotNull(result.error)
}
```

## Performance Testing

### Load Testing Edge Cache Operations

```kotlin
@Test
fun `should handle high load`() = runTest {
    // Given
    val edgeCacheService = EdgeCacheIntegrationService(edgeCacheManager)
    val urls = (1..1000).map { "https://example.com/api/users/$it" }

    // When
    val startTime = System.currentTimeMillis()
    val results = edgeCacheService.purgeUrls(urls).toList()
    val endTime = System.currentTimeMillis()

    // Then
    val duration = endTime - startTime
    println("Processed ${urls.size} URLs in ${duration}ms")

    assertTrue(duration < 10000) // Should complete within 10 seconds
    assertTrue(results.isNotEmpty())
}
```

### Memory Usage Testing

```kotlin
@Test
fun `should not leak memory under load`() = runTest {
    // Given
    val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    // When - perform many operations
    repeat(1000) {
        edgeCacheService.purgeUrl("https://example.com/api/users/$it")
    }

    // Force garbage collection
    System.gc()
    Thread.sleep(1000)

    val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    val memoryIncrease = finalMemory - initialMemory

    // Then
    assertTrue(memoryIncrease < 10 * 1024 * 1024) // Should not increase by more than 10MB
}
```

## Mock Testing

### Mocking Edge Cache Providers

```kotlin
@ExtendWith(MockitoExtension::class)
class MockEdgeCacheProvider : EdgeCacheProvider {

    override val providerName: String = "mock"

    private val cache = mutableMapOf<String, String>()

    override suspend fun isHealthy(): Boolean = true

    override suspend fun purgeUrl(url: String): EdgeCacheResult {
        cache.remove(url)
        return EdgeCacheResult.success(
            provider = providerName,
            operation = EdgeCacheOperation.PURGE_URL,
            url = url,
            purgedCount = 1
        )
    }

    override fun purgeUrls(urls: Flow<String>): Flow<EdgeCacheResult> = flow {
        urls.collect { url ->
            emit(purgeUrl(url))
        }
    }

    override suspend fun purgeByTag(tag: String): EdgeCacheResult {
        val purgedCount = cache.size.toLong()
        cache.clear()
        return EdgeCacheResult.success(
            provider = providerName,
            operation = EdgeCacheOperation.PURGE_TAG,
            tag = tag,
            purgedCount = purgedCount
        )
    }

    override suspend fun purgeAll(): EdgeCacheResult {
        val purgedCount = cache.size.toLong()
        cache.clear()
        return EdgeCacheResult.success(
            provider = providerName,
            operation = EdgeCacheOperation.PURGE_ALL,
            purgedCount = purgedCount
        )
    }

    override suspend fun getStatistics(): EdgeCacheStatistics {
        return EdgeCacheStatistics(
            provider = providerName,
            totalRequests = 0,
            successfulRequests = 0,
            failedRequests = 0,
            averageLatency = Duration.ZERO,
            totalCost = 0.0
        )
    }

    override fun getConfiguration(): EdgeCacheConfiguration {
        return EdgeCacheConfiguration(
            provider = providerName,
            enabled = true
        )
    }
}
```

## Test Configuration

### Test Application Properties

```yaml
# application-test.yml
cacheflow:
  enabled: true
  base-url: "http://localhost:8080"
  cloudflare:
    enabled: false # Disable in tests
  aws-cloud-front:
    enabled: false
  fastly:
    enabled: false
  rate-limit:
    requests-per-second: 100 # Higher limits for tests
    burst-size: 200
  circuit-breaker:
    failure-threshold: 10 # More tolerant in tests
    recovery-timeout: 10 # Faster recovery in tests

logging:
  level:
    com.yourcompany.russiandollcache.edge: DEBUG
```

### Test Profile Configuration

```kotlin
@ActiveProfiles("test")
@SpringBootTest
class EdgeCacheTest {
    // Test implementation
}
```

## Best Practices

### 1. Test Isolation

- Use `@DirtiesContext` for tests that modify configuration
- Reset mocks between tests
- Use test-specific configuration profiles

### 2. Test Data Management

- Use builders for test data creation
- Create reusable test fixtures
- Use parameterized tests for multiple scenarios

### 3. Assertion Strategies

- Test both success and failure scenarios
- Verify side effects (e.g., cache purging)
- Check metrics and monitoring data

### 4. Performance Considerations

- Use `@Timeout` annotations for performance tests
- Monitor memory usage in long-running tests
- Use test containers for realistic integration testing

## Conclusion

This testing guide provides comprehensive strategies for testing edge caching functionality at all levels. By following these patterns, you can ensure your edge caching implementation is robust, performant, and reliable in production environments.
