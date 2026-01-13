package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.EdgeCacheOperation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class CloudflareEdgeCacheProviderTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var provider: CloudflareEdgeCacheProvider
    private val zoneId = "test-zone"
    private val apiToken = "test-token"

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val webClient =
            WebClient
                .builder()
                .build()

        val serverUrl = mockWebServer.url("").toString().removeSuffix("/")
        provider =
            CloudflareEdgeCacheProvider(
                webClient = webClient,
                zoneId = zoneId,
                apiToken = apiToken,
                baseUrl = "$serverUrl/client/v4/zones/$zoneId",
            )
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `should purge URL successfully`() =
        runTest {
            // Given
            val responseBody =
                """
                {
                    "success": true,
                    "errors": [],
                    "messages": [],
                    "result": { "id": "test-id" }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(responseBody),
            )

            // When
            val result = provider.purgeUrl("https://example.com/test")

            // Then
            assertTrue(result.success)
            assertEquals("cloudflare", result.provider)
            assertEquals(EdgeCacheOperation.PURGE_URL, result.operation)
            assertEquals("https://example.com/test", result.url)
            assertNotNull(result.cost)
            assertEquals(0.001, result.cost?.costPerOperation)

            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("POST", recordedRequest.method)
            assertEquals("/client/v4/zones/$zoneId/purge_cache", recordedRequest.path)
            assertEquals("Bearer $apiToken", recordedRequest.getHeader("Authorization"))
        }

    @Test
    fun `should handle purge URL failure`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(400)
                    .setBody("Bad Request"),
            )

            // When
            val result = provider.purgeUrl("https://example.com/test")

            // Then
            assertFalse(result.success)
            assertNotNull(result.error)
            assertEquals("cloudflare", result.provider)
            assertEquals(EdgeCacheOperation.PURGE_URL, result.operation)
        }

    @Test
    fun `should purge by tag successfully`() =
        runTest {
            // Given
            val responseBody =
                """
                {
                    "success": true,
                    "errors": [],
                    "messages": [],
                    "result": { "id": "tag-purge-id", "purgedCount": 42 }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(responseBody),
            )

            // When
            val result = provider.purgeByTag("test-tag")

            // Then
            assertTrue(result.success)
            assertEquals("cloudflare", result.provider)
            assertEquals(EdgeCacheOperation.PURGE_TAG, result.operation)
            assertEquals("test-tag", result.tag)
            assertEquals(42L, result.purgedCount)

            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("POST", recordedRequest.method)
            assertTrue(recordedRequest.body.readUtf8().contains("\"tags\""))
        }

    @Test
    fun `should handle purge by tag with null purgedCount`() =
        runTest {
            // Given
            val responseBody =
                """
                {
                    "success": true,
                    "errors": [],
                    "messages": [],
                    "result": { "id": "tag-purge-id" }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(responseBody),
            )

            // When
            val result = provider.purgeByTag("test-tag")

            // Then
            assertTrue(result.success)
            assertEquals(0L, result.purgedCount) // Should default to 0
        }

    @Test
    fun `should handle purge by tag failure`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error"),
            )

            // When
            val result = provider.purgeByTag("test-tag")

            // Then
            assertFalse(result.success)
            assertNotNull(result.error)
            assertEquals(EdgeCacheOperation.PURGE_TAG, result.operation)
        }

    @Test
    fun `should purge all successfully`() =
        runTest {
            // Given
            val responseBody =
                """
                {
                    "success": true,
                    "errors": [],
                    "messages": [],
                    "result": { "id": "purge-all-id", "purgedCount": 1000 }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(responseBody),
            )

            // When
            val result = provider.purgeAll()

            // Then
            assertTrue(result.success)
            assertEquals("cloudflare", result.provider)
            assertEquals(EdgeCacheOperation.PURGE_ALL, result.operation)
            assertEquals(1000L, result.purgedCount)

            val recordedRequest = mockWebServer.takeRequest()
            assertTrue(recordedRequest.body.readUtf8().contains("\"purge_everything\""))
        }

    @Test
    fun `should handle purge all failure`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(403)
                    .setBody("Forbidden"),
            )

            // When
            val result = provider.purgeAll()

            // Then
            assertFalse(result.success)
            assertNotNull(result.error)
            assertEquals(EdgeCacheOperation.PURGE_ALL, result.operation)
        }

    @Test
    fun `should purge multiple URLs using Flow`() =
        runTest {
            // Given
            val responseBody =
                """
                {
                    "success": true,
                    "errors": [],
                    "messages": [],
                    "result": { "id": "test-id" }
                }
                """.trimIndent()

            // Enqueue 3 responses
            repeat(3) {
                mockWebServer.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(responseBody),
                )
            }

            // When
            val urls = flowOf("url1", "url2", "url3")
            val results = provider.purgeUrls(urls).toList()

            // Then
            assertEquals(3, results.size)
            assertTrue(results.all { it.success })
        }

    @Test
    fun `should get statistics successfully`() =
        runTest {
            // Given
            val responseBody =
                """
                {
                    "totalRequests": 10000,
                    "successfulRequests": 9500,
                    "failedRequests": 500,
                    "averageLatency": 150,
                    "totalCost": 10.50,
                    "cacheHitRate": 0.85
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(responseBody),
            )

            // When
            val stats = provider.getStatistics()

            // Then
            assertEquals("cloudflare", stats.provider)
            assertEquals(10000L, stats.totalRequests)
            assertEquals(9500L, stats.successfulRequests)
            assertEquals(500L, stats.failedRequests)
            assertEquals(150L, stats.averageLatency.toMillis())
            assertEquals(10.50, stats.totalCost)
            assertEquals(0.85, stats.cacheHitRate)
        }

    @Test
    fun `should handle get statistics failure`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error"),
            )

            // When
            val stats = provider.getStatistics()

            // Then
            assertEquals("cloudflare", stats.provider)
            assertEquals(0L, stats.totalRequests)
            assertEquals(0L, stats.successfulRequests)
            assertEquals(0L, stats.failedRequests)
        }

    @Test
    fun `should check health successfully`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("OK"),
            )

            // When
            val isHealthy = provider.isHealthy()

            // Then
            assertTrue(isHealthy)
        }

    @Test
    fun `should handle health check failure`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Error"),
            )

            // When
            val isHealthy = provider.isHealthy()

            // Then
            assertFalse(isHealthy)
        }

    @Test
    fun `should return correct configuration`() {
        // When
        val config = provider.getConfiguration()

        // Then
        assertEquals("cloudflare", config.provider)
        assertTrue(config.enabled)
        assertEquals(10, config.rateLimit?.requestsPerSecond)
        assertEquals(20, config.rateLimit?.burstSize)
        assertEquals(5, config.circuitBreaker?.failureThreshold)
        assertEquals(100, config.batching?.batchSize)
        assertTrue(config.monitoring?.enableMetrics == true)
    }
}
