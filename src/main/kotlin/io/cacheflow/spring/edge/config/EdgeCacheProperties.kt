package io.cacheflow.spring.edge.config

import io.cacheflow.spring.edge.*
import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration properties for edge cache providers */
@ConfigurationProperties(prefix = "cacheflow.edge")
data class EdgeCacheProperties(
        val enabled: Boolean = true,
        val cloudflare: CloudflareEdgeCacheProperties = CloudflareEdgeCacheProperties(),
        val awsCloudFront: AwsCloudFrontEdgeCacheProperties = AwsCloudFrontEdgeCacheProperties(),
        val fastly: FastlyEdgeCacheProperties = FastlyEdgeCacheProperties(),
        val rateLimit: EdgeCacheRateLimitProperties? = null,
        val circuitBreaker: EdgeCacheCircuitBreakerProperties? = null,
        val batching: EdgeCacheBatchingProperties? = null,
        val monitoring: EdgeCacheMonitoringProperties? = null
) {
        data class CloudflareEdgeCacheProperties(
                val enabled: Boolean = false,
                val zoneId: String = "",
                val apiToken: String = "",
                val keyPrefix: String = "rd-cache:",
                val defaultTtl: Long = 3600,
                val autoPurge: Boolean = true,
                val purgeOnEvict: Boolean = true
        )

        data class AwsCloudFrontEdgeCacheProperties(
                val enabled: Boolean = false,
                val distributionId: String = "",
                val keyPrefix: String = "rd-cache:",
                val defaultTtl: Long = 3600,
                val autoPurge: Boolean = true,
                val purgeOnEvict: Boolean = true
        )

        data class FastlyEdgeCacheProperties(
                val enabled: Boolean = false,
                val serviceId: String = "",
                val apiToken: String = "",
                val keyPrefix: String = "rd-cache:",
                val defaultTtl: Long = 3600,
                val autoPurge: Boolean = true,
                val purgeOnEvict: Boolean = true
        )

        data class EdgeCacheRateLimitProperties(
                val requestsPerSecond: Int = 10,
                val burstSize: Int = 20,
                val windowSize: Long = 60 // seconds
        )

        data class EdgeCacheCircuitBreakerProperties(
                val failureThreshold: Int = 5,
                val recoveryTimeout: Long = 60, // seconds
                val halfOpenMaxCalls: Int = 3
        )

        data class EdgeCacheBatchingProperties(
                val batchSize: Int = 100,
                val batchTimeout: Long = 5, // seconds
                val maxConcurrency: Int = 10
        )

        data class EdgeCacheMonitoringProperties(
                val enableMetrics: Boolean = true,
                val enableTracing: Boolean = true,
                val logLevel: String = "INFO"
        )
}
