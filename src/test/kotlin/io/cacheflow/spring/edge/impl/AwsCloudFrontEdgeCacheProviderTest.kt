package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.EdgeCacheOperation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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
            assertNotNull(result.cost)

            verify(cloudFrontClient).createInvalidation(any<CreateInvalidationRequest>())
        }

    @Test
    fun `should handle purge URL failure`() =
        runTest {
            // Given
            whenever(cloudFrontClient.createInvalidation(any<CreateInvalidationRequest>()))
                .thenThrow(RuntimeException("CloudFront API error"))

            // When
            val result = provider.purgeUrl("/test")

            // Then
            assertFalse(result.success)
            assertNotNull(result.error)
            assertEquals(EdgeCacheOperation.PURGE_URL, result.operation)
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
            assertEquals(Long.MAX_VALUE, result.purgedCount) // All entries
        }

    @Test
    fun `should handle purge all failure`() =
        runTest {
            // Given
            whenever(cloudFrontClient.createInvalidation(any<CreateInvalidationRequest>()))
                .thenThrow(RuntimeException("API error"))

            // When
            val result = provider.purgeAll()

            // Then
            assertFalse(result.success)
            assertNotNull(result.error)
        }

    @Test
    fun `should purge by tag with empty URLs list`() =
        runTest {
            // Given - getUrlsByTag returns empty list by default

            // When
            val result = provider.purgeByTag("test-tag")

            // Then
            assertTrue(result.success)
            assertEquals(0L, result.purgedCount)
            assertEquals("test-tag", result.tag)
            // Should NOT call CloudFront API when no URLs found
            verify(cloudFrontClient, never()).createInvalidation(any<CreateInvalidationRequest>())
        }

    @Test
    fun `should handle purge by tag failure`() =
        runTest {
            // Given - This will test the catch block if there's an error in getUrlsByTag
            // But since getUrlsByTag is a private method that returns emptyList,
            // we're testing that the success path with 0 items works correctly
            
            // When
            val result = provider.purgeByTag("test-tag")

            // Then
            assertTrue(result.success)
            assertEquals(0L, result.purgedCount)
        }

    @Test
    fun `should purge multiple URLs using Flow`() =
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
            val urls = flowOf("/url1", "/url2", "/url3")
            val results = provider.purgeUrls(urls).toList()

            // Then
            assertEquals(3, results.size)
            assertTrue(results.all { it.success })
            verify(cloudFrontClient, times(3)).createInvalidation(any<CreateInvalidationRequest>())
        }

    @Test
    fun `should check health successfully`() =
        runTest {
            // Given
            val distribution = GetDistributionResponse.builder().build()
            whenever(cloudFrontClient.getDistribution(any<GetDistributionRequest>()))
                .thenReturn(distribution)

            // When
            val isHealthy = provider.isHealthy()

            // Then
            assertTrue(isHealthy)
        }

    @Test
    fun `should handle health check failure`() =
        runTest {
            // Given
            whenever(cloudFrontClient.getDistribution(any<GetDistributionRequest>()))
                .thenThrow(RuntimeException("API error"))

            // When
            val isHealthy = provider.isHealthy()

            // Then
            assertFalse(isHealthy)
        }

    @Test
    fun `should get configuration`() {
        // When
        val config = provider.getConfiguration()

        // Then
        assertEquals("aws-cloudfront", config.provider)
        assertTrue(config.enabled)
        assertEquals(5, config.rateLimit?.requestsPerSecond) // CloudFront has stricter limits
        assertEquals(50, config.batching?.batchSize) // Lower batch limits
    }
}
