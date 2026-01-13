package io.cacheflow.spring.edge.management

import io.cacheflow.spring.edge.CircuitBreakerStatus
import io.cacheflow.spring.edge.EdgeCacheCircuitBreaker
import io.cacheflow.spring.edge.EdgeCacheManager
import io.cacheflow.spring.edge.EdgeCacheMetrics
import io.cacheflow.spring.edge.EdgeCacheOperation
import io.cacheflow.spring.edge.EdgeCacheResult
import io.cacheflow.spring.edge.EdgeCacheStatistics
import io.cacheflow.spring.edge.RateLimiterStatus
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.time.Duration

class EdgeCacheManagementEndpointTest {
    private lateinit var edgeCacheManager: EdgeCacheManager
    private lateinit var endpoint: EdgeCacheManagementEndpoint

    @BeforeEach
    fun setUp() {
        edgeCacheManager = mock(EdgeCacheManager::class.java)
        endpoint = EdgeCacheManagementEndpoint(edgeCacheManager)
    }

    @Test
    fun `should get health status successfully`() =
        runTest {
            // Given
            val healthStatus = mapOf("provider1" to true, "provider2" to false)
            val rateLimiterStatus = RateLimiterStatus(availableTokens = 5, timeUntilNextToken = Duration.ofSeconds(2))
            val circuitBreakerStatus = CircuitBreakerStatus(state = EdgeCacheCircuitBreaker.CircuitBreakerState.CLOSED, failureCount = 0)
            val metrics = mock(EdgeCacheMetrics::class.java)

            whenever(edgeCacheManager.getHealthStatus()).thenReturn(healthStatus)
            whenever(edgeCacheManager.getRateLimiterStatus()).thenReturn(rateLimiterStatus)
            whenever(edgeCacheManager.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus)
            whenever(edgeCacheManager.getMetrics()).thenReturn(metrics)
            whenever(metrics.getTotalOperations()).thenReturn(100L)
            whenever(metrics.getSuccessfulOperations()).thenReturn(95L)
            whenever(metrics.getFailedOperations()).thenReturn(5L)
            whenever(metrics.getTotalCost()).thenReturn(10.50)
            whenever(metrics.getAverageLatency()).thenReturn(Duration.ofMillis(150))
            whenever(metrics.getSuccessRate()).thenReturn(0.95)

            // When
            val result = endpoint.getHealthStatus()

            // Then
            assertNotNull(result)
            assertEquals(healthStatus, result["providers"])

            @Suppress("UNCHECKED_CAST")
            val rateLimiter = result["rateLimiter"] as Map<String, Any>
            assertEquals(5, rateLimiter["availableTokens"])

            @Suppress("UNCHECKED_CAST")
            val circuitBreaker = result["circuitBreaker"] as Map<String, Any>
            assertEquals("CLOSED", circuitBreaker["state"])
            assertEquals(0, circuitBreaker["failureCount"])

            @Suppress("UNCHECKED_CAST")
            val metricsMap = result["metrics"] as Map<String, Any>
            assertEquals(100L, metricsMap["totalOperations"])
            assertEquals(95L, metricsMap["successfulOperations"])
            assertEquals(5L, metricsMap["failedOperations"])
            assertEquals(10.50, metricsMap["totalCost"])
            assertEquals(0.95, metricsMap["successRate"])
        }

    @Test
    fun `should get statistics successfully`() =
        runTest {
            // Given
            val statistics =
                EdgeCacheStatistics(
                    provider = "test",
                    totalRequests = 1000L,
                    successfulRequests = 950L,
                    failedRequests = 50L,
                    averageLatency = Duration.ofMillis(100),
                    totalCost = 25.0,
                    cacheHitRate = 0.85,
                )

            whenever(edgeCacheManager.getAggregatedStatistics()).thenReturn(statistics)

            // When
            val result = endpoint.getStatistics()

            // Then
            assertEquals("test", result.provider)
            assertEquals(1000L, result.totalRequests)
            assertEquals(950L, result.successfulRequests)
            assertEquals(50L, result.failedRequests)
            assertEquals(Duration.ofMillis(100), result.averageLatency)
            assertEquals(25.0, result.totalCost)
            assertEquals(0.85, result.cacheHitRate)
        }

