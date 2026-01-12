package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.EdgeCacheOperation
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.cloudfront.model.*

class AwsCloudFrontEdgeCacheProviderTest {
    private lateinit var cloudFrontClient: CloudFrontClient
    private lateinit var provider: AwsCloudFrontEdgeCacheProvider
    private val distributionId = "test-dist"

    @BeforeEach
    fun setUp() {
        cloudFrontClient = mock(CloudFrontClient::class.java)
        provider = AwsCloudFrontEdgeCacheProvider(cloudFrontClient, distributionId)
    }

    @Test
    fun `should purge URL successfully`() =
        runTest {
            // Given
            val invalidation =
                Invalidation
                    .builder()
                    .id("test-id")
                    .status("InProgress")
                    .build()
            val response = CreateInvalidationResponse.builder().invalidation(invalidation).build()

            whenever(cloudFrontClient.createInvalidation(any<CreateInvalidationRequest>()))
                .thenReturn(response)

            // When
            val result = provider.purgeUrl("/test")

            // Then
            assertTrue(result.success)
            assertEquals("aws-cloudfront", result.provider)
            assertEquals(EdgeCacheOperation.PURGE_URL, result.operation)
            assertEquals("/test", result.url)

            verify(cloudFrontClient).createInvalidation(any<CreateInvalidationRequest>())
        }

    @Test
    fun `should purge all successfully`() =
        runTest {
            // Given
            val invalidation =
                Invalidation
                    .builder()
                    .id("test-all-id")
                    .status("InProgress")
                    .build()
            val response = CreateInvalidationResponse.builder().invalidation(invalidation).build()

            whenever(cloudFrontClient.createInvalidation(any<CreateInvalidationRequest>()))
                .thenReturn(response)

            // When
            val result = provider.purgeAll()

            // Then
            assertTrue(result.success)
            assertEquals(EdgeCacheOperation.PURGE_ALL, result.operation)
        }

    @Test
    fun `should purge by tag successfully`() =
        runTest {
            // Given
            // CloudFront doesn't support tags directly, returns success with 0 purged if no URLs found
            // In our mock, getUrlsByTag returns empty list

            // When
            val result = provider.purgeByTag("test-tag")

            // Then
            assertTrue(result.success)
            assertEquals(0L, result.purgedCount)
        }

    @Test
    fun `should get configuration`() {
        // When
        val config = provider.getConfiguration()

        // Then
        assertEquals("aws-cloudfront", config.provider)
        assertTrue(config.enabled)
    }
}
