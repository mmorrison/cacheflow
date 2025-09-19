package com.yourcompany.cacheflow.edge

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Component

/**
 * Generic edge cache manager that orchestrates multiple edge cache providers with rate limiting,
 * circuit breaking, and monitoring
 */
@Component
class EdgeCacheManager(
        private val providers: List<EdgeCacheProvider>,
        private val configuration: EdgeCacheConfiguration,
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val rateLimiter =
            EdgeCacheRateLimiter(configuration.rateLimit ?: RateLimit(10, 20), scope)

    private val circuitBreaker =
            EdgeCacheCircuitBreaker(configuration.circuitBreaker ?: CircuitBreakerConfig(), scope)

    private val batcher = EdgeCacheBatcher(configuration.batching ?: BatchingConfig(), scope)

    private val metrics = EdgeCacheMetrics()

    /** Purge a single URL from all enabled providers */
    suspend fun purgeUrl(url: String): Flow<EdgeCacheResult> = flow {
        if (!configuration.enabled) {
            emit(
                    EdgeCacheResult.failure(
                            "disabled",
                            EdgeCacheOperation.PURGE_URL,
                            IllegalStateException("Edge caching is disabled")
                    )
            )
            return@flow
        }

        val startTime = Instant.now()

        try {
            // Check rate limit
            if (!rateLimiter.tryAcquire()) {
                emit(
                        EdgeCacheResult.failure(
                                "rate_limited",
                                EdgeCacheOperation.PURGE_URL,
                                RateLimitExceededException("Rate limit exceeded")
                        )
                )
                return@flow
            }

            // Execute with circuit breaker protection
            val results =
                    circuitBreaker.execute {
                        providers
                                .filter { it.isHealthy() }
                                .map { provider ->
                                    scope.async {
                                        val result = provider.purgeUrl(url)
                                        metrics.recordOperation(result)
                                        result
                                    }
                                }
                                .awaitAll()
                    }

            results.forEach { emit(it) }
        } catch (e: Exception) {
            emit(EdgeCacheResult.failure("error", EdgeCacheOperation.PURGE_URL, e, url))
        } finally {
            val latency = Duration.between(startTime, Instant.now())
            metrics.recordLatency(latency)
        }
    }

    /** Purge multiple URLs using batching */
    fun purgeUrls(urls: Flow<String>): Flow<EdgeCacheResult> = flow {
        urls.collect { url -> batcher.addUrl(url) }

        // Process batched URLs
        batcher.getBatchedUrls().collect { batch ->
            batch
                    .map { url -> scope.async { purgeUrl(url).collect { result -> emit(result) } } }
                    .awaitAll()
        }
    }

    /** Purge by tag from all enabled providers */
    suspend fun purgeByTag(tag: String): Flow<EdgeCacheResult> = flow {
        if (!configuration.enabled) {
            emit(
                    EdgeCacheResult.failure(
                            "disabled",
                            EdgeCacheOperation.PURGE_TAG,
                            IllegalStateException("Edge caching is disabled")
                    )
            )
            return@flow
        }

        val startTime = Instant.now()

        try {
            // Check rate limit
            if (!rateLimiter.tryAcquire()) {
                emit(
                        EdgeCacheResult.failure(
                                "rate_limited",
                                EdgeCacheOperation.PURGE_TAG,
                                RateLimitExceededException("Rate limit exceeded")
                        )
                )
                return@flow
            }

            // Execute with circuit breaker protection
            val results =
                    circuitBreaker.execute {
                        providers
                                .filter { it.isHealthy() }
                                .map { provider ->
                                    scope.async {
                                        val result = provider.purgeByTag(tag)
                                        metrics.recordOperation(result)
                                        result
                                    }
                                }
                                .awaitAll()
                    }

            results.forEach { emit(it) }
        } catch (e: Exception) {
            emit(EdgeCacheResult.failure("error", EdgeCacheOperation.PURGE_TAG, e, tag = tag))
        } finally {
            val latency = Duration.between(startTime, Instant.now())
            metrics.recordLatency(latency)
        }
    }

    /** Purge all cache entries from all enabled providers */
    suspend fun purgeAll(): Flow<EdgeCacheResult> = flow {
        if (!configuration.enabled) {
            emit(
                    EdgeCacheResult.failure(
                            "disabled",
                            EdgeCacheOperation.PURGE_ALL,
                            IllegalStateException("Edge caching is disabled")
                    )
            )
            return@flow
        }

        val startTime = Instant.now()

        try {
            // Check rate limit
            if (!rateLimiter.tryAcquire()) {
                emit(
                        EdgeCacheResult.failure(
                                "rate_limited",
                                EdgeCacheOperation.PURGE_ALL,
                                RateLimitExceededException("Rate limit exceeded")
                        )
                )
                return@flow
            }

            // Execute with circuit breaker protection
            val results =
                    circuitBreaker.execute {
                        providers
                                .filter { it.isHealthy() }
                                .map { provider ->
                                    scope.async {
                                        val result = provider.purgeAll()
                                        metrics.recordOperation(result)
                                        result
                                    }
                                }
                                .awaitAll()
                    }

            results.forEach { emit(result) }
        } catch (e: Exception) {
            emit(EdgeCacheResult.failure("error", EdgeCacheOperation.PURGE_ALL, e))
        } finally {
            val latency = Duration.between(startTime, Instant.now())
            metrics.recordLatency(latency)
        }
    }

    /** Get health status of all providers */
    suspend fun getHealthStatus(): Map<String, Boolean> {
        return providers.associate { provider -> provider.providerName to provider.isHealthy() }
    }

    /** Get aggregated statistics from all providers */
    suspend fun getAggregatedStatistics(): EdgeCacheStatistics {
        val allStats = providers.map { it.getStatistics() }

        return EdgeCacheStatistics(
                provider = "aggregated",
                totalRequests = allStats.sumOf { it.totalRequests },
                successfulRequests = allStats.sumOf { it.successfulRequests },
                failedRequests = allStats.sumOf { it.failedRequests },
                averageLatency =
                        allStats.map { it.averageLatency }.average().let {
                            Duration.ofMillis(it.toLong())
                        },
                totalCost = allStats.sumOf { it.totalCost },
                cacheHitRate =
                        allStats.mapNotNull { it.cacheHitRate }.average().let {
                            if (it.isNaN()) null else it
                        }
        )
    }

    /** Get rate limiter status */
    fun getRateLimiterStatus(): RateLimiterStatus {
        return RateLimiterStatus(
                availableTokens = rateLimiter.getAvailableTokens(),
                timeUntilNextToken = rateLimiter.getTimeUntilNextToken()
        )
    }

    /** Get circuit breaker status */
    fun getCircuitBreakerStatus(): CircuitBreakerStatus {
        return CircuitBreakerStatus(
                state = circuitBreaker.getState(),
                failureCount = circuitBreaker.getFailureCount()
        )
    }

    /** Get metrics */
    fun getMetrics(): EdgeCacheMetrics = metrics

    fun close() {
        batcher.close()
        scope.cancel()
    }
}

