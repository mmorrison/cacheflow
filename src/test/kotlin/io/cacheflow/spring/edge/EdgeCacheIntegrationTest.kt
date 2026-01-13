package io.cacheflow.spring.edge

import io.cacheflow.spring.edge.impl.AwsCloudFrontEdgeCacheProvider
import io.cacheflow.spring.edge.impl.CloudflareEdgeCacheProvider
import io.cacheflow.spring.edge.impl.FastlyEdgeCacheProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Duration

class EdgeCacheIntegrationTest {
    private lateinit var cloudflareProvider: CloudflareEdgeCacheProvider
    private lateinit var awsProvider: AwsCloudFrontEdgeCacheProvider
    private lateinit var fastlyProvider: FastlyEdgeCacheProvider
    private lateinit var edgeCacheManager: EdgeCacheManager

    @BeforeEach
    fun setUp() {
        // Mock providers
        cloudflareProvider = mock(CloudflareEdgeCacheProvider::class.java)
        awsProvider = mock(AwsCloudFrontEdgeCacheProvider::class.java)
        fastlyProvider = mock(FastlyEdgeCacheProvider::class.java)

        val allProviders = listOf(cloudflareProvider, awsProvider, fastlyProvider)

        allProviders.forEach { provider ->
            runBlocking {
                whenever(provider.providerName).thenReturn(
                    when (provider) {
                        cloudflareProvider -> "cloudflare"
                        awsProvider -> "aws-cloudfront"
                        else -> "fastly"
                    },
                )
                whenever(provider.isHealthy()).thenReturn(true)
                whenever(provider.purgeUrl(anyString())).thenAnswer { invocation ->
                    EdgeCacheResult.success(
                        provider = (invocation.mock as EdgeCacheProvider).providerName,
                        operation = EdgeCacheOperation.PURGE_URL,
                        url = invocation.getArgument(0),
                    )
                }
                whenever(provider.purgeByTag(anyString())).thenAnswer { invocation ->
                    EdgeCacheResult.success(
                        provider = (invocation.mock as EdgeCacheProvider).providerName,
                        operation = EdgeCacheOperation.PURGE_TAG,
                        tag = invocation.getArgument(0),
                    )
                }
                whenever(provider.purgeAll()).thenAnswer { invocation ->
                    EdgeCacheResult.success(
                        provider = (invocation.mock as EdgeCacheProvider).providerName,
                        operation = EdgeCacheOperation.PURGE_ALL,
                    )
                }
                whenever(provider.getStatistics()).thenAnswer { invocation ->
                    EdgeCacheStatistics(
                        provider = (invocation.mock as EdgeCacheProvider).providerName,
                        totalRequests = 10,
                        successfulRequests = 10,
                        failedRequests = 0,
                        averageLatency = Duration.ofMillis(10),
                        totalCost = 0.1,
                    )
                }
            }
        }

        // Initialize edge cache manager
        edgeCacheManager =
            EdgeCacheManager(
                providers = allProviders,
                configuration =
                    EdgeCacheConfiguration(
                        provider = "test",
                        enabled = true,
                        rateLimit = RateLimit(100, 200),
                        circuitBreaker = CircuitBreakerConfig(),
                        batching = BatchingConfig(batchSize = 2, batchTimeout = Duration.ofMillis(100)),
                        monitoring = MonitoringConfig(),
                    ),
            )
    }

    @Test
    fun `should handle rate limit exceeded exception`() {
        val exception = RateLimitExceededException("Limit reached")
        assertEquals("Limit reached", exception.message)
    }

    @AfterEach
    fun tearDown() {
        edgeCacheManager.close()
    }

    @Test
    fun `should purge single URL from all providers`() =
        runTest {
            // Given
            val url = "https://example.com/api/users/123"

            // When
            val results = edgeCacheManager.purgeUrl(url).toList()

            // Then
            assertTrue(results.isNotEmpty())
            results.forEach { result ->
                assertNotNull(result)
                assertEquals(EdgeCacheOperation.PURGE_URL, result.operation)
                assertEquals(url, result.url)
            }
        }

    @Test
    fun `should purge multiple URLs using batching`() =
        runTest {
            // Given
            val urls =
                listOf(
                    "https://example.com/api/users/1",
                    "https://example.com/api/users/2",
                    "https://example.com/api/users/3",
                )

            // When
            val results = edgeCacheManager.purgeUrls(urls.asFlow()).take(urls.size * 3).toList()

            // Then
            assertTrue(results.isNotEmpty())
            assertEquals(urls.size * 3, results.size)
        }

