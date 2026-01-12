package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.EdgeCacheOperation
import io.cacheflow.spring.edge.EdgeCacheResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class AbstractEdgeCacheProviderTest {
    private open class TestEdgeCacheProvider(
        override val costPerOperation: Double = 0.01,
        private val simulateError: Boolean = false,
    ) : AbstractEdgeCacheProvider() {
        override val providerName: String = "test-provider"

        var purgeUrlCalled = false
        var purgeUrlArgument: String? = null

        override suspend fun isHealthy(): Boolean = true

        override suspend fun purgeUrl(url: String): EdgeCacheResult {
            purgeUrlCalled = true
            purgeUrlArgument = url

            if (simulateError) {
                return buildFailureResult(
                    operation = EdgeCacheOperation.PURGE_URL,
                    error = RuntimeException("Simulated error"),
                    url = url,
                )
            }

            val startTime = Instant.now()
            return buildSuccessResult(
                operation = EdgeCacheOperation.PURGE_URL,
                startTime = startTime,
                purgedCount = 1,
                url = url,
                metadata = mapOf("test" to "value"),
            )
        }

        override suspend fun purgeByTag(tag: String): EdgeCacheResult {
            val startTime = Instant.now()
            return buildSuccessResult(
                operation = EdgeCacheOperation.PURGE_TAG,
                startTime = startTime,
                purgedCount = 5,
                tag = tag,
            )
        }

        override suspend fun purgeAll(): EdgeCacheResult {
            val startTime = Instant.now()
            return buildSuccessResult(
                operation = EdgeCacheOperation.PURGE_ALL,
                startTime = startTime,
                purgedCount = 100,
            )
        }
    }

    @Test
    fun `should purge multiple URLs using Flow`() =
        runTest {
            // Given
            val provider = TestEdgeCacheProvider()
            val urls = flowOf("url1", "url2", "url3")

            // When
            val results = provider.purgeUrls(urls).toList()

            // Then
            assertEquals(3, results.size)
            assertTrue(results.all { it.success })
            assertEquals("url1", results[0].url)
            assertEquals("url2", results[1].url)
            assertEquals("url3", results[2].url)
        }

    @Test
    fun `buildSuccessResult should create result with correct fields`() =
        runTest {
            // Given
            val provider = TestEdgeCacheProvider(costPerOperation = 0.005)
            val startTime = Instant.now().minusSeconds(1)

            // When
            val result = provider.purgeUrl("https://example.com/test")

            // Then
            assertTrue(result.success)
            assertEquals("test-provider", result.provider)
            assertEquals(EdgeCacheOperation.PURGE_URL, result.operation)
            assertEquals("https://example.com/test", result.url)
            assertEquals(1L, result.purgedCount)
            assertNotNull(result.cost)
            assertEquals(0.005, result.cost?.costPerOperation)
            assertEquals(0.005, result.cost?.totalCost)
            assertNotNull(result.latency)
            assertTrue(result.latency!! >= Duration.ZERO)
            assertEquals("value", result.metadata["test"])
        }

    @Test
    fun `buildSuccessResult should calculate cost correctly for multiple items`() =
        runTest {
            // Given
            val provider = TestEdgeCacheProvider(costPerOperation = 0.01)

            // When
            val result = provider.purgeByTag("test-tag")

            // Then
            assertTrue(result.success)
            assertEquals(5L, result.purgedCount)
            assertEquals(0.01, result.cost?.costPerOperation)
            assertEquals(0.05, result.cost?.totalCost) // 5 * 0.01
        }

    @Test
    fun `buildFailureResult should create failure result with error`() =
        runTest {
            // Given
            val provider = TestEdgeCacheProvider(simulateError = true)

            // When
            val result = provider.purgeUrl("https://example.com/test")

            // Then
            assertFalse(result.success)
            assertEquals("test-provider", result.provider)
            assertEquals(EdgeCacheOperation.PURGE_URL, result.operation)
            assertEquals("https://example.com/test", result.url)
            assertNotNull(result.error)
            assertEquals("Simulated error", result.error?.message)
        }

    @Test
    fun `getStatistics should return default values on error`() =
        runTest {
            // Given
            val provider =
                object : TestEdgeCacheProvider() {
                    override suspend fun getStatisticsFromProvider() =
                        throw RuntimeException("API error")
                }

            // When
            val stats = provider.getStatistics()

            // Then
            assertEquals("test-provider", stats.provider)
            assertEquals(0L, stats.totalRequests)
            assertEquals(0L, stats.successfulRequests)
            assertEquals(0L, stats.failedRequests)
            assertEquals(Duration.ZERO, stats.averageLatency)
            assertEquals(0.0, stats.totalCost)
        }

    @Test
    fun `getConfiguration should return default configuration`() {
        // Given
        val provider = TestEdgeCacheProvider()

        // When
        val config = provider.getConfiguration()

        // Then
        assertEquals("test-provider", config.provider)
        assertTrue(config.enabled)
        assertNotNull(config.rateLimit)
        assertEquals(10, config.rateLimit?.requestsPerSecond)
        assertEquals(20, config.rateLimit?.burstSize)
        assertEquals(Duration.ofMinutes(1), config.rateLimit?.windowSize)
        assertNotNull(config.circuitBreaker)
        assertEquals(5, config.circuitBreaker?.failureThreshold)
        assertEquals(Duration.ofMinutes(1), config.circuitBreaker?.recoveryTimeout)
        assertEquals(3, config.circuitBreaker?.halfOpenMaxCalls)
        assertNotNull(config.batching)
        assertEquals(100, config.batching?.batchSize)
        assertEquals(Duration.ofSeconds(5), config.batching?.batchTimeout)
        assertEquals(10, config.batching?.maxConcurrency)
        assertNotNull(config.monitoring)
        assertTrue(config.monitoring?.enableMetrics == true)
        assertTrue(config.monitoring?.enableTracing == true)
        assertEquals("INFO", config.monitoring?.logLevel)
    }

    @Test
    fun `should support custom rate limit overrides`() {
        // Given
        val provider =
            object : TestEdgeCacheProvider() {
                override fun createRateLimit() =
                    super.createRateLimit().copy(requestsPerSecond = 50)
            }

        // When
        val config = provider.getConfiguration()

        // Then
        assertEquals(50, config.rateLimit?.requestsPerSecond)
    }

    @Test
    fun `should support custom batching config overrides`() {
        // Given
        val provider =
            object : TestEdgeCacheProvider() {
                override fun createBatchingConfig() =
                    super.createBatchingConfig().copy(batchSize = 200)
            }

        // When
        val config = provider.getConfiguration()

        // Then
        assertEquals(200, config.batching?.batchSize)
    }

    @Test
    fun `purgeUrls should handle empty flow`() =
        runTest {
            // Given
            val provider = TestEdgeCacheProvider()
            val urls = flowOf<String>()

            // When
            val results = provider.purgeUrls(urls).toList()

            // Then
            assertTrue(results.isEmpty())
        }

    @Test
    fun `buildSuccessResult should handle operations without URL or tag`() =
        runTest {
            // Given
            val provider = TestEdgeCacheProvider()

            // When
            val result = provider.purgeAll()

            // Then
            assertTrue(result.success)
            assertNull(result.url)
            assertNull(result.tag)
            assertEquals(100L, result.purgedCount)
        }

    @Test
    fun `buildSuccessResult should handle zero purged count`() =
        runTest {
            // Given
            val provider =
                object : TestEdgeCacheProvider() {
                    override suspend fun purgeByTag(tag: String): EdgeCacheResult {
                        val startTime = Instant.now()
                        return buildSuccessResult(
                            operation = EdgeCacheOperation.PURGE_TAG,
                            startTime = startTime,
                            purgedCount = 0,
                            tag = tag,
                        )
                    }
                }

            // When
            val result = provider.purgeByTag("empty-tag")

            // Then
            assertTrue(result.success)
            assertEquals(0L, result.purgedCount)
            assertEquals(0.0, result.cost?.totalCost) // 0 * costPerOperation
        }

    @Test
    fun `should use provider name in results`() =
        runTest {
            // Given
            val provider = TestEdgeCacheProvider()

            // When
            val result = provider.purgeUrl("https://example.com/test")

            // Then
            assertEquals("test-provider", result.provider)
        }
}
