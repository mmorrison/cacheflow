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

            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("POST", recordedRequest.method)
            assertEquals("/purge/$url", recordedRequest.path)
            assertEquals(apiToken, recordedRequest.getHeader("Fastly-Key"))
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
