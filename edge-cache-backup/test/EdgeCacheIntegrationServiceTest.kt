package com.yourcompany.cacheflow.edge

import com.yourcompany.cacheflow.edge.service.EdgeCacheIntegrationService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class EdgeCacheIntegrationServiceTest {

    private lateinit var edgeCacheManager: EdgeCacheManager
    private lateinit var edgeCacheService: EdgeCacheIntegrationService

    @BeforeEach
    fun setUp() {
        edgeCacheManager = mock(EdgeCacheManager::class.java)
        edgeCacheService = EdgeCacheIntegrationService(edgeCacheManager)
    }

    @Test
    fun `should purge single URL`() = runTest {
        // Given
        val url = "https://example.com/api/users/123"
        val expectedResult =
                EdgeCacheResult.success(
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
    fun `should purge multiple URLs`() = runTest {
        // Given
        val urls =
                listOf(
                        "https://example.com/api/users/1",
                        "https://example.com/api/users/2",
                        "https://example.com/api/users/3"
                )
        val expectedResults =
                urls.map { url ->
                    EdgeCacheResult.success(
                            provider = "test",
                            operation = EdgeCacheOperation.PURGE_URL,
                            url = url
                    )
                }

        `when`(edgeCacheManager.purgeUrls(any())).thenReturn(expectedResults.asFlow())

        // When
        val results = edgeCacheService.purgeUrls(urls).toList()

        // Then
        assertEquals(3, results.size)
        assertEquals(expectedResults, results)
        verify(edgeCacheManager).purgeUrls(any())
    }

    @Test
    fun `should purge by tag`() = runTest {
        // Given
        val tag = "users"
        val expectedResult =
                EdgeCacheResult.success(
                        provider = "test",
                        operation = EdgeCacheOperation.PURGE_TAG,
                        tag = tag,
                        purgedCount = 5
                )

        `when`(edgeCacheManager.purgeByTag(tag)).thenReturn(flowOf(expectedResult))

        // When
        val results = edgeCacheService.purgeByTag(tag).toList()

        // Then
        assertEquals(1, results.size)
        assertEquals(expectedResult, results[0])
        verify(edgeCacheManager).purgeByTag(tag)
    }

    @Test
    fun `should purge all cache entries`() = runTest {
        // Given
        val expectedResult =
                EdgeCacheResult.success(
                        provider = "test",
                        operation = EdgeCacheOperation.PURGE_ALL,
                        purgedCount = 100
                )

        `when`(edgeCacheManager.purgeAll()).thenReturn(flowOf(expectedResult))

        // When
        val results = edgeCacheService.purgeAll().toList()

        // Then
        assertEquals(1, results.size)
        assertEquals(expectedResult, results[0])
        verify(edgeCacheManager).purgeAll()
    }

    @Test
    fun `should build URL correctly`() {
        // Given
        val baseUrl = "https://example.com"
        val cacheKey = "user-123"

        // When
        val url = edgeCacheService.buildUrl(baseUrl, cacheKey)

        // Then
        assertEquals("https://example.com/api/cache/user-123", url)
    }

    @Test
    fun `should build multiple URLs correctly`() {
        // Given
        val baseUrl = "https://example.com"
        val cacheKeys = listOf("user-1", "user-2", "user-3")

        // When
        val urls = edgeCacheService.buildUrls(baseUrl, cacheKeys)

        // Then
        assertEquals(3, urls.size)
        assertEquals("https://example.com/api/cache/user-1", urls[0])
        assertEquals("https://example.com/api/cache/user-2", urls[1])
        assertEquals("https://example.com/api/cache/user-3", urls[2])
    }

    @Test
    fun `should purge cache key using base URL`() = runTest {
        // Given
        val baseUrl = "https://example.com"
        val cacheKey = "user-123"
        val expectedResult =
                EdgeCacheResult.success(
                        provider = "test",
                        operation = EdgeCacheOperation.PURGE_URL,
                        url = "https://example.com/api/cache/user-123"
                )

        `when`(edgeCacheManager.purgeUrl("https://example.com/api/cache/user-123"))
                .thenReturn(flowOf(expectedResult))

        // When
        val results = edgeCacheService.purgeCacheKey(baseUrl, cacheKey).toList()

        // Then
        assertEquals(1, results.size)
        assertEquals(expectedResult, results[0])
        verify(edgeCacheManager).purgeUrl("https://example.com/api/cache/user-123")
    }

    @Test
    fun `should purge multiple cache keys using base URL`() = runTest {
        // Given
        val baseUrl = "https://example.com"
        val cacheKeys = listOf("user-1", "user-2", "user-3")
        val expectedResults =
                cacheKeys.map { key ->
                    EdgeCacheResult.success(
                            provider = "test",
                            operation = EdgeCacheOperation.PURGE_URL,
                            url = "https://example.com/api/cache/$key"
                    )
                }

        `when`(edgeCacheManager.purgeUrls(any())).thenReturn(expectedResults.asFlow())

        // When
        val results = edgeCacheService.purgeCacheKeys(baseUrl, cacheKeys).toList()

        // Then
        assertEquals(3, results.size)
        assertEquals(expectedResults, results)
        verify(edgeCacheManager).purgeUrls(any())
    }

    @Test
    fun `should get health status`() = runTest {
        // Given
        val expectedHealthStatus =
                mapOf("cloudflare" to true, "aws-cloudfront" to false, "fastly" to true)

        `when`(edgeCacheManager.getHealthStatus()).thenReturn(expectedHealthStatus)

        // When
        val healthStatus = edgeCacheService.getHealthStatus()

        // Then
        assertEquals(expectedHealthStatus, healthStatus)
        verify(edgeCacheManager).getHealthStatus()
    }

    @Test
    fun `should get statistics`() = runTest {
        // Given
        val expectedStatistics =
                EdgeCacheStatistics(
                        provider = "test",
                        totalRequests = 100,
                        successfulRequests = 95,
                        failedRequests = 5,
                        averageLatency = java.time.Duration.ofMillis(50),
                        totalCost = 10.0,
                        cacheHitRate = 0.95
                )

        `when`(edgeCacheManager.getAggregatedStatistics()).thenReturn(expectedStatistics)

        // When
        val statistics = edgeCacheService.getStatistics()

        // Then
        assertEquals(expectedStatistics, statistics)
        verify(edgeCacheManager).getAggregatedStatistics()
    }

    @Test
    fun `should get rate limiter status`() {
        // Given
        val expectedStatus =
                RateLimiterStatus(
                        availableTokens = 5,
                        timeUntilNextToken = java.time.Duration.ofSeconds(10)
                )

        `when`(edgeCacheManager.getRateLimiterStatus()).thenReturn(expectedStatus)

        // When
        val status = edgeCacheService.getRateLimiterStatus()

        // Then
        assertEquals(expectedStatus, status)
        verify(edgeCacheManager).getRateLimiterStatus()
    }

    @Test
    fun `should get circuit breaker status`() {
        // Given
        val expectedStatus =
                CircuitBreakerStatus(
                        state = EdgeCacheCircuitBreaker.CircuitBreakerState.CLOSED,
                        failureCount = 0
                )

        `when`(edgeCacheManager.getCircuitBreakerStatus()).thenReturn(expectedStatus)

        // When
        val status = edgeCacheService.getCircuitBreakerStatus()

        // Then
        assertEquals(expectedStatus, status)
        verify(edgeCacheManager).getCircuitBreakerStatus()
    }

    @Test
    fun `should get metrics`() {
        // Given
        val expectedMetrics = EdgeCacheMetrics()

        `when`(edgeCacheManager.getMetrics()).thenReturn(expectedMetrics)

        // When
        val metrics = edgeCacheService.getMetrics()

        // Then
        assertEquals(expectedMetrics, metrics)
        verify(edgeCacheManager).getMetrics()
    }
}