    @Test
    fun `should purge by tag`() =
        runTest {
            // Given
            val tag = "users"

            // When
            val results = edgeCacheManager.purgeByTag(tag).toList()

            // Then
            assertTrue(results.isNotEmpty())
            results.forEach { result ->
                assertEquals(EdgeCacheOperation.PURGE_TAG, result.operation)
                assertEquals(tag, result.tag)
            }
        }

    @Test
    fun `should purge all cache entries`() =
        runTest {
            // When
            val results = edgeCacheManager.purgeAll().toList()

            // Then
            assertTrue(results.isNotEmpty())
            results.forEach { result -> assertEquals(EdgeCacheOperation.PURGE_ALL, result.operation) }
        }

    @Test
    fun `should handle rate limiting`() =
        runTest {
            // Given
            val rateLimiter = EdgeCacheRateLimiter(RateLimit(1, 1)) // Very restrictive
            val urls = (1..10).map { "https://example.com/api/users/$it" }

            // When
            val results = urls.map { url -> rateLimiter.tryAcquire() }

            // Then
            assertTrue(results.any { it }) // At least one should succeed
            assertTrue(results.any { !it }) // At least one should be rate limited
        }

    @Test
    fun `should handle circuit breaker`() =
        runTest {
            // Given
            val circuitBreaker = EdgeCacheCircuitBreaker(CircuitBreakerConfig(failureThreshold = 2))

            // When - simulate failures
            repeat(3) {
                try {
                    circuitBreaker.execute { throw RuntimeException("Simulated failure") }
                } catch (e: Exception) {
                    // Expected
                }
            }

            // Then
            assertEquals(EdgeCacheCircuitBreaker.CircuitBreakerState.OPEN, circuitBreaker.getState())
            assertEquals(2, circuitBreaker.getFailureCount())
        }

    @Test
    fun `should collect metrics`() =
        runTest {
            // Given
            val metrics = EdgeCacheMetrics()

            // When
            val successResult =
                EdgeCacheResult.success(
                    provider = "test",
                    operation = EdgeCacheOperation.PURGE_URL,
                    url = "https://example.com/test",
                )

            val failureResult =
                EdgeCacheResult.failure(
                    provider = "test",
                    operation = EdgeCacheOperation.PURGE_URL,
                    error = RuntimeException("Test error"),
                )

            metrics.recordOperation(successResult)
            metrics.recordOperation(failureResult)
            metrics.recordLatency(Duration.ofMillis(100))

            // Then
            assertEquals(2, metrics.getTotalOperations())
            assertEquals(1, metrics.getSuccessfulOperations())
            assertEquals(1, metrics.getFailedOperations())
            assertEquals(0.5, metrics.getSuccessRate(), 0.01)
            assertEquals(Duration.ofMillis(100), metrics.getAverageLatency())
        }

    @Test
    fun `should handle batching`() =
        runTest {
            // Given
            val batcher =
                EdgeCacheBatcher(
                    BatchingConfig(batchSize = 3, batchTimeout = Duration.ofSeconds(1)),
                )
            val urls = (1..10).map { "https://example.com/api/users/$it" }

            // When
            val batchesFlow = batcher.getBatchedUrls()

            launch {
                urls.forEach { url ->
                    batcher.addUrl(url)
                    delay(10)
                }
                batcher.close()
            }

            val batches = batchesFlow.toList()

            // Then
            assertTrue(batches.isNotEmpty())
            assertEquals(4, batches.size) // 10 URLs / 3 = 3 batches of 3 + 1 batch of 1
            batches.forEach { batch ->
                assertTrue(batch.size <= 3) // Should respect batch size
            }
        }

    @Test
    fun `should get health status`() =
        runTest {
            // When
            val healthStatus = edgeCacheManager.getHealthStatus()

            // Then
            assertTrue(healthStatus.containsKey("cloudflare"))
            assertTrue(healthStatus.containsKey("aws-cloudfront"))
            assertTrue(healthStatus.containsKey("fastly"))
        }

    @Test
    fun `should get aggregated statistics`() =
        runTest {
            // When
            val statistics = edgeCacheManager.getAggregatedStatistics()

            // Then
            assertNotNull(statistics)
            assertEquals("aggregated", statistics.provider)
            assertTrue(statistics.totalRequests >= 0)
            assertTrue(statistics.totalCost >= 0.0)
        }

    @Test
    fun `should get rate limiter status`() =
        runTest {
            // When
            val status = edgeCacheManager.getRateLimiterStatus()

            // Then
            assertTrue(status.availableTokens >= 0)
            assertNotNull(status.timeUntilNextToken)
        }

    @Test
    fun `should get circuit breaker status`() =
        runTest {
            // When
            val status = edgeCacheManager.getCircuitBreakerStatus()

            // Then
            assertNotNull(status.state)
            assertTrue(status.failureCount >= 0)
        }
}
