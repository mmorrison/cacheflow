package io.cacheflow.spring.edge.config

import org.springframework.boot.context.properties.ConfigurationProperties

private const val DEFAULT_REQUESTS_PER_SECOND = 10
private const val DEFAULT_BURST_SIZE = 20
private const val DEFAULT_WINDOW_SIZE_SECONDS = 60L
private const val DEFAULT_FAILURE_THRESHOLD = 5
private const val DEFAULT_RECOVERY_TIMEOUT_SECONDS = 60L
private const val DEFAULT_HALF_OPEN_MAX_CALLS = 3
private const val DEFAULT_BATCH_SIZE = 100
private const val DEFAULT_BATCH_TIMEOUT_SECONDS = 5L
private const val DEFAULT_MAX_CONCURRENCY = 10

private const val DEFAULT_KEY_PREFIX = "rd-cache:"

/**
 * Configuration properties for edge cache providers.
 *
 * @property enabled Whether edge caching is enabled
 * @property cloudflare Cloudflare edge cache configuration
 * @property awsCloudFront AWS CloudFront edge cache configuration
 * @property fastly Fastly edge cache configuration
 * @property rateLimit Rate limiting configuration
 * @property circuitBreaker Circuit breaker configuration
 * @property batching Batching configuration
 * @property monitoring Monitoring configuration
 */
@ConfigurationProperties(prefix = "cacheflow.edge")
data class EdgeCacheProperties(
    val enabled: Boolean = true,
    val cloudflare: CloudflareEdgeCacheProperties = CloudflareEdgeCacheProperties(),
    val awsCloudFront: AwsCloudFrontEdgeCacheProperties = AwsCloudFrontEdgeCacheProperties(),
    val fastly: FastlyEdgeCacheProperties = FastlyEdgeCacheProperties(),
    val rateLimit: EdgeCacheRateLimitProperties? = null,
    val circuitBreaker: EdgeCacheCircuitBreakerProperties? = null,
    val batching: EdgeCacheBatchingProperties? = null,
    val monitoring: EdgeCacheMonitoringProperties? = null,
) {
    /**
     * Cloudflare edge cache configuration properties.
     *
     * @property enabled Whether Cloudflare edge caching is enabled
     * @property zoneId Cloudflare zone ID
     * @property apiToken Cloudflare API token
     * @property keyPrefix Prefix for cache keys
     * @property defaultTtl Default TTL in seconds
     * @property autoPurge Whether to auto-purge on updates
     * @property purgeOnEvict Whether to purge on eviction
     */
    data class CloudflareEdgeCacheProperties(
        val enabled: Boolean = false,
        val zoneId: String = "",
        val apiToken: String = "",
        val keyPrefix: String = DEFAULT_KEY_PREFIX,
        val defaultTtl: Long = 3_600,
        val autoPurge: Boolean = true,
        val purgeOnEvict: Boolean = true,
    )

    /**
     * AWS CloudFront edge cache configuration properties.
     *
     * @property enabled Whether AWS CloudFront edge caching is enabled
     * @property distributionId CloudFront distribution ID
     * @property keyPrefix Prefix for cache keys
     * @property defaultTtl Default TTL in seconds
     * @property autoPurge Whether to auto-purge on updates
     * @property purgeOnEvict Whether to purge on eviction
     */
    data class AwsCloudFrontEdgeCacheProperties(
        val enabled: Boolean = false,
        val distributionId: String = "",
        val keyPrefix: String = DEFAULT_KEY_PREFIX,
        val defaultTtl: Long = 3_600,
        val autoPurge: Boolean = true,
        val purgeOnEvict: Boolean = true,
    )

    /**
     * Fastly edge cache configuration properties.
     *
     * @property enabled Whether Fastly edge caching is enabled
     * @property serviceId Fastly service ID
     * @property apiToken Fastly API token
     * @property keyPrefix Prefix for cache keys
     * @property defaultTtl Default TTL in seconds
     * @property autoPurge Whether to auto-purge on updates
     * @property purgeOnEvict Whether to purge on eviction
     */
    data class FastlyEdgeCacheProperties(
        val enabled: Boolean = false,
        val serviceId: String = "",
        val apiToken: String = "",
        val keyPrefix: String = DEFAULT_KEY_PREFIX,
        val defaultTtl: Long = 3_600,
        val autoPurge: Boolean = true,
        val purgeOnEvict: Boolean = true,
    )

    /**
     * Edge cache rate limiting configuration.
     *
     * @property requestsPerSecond Maximum requests per second
     * @property burstSize Maximum burst size
     * @property windowSize Time window in seconds
     */
    data class EdgeCacheRateLimitProperties(
        val requestsPerSecond: Int = DEFAULT_REQUESTS_PER_SECOND,
        val burstSize: Int = DEFAULT_BURST_SIZE,
        val windowSize: Long = DEFAULT_WINDOW_SIZE_SECONDS, // seconds
    )

    /**
     * Edge cache circuit breaker configuration.
     *
     * @property failureThreshold Number of failures before opening circuit
     * @property recoveryTimeout Time to wait before attempting recovery in seconds
     * @property halfOpenMaxCalls Maximum calls in half-open state
     */
    data class EdgeCacheCircuitBreakerProperties(
        val failureThreshold: Int = DEFAULT_FAILURE_THRESHOLD,
        val recoveryTimeout: Long = DEFAULT_RECOVERY_TIMEOUT_SECONDS, // seconds
        val halfOpenMaxCalls: Int = DEFAULT_HALF_OPEN_MAX_CALLS,
    )

    /**
     * Edge cache batching configuration.
     *
     * @property batchSize Number of operations per batch
     * @property batchTimeout Maximum time to wait for batch completion in seconds
     * @property maxConcurrency Maximum concurrent batch operations
     */
    data class EdgeCacheBatchingProperties(
        val batchSize: Int = DEFAULT_BATCH_SIZE,
        val batchTimeout: Long = DEFAULT_BATCH_TIMEOUT_SECONDS, // seconds
        val maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
    )

    /**
     * Edge cache monitoring configuration.
     *
     * @property enableMetrics Whether to enable metrics collection
     * @property enableTracing Whether to enable distributed tracing
     * @property logLevel Log level for edge cache operations
     */
    data class EdgeCacheMonitoringProperties(
        val enableMetrics: Boolean = true,
        val enableTracing: Boolean = true,
        val logLevel: String = "INFO",
    )
}
