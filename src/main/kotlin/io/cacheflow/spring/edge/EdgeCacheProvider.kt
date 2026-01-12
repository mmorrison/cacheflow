package io.cacheflow.spring.edge

import kotlinx.coroutines.flow.Flow
import java.time.Duration

/**
 * Generic interface for edge cache providers (Cloudflare, AWS CloudFront, Fastly, etc.) Uses Kotlin
 * Flow for reactive, backpressure-aware operations.
 */
interface EdgeCacheProvider {
    /** Provider identification */
    val providerName: String

    /** Check if the provider is available and healthy */
    suspend fun isHealthy(): Boolean

    /**
     * Purge a single URL from edge cache
     * @param url The URL to purge
     * @return Result indicating success/failure with metadata
     */
    suspend fun purgeUrl(url: String): EdgeCacheResult

    /**
     * Purge multiple URLs from edge cache Uses Flow for backpressure-aware batch processing
     * @param urls Flow of URLs to purge
     * @return Flow of results for each URL
     */
    fun purgeUrls(urls: Flow<String>): Flow<EdgeCacheResult>

    /**
     * Purge URLs by tag/pattern
     * @param tag The tag/pattern to match
     * @return Result indicating success/failure with count of purged URLs
     */
    suspend fun purgeByTag(tag: String): EdgeCacheResult

    /**
     * Purge all cache entries (use with caution)
     * @return Result indicating success/failure
     */
    suspend fun purgeAll(): EdgeCacheResult

    /**
     * Get cache statistics
     * @return Current cache statistics
     */
    suspend fun getStatistics(): EdgeCacheStatistics

    /** Get provider-specific configuration */
    fun getConfiguration(): EdgeCacheConfiguration
}

/** Result of an edge cache operation */
data class EdgeCacheResult(
    val success: Boolean,
    val provider: String,
    val operation: EdgeCacheOperation,
    val url: String? = null,
    val tag: String? = null,
    val purgedCount: Long = 0,
    val cost: EdgeCacheCost? = null,
    val latency: Duration? = null,
    val error: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap(),
) {
    companion object {
        fun success(
            provider: String,
            operation: EdgeCacheOperation,
            url: String? = null,
            tag: String? = null,
            purgedCount: Long = 0,
            cost: EdgeCacheCost? = null,
            latency: Duration? = null,
            metadata: Map<String, Any> = emptyMap(),
        ) = EdgeCacheResult(
            success = true,
            provider = provider,
            operation = operation,
            url = url,
            tag = tag,
            purgedCount = purgedCount,
            cost = cost,
            latency = latency,
            metadata = metadata,
        )

        fun failure(
            provider: String,
            operation: EdgeCacheOperation,
            error: Throwable,
            url: String? = null,
            tag: String? = null,
        ) = EdgeCacheResult(
            success = false,
            provider = provider,
            operation = operation,
            url = url,
            tag = tag,
            error = error,
        )
    }
}

/** Types of edge cache operations */
enum class EdgeCacheOperation {
    PURGE_URL,
    PURGE_URLS,
    PURGE_TAG,
    PURGE_ALL,
    HEALTH_CHECK,
    STATISTICS,
}

/** Cost information for edge cache operations */
data class EdgeCacheCost(
    val operation: EdgeCacheOperation,
    val costPerOperation: Double,
    val currency: String = "USD",
    val totalCost: Double = 0.0,
    val freeTierRemaining: Long? = null,
)

/** Edge cache statistics */
data class EdgeCacheStatistics(
    val provider: String,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageLatency: Duration,
    val totalCost: Double,
    val cacheHitRate: Double? = null,
    val lastUpdated: java.time.Instant = java.time.Instant.now(),
)

/** Edge cache configuration */
data class EdgeCacheConfiguration(
    val provider: String,
    val enabled: Boolean,
    val rateLimit: RateLimit? = null,
    val circuitBreaker: CircuitBreakerConfig? = null,
    val batching: BatchingConfig? = null,
    val monitoring: MonitoringConfig? = null,
)

/** Rate limiting configuration */
data class RateLimit(
    val requestsPerSecond: Int,
    val burstSize: Int,
    val windowSize: Duration = Duration.ofMinutes(1),
)

/** Circuit breaker configuration */
data class CircuitBreakerConfig(
    val failureThreshold: Int = 5,
    val recoveryTimeout: Duration = Duration.ofMinutes(1),
    val halfOpenMaxCalls: Int = 3,
)

/** Batching configuration for bulk operations */
data class BatchingConfig(
    val batchSize: Int = 100,
    val batchTimeout: Duration = Duration.ofSeconds(5),
    val maxConcurrency: Int = 10,
)

/** Monitoring configuration */
data class MonitoringConfig(
    val enableMetrics: Boolean = true,
    val enableTracing: Boolean = true,
    val logLevel: String = "INFO",
)