    @Test
    fun `should purge URL successfully`() =
        runTest {
            // Given
            val url = "https://example.com/test"
            val result1 =
                EdgeCacheResult.success(
                    provider = "provider1",
                    operation = EdgeCacheOperation.PURGE_URL,
                    url = url,
                    purgedCount = 1,
                    latency = Duration.ofMillis(100),
                )
            val result2 =
                EdgeCacheResult.failure(
                    provider = "provider2",
                    operation = EdgeCacheOperation.PURGE_URL,
                    error = RuntimeException("Test error"),
                    url = url,
                )

            whenever(edgeCacheManager.purgeUrl(url)).thenReturn(flowOf(result1, result2))

            // When
            val response = endpoint.purgeUrl(url)

            // Then
            assertEquals(url, response["url"])

            @Suppress("UNCHECKED_CAST")
            val results = response["results"] as List<Map<String, Any?>>
            assertEquals(2, results.size)
            assertEquals("provider1", results[0]["provider"])
            assertEquals(true, results[0]["success"])
            assertEquals(1L, results[0]["purgedCount"])
            assertEquals("provider2", results[1]["provider"])
            assertEquals(false, results[1]["success"])

            @Suppress("UNCHECKED_CAST")
            val summary = response["summary"] as Map<String, Any>
            assertEquals(2, summary["totalProviders"])
            assertEquals(1, summary["successfulProviders"])
            assertEquals(1, summary["failedProviders"])
        }

    @Test
    fun `should purge by tag successfully`() =
        runTest {
            // Given
            val tag = "test-tag"
            val result1 =
                EdgeCacheResult.success(
                    provider = "provider1",
                    operation = EdgeCacheOperation.PURGE_TAG,
                    tag = tag,
                    purgedCount = 10,
                    latency = Duration.ofMillis(200),
                )
            val result2 =
                EdgeCacheResult.success(
                    provider = "provider2",
                    operation = EdgeCacheOperation.PURGE_TAG,
                    tag = tag,
                    purgedCount = 5,
                    latency = Duration.ofMillis(150),
                )

            whenever(edgeCacheManager.purgeByTag(tag)).thenReturn(flowOf(result1, result2))

            // When
            val response = endpoint.purgeByTag(tag)

            // Then
            assertEquals(tag, response["tag"])

            @Suppress("UNCHECKED_CAST")
            val results = response["results"] as List<Map<String, Any?>>
            assertEquals(2, results.size)

            @Suppress("UNCHECKED_CAST")
            val summary = response["summary"] as Map<String, Any>
            assertEquals(2, summary["totalProviders"])
            assertEquals(2, summary["successfulProviders"])
            assertEquals(0, summary["failedProviders"])
            assertEquals(15L, summary["totalPurged"])
        }

    @Test
    fun `should purge all successfully`() =
        runTest {
            // Given
            val result1 =
                EdgeCacheResult.success(
                    provider = "provider1",
                    operation = EdgeCacheOperation.PURGE_ALL,
                    purgedCount = 100,
                    latency = Duration.ofMillis(300),
                )
            val result2 =
                EdgeCacheResult.success(
                    provider = "provider2",
                    operation = EdgeCacheOperation.PURGE_ALL,
                    purgedCount = 50,
                    latency = Duration.ofMillis(250),
                )

            whenever(edgeCacheManager.purgeAll()).thenReturn(flowOf(result1, result2))

            // When
            val response = endpoint.purgeAll()

            // Then
            @Suppress("UNCHECKED_CAST")
            val results = response["results"] as List<Map<String, Any?>>
            assertEquals(2, results.size)

            @Suppress("UNCHECKED_CAST")
            val summary = response["summary"] as Map<String, Any>
            assertEquals(2, summary["totalProviders"])
            assertEquals(2, summary["successfulProviders"])
            assertEquals(150L, summary["totalPurged"])
        }

