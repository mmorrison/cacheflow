package com.yourcompany.cacheflow.edge

import com.yourcompany.cacheflow.edge.impl.AwsCloudFrontEdgeCacheProvider
import com.yourcompany.cacheflow.edge.impl.CloudflareEdgeCacheProvider
import com.yourcompany.cacheflow.edge.impl.FastlyEdgeCacheProvider
import java.time.Duration
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.web.reactive.function.client.WebClient
import software.amazon.awssdk.services.cloudfront.CloudFrontClient

class EdgeCacheIntegrationTest {

    private lateinit var cloudflareProvider: CloudflareEdgeCacheProvider
    private lateinit var awsProvider: AwsCloudFrontEdgeCacheProvider
    private lateinit var fastlyProvider: FastlyEdgeCacheProvider
    private lateinit var edgeCacheManager: EdgeCacheManager

    @BeforeEach
    fun setUp() {
        // Mock WebClient for Cloudflare and Fastly
        val webClient = mock(WebClient::class.java)

        // Mock CloudFront client
        val cloudFrontClient = mock(CloudFrontClient::class.java)

        // Initialize providers
        cloudflareProvider =
                CloudflareEdgeCacheProvider(
                        webClient = webClient,
                        zoneId = "test-zone-id",
                        apiToken = "test-token"
                )

        awsProvider =
                AwsCloudFrontEdgeCacheProvider(
                        cloudFrontClient = cloudFrontClient,
                        distributionId = "test-distribution-id"
                )

        fastlyProvider =
                FastlyEdgeCacheProvider(
                        webClient = webClient,
                        serviceId = "test-service-id",
                        apiToken = "test-token"
                )

        // Initialize edge cache manager
        edgeCacheManager =
                EdgeCacheManager(
                        providers = listOf(cloudflareProvider, awsProvider, fastlyProvider),
                        configuration =
                                EdgeCacheConfiguration(
                                        provider = "test",
                                        enabled = true,
                                        rateLimit = RateLimit(10, 20),
                                        circuitBreaker = CircuitBreakerConfig(),
                                        batching = BatchingConfig(),
                                        monitoring = MonitoringConfig()
                                )
                )
    }

    @Test
    fun `should purge single URL from all providers`() = runTest {
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
    fun `should purge multiple URLs using batching`() = runTest {
        // Given
        val urls =
                listOf(
                        "https://example.com/api/users/1",
                        "https://example.com/api/users/2",
                        "https://example.com/api/users/3"
                )

        // When
        val results = edgeCacheManager.purgeUrls(urls.asFlow()).toList()

        // Then
        assertTrue(results.isNotEmpty())
        assertEquals(urls.size, results.size)
    }

    @Test
    fun `should purge by tag`() = runTest {
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
    fun `should purge all cache entries`() = runTest {
        // When
        val results = edgeCacheManager.purgeAll().toList()

        // Then
        assertTrue(results.isNotEmpty())
        results.forEach { result -> assertEquals(EdgeCacheOperation.PURGE_ALL, result.operation) }
    }

    @Test
    fun `should handle rate limiting`() = runTest {
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
    fun `should handle circuit breaker`() = runTest {
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
        assertEquals(3, circuitBreaker.getFailureCount())
    }

    @Test
    fun `should collect metrics`() = runTest {
        // Given
        val metrics = EdgeCacheMetrics()

        // When
        val successResult =
                EdgeCacheResult.success(
                        provider = "test",
                        operation = EdgeCacheOperation.PURGE_URL,
                        url = "https://example.com/test"
                )

        val failureResult =
                EdgeCacheResult.failure(
                        provider = "test",
                        operation = EdgeCacheOperation.PURGE_URL,
                        error = RuntimeException("Test error")
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
    fun `should handle batching`() = runTest {
        // Given
        val batcher =
                EdgeCacheBatcher(
                        BatchingConfig(batchSize = 3, batchTimeout = Duration.ofSeconds(1))
                )
        val urls = (1..10).map { "https://example.com/api/users/$it" }

        // When
        urls.forEach { url -> batcher.addUrl(url) }

        val batches = batcher.getBatchedUrls().take(5).toList()

        // Then
        assertTrue(batches.isNotEmpty())
        batches.forEach { batch ->
            assertTrue(batch.size <= 3) // Should respect batch size
        }

        batcher.close()
    }

    @Test
    fun `should get health status`() = runTest {
        // When
        val healthStatus = edgeCacheManager.getHealthStatus()

        // Then
        assertTrue(healthStatus.containsKey("cloudflare"))
        assertTrue(healthStatus.containsKey("aws-cloudfront"))
        assertTrue(healthStatus.containsKey("fastly"))
    }

    @Test
    fun `should get aggregated statistics`() = runTest {
        // When
        val statistics = edgeCacheManager.getAggregatedStatistics()

        // Then
        assertNotNull(statistics)
        assertEquals("aggregated", statistics.provider)
        assertTrue(statistics.totalRequests >= 0)
        assertTrue(statistics.totalCost >= 0.0)
    }

    @Test
    fun `should get rate limiter status`() = runTest {
        // When
        val status = edgeCacheManager.getRateLimiterStatus()

        // Then
        assertTrue(status.availableTokens >= 0)
        assertNotNull(status.timeUntilNextToken)
    }

    @Test
    fun `should get circuit breaker status`() = runTest {
        // When
        val status = edgeCacheManager.getCircuitBreakerStatus()

        // Then
        assertNotNull(status.state)
        assertTrue(status.failureCount >= 0)
    }
}
