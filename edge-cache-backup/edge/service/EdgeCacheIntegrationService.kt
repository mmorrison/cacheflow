package com.yourcompany.cacheflow.edge.service

import com.yourcompany.cacheflow.edge.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Service

/** Service that integrates edge cache operations with Russian Doll Cache */
@Service
class EdgeCacheIntegrationService(private val edgeCacheManager: EdgeCacheManager) {

    /** Purge a single URL from edge cache */
    suspend fun purgeUrl(url: String): Flow<EdgeCacheResult> {
        return edgeCacheManager.purgeUrl(url)
    }

    /** Purge multiple URLs from edge cache */
    fun purgeUrls(urls: List<String>): Flow<EdgeCacheResult> {
        return edgeCacheManager.purgeUrls(urls.asFlow())
    }

    /** Purge URLs by tag from edge cache */
    suspend fun purgeByTag(tag: String): Flow<EdgeCacheResult> {
        return edgeCacheManager.purgeByTag(tag)
    }

    /** Purge all cache entries from edge cache */
    suspend fun purgeAll(): Flow<EdgeCacheResult> {
        return edgeCacheManager.purgeAll()
    }

    /** Build a URL for a given cache key and base URL */
    fun buildUrl(baseUrl: String, cacheKey: String): String {
        val encodedKey = URLEncoder.encode(cacheKey, StandardCharsets.UTF_8.toString())
        return "$baseUrl/api/cache/$encodedKey"
    }

    /** Build URLs for multiple cache keys */
    fun buildUrls(baseUrl: String, cacheKeys: List<String>): List<String> {
        return cacheKeys.map { buildUrl(baseUrl, it) }
    }

    /** Purge cache key from edge cache using base URL */
    suspend fun purgeCacheKey(baseUrl: String, cacheKey: String): Flow<EdgeCacheResult> {
        val url = buildUrl(baseUrl, cacheKey)
        return purgeUrl(url)
    }

    /** Purge multiple cache keys from edge cache using base URL */
    fun purgeCacheKeys(baseUrl: String, cacheKeys: List<String>): Flow<EdgeCacheResult> {
        val urls = buildUrls(baseUrl, cacheKeys)
        return purgeUrls(urls)
    }

    /** Get health status of all edge cache providers */
    suspend fun getHealthStatus(): Map<String, Boolean> {
        return edgeCacheManager.getHealthStatus()
    }

    /** Get aggregated statistics from all edge cache providers */
    suspend fun getStatistics(): EdgeCacheStatistics {
        return edgeCacheManager.getAggregatedStatistics()
    }

    /** Get rate limiter status */
    fun getRateLimiterStatus(): RateLimiterStatus {
        return edgeCacheManager.getRateLimiterStatus()
    }

    /** Get circuit breaker status */
    fun getCircuitBreakerStatus(): CircuitBreakerStatus {
        return edgeCacheManager.getCircuitBreakerStatus()
    }

    /** Get metrics */
    fun getMetrics(): EdgeCacheMetrics {
        return edgeCacheManager.getMetrics()
    }
}