    @Test
    fun `should handle circuit breaker in open state`() =
        runTest {
            // Given
            val healthStatus = mapOf<String, Boolean>()
            val rateLimiterStatus = RateLimiterStatus(availableTokens = 0, timeUntilNextToken = Duration.ofSeconds(5))
            val circuitBreakerStatus = CircuitBreakerStatus(state = EdgeCacheCircuitBreaker.CircuitBreakerState.OPEN, failureCount = 10)
            val metrics = mock(EdgeCacheMetrics::class.java)

            whenever(edgeCacheManager.getHealthStatus()).thenReturn(healthStatus)
            whenever(edgeCacheManager.getRateLimiterStatus()).thenReturn(rateLimiterStatus)
            whenever(edgeCacheManager.getCircuitBreakerStatus()).thenReturn(circuitBreakerStatus)
            whenever(edgeCacheManager.getMetrics()).thenReturn(metrics)
            whenever(metrics.getTotalOperations()).thenReturn(100L)
            whenever(metrics.getSuccessfulOperations()).thenReturn(50L)
            whenever(metrics.getFailedOperations()).thenReturn(50L)
            whenever(metrics.getTotalCost()).thenReturn(5.0)
            whenever(metrics.getAverageLatency()).thenReturn(Duration.ofMillis(500))
            whenever(metrics.getSuccessRate()).thenReturn(0.50)

            // When
            val result = endpoint.getHealthStatus()

            // Then
            @Suppress("UNCHECKED_CAST")
            val circuitBreaker = result["circuitBreaker"] as Map<String, Any>
            assertEquals("OPEN", circuitBreaker["state"])
            assertEquals(10, circuitBreaker["failureCount"])
        }

    @Test
    fun `should reset metrics`() =
        runTest {
            // When
            val result = endpoint.resetMetrics()

            // Then
            assertEquals("Metrics reset not implemented in this version", result["message"])
        }

    @Test
    fun `should handle empty purge results`() =
        runTest {
            // Given
            val url = "https://example.com/test"
            whenever(edgeCacheManager.purgeUrl(url)).thenReturn(flowOf())

            // When
            val response = endpoint.purgeUrl(url)

            // Then
            @Suppress("UNCHECKED_CAST")
            val summary = response["summary"] as Map<String, Any>
            assertEquals(0, summary["totalProviders"])
            assertEquals(0, summary["successfulProviders"])
            assertEquals(0, summary["failedProviders"])
            assertEquals(0.0, summary["totalCost"])
            assertEquals(0L, summary["totalPurged"])
        }

    @Test
    fun `should calculate cost correctly in purge summary`() =
        runTest {
            // Given
            val url = "https://example.com/test"
            val result1 =
                EdgeCacheResult
                    .success(
                        provider = "provider1",
                        operation = EdgeCacheOperation.PURGE_URL,
                        url = url,
                        purgedCount = 1,
                        latency = Duration.ofMillis(100),
                    ).copy(
                        cost =
                            io.cacheflow.spring.edge
                                .EdgeCacheCost(EdgeCacheOperation.PURGE_URL, 0.01, "USD", 0.01),
                    )
            val result2 =
                EdgeCacheResult
                    .success(
                        provider = "provider2",
                        operation = EdgeCacheOperation.PURGE_URL,
                        url = url,
                        purgedCount = 1,
                        latency = Duration.ofMillis(100),
                    ).copy(
                        cost =
                            io.cacheflow.spring.edge
                                .EdgeCacheCost(EdgeCacheOperation.PURGE_URL, 0.02, "USD", 0.02),
                    )

            whenever(edgeCacheManager.purgeUrl(url)).thenReturn(flowOf(result1, result2))

            // When
            val response = endpoint.purgeUrl(url)

            // Then
            @Suppress("UNCHECKED_CAST")
            val summary = response["summary"] as Map<String, Any>
            assertEquals(0.03, summary["totalCost"])
        }
}
