package io.cacheflow.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties

private const val DEFAULT_KEY_PREFIX = "rd-cache:"

/**
 * Configuration properties for CacheFlow.
 *
 * @property enabled Whether CacheFlow is enabled
 * @property defaultTtl Default time-to-live for cache entries in seconds
 * @property maxSize Maximum number of cache entries
 * @property storage Storage type for cache implementation
 * @property redis Redis-specific configuration
 * @property cloudflare Cloudflare-specific configuration
 * @property awsCloudFront AWS CloudFront-specific configuration
 * @property fastly Fastly-specific configuration
 * @property metrics Metrics configuration
 * @property baseUrl Base URL for the application
 */
@ConfigurationProperties(prefix = "cacheflow")
data class CacheFlowProperties(
    val enabled: Boolean = true,
    val defaultTtl: Long = 3_600,
    val maxSize: Long = 10_000,
    val storage: StorageType = StorageType.IN_MEMORY,
    val redis: RedisProperties = RedisProperties(),
    val cloudflare: CloudflareProperties = CloudflareProperties(),
    val awsCloudFront: AwsCloudFrontProperties = AwsCloudFrontProperties(),
    val fastly: FastlyProperties = FastlyProperties(),
    val metrics: MetricsProperties = MetricsProperties(),
    val warming: WarmingProperties = WarmingProperties(),
    val baseUrl: String = "https://yourdomain.com",
) {
    /**
     * Storage type enumeration for cache implementation.
     */
    enum class StorageType {
        IN_MEMORY,
        REDIS,
        CAFFEINE,
        CLOUDFLARE,
    }

    /**
     * Redis-specific configuration properties.
     *
     * @property keyPrefix Prefix for Redis keys
     * @property database Redis database number
     * @property timeout Connection timeout in milliseconds
     */
    data class RedisProperties(
        val keyPrefix: String = DEFAULT_KEY_PREFIX,
        val database: Int = 0,
        val timeout: Long = 5_000,
    )

    /**
     * Cloudflare-specific configuration properties.
     *
     * @property enabled Whether Cloudflare caching is enabled
     * @property zoneId Cloudflare zone ID
     * @property apiToken Cloudflare API token
     * @property keyPrefix Prefix for cache keys
     * @property defaultTtl Default TTL in seconds
     * @property autoPurge Whether to auto-purge on updates
     * @property purgeOnEvict Whether to purge on eviction
     * @property rateLimit Rate limiting configuration
     * @property circuitBreaker Circuit breaker configuration
     */
    data class CloudflareProperties(
        val enabled: Boolean = false,
        val zoneId: String = "",
        val apiToken: String = "",
        val keyPrefix: String = DEFAULT_KEY_PREFIX,
        val defaultTtl: Long = 3_600,
        val autoPurge: Boolean = true,
        val purgeOnEvict: Boolean = true,
        val rateLimit: RateLimit? = null,
        val circuitBreaker: CircuitBreakerConfig? = null,
    )

    /**
     * AWS CloudFront-specific configuration properties.
     *
     * @property enabled Whether AWS CloudFront caching is enabled
     * @property distributionId CloudFront distribution ID
     * @property keyPrefix Prefix for cache keys
     * @property defaultTtl Default TTL in seconds
     * @property autoPurge Whether to auto-purge on updates
     * @property purgeOnEvict Whether to purge on eviction
     * @property rateLimit Rate limiting configuration
     * @property circuitBreaker Circuit breaker configuration
     */
    data class AwsCloudFrontProperties(
        val enabled: Boolean = false,
        val distributionId: String = "",
        val keyPrefix: String = DEFAULT_KEY_PREFIX,
        val defaultTtl: Long = 3_600,
        val autoPurge: Boolean = true,
        val purgeOnEvict: Boolean = true,
        val rateLimit: RateLimit? = null,
        val circuitBreaker: CircuitBreakerConfig? = null,
    )

    /**
     * Fastly-specific configuration properties.
     *
     * @property enabled Whether Fastly caching is enabled
     * @property serviceId Fastly service ID
     * @property apiToken Fastly API token
     * @property keyPrefix Prefix for cache keys
     * @property defaultTtl Default TTL in seconds
     * @property autoPurge Whether to auto-purge on updates
     * @property purgeOnEvict Whether to purge on eviction
     * @property rateLimit Rate limiting configuration
     * @property circuitBreaker Circuit breaker configuration
     */
    data class FastlyProperties(
        val enabled: Boolean = false,
        val serviceId: String = "",
        val apiToken: String = "",
        val keyPrefix: String = DEFAULT_KEY_PREFIX,
        val defaultTtl: Long = 3_600,
        val autoPurge: Boolean = true,
        val purgeOnEvict: Boolean = true,
        val rateLimit: RateLimit? = null,
        val circuitBreaker: CircuitBreakerConfig? = null,
    )

    /**
     * Rate limiting configuration.
     *
     * @property requestsPerSecond Maximum requests per second
     * @property burstSize Maximum burst size
     * @property windowSize Time window in seconds
     */
    data class RateLimit(
        val requestsPerSecond: Int = 10,
        val burstSize: Int = 20,
        val windowSize: Long = 60, // seconds
    )

    /**
     * Circuit breaker configuration.
     *
     * @property failureThreshold Number of failures before opening circuit
     * @property recoveryTimeout Time to wait before attempting recovery in seconds
     * @property halfOpenMaxCalls Maximum calls in half-open state
     */
    data class CircuitBreakerConfig(
        val failureThreshold: Int = 5,
        val recoveryTimeout: Long = 60, // seconds
        val halfOpenMaxCalls: Int = 3,
    )

    /**
     * Metrics configuration.
     *
     * @property enabled Whether metrics are enabled
     * @property exportInterval Export interval in seconds
     */
    data class MetricsProperties(
        val enabled: Boolean = true,
        val exportInterval: Long = 60,
    )

    /**
     * Cache warming configuration.
     *
     * @property enabled Whether cache warming is enabled
     */
    data class WarmingProperties(
        val enabled: Boolean = true,
    )
}
