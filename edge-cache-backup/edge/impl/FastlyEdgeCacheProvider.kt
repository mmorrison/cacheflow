package com.yourcompany.cacheflow.edge.impl

import com.yourcompany.cacheflow.edge.*
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.awaitSingleOrNull
import org.springframework.web.reactive.function.client.WebClient

/** Fastly edge cache provider implementation */
class FastlyEdgeCacheProvider(
        private val webClient: WebClient,
        private val serviceId: String,
        private val apiToken: String,
        private val keyPrefix: String = "rd-cache:",
        private val baseUrl: String = "https://api.fastly.com"
) : EdgeCacheProvider {

    override val providerName: String = "fastly"

    private val costPerPurge = 0.002 // $0.002 per purge operation
    private val freeTierLimit = 500 // 500 free purges per month

    override suspend fun isHealthy(): Boolean {
        return try {
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

            val latency = Duration.between(startTime, Instant.now())
            val cost =
                    EdgeCacheCost(
                            operation = EdgeCacheOperation.PURGE_URL,
                            costPerOperation = costPerPurge,
                            totalCost = costPerPurge
                    )

            EdgeCacheResult.success(
                    provider = providerName,
                    operation = EdgeCacheOperation.PURGE_URL,
                    url = url,
                    purgedCount = 1,
                    cost = cost,
                    latency = latency,
                    metadata = mapOf("fastly_response" to response, "service_id" to serviceId)
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

            val latency = Duration.between(startTime, Instant.now())
            val cost =
                    EdgeCacheCost(
                            operation = EdgeCacheOperation.PURGE_TAG,
                            costPerOperation = costPerPurge,
                            totalCost = costPerPurge
                    )

            EdgeCacheResult.success(
                    provider = providerName,
                    operation = EdgeCacheOperation.PURGE_TAG,
                    tag = tag,
                    purgedCount = response.purgedCount ?: 0,
                    cost = cost,
                    latency = latency,
                    metadata = mapOf("fastly_response" to response, "service_id" to serviceId)
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
                    webClient
                            .post()
                            .uri("$baseUrl/service/$serviceId/purge_all")
                            .header("Fastly-Key", apiToken)
                            .retrieve()
                            .bodyToMono(FastlyPurgeResponse::class.java)
                            .awaitSingle()

            val latency = Duration.between(startTime, Instant.now())
            val cost =
                    EdgeCacheCost(
                            operation = EdgeCacheOperation.PURGE_ALL,
                            costPerOperation = costPerPurge,
                            totalCost = costPerPurge
                    )

            EdgeCacheResult.success(
                    provider = providerName,
                    operation = EdgeCacheOperation.PURGE_ALL,
                    purgedCount = response.purgedCount ?: 0,
                    cost = cost,
                    latency = latency,
                    metadata = mapOf("fastly_response" to response, "service_id" to serviceId)
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
                    webClient
                            .get()
                            .uri("$baseUrl/service/$serviceId/stats")
                            .header("Fastly-Key", apiToken)
                            .retrieve()
                            .bodyToMono(FastlyStatsResponse::class.java)
                            .awaitSingle()

            EdgeCacheStatistics(
                    provider = providerName,
                    totalRequests = response.totalRequests ?: 0,
                    successfulRequests = response.successfulRequests ?: 0,
                    failedRequests = response.failedRequests ?: 0,
                    averageLatency = Duration.ofMillis(response.averageLatency ?: 0),
                    totalCost = response.totalCost ?: 0.0,
                    cacheHitRate = response.cacheHitRate
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
                                requestsPerSecond = 15,
                                burstSize = 30,
                                windowSize = Duration.ofMinutes(1)
                        ),
                circuitBreaker =
                        CircuitBreakerConfig(
                                failureThreshold = 5,
                                recoveryTimeout = Duration.ofMinutes(1),
                                halfOpenMaxCalls = 3
                        ),
                batching =
                        BatchingConfig(
                                batchSize = 200,
                                batchTimeout = Duration.ofSeconds(3),
                                maxConcurrency = 15
                        ),
                monitoring =
                        MonitoringConfig(
                                enableMetrics = true,
                                enableTracing = true,
                                logLevel = "INFO"
                        )
        )
    }
}

/** Fastly purge response */
data class FastlyPurgeResponse(
        val status: String,
        val purgedCount: Long? = null,
        val message: String? = null
)

/** Fastly statistics response */
data class FastlyStatsResponse(
        val totalRequests: Long? = null,
        val successfulRequests: Long? = null,
        val failedRequests: Long? = null,
        val averageLatency: Long? = null,
        val totalCost: Double? = null,
        val cacheHitRate: Double? = null
)
