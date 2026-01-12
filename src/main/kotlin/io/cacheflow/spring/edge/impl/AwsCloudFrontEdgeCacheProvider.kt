package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.BatchingConfig
import io.cacheflow.spring.edge.CircuitBreakerConfig
import io.cacheflow.spring.edge.EdgeCacheOperation
import io.cacheflow.spring.edge.EdgeCacheResult
import io.cacheflow.spring.edge.MonitoringConfig
import io.cacheflow.spring.edge.RateLimit
import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest
import software.amazon.awssdk.services.cloudfront.model.GetDistributionRequest
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch
import software.amazon.awssdk.services.cloudfront.model.Paths
import java.time.Duration
import java.time.Instant

/** AWS CloudFront edge cache provider implementation */
class AwsCloudFrontEdgeCacheProvider(
    private val cloudFrontClient: CloudFrontClient,
    private val distributionId: String,
    private val keyPrefix: String = "rd-cache:",
) : AbstractEdgeCacheProvider() {
    override val providerName: String = "aws-cloudfront"
    override val costPerOperation = 0.005 // $0.005 per invalidation

    override suspend fun isHealthy(): Boolean =
        try {
            cloudFrontClient.getDistribution(
                GetDistributionRequest.builder().id(distributionId).build(),
            )
            true
        } catch (e: Exception) {
            false
        }

    override suspend fun purgeUrl(url: String): EdgeCacheResult {
        val startTime = Instant.now()

        return try {
            val response =
                cloudFrontClient.createInvalidation(
                    CreateInvalidationRequest
                        .builder()
                        .distributionId(distributionId)
                        .invalidationBatch(
                            InvalidationBatch
                                .builder()
                                .paths(
                                    Paths
                                        .builder()
                                        .quantity(1)
                                        .items(url)
                                        .build(),
                                ).callerReference(
                                    "russian-doll-cache-${Instant.now().toEpochMilli()}",
                                ).build(),
                        ).build(),
                )

            buildSuccessResult(
                operation = EdgeCacheOperation.PURGE_URL,
                startTime = startTime,
                purgedCount = 1,
                url = url,
                metadata =
                    mapOf(
                        "invalidation_id" to response.invalidation().id(),
                        "distribution_id" to distributionId,
                        "status" to response.invalidation().status(),
                    ),
            )
        } catch (e: Exception) {
            buildFailureResult(
                operation = EdgeCacheOperation.PURGE_URL,
                error = e,
                url = url,
            )
        }
    }

    override suspend fun purgeByTag(tag: String): EdgeCacheResult {
        val startTime = Instant.now()

        return try {
            // CloudFront doesn't support tag-based invalidation directly
            // We need to maintain a mapping of tags to URLs
            val urls = getUrlsByTag(tag)

            if (urls.isEmpty()) {
                return buildSuccessResult(
                    operation = EdgeCacheOperation.PURGE_TAG,
                    startTime = startTime,
                    purgedCount = 0,
                    tag = tag,
                    metadata = mapOf("message" to "No URLs found for tag"),
                )
            }

            val response =
                cloudFrontClient.createInvalidation(
                    CreateInvalidationRequest
                        .builder()
                        .distributionId(distributionId)
                        .invalidationBatch(
                            InvalidationBatch
                                .builder()
                                .paths(
                                    Paths
                                        .builder()
                                        .quantity(urls.size)
                                        .items(urls)
                                        .build(),
                                ).callerReference(
                                    "russian-doll-cache-tag-$tag-${Instant.now().toEpochMilli()}",
                                ).build(),
                        ).build(),
                )

            buildSuccessResult(
                operation = EdgeCacheOperation.PURGE_TAG,
                startTime = startTime,
                purgedCount = urls.size.toLong(),
                tag = tag,
                metadata =
                    mapOf(
                        "invalidation_id" to response.invalidation().id(),
                        "distribution_id" to distributionId,
                        "status" to response.invalidation().status(),
                        "urls_count" to urls.size,
                    ),
            )
        } catch (e: Exception) {
            buildFailureResult(
                operation = EdgeCacheOperation.PURGE_TAG,
                error = e,
                tag = tag,
            )
        }
    }

    override suspend fun purgeAll(): EdgeCacheResult {
        val startTime = Instant.now()

        return try {
            val response =
                cloudFrontClient.createInvalidation(
                    CreateInvalidationRequest
                        .builder()
                        .distributionId(distributionId)
                        .invalidationBatch(
                            InvalidationBatch
                                .builder()
                                .paths(
                                    Paths
                                        .builder()
                                        .quantity(1)
                                        .items("/*")
                                        .build(),
                                ).callerReference(
                                    "russian-doll-cache-all-${Instant.now().toEpochMilli()}",
                                ).build(),
                        ).build(),
                )

            buildSuccessResult(
                operation = EdgeCacheOperation.PURGE_ALL,
                startTime = startTime,
                purgedCount = Long.MAX_VALUE, // All entries
                metadata =
                    mapOf(
                        "invalidation_id" to response.invalidation().id(),
                        "distribution_id" to distributionId,
                        "status" to response.invalidation().status(),
                    ),
            )
        } catch (e: Exception) {
            buildFailureResult(
                operation = EdgeCacheOperation.PURGE_ALL,
                error = e,
            )
        }
    }

    override fun createRateLimit(): RateLimit =
        RateLimit(
            requestsPerSecond = 5, // CloudFront has stricter limits
            burstSize = 10,
            windowSize = Duration.ofMinutes(1),
        )

    override fun createCircuitBreaker(): CircuitBreakerConfig =
        CircuitBreakerConfig(
            failureThreshold = 3,
            recoveryTimeout = Duration.ofMinutes(2),
            halfOpenMaxCalls = 2,
        )

    override fun createBatchingConfig(): BatchingConfig =
        BatchingConfig(
            batchSize = 50, // CloudFront has lower batch limits
            batchTimeout = Duration.ofSeconds(10),
            maxConcurrency = 5,
        )

    override fun createMonitoringConfig(): MonitoringConfig =
        MonitoringConfig(
            enableMetrics = true,
            enableTracing = true,
            logLevel = "INFO",
        )

    /** Get URLs by tag (requires external storage/mapping) This is a placeholder implementation */
    private suspend fun getUrlsByTag(tag: String): List<String> {
        // In a real implementation, you would maintain a mapping
        // of tags to URLs in a database or cache
        return emptyList()
    }
}
