package io.cacheflow.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cacheflow")
data class CacheFlowProperties(
        val enabled: Boolean = true,
        val defaultTtl: Long = 3600,
        val maxSize: Long = 10000,
        val storage: StorageType = StorageType.IN_MEMORY,
        val redis: RedisProperties = RedisProperties(),
        val cloudflare: CloudflareProperties = CloudflareProperties(),
        val awsCloudFront: AwsCloudFrontProperties = AwsCloudFrontProperties(),
        val fastly: FastlyProperties = FastlyProperties(),
        val metrics: MetricsProperties = MetricsProperties(),
        val baseUrl: String = "https://yourdomain.com"
) {
        enum class StorageType {
                IN_MEMORY,
                REDIS,
                CAFFEINE,
                CLOUDFLARE
        }

        data class RedisProperties(
                val keyPrefix: String = "rd-cache:",
                val database: Int = 0,
                val timeout: Long = 5000
        )

        data class CloudflareProperties(
                val enabled: Boolean = false,
                val zoneId: String = "",
                val apiToken: String = "",
                val keyPrefix: String = "rd-cache:",
                val defaultTtl: Long = 3600,
                val autoPurge: Boolean = true,
                val purgeOnEvict: Boolean = true,
                val rateLimit: RateLimit? = null,
                val circuitBreaker: CircuitBreakerConfig? = null
        )

        data class AwsCloudFrontProperties(
                val enabled: Boolean = false,
                val distributionId: String = "",
                val keyPrefix: String = "rd-cache:",
                val defaultTtl: Long = 3600,
                val autoPurge: Boolean = true,
                val purgeOnEvict: Boolean = true,
                val rateLimit: RateLimit? = null,
                val circuitBreaker: CircuitBreakerConfig? = null
        )

        data class FastlyProperties(
                val enabled: Boolean = false,
                val serviceId: String = "",
                val apiToken: String = "",
                val keyPrefix: String = "rd-cache:",
                val defaultTtl: Long = 3600,
                val autoPurge: Boolean = true,
                val purgeOnEvict: Boolean = true,
                val rateLimit: RateLimit? = null,
                val circuitBreaker: CircuitBreakerConfig? = null
        )

        data class RateLimit(
                val requestsPerSecond: Int = 10,
                val burstSize: Int = 20,
                val windowSize: Long = 60 // seconds
        )

        data class CircuitBreakerConfig(
                val failureThreshold: Int = 5,
                val recoveryTimeout: Long = 60, // seconds
                val halfOpenMaxCalls: Int = 3
        )

        data class MetricsProperties(val enabled: Boolean = true, val exportInterval: Long = 60)
}
