package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.EdgeCacheOperation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class FastlyEdgeCacheProviderTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var provider: FastlyEdgeCacheProvider
    private val serviceId = "test-service"
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
            FastlyEdgeCacheProvider(
                webClient = webClient,
                serviceId = serviceId,
                apiToken = apiToken,
                baseUrl = serverUrl,
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
                    "status": "ok"
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(responseBody),
            )

            // When
            val url = "path/to/resource"
            val result = provider.purgeUrl(url)

            // Then
            assertTrue(result.success)
            assertEquals("fastly", result.provider)
            assertEquals(EdgeCacheOperation.PURGE_URL, result.operation)
            assertNotNull(result.cost)

            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("POST", recordedRequest.method)
            assertEquals("/purge/$url", recordedRequest.path)
            assertEquals(apiToken, recordedRequest.getHeader("Fastly-Key"))
        }

    @Test
    fun `should handle purge URL failure`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Server Error"),
            )

            // When
            val result = provider.purgeUrl("test-url")

            // Then
            assertFalse(result.success)
            assertNotNull(result.error)
        }

    @Test
    fun `should purge by tag successfully`() =
        runTest {
            // Given
            val responseBody =
                """
                {
                    "status": "ok",
                    "purgedCount": 25
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
            assertEquals("fastly", result.provider)
            assertEquals(EdgeCacheOperation.PURGE_TAG, result.operation)
            assertEquals("test-tag", result.tag)
            assertEquals(25L, result.purgedCount)

            val recordedRequest = mockWebServer.takeRequest()
            assertEquals(apiToken, recordedRequest.getHeader("Fastly-Key"))
            assertEquals("test-tag", recordedRequest.getHeader("Fastly-Tags"))
        }

    @Test
    fun `should handle purge by tag with null purgedCount`() =
        runTest {
            // Given
            val responseBody =
                """
                {
                    "status": "ok"
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
            assertEquals(0L, result.purgedCount) // Defaults to 0 when null
        }

    @Test
    fun `should handle purge by tag failure`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(403)
                    .setBody("Forbidden"),
            )

            // When
            val result = provider.purgeByTag("test-tag")

            // Then
            assertFalse(result.success)
            assertNotNull(result.error)
        }

    @Test
    fun `should purge all successfully`() =
        runTest {
            // Given
            val responseBody =
                """
                {
                    "status": "ok",
                    "purgedCount": 500
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
            assertEquals(EdgeCacheOperation.PURGE_ALL, result.operation)
            assertEquals(500L, result.purgedCount)
        }

    @Test
    fun `should handle purge all failure`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("Unauthorized"),
            )

            // When
            val result = provider.purgeAll()

            // Then
            assertFalse(result.success)
            assertNotNull(result.error)
        }

    @Test
    fun `should purge multiple URLs using Flow`() =
        runTest {
            // Given
            val responseBody = """{"status": "ok"}"""
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
                    "totalRequests": 5000,
                    "successfulRequests": 4800,
                    "failedRequests": 200,
                    "averageLatency": 75,
                    "totalCost": 5.25,
                    "cacheHitRate": 0.92
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
            assertEquals("fastly", stats.provider)
            assertEquals(5000L, stats.totalRequests)
            assertEquals(4800L, stats.successfulRequests)
            assertEquals(200L, stats.failedRequests)
            assertEquals(75L, stats.averageLatency.toMillis())
            assertEquals(5.25, stats.totalCost)
            assertEquals(0.92, stats.cacheHitRate)
        }

    @Test
    fun `should handle get statistics failure`() =
        runTest {
            // Given
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Server Error"),
            )

            // When
            val stats = provider.getStatistics()

            // Then
            assertEquals("fastly", stats.provider)
            assertEquals(0L, stats.totalRequests)
            assertEquals(0L, stats.successfulRequests)
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
                    .setResponseCode(503)
                    .setBody("Service Unavailable"),
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
        assertEquals("fastly", config.provider)
        assertTrue(config.enabled)
        assertEquals(15, config.rateLimit?.requestsPerSecond)
        assertEquals(200, config.batching?.batchSize)
    }
}
