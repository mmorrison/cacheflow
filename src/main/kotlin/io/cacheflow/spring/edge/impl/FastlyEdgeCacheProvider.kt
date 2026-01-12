package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.BatchingConfig
import io.cacheflow.spring.edge.CircuitBreakerConfig
import io.cacheflow.spring.edge.EdgeCacheOperation
import io.cacheflow.spring.edge.EdgeCacheResult
import io.cacheflow.spring.edge.EdgeCacheStatistics
import io.cacheflow.spring.edge.MonitoringConfig
import io.cacheflow.spring.edge.RateLimit
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.time.Instant

/** Fastly edge cache provider implementation */
class FastlyEdgeCacheProvider(
    private val webClient: WebClient,
    private val serviceId: String,
    private val apiToken: String,
    private val keyPrefix: String = "rd-cache:",
    private val baseUrl: String = "https://api.fastly.com",
) : AbstractEdgeCacheProvider() {
    override val providerName: String = "fastly"
    override val costPerOperation = 0.002 // $0.002 per purge operation

    override suspend fun isHealthy(): Boolean =
        try {
            webClient
                .get()
                .uri("$baseUrl/service/$serviceId/health")
                .header("Fastly-Key", apiToken)
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
                    .uri("$baseUrl/purge/$url")
                    .header("Fastly-Key", apiToken)
                    .header("Fastly-Soft-Purge", "0")
                    .retrieve()
                    .bodyToMono(FastlyPurgeResponse::class.java)
                    .awaitSingle()

            buildSuccessResult(
                operation = EdgeCacheOperation.PURGE_URL,
                startTime = startTime,
                purgedCount = 1,
                url = url,
                metadata = mapOf("fastly_response" to response, "service_id" to serviceId),
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
            val response =
                webClient
                    .post()
                    .uri("$baseUrl/service/$serviceId/purge")
                    .header("Fastly-Key", apiToken)
                    .header("Fastly-Soft-Purge", "0")
                    .header("Fastly-Tags", tag)
                    .retrieve()
                    .bodyToMono(FastlyPurgeResponse::class.java)
                    .awaitSingle()

            buildSuccessResult(
                operation = EdgeCacheOperation.PURGE_TAG,
                startTime = startTime,
                purgedCount = response.purgedCount ?: 0,
                tag = tag,
                metadata = mapOf("fastly_response" to response, "service_id" to serviceId),
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
                webClient
                    .post()
                    .uri("$baseUrl/service/$serviceId/purge_all")
                    .header("Fastly-Key", apiToken)
                    .retrieve()
                    .bodyToMono(FastlyPurgeResponse::class.java)
                    .awaitSingle()

            buildSuccessResult(
                operation = EdgeCacheOperation.PURGE_ALL,
                startTime = startTime,
                purgedCount = response.purgedCount ?: 0,
                metadata = mapOf("fastly_response" to response, "service_id" to serviceId),
            )
        } catch (e: Exception) {
            buildFailureResult(
                operation = EdgeCacheOperation.PURGE_ALL,
                error = e,
            )
        }
    }

    override suspend fun getStatisticsFromProvider(): EdgeCacheStatistics {
        val response =
            webClient
                .get()
                .uri("$baseUrl/service/$serviceId/stats")
                .header("Fastly-Key", apiToken)
                .retrieve()
                .bodyToMono(FastlyStatsResponse::class.java)
                .awaitSingle()

        return EdgeCacheStatistics(
            provider = providerName,
            totalRequests = response.totalRequests ?: 0,
            successfulRequests = response.successfulRequests ?: 0,
            failedRequests = response.failedRequests ?: 0,
            averageLatency = Duration.ofMillis(response.averageLatency ?: 0),
            totalCost = response.totalCost ?: 0.0,
            cacheHitRate = response.cacheHitRate,
        )
    }

    override fun createRateLimit(): RateLimit =
        RateLimit(
            requestsPerSecond = 15,
            burstSize = 30,
            windowSize = Duration.ofMinutes(1),
        )

    override fun createCircuitBreaker(): CircuitBreakerConfig =
        CircuitBreakerConfig(
            failureThreshold = 5,
            recoveryTimeout = Duration.ofMinutes(1),
            halfOpenMaxCalls = 3,
        )

    override fun createBatchingConfig(): BatchingConfig =
        BatchingConfig(
            batchSize = 200,
            batchTimeout = Duration.ofSeconds(3),
            maxConcurrency = 15,
        )

    override fun createMonitoringConfig(): MonitoringConfig =
        MonitoringConfig(
            enableMetrics = true,
            enableTracing = true,
            logLevel = "INFO",
        )
}

/** Fastly purge response */
data class FastlyPurgeResponse(
    val status: String,
    val purgedCount: Long? = null,
    val message: String? = null,
)

/** Fastly statistics response */
data class FastlyStatsResponse(
    val totalRequests: Long? = null,
    val successfulRequests: Long? = null,
    val failedRequests: Long? = null,
    val averageLatency: Long? = null,
    val totalCost: Double? = null,
    val cacheHitRate: Double? = null,
)
