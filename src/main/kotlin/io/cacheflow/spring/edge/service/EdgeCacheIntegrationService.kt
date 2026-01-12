package io.cacheflow.spring.edge.service

import io.cacheflow.spring.edge.CircuitBreakerStatus
import io.cacheflow.spring.edge.EdgeCacheManager
import io.cacheflow.spring.edge.EdgeCacheMetrics
import io.cacheflow.spring.edge.EdgeCacheResult
import io.cacheflow.spring.edge.EdgeCacheStatistics
import io.cacheflow.spring.edge.RateLimiterStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Service that integrates edge cache operations with Russian Doll Cache */
@Service
class EdgeCacheIntegrationService(
    private val edgeCacheManager: EdgeCacheManager,
) {
    /** Purge a single URL from edge cache */
    fun purgeUrl(url: String): Flow<EdgeCacheResult> = edgeCacheManager.purgeUrl(url)

    /** Purge multiple URLs from edge cache */
    fun purgeUrls(urls: List<String>): Flow<EdgeCacheResult> = edgeCacheManager.purgeUrls(urls.asFlow())

    /** Purge URLs by tag from edge cache */
    fun purgeByTag(tag: String): Flow<EdgeCacheResult> = edgeCacheManager.purgeByTag(tag)

    /** Purge all cache entries from edge cache */
    fun purgeAll(): Flow<EdgeCacheResult> = edgeCacheManager.purgeAll()

    /** Build a URL for a given cache key and base URL */
    fun buildUrl(
        baseUrl: String,
        cacheKey: String,
    ): String {
        val encodedKey = URLEncoder.encode(cacheKey, StandardCharsets.UTF_8.toString())
        return "$baseUrl/api/cache/$encodedKey"
    }

    /** Build URLs for multiple cache keys */
    fun buildUrls(
        baseUrl: String,
        cacheKeys: List<String>,
    ): List<String> = cacheKeys.map { buildUrl(baseUrl, it) }

    /** Purge cache key from edge cache using base URL */
    fun purgeCacheKey(
        baseUrl: String,
        cacheKey: String,
    ): Flow<EdgeCacheResult> {
        val url = buildUrl(baseUrl, cacheKey)
        return purgeUrl(url)
    }

    /** Purge multiple cache keys from edge cache using base URL */
    fun purgeCacheKeys(
        baseUrl: String,
        cacheKeys: List<String>,
    ): Flow<EdgeCacheResult> {
        val urls = buildUrls(baseUrl, cacheKeys)
        return purgeUrls(urls)
    }

    /** Get health status of all edge cache providers */
    suspend fun getHealthStatus(): Map<String, Boolean> = edgeCacheManager.getHealthStatus()

    /** Get aggregated statistics from all edge cache providers */
    suspend fun getStatistics(): EdgeCacheStatistics = edgeCacheManager.getAggregatedStatistics()

    /** Get rate limiter status */
    fun getRateLimiterStatus(): RateLimiterStatus = edgeCacheManager.getRateLimiterStatus()

    /** Get circuit breaker status */
    fun getCircuitBreakerStatus(): CircuitBreakerStatus = edgeCacheManager.getCircuitBreakerStatus()

    /** Get metrics */
    fun getMetrics(): EdgeCacheMetrics = edgeCacheManager.getMetrics()
}
