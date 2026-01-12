package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.time.Instant

/** Cloudflare edge cache provider implementation */
class CloudflareEdgeCacheProvider(
    private val webClient: WebClient,
    private val zoneId: String,
    private val apiToken: String,
    private val keyPrefix: String = "rd-cache:",
    private val baseUrl: String = "https://api.cloudflare.com/client/v4/zones/$zoneId",
) : EdgeCacheProvider {
    override val providerName: String = "cloudflare"

    private val costPerPurge = 0.001 // $0.001 per purge operation

    override suspend fun isHealthy(): Boolean =
        try {
            webClient
                .get()
                .uri("$baseUrl/health")
                .header("Authorization", "Bearer $apiToken")
                .retrieve()
                .bodyToMono(String::class.java)
                .awaitSingleOrNull()
            true
        } catch (e: Exception) {
            false
        }

    override suspend fun purgeUrl(url: String): EdgeCacheResult {
        val startTime = Instant.now()

        return try {
            val response =
                webClient
                    .post()
                    .uri("$baseUrl/purge_cache")
                    .header("Authorization", "Bearer $apiToken")
                    .header("Content-Type", "application/json")
                    .bodyValue(mapOf("files" to listOf(url)))
                    .retrieve()
                    .bodyToMono(CloudflarePurgeResponse::class.java)
                    .awaitSingle()

            val latency = Duration.between(startTime, Instant.now())
            val cost =
                EdgeCacheCost(
                    operation = EdgeCacheOperation.PURGE_URL,
                    costPerOperation = costPerPurge,
                    totalCost = costPerPurge,
                )

            EdgeCacheResult.success(
                provider = providerName,
                operation = EdgeCacheOperation.PURGE_URL,
                url = url,
                purgedCount = 1,
                cost = cost,
                latency = latency,
                metadata = mapOf("cloudflare_response" to response, "zone_id" to zoneId),
            )
        } catch (e: Exception) {
            EdgeCacheResult.failure(
                provider = providerName,
                operation = EdgeCacheOperation.PURGE_URL,
                error = e,
                url = url,
            )
        }
    }

    override fun purgeUrls(urls: Flow<String>): Flow<EdgeCacheResult> =
        flow {
            urls
                .buffer(100) // Buffer up to 100 URLs
                .collect { url -> emit(purgeUrl(url)) }
        }

    override suspend fun purgeByTag(tag: String): EdgeCacheResult {
        val startTime = Instant.now()

        return try {
            val response =
                webClient
                    .post()
                    .uri("$baseUrl/purge_cache")
                    .header("Authorization", "Bearer $apiToken")
                    .header("Content-Type", "application/json")
                    .bodyValue(mapOf("tags" to listOf(tag)))
                    .retrieve()
                    .bodyToMono(CloudflarePurgeResponse::class.java)
                    .awaitSingle()

            val latency = Duration.between(startTime, Instant.now())
            val cost =
                EdgeCacheCost(
                    operation = EdgeCacheOperation.PURGE_TAG,
                    costPerOperation = costPerPurge,
                    totalCost = costPerPurge,
                )

            EdgeCacheResult.success(
                provider = providerName,
                operation = EdgeCacheOperation.PURGE_TAG,
                tag = tag,
                purgedCount = response.result?.purgedCount ?: 0,
                cost = cost,
                latency = latency,
                metadata = mapOf("cloudflare_response" to response, "zone_id" to zoneId),
            )
        } catch (e: Exception) {
            EdgeCacheResult.failure(
                provider = providerName,
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
                webClient
                    .post()
                    .uri("$baseUrl/purge_cache")
                    .header("Authorization", "Bearer $apiToken")
                    .header("Content-Type", "application/json")
                    .bodyValue(mapOf("purge_everything" to true))
                    .retrieve()
                    .bodyToMono(CloudflarePurgeResponse::class.java)
                    .awaitSingle()

            val latency = Duration.between(startTime, Instant.now())
            val cost =
                EdgeCacheCost(
                    operation = EdgeCacheOperation.PURGE_ALL,
                    costPerOperation = costPerPurge,
                    totalCost = costPerPurge,
                )

            EdgeCacheResult.success(
                provider = providerName,
                operation = EdgeCacheOperation.PURGE_ALL,
                purgedCount = response.result?.purgedCount ?: 0,
                cost = cost,
                latency = latency,
                metadata = mapOf("cloudflare_response" to response, "zone_id" to zoneId),
            )
        } catch (e: Exception) {
            EdgeCacheResult.failure(
                provider = providerName,
                operation = EdgeCacheOperation.PURGE_ALL,
                error = e,
            )
        }
    }

    override suspend fun getStatistics(): EdgeCacheStatistics =
        try {
            val response =
                webClient
                    .get()
                    .uri("$baseUrl/analytics/dashboard")
                    .header("Authorization", "Bearer $apiToken")
                    .retrieve()
                    .bodyToMono(CloudflareAnalyticsResponse::class.java)
                    .awaitSingle()

            EdgeCacheStatistics(
                provider = providerName,
                totalRequests = response.totalRequests ?: 0,
                successfulRequests = response.successfulRequests ?: 0,
                failedRequests = response.failedRequests ?: 0,
                averageLatency = Duration.ofMillis(response.averageLatency ?: 0),
                totalCost = response.totalCost ?: 0.0,
                cacheHitRate = response.cacheHitRate,
            )
        } catch (e: Exception) {
            // Return default statistics if API call fails
            EdgeCacheStatistics(
                provider = providerName,
                totalRequests = 0,
                successfulRequests = 0,
                failedRequests = 0,
                averageLatency = Duration.ZERO,
                totalCost = 0.0,
            )
        }

    override fun getConfiguration(): EdgeCacheConfiguration =
        EdgeCacheConfiguration(
            provider = providerName,
            enabled = true,
            rateLimit =
                RateLimit(
                    requestsPerSecond = 10,
                    burstSize = 20,
                    windowSize = Duration.ofMinutes(1),
                ),
            circuitBreaker =
                CircuitBreakerConfig(
                    failureThreshold = 5,
                    recoveryTimeout = Duration.ofMinutes(1),
                    halfOpenMaxCalls = 3,
                ),
            batching =
                BatchingConfig(
                    batchSize = 100,
                    batchTimeout = Duration.ofSeconds(5),
                    maxConcurrency = 10,
                ),
            monitoring =
                MonitoringConfig(
                    enableMetrics = true,
                    enableTracing = true,
                    logLevel = "INFO",
                ),
        )
}

/** Cloudflare purge response */
data class CloudflarePurgeResponse(
    val success: Boolean,
    val errors: List<CloudflareError>? = null,
    val messages: List<String>? = null,
    val result: CloudflarePurgeResult? = null,
)

data class CloudflarePurgeResult(
    val id: String? = null,
    val purgedCount: Long? = null,
)

data class CloudflareError(
    val code: Int,
    val message: String,
)

/** Cloudflare analytics response */
data class CloudflareAnalyticsResponse(
    val totalRequests: Long? = null,
    val successfulRequests: Long? = null,
    val failedRequests: Long? = null,
    val averageLatency: Long? = null,
    val totalCost: Double? = null,
    val cacheHitRate: Double? = null,
)
