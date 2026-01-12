package io.cacheflow.spring.edge.impl

import io.cacheflow.spring.edge.BatchingConfig
import io.cacheflow.spring.edge.CircuitBreakerConfig
import io.cacheflow.spring.edge.EdgeCacheConfiguration
import io.cacheflow.spring.edge.EdgeCacheCost
import io.cacheflow.spring.edge.EdgeCacheOperation
import io.cacheflow.spring.edge.EdgeCacheProvider
import io.cacheflow.spring.edge.EdgeCacheResult
import io.cacheflow.spring.edge.EdgeCacheStatistics
import io.cacheflow.spring.edge.MonitoringConfig
import io.cacheflow.spring.edge.RateLimit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant

/**
 * Abstract base class for edge cache providers that consolidates common functionality.
 *
 * This class provides default implementations for common operations like purging multiple URLs,
 * error handling, and statistics retrieval, reducing code duplication across provider implementations.
 */
abstract class AbstractEdgeCacheProvider : EdgeCacheProvider {
    /**
     * Cost per operation in USD. Override in subclasses to provide provider-specific pricing.
     */
    protected abstract val costPerOperation: Double

    /**
     * Default implementation for purging multiple URLs using Flow.
     * Buffers up to 100 URLs and processes them individually.
     */
    override fun purgeUrls(urls: Flow<String>): Flow<EdgeCacheResult> =
        flow {
            urls
                .buffer(100) // Buffer up to 100 URLs
                .collect { url -> emit(purgeUrl(url)) }
        }

    /**
     * Default implementation for getting statistics with error handling.
     * Subclasses can override to provide provider-specific statistics.
     */
    override suspend fun getStatistics(): EdgeCacheStatistics =
        try {
            getStatisticsFromProvider()
        } catch (e: Exception) {
            EdgeCacheStatistics(
                provider = providerName,
                totalRequests = 0,
                successfulRequests = 0,
                failedRequests = 0,
                averageLatency = Duration.ZERO,
                totalCost = 0.0,
            )
        }

    /**
     * Template method for retrieving provider-specific statistics.
     * Override this method to implement provider-specific statistics retrieval.
     */
    protected open suspend fun getStatisticsFromProvider(): EdgeCacheStatistics =
        EdgeCacheStatistics(
            provider = providerName,
            totalRequests = 0,
            successfulRequests = 0,
            failedRequests = 0,
            averageLatency = Duration.ZERO,
            totalCost = 0.0,
        )

    /**
     * Creates a standard configuration for the edge cache provider.
     * Override this method to customize configuration parameters.
     */
    override fun getConfiguration(): EdgeCacheConfiguration =
        EdgeCacheConfiguration(
            provider = providerName,
            enabled = true,
            rateLimit = createRateLimit(),
            circuitBreaker = createCircuitBreaker(),
            batching = createBatchingConfig(),
            monitoring = createMonitoringConfig(),
        )

    /**
     * Creates rate limit configuration. Override to customize.
     */
    protected open fun createRateLimit(): RateLimit =
        RateLimit(
            requestsPerSecond = 10,
            burstSize = 20,
            windowSize = Duration.ofMinutes(1),
        )

    /**
     * Creates circuit breaker configuration. Override to customize.
     */
    protected open fun createCircuitBreaker(): CircuitBreakerConfig =
        CircuitBreakerConfig(
            failureThreshold = 5,
            recoveryTimeout = Duration.ofMinutes(1),
            halfOpenMaxCalls = 3,
        )

    /**
     * Creates batching configuration. Override to customize.
     */
    protected open fun createBatchingConfig(): BatchingConfig =
        BatchingConfig(
            batchSize = 100,
            batchTimeout = Duration.ofSeconds(5),
            maxConcurrency = 10,
        )

    /**
     * Creates monitoring configuration. Override to customize.
     */
    protected open fun createMonitoringConfig(): MonitoringConfig =
        MonitoringConfig(
            enableMetrics = true,
            enableTracing = true,
            logLevel = "INFO",
        )

    /**
     * Helper method to build a success result with common fields populated.
     */
    protected fun buildSuccessResult(
        operation: EdgeCacheOperation,
        startTime: Instant,
        purgedCount: Long = 1,
        url: String? = null,
        tag: String? = null,
        metadata: Map<String, Any> = emptyMap(),
    ): EdgeCacheResult {
        val latency = Duration.between(startTime, Instant.now())
        val cost =
            EdgeCacheCost(
                operation = operation,
                costPerOperation = costPerOperation,
                totalCost = costPerOperation * purgedCount,
            )

        return EdgeCacheResult.success(
            provider = providerName,
            operation = operation,
            url = url,
            tag = tag,
            purgedCount = purgedCount,
            cost = cost,
            latency = latency,
            metadata = metadata,
        )
    }

    /**
     * Helper method to build a failure result with common fields populated.
     */
    protected fun buildFailureResult(
        operation: EdgeCacheOperation,
        error: Exception,
        url: String? = null,
        tag: String? = null,
    ): EdgeCacheResult =
        EdgeCacheResult.failure(
            provider = providerName,
            operation = operation,
            error = error,
            url = url,
            tag = tag,
        )
}
