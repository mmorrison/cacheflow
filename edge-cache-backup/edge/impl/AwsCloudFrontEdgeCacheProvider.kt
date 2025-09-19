package com.yourcompany.cacheflow.edge.impl

import com.yourcompany.cacheflow.edge.*
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.*
import software.amazon.awssdk.services.cloudfront.CloudFrontClient
import software.amazon.awssdk.services.cloudfront.model.*

/** AWS CloudFront edge cache provider implementation */
class AwsCloudFrontEdgeCacheProvider(
        private val cloudFrontClient: CloudFrontClient,
        private val distributionId: String,
        private val keyPrefix: String = "rd-cache:"
) : EdgeCacheProvider {

    override val providerName: String = "aws-cloudfront"

    private val costPerInvalidation = 0.005 // $0.005 per invalidation
    private val freeTierLimit = 1000 // 1000 free invalidations per month

    override suspend fun isHealthy(): Boolean {
        return try {
            cloudFrontClient.getDistribution(
                    GetDistributionRequest.builder().id(distributionId).build()
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun purgeUrl(url: String): EdgeCacheResult {
        val startTime = Instant.now()

        return try {
            val response =
                    cloudFrontClient.createInvalidation(
                            CreateInvalidationRequest.builder()
                                    .distributionId(distributionId)
                                    .invalidationBatch(
                                            InvalidationBatch.builder()
                                                    .paths(
                                                            Paths.builder()
                                                                    .quantity(1)
                                                                    .items(url)
                                                                    .build()
                                                    )
                                                    .callerReference(
                                                            "russian-doll-cache-${Instant.now().toEpochMilli()}"
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )

            val latency = Duration.between(startTime, Instant.now())
            val cost =
                    EdgeCacheCost(
                            operation = EdgeCacheOperation.PURGE_URL,
                            costPerOperation = costPerInvalidation,
                            totalCost = costPerInvalidation
                    )

            EdgeCacheResult.success(
                    provider = providerName,
                    operation = EdgeCacheOperation.PURGE_URL,
                    url = url,
                    purgedCount = 1,
                    cost = cost,
                    latency = latency,
                    metadata =
                            mapOf(
                                    "invalidation_id" to response.invalidation().id(),
                                    "distribution_id" to distributionId,
                                    "status" to response.invalidation().status()
                            )
            )
        } catch (e: Exception) {
            EdgeCacheResult.failure(
                    provider = providerName,
                    operation = EdgeCacheOperation.PURGE_URL,
                    error = e,
                    url = url
            )
        }
    }

    override fun purgeUrls(urls: Flow<String>): Flow<EdgeCacheResult> = flow {
        urls.buffer(100) // Buffer up to 100 URLs
                .collect { url -> emit(purgeUrl(url)) }
    }

    override suspend fun purgeByTag(tag: String): EdgeCacheResult {
        val startTime = Instant.now()

        return try {
            // CloudFront doesn't support tag-based invalidation directly
            // We need to maintain a mapping of tags to URLs
            val urls = getUrlsByTag(tag)

            if (urls.isEmpty()) {
                return EdgeCacheResult.success(
                        provider = providerName,
                        operation = EdgeCacheOperation.PURGE_TAG,
                        tag = tag,
                        purgedCount = 0,
                        metadata = mapOf("message" to "No URLs found for tag")
                )
            }

            val response =
                    cloudFrontClient.createInvalidation(
                            CreateInvalidationRequest.builder()
                                    .distributionId(distributionId)
                                    .invalidationBatch(
                                            InvalidationBatch.builder()
                                                    .paths(
                                                            Paths.builder()
                                                                    .quantity(urls.size)
                                                                    .items(urls)
                                                                    .build()
                                                    )
                                                    .callerReference(
                                                            "russian-doll-cache-tag-${tag}-${Instant.now().toEpochMilli()}"
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )

            val latency = Duration.between(startTime, Instant.now())
            val cost =
                    EdgeCacheCost(
                            operation = EdgeCacheOperation.PURGE_TAG,
                            costPerOperation = costPerInvalidation,
                            totalCost = costPerInvalidation * urls.size
                    )

            EdgeCacheResult.success(
                    provider = providerName,
                    operation = EdgeCacheOperation.PURGE_TAG,
                    tag = tag,
                    purgedCount = urls.size.toLong(),
                    cost = cost,
                    latency = latency,
                    metadata =
                            mapOf(
                                    "invalidation_id" to response.invalidation().id(),
                                    "distribution_id" to distributionId,
                                    "status" to response.invalidation().status(),
                                    "urls_count" to urls.size
                            )
            )
        } catch (e: Exception) {
            EdgeCacheResult.failure(
                    provider = providerName,
                    operation = EdgeCacheOperation.PURGE_TAG,
                    error = e,
                    tag = tag
            )
        }
    }

    override suspend fun purgeAll(): EdgeCacheResult {
        val startTime = Instant.now()

        return try {
            val response =
                    cloudFrontClient.createInvalidation(
                            CreateInvalidationRequest.builder()
                                    .distributionId(distributionId)
                                    .invalidationBatch(
                                            InvalidationBatch.builder()
                                                    .paths(
                                                            Paths.builder()
                                                                    .quantity(1)
                                                                    .items("/*")
                                                                    .build()
                                                    )
                                                    .callerReference(
                                                            "russian-doll-cache-all-${Instant.now().toEpochMilli()}"
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )

            val latency = Duration.between(startTime, Instant.now())
            val cost =
                    EdgeCacheCost(
                            operation = EdgeCacheOperation.PURGE_ALL,
                            costPerOperation = costPerInvalidation,
                            totalCost = costPerInvalidation
                    )

            EdgeCacheResult.success(
                    provider = providerName,
                    operation = EdgeCacheOperation.PURGE_ALL,
                    purgedCount = Long.MAX_VALUE, // All entries
                    cost = cost,
                    latency = latency,
                    metadata =
                            mapOf(
                                    "invalidation_id" to response.invalidation().id(),
                                    "distribution_id" to distributionId,
                                    "status" to response.invalidation().status()
                            )
            )
        } catch (e: Exception) {
            EdgeCacheResult.failure(
                    provider = providerName,
                    operation = EdgeCacheOperation.PURGE_ALL,
                    error = e
            )
        }
    }

    override suspend fun getStatistics(): EdgeCacheStatistics {
        return try {
            val response =
                    cloudFrontClient.getDistribution(
                            GetDistributionRequest.builder().id(distributionId).build()
                    )

            EdgeCacheStatistics(
                    provider = providerName,
                    totalRequests = 0, // CloudFront doesn't provide this via API
                    successfulRequests = 0,
                    failedRequests = 0,
                    averageLatency = Duration.ZERO,
                    totalCost = 0.0,
                    cacheHitRate = null
            )
        } catch (e: Exception) {
            EdgeCacheStatistics(
                    provider = providerName,
                    totalRequests = 0,
                    successfulRequests = 0,
                    failedRequests = 0,
                    averageLatency = Duration.ZERO,
                    totalCost = 0.0
            )
        }
    }

    override fun getConfiguration(): EdgeCacheConfiguration {
        return EdgeCacheConfiguration(
                provider = providerName,
                enabled = true,
                rateLimit =
                        RateLimit(
                                requestsPerSecond = 5, // CloudFront has stricter limits
                                burstSize = 10,
                                windowSize = Duration.ofMinutes(1)
                        ),
                circuitBreaker =
                        CircuitBreakerConfig(
                                failureThreshold = 3,
                                recoveryTimeout = Duration.ofMinutes(2),
                                halfOpenMaxCalls = 2
                        ),
                batching =
                        BatchingConfig(
                                batchSize = 50, // CloudFront has lower batch limits
                                batchTimeout = Duration.ofSeconds(10),
                                maxConcurrency = 5
                        ),
                monitoring =
                        MonitoringConfig(
                                enableMetrics = true,
                                enableTracing = true,
                                logLevel = "INFO"
                        )
        )
    }

    /** Get URLs by tag (requires external storage/mapping) This is a placeholder implementation */
    private suspend fun getUrlsByTag(tag: String): List<String> {
        // In a real implementation, you would maintain a mapping
        // of tags to URLs in a database or cache
        return emptyList()
    }
}
