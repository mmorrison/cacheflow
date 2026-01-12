package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.EdgeCacheOperation
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
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

            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("POST", recordedRequest.method)
            assertEquals("/client/v4/zones/$zoneId/purge_cache", recordedRequest.path)
            assertEquals("Bearer $apiToken", recordedRequest.getHeader("Authorization"))
        }

    @Test
    fun `should handle purge failure`() =
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
}