/** Rate limiter status */
data class RateLimiterStatus(val availableTokens: Int, val timeUntilNextToken: Duration)

/** Circuit breaker status */
data class CircuitBreakerStatus(
        val state: EdgeCacheCircuitBreaker.CircuitBreakerState,
        val failureCount: Int
)

/** Exception thrown when rate limit is exceeded */
class RateLimitExceededException(message: String) : Exception(message)

/** Metrics collector for edge cache operations */
class EdgeCacheMetrics {
    private val totalOperations = AtomicLong(0)
    private val successfulOperations = AtomicLong(0)
    private val failedOperations = AtomicLong(0)
    private val totalCost = AtomicLong(0) // in cents
    private val totalLatency = AtomicLong(0) // in milliseconds
    private val operationCount = AtomicLong(0)

    fun recordOperation(result: EdgeCacheResult) {
        totalOperations.incrementAndGet()

        if (result.success) {
            successfulOperations.incrementAndGet()
        } else {
            failedOperations.incrementAndGet()
        }

        result.cost?.let { cost ->
            totalCost.addAndGet((cost.totalCost * 100).toLong()) // Convert to cents
        }
    }

    fun recordLatency(latency: Duration) {
        totalLatency.addAndGet(latency.toMillis())
        operationCount.incrementAndGet()
    }

    fun getTotalOperations(): Long = totalOperations.get()
    fun getSuccessfulOperations(): Long = successfulOperations.get()
    fun getFailedOperations(): Long = failedOperations.get()
    fun getTotalCost(): Double = totalCost.get() / 100.0 // Convert back to dollars
    fun getAverageLatency(): Duration =
            if (operationCount.get() > 0) {
                Duration.ofMillis(totalLatency.get() / operationCount.get())
            } else {
                Duration.ZERO
            }
    fun getSuccessRate(): Double =
            if (totalOperations.get() > 0) {
                successfulOperations.get().toDouble() / totalOperations.get()
            } else {
                0.0
            }
}
