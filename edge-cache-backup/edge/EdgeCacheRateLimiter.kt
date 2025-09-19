package com.yourcompany.cacheflow.edge

import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Rate limiter for edge cache operations using token bucket algorithm */
class EdgeCacheRateLimiter(
        private val rateLimit: RateLimit,
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val tokens = AtomicInteger(rateLimit.burstSize)
    private val lastRefill = AtomicLong(System.currentTimeMillis())
    private val mutex = Mutex()

    /**
     * Try to acquire a token for operation
     * @return true if token acquired, false if rate limited
     */
    suspend fun tryAcquire(): Boolean {
        return mutex.withLock {
            refillTokens()
            if (tokens.get() > 0) {
                tokens.decrementAndGet()
                true
            } else {
                false
            }
        }
    }

    /**
     * Wait for a token to become available
     * @param timeout Maximum time to wait
     * @return true if token acquired, false if timeout
     */
    suspend fun acquire(timeout: Duration = Duration.ofSeconds(30)): Boolean {
        val startTime = Instant.now()

        while (Instant.now().isBefore(startTime.plus(timeout))) {
            if (tryAcquire()) {
                return true
            }
            delay(100) // Wait 100ms before retry
        }
        return false
    }

    /** Get current token count */
    fun getAvailableTokens(): Int = tokens.get()

    /** Get time until next token is available */
    fun getTimeUntilNextToken(): Duration {
        val now = System.currentTimeMillis()
        val timeSinceLastRefill = now - lastRefill.get()
        val tokensToAdd = (timeSinceLastRefill / 1000.0 * rateLimit.requestsPerSecond).toInt()

        return if (tokensToAdd > 0) {
            Duration.ZERO
        } else {
            val timeUntilNextToken = 1000.0 / rateLimit.requestsPerSecond
            Duration.ofMillis(timeUntilNextToken.toLong())
        }
    }

    private fun refillTokens() {
        val now = System.currentTimeMillis()
        val timeSinceLastRefill = now - lastRefill.get()
        val tokensToAdd = (timeSinceLastRefill / 1000.0 * rateLimit.requestsPerSecond).toInt()

        if (tokensToAdd > 0) {
            val currentTokens = tokens.get()
            val newTokens = minOf(currentTokens + tokensToAdd, rateLimit.burstSize)
            tokens.set(newTokens)
            lastRefill.set(now)
        }
    }
}

/** Circuit breaker for edge cache operations */
class EdgeCacheCircuitBreaker(
        private val config: CircuitBreakerConfig,
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private var state = CircuitBreakerState.CLOSED
    private var failureCount = 0
    private var lastFailureTime = Instant.MIN
    private var halfOpenCalls = 0
    private val mutex = Mutex()

    enum class CircuitBreakerState {
        CLOSED, // Normal operation
        OPEN, // Circuit is open, calls fail fast
        HALF_OPEN // Testing if service is back
    }

    /** Execute operation with circuit breaker protection */
    suspend fun <T> execute(operation: suspend () -> T): T {
        return mutex.withLock {
            when (state) {
                CircuitBreakerState.CLOSED -> executeWithFallback(operation)
                CircuitBreakerState.OPEN -> {
                    if (shouldAttemptReset()) {
                        state = CircuitBreakerState.HALF_OPEN
                        halfOpenCalls = 0
                        executeWithFallback(operation)
                    } else {
                        throw CircuitBreakerOpenException("Circuit breaker is OPEN")
                    }
                }
                CircuitBreakerState.HALF_OPEN -> {
                    if (halfOpenCalls < config.halfOpenMaxCalls) {
                        halfOpenCalls++
                        executeWithFallback(operation)
                    } else {
                        throw CircuitBreakerOpenException(
                                "Circuit breaker is HALF_OPEN, max calls exceeded"
                        )
                    }
                }
            }
        }
    }

    private suspend fun <T> executeWithFallback(operation: suspend () -> T): T {
        return try {
            val result = operation()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }

    private fun onSuccess() {
        failureCount = 0
        state = CircuitBreakerState.CLOSED
    }

    private fun onFailure() {
        failureCount++
        lastFailureTime = Instant.now()

        if (failureCount >= config.failureThreshold) {
            state = CircuitBreakerState.OPEN
        }
    }

    private fun shouldAttemptReset(): Boolean {
        return Instant.now().isAfter(lastFailureTime.plus(config.recoveryTimeout))
    }

    fun getState(): CircuitBreakerState = state
    fun getFailureCount(): Int = failureCount
}

/** Exception thrown when circuit breaker is open */
class CircuitBreakerOpenException(message: String) : Exception(message)

/** Batching processor for edge cache operations */
class EdgeCacheBatcher(
        private val config: BatchingConfig,
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val batchChannel = Channel<String>(Channel.UNLIMITED)
    private val batches = mutableListOf<String>()
    private val mutex = Mutex()

    init {
        scope.launch { processBatches() }
    }

    /** Add URL to batch processing */
    suspend fun addUrl(url: String) {
        batchChannel.send(url)
    }

    /** Get flow of batched URLs */
    fun getBatchedUrls(): Flow<List<String>> = flow {
        val batch = mutableListOf<String>()
        val timeout = config.batchTimeout

        while (true) {
            try {
                val url = withTimeoutOrNull(timeout) { batchChannel.receive() }

                if (url != null) {
                    batch.add(url)

                    if (batch.size >= config.batchSize) {
                        emit(batch.toList())
                        batch.clear()
                    }
                } else {
                    // Timeout reached, emit current batch if not empty
                    if (batch.isNotEmpty()) {
                        emit(batch.toList())
                        batch.clear()
                    }
                }
            } catch (e: Exception) {
                // Channel closed or other error
                break
            }
        }
    }

    private suspend fun processBatches() {
        getBatchedUrls().collect { batch ->
            // Process batch concurrently
            batch.chunked(config.maxConcurrency).forEach { chunk ->
                scope.launch { processBatch(chunk) }
            }
        }
    }

    private suspend fun processBatch(batch: List<String>) {
        // This would be implemented by the specific edge cache provider
        // For now, just log the batch
        println("Processing batch of ${batch.size} URLs: $batch")
    }

    fun close() {
        batchChannel.close()
    }
}
