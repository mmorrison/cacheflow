package io.cacheflow.spring.edge

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Rate limiter for edge cache operations using token bucket algorithm */
class EdgeCacheRateLimiter(
    private val rateLimit: RateLimit,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    private val tokens = AtomicInteger(rateLimit.burstSize)
    private val lastRefill = AtomicLong(System.currentTimeMillis())
    private val mutex = Mutex()

    /**
     * Try to acquire a token for operation
     * @return true if token acquired, false if rate limited
     */
    suspend fun tryAcquire(): Boolean =
        mutex.withLock {
            refillTokens()
            if (tokens.get() > 0) {
                tokens.decrementAndGet()
                true
            } else {
                false
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    private var state = CircuitBreakerState.CLOSED
    private var failureCount = 0
    private var lastFailureTime = Instant.MIN
    private var halfOpenCalls = 0
    private val mutex = Mutex()

    enum class CircuitBreakerState {
        CLOSED, // Normal operation
        OPEN, // Circuit is open, calls fail fast
        HALF_OPEN, // Testing if service is back
    }

    /** Execute operation with circuit breaker protection */
    suspend fun <T> execute(operation: suspend () -> T): T =
        mutex.withLock {
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
                            "Circuit breaker is HALF_OPEN, max calls exceeded",
                        )
                    }
                }
            }
        }

    private suspend fun <T> executeWithFallback(operation: suspend () -> T): T =
        try {
            val result = operation()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
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

    private fun shouldAttemptReset(): Boolean = Instant.now().isAfter(lastFailureTime.plus(config.recoveryTimeout))

    fun getState(): CircuitBreakerState = state

    fun getFailureCount(): Int = failureCount
}

/** Exception thrown when circuit breaker is open */
class CircuitBreakerOpenException(
    message: String,
) : Exception(message)

/** Batching processor for edge cache operations */
class EdgeCacheBatcher(
    private val config: BatchingConfig,
) {
    private val batchChannel = Channel<String>(Channel.UNLIMITED)

    /** Add URL to batch processing */
    suspend fun addUrl(url: String) {
        batchChannel.send(url)
    }

    /** Get flow of batched URLs */
    fun getBatchedUrls(): Flow<List<String>> =
        flow {
            val batch = mutableListOf<String>()
            val timeoutMillis = config.batchTimeout.toMillis()

            while (true) {
                try {
                    val url = withTimeoutOrNull(timeoutMillis) { batchChannel.receive() }

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
                    if (batch.isNotEmpty()) {
                        emit(batch.toList())
                        batch.clear()
                    }
                    break
                }
            }
        }

    fun close() {
        batchChannel.close()
    }
}
