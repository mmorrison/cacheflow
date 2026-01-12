package io.cacheflow.spring.edge.management

import io.cacheflow.spring.edge.*
import kotlinx.coroutines.flow.toList
import org.springframework.boot.actuate.endpoint.annotation.*
import org.springframework.stereotype.Component

/** Management endpoint for edge cache operations */
@Component
@Endpoint(id = "edgecache")
class EdgeCacheManagementEndpoint(
    private val edgeCacheManager: EdgeCacheManager,
) {
    @ReadOperation
    suspend fun getHealthStatus(): Map<String, Any> {
        val healthStatus = edgeCacheManager.getHealthStatus()
        val rateLimiterStatus = edgeCacheManager.getRateLimiterStatus()
        val circuitBreakerStatus = edgeCacheManager.getCircuitBreakerStatus()
        val metrics = edgeCacheManager.getMetrics()

        return mapOf(
            "providers" to healthStatus,
            "rateLimiter" to
                mapOf(
                    "availableTokens" to rateLimiterStatus.availableTokens,
                    "timeUntilNextToken" to
                        rateLimiterStatus.timeUntilNextToken.toString(),
                ),
            "circuitBreaker" to
                mapOf(
                    "state" to circuitBreakerStatus.state.name,
                    "failureCount" to circuitBreakerStatus.failureCount,
                ),
            "metrics" to
                mapOf(
                    "totalOperations" to metrics.getTotalOperations(),
                    "successfulOperations" to metrics.getSuccessfulOperations(),
                    "failedOperations" to metrics.getFailedOperations(),
                    "totalCost" to metrics.getTotalCost(),
                    "averageLatency" to metrics.getAverageLatency().toString(),
                    "successRate" to metrics.getSuccessRate(),
                ),
        )
    }

    @ReadOperation
    suspend fun getStatistics(): EdgeCacheStatistics = edgeCacheManager.getAggregatedStatistics()

    @WriteOperation
    suspend fun purgeUrl(
        @Selector url: String,
    ): Map<String, Any> {
        val results = edgeCacheManager.purgeUrl(url).toList()

        return mapOf(
            "url" to url,
            "results" to
                results.map { result ->
                    mapOf(
                        "provider" to result.provider,
                        "success" to result.success,
                        "purgedCount" to result.purgedCount,
                        "cost" to result.cost?.totalCost,
                        "latency" to result.latency?.toString(),
                        "error" to result.error?.message,
                    )
                },
            "summary" to
                mapOf(
                    "totalProviders" to results.size,
                    "successfulProviders" to results.count { it.success },
                    "failedProviders" to results.count { !it.success },
                    "totalCost" to results.sumOf { it.cost?.totalCost ?: 0.0 },
                    "totalPurged" to results.sumOf { it.purgedCount },
                ),
        )
    }

    @WriteOperation
    suspend fun purgeByTag(
        @Selector tag: String,
    ): Map<String, Any> {
        val results = edgeCacheManager.purgeByTag(tag).toList()

        return mapOf(
            "tag" to tag,
            "results" to
                results.map { result ->
                    mapOf(
                        "provider" to result.provider,
                        "success" to result.success,
                        "purgedCount" to result.purgedCount,
                        "cost" to result.cost?.totalCost,
                        "latency" to result.latency?.toString(),
                        "error" to result.error?.message,
                    )
                },
            "summary" to
                mapOf(
                    "totalProviders" to results.size,
                    "successfulProviders" to results.count { it.success },
                    "failedProviders" to results.count { !it.success },
                    "totalCost" to results.sumOf { it.cost?.totalCost ?: 0.0 },
                    "totalPurged" to results.sumOf { it.purgedCount },
                ),
        )
    }

    @WriteOperation
    suspend fun purgeAll(): Map<String, Any> {
        val results = edgeCacheManager.purgeAll().toList()

        return mapOf(
            "results" to
                results.map { result ->
                    mapOf(
                        "provider" to result.provider,
                        "success" to result.success,
                        "purgedCount" to result.purgedCount,
                        "cost" to result.cost?.totalCost,
                        "latency" to result.latency?.toString(),
                        "error" to result.error?.message,
                    )
                },
            "summary" to
                mapOf(
                    "totalProviders" to results.size,
                    "successfulProviders" to results.count { it.success },
                    "failedProviders" to results.count { !it.success },
                    "totalCost" to results.sumOf { it.cost?.totalCost ?: 0.0 },
                    "totalPurged" to results.sumOf { it.purgedCount },
                ),
        )
    }

    @DeleteOperation
    suspend fun resetMetrics(): Map<String, String> = mapOf("message" to "Metrics reset not implemented in this version")
}
