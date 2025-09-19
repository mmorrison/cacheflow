# Generic Edge Caching Architecture

## Overview

This document describes the generic edge caching architecture implemented in the CacheFlow Spring Boot Starter. The architecture provides a unified, reactive, and robust solution for integrating with multiple edge cache providers while addressing common challenges like rate limiting, circuit breaking, and cost management.

## Architecture Components

### 1. Core Interfaces

#### `EdgeCacheProvider`

The main interface that all edge cache providers must implement:

```kotlin
interface EdgeCacheProvider {
    val providerName: String
    suspend fun isHealthy(): Boolean
    suspend fun purgeUrl(url: String): EdgeCacheResult
    fun purgeUrls(urls: Flow<String>): Flow<EdgeCacheResult>
    suspend fun purgeByTag(tag: String): EdgeCacheResult
    suspend fun purgeAll(): EdgeCacheResult
    suspend fun getStatistics(): EdgeCacheStatistics
    fun getConfiguration(): EdgeCacheConfiguration
}
```

#### `EdgeCacheResult`

Represents the result of an edge cache operation with comprehensive metadata:

```kotlin
data class EdgeCacheResult(
    val success: Boolean,
    val provider: String,
    val operation: EdgeCacheOperation,
    val url: String? = null,
    val tag: String? = null,
    val purgedCount: Long = 0,
    val cost: EdgeCacheCost? = null,
    val latency: Duration? = null,
    val error: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap()
)
```

### 2. Rate Limiting & Circuit Breaking

#### `EdgeCacheRateLimiter`

Implements token bucket algorithm for rate limiting:

```kotlin
class EdgeCacheRateLimiter(
    private val rateLimit: RateLimit,
    private val scope: CoroutineScope
) {
    suspend fun tryAcquire(): Boolean
    suspend fun acquire(timeout: Duration): Boolean
    fun getAvailableTokens(): Int
    fun getTimeUntilNextToken(): Duration
}
```

#### `EdgeCacheCircuitBreaker`

Implements circuit breaker pattern for fault tolerance:

```kotlin
class EdgeCacheCircuitBreaker(
    private val config: CircuitBreakerConfig,
    private val scope: CoroutineScope
) {
    suspend fun <T> execute(operation: suspend () -> T): T
    fun getState(): CircuitBreakerState
    fun getFailureCount(): Int
}
```

### 3. Batching & Flow Processing

#### `EdgeCacheBatcher`

Handles batch processing of edge cache operations:

```kotlin
class EdgeCacheBatcher(
    private val config: BatchingConfig,
    private val scope: CoroutineScope
) {
    suspend fun addUrl(url: String)
    fun getBatchedUrls(): Flow<List<String>>
}
```

### 4. Edge Cache Manager

#### `EdgeCacheManager`

Orchestrates all edge cache operations with comprehensive error handling:

```kotlin
@Component
class EdgeCacheManager(
    private val providers: List<EdgeCacheProvider>,
    private val configuration: EdgeCacheConfiguration
) {
    suspend fun purgeUrl(url: String): Flow<EdgeCacheResult>
    fun purgeUrls(urls: Flow<String>): Flow<EdgeCacheResult>
    suspend fun purgeByTag(tag: String): Flow<EdgeCacheResult>
    suspend fun purgeAll(): Flow<EdgeCacheResult>
    suspend fun getHealthStatus(): Map<String, Boolean>
    suspend fun getAggregatedStatistics(): EdgeCacheStatistics
}
```

## Supported Edge Cache Providers

### 1. Cloudflare

- **Provider**: `CloudflareEdgeCacheProvider`
- **API**: Cloudflare Cache API
- **Rate Limit**: 10 requests/second, 20 burst
- **Cost**: $0.001 per purge operation
- **Features**: URL purging, tag-based purging, analytics

### 2. AWS CloudFront

- **Provider**: `AwsCloudFrontEdgeCacheProvider`
- **API**: AWS CloudFront API
- **Rate Limit**: 5 requests/second, 10 burst
- **Cost**: $0.005 per invalidation
- **Features**: URL invalidation, distribution management

### 3. Fastly

- **Provider**: `FastlyEdgeCacheProvider`
- **API**: Fastly API
- **Rate Limit**: 15 requests/second, 30 burst
- **Cost**: $0.002 per purge operation
- **Features**: URL purging, tag-based purging, soft purging

## Configuration

### YAML Configuration Example

```yaml
cacheflow:
  enabled: true
  default-ttl: 1800

  # Cloudflare configuration
  cloudflare:
    enabled: true
    zone-id: "your-zone-id"
    api-token: "your-api-token"
    rate-limit:
      requests-per-second: 10
      burst-size: 20
    circuit-breaker:
      failure-threshold: 5
      recovery-timeout: 60

  # AWS CloudFront configuration
  aws-cloud-front:
    enabled: false
    distribution-id: "your-distribution-id"
    rate-limit:
      requests-per-second: 5
      burst-size: 10

  # Fastly configuration
  fastly:
    enabled: false
    service-id: "your-service-id"
    api-token: "your-api-token"
    rate-limit:
      requests-per-second: 15
      burst-size: 30
```

## Usage Examples

### 1. Basic URL Purging

```kotlin
@Service
class UserService(
    private val edgeCacheManager: EdgeCacheManager
) {

    @CacheFlowEvict(key = "user-#{#user.id}")
    suspend fun updateUser(user: User) {
        userRepository.save(user)

        // Purge from edge cache
        edgeCacheManager.purgeUrl("/api/users/${user.id}")
            .collect { result ->
                if (result.success) {
                    logger.info("Successfully purged URL: ${result.url}")
                } else {
                    logger.error("Failed to purge URL: ${result.error}")
                }
            }
    }
}
```

### 2. Batch URL Purging

```kotlin
@Service
class UserService(
    private val edgeCacheManager: EdgeCacheManager
) {

    suspend fun updateMultipleUsers(users: List<User>) {
        userRepository.saveAll(users)

        // Purge multiple URLs in batch
        val urls = users.map { "/api/users/${it.id}" }
        edgeCacheManager.purgeUrls(urls.asFlow())
            .collect { result ->
                logger.info("Purged URL: ${result.url}, Success: ${result.success}")
            }
    }
}
```

### 3. Tag-based Purging

```kotlin
@Service
class UserService(
    private val edgeCacheManager: EdgeCacheManager
) {

    @CacheFlowEvict(tags = ["users"])
    suspend fun updateUser(user: User) {
        userRepository.save(user)

        // Purge all URLs tagged with "users"
        edgeCacheManager.purgeByTag("users")
            .collect { result ->
                logger.info("Purged ${result.purgedCount} URLs with tag: ${result.tag}")
            }
    }
}
```

## Monitoring & Metrics

### 1. Health Checks

```kotlin
@RestController
class EdgeCacheHealthController(
    private val edgeCacheManager: EdgeCacheManager
) {

    @GetMapping("/health/edge-cache")
    suspend fun getHealthStatus(): Map<String, Any> {
        val healthStatus = edgeCacheManager.getHealthStatus()
        val rateLimiterStatus = edgeCacheManager.getRateLimiterStatus()
        val circuitBreakerStatus = edgeCacheManager.getCircuitBreakerStatus()

        return mapOf(
            "providers" to healthStatus,
            "rateLimiter" to rateLimiterStatus,
            "circuitBreaker" to circuitBreakerStatus
        )
    }
}
```

### 2. Metrics Collection

```kotlin
@Component
class EdgeCacheMetricsCollector(
    private val edgeCacheManager: EdgeCacheManager,
    private val meterRegistry: MeterRegistry
) {

    @EventListener
    fun onCacheOperation(event: EdgeCacheOperationEvent) {
        val result = event.result

        // Record operation metrics
        meterRegistry.counter("edge.cache.operations",
            "provider", result.provider,
            "operation", result.operation.name,
            "success", result.success.toString()
        ).increment()

        // Record cost metrics
        result.cost?.let { cost ->
            meterRegistry.gauge("edge.cache.cost", cost.totalCost)
        }

        // Record latency metrics
        result.latency?.let { latency ->
            meterRegistry.timer("edge.cache.latency",
                "provider", result.provider
            ).record(latency)
        }
    }
}
```

## Error Handling & Resilience

### 1. Rate Limiting

The system automatically handles rate limiting with exponential backoff:

```kotlin
// Automatic retry with backoff
edgeCacheManager.purgeUrl(url)
    .retryWhen { flow ->
        flow.flatMapLatest { result ->
            if (result.error is RateLimitExceededException) {
                flowOf(result).delay(1000) // Wait 1 second
            } else {
                flowOf(result)
            }
        }
    }
    .collect { result ->
        // Handle result
    }
```

### 2. Circuit Breaker

The circuit breaker automatically opens when failures exceed the threshold:

```kotlin
// Circuit breaker state monitoring
val status = edgeCacheManager.getCircuitBreakerStatus()
when (status.state) {
    CircuitBreakerState.CLOSED -> logger.info("Circuit breaker is closed")
    CircuitBreakerState.OPEN -> logger.warn("Circuit breaker is open")
    CircuitBreakerState.HALF_OPEN -> logger.info("Circuit breaker is half-open")
}
```

### 3. Cost Management

The system tracks costs and can enforce limits:

```kotlin
// Cost monitoring
val statistics = edgeCacheManager.getAggregatedStatistics()
logger.info("Total edge cache cost: $${statistics.totalCost}")

// Cost-based decisions
if (statistics.totalCost > MAX_MONTHLY_COST) {
    logger.warn("Edge cache cost limit exceeded")
    // Implement cost control logic
}
```

## Best Practices

### 1. TTL Strategy

```yaml
# Recommended TTL hierarchy
edge-cache: 3600s # 1 hour
redis-cache: 1800s # 30 minutes
local-cache: 300s # 5 minutes
```

### 2. Rate Limiting

```yaml
# Conservative rate limits
cloudflare:
  rate-limit:
    requests-per-second: 5 # Start conservative
    burst-size: 10
```

### 3. Circuit Breaker

```yaml
# Aggressive circuit breaker for cost control
circuit-breaker:
  failure-threshold: 3
  recovery-timeout: 300 # 5 minutes
```

### 4. Monitoring

```yaml
# Comprehensive monitoring
monitoring:
  enable-metrics: true
  enable-tracing: true
  log-level: INFO
```

## Testing

### 1. Unit Tests

```kotlin
@Test
fun `should handle rate limiting`() = runTest {
    val rateLimiter = EdgeCacheRateLimiter(RateLimit(1, 1))

    assertTrue(rateLimiter.tryAcquire())
    assertFalse(rateLimiter.tryAcquire())
}
```

### 2. Integration Tests

```kotlin
@Test
fun `should purge URL from all providers`() = runTest {
    val results = edgeCacheManager.purgeUrl("https://example.com/test")
        .toList()

    assertTrue(results.isNotEmpty())
    results.forEach { assertNotNull(it) }
}
```

## Conclusion

The generic edge caching architecture provides a robust, scalable, and cost-effective solution for integrating with multiple edge cache providers. It addresses all the key concerns:

- **API Limits**: Rate limiting with token bucket algorithm
- **Async Operations**: Flow-based reactive processing
- **Cost Implications**: Comprehensive cost tracking and management
- **Monitoring**: Detailed metrics and health checks

The architecture is designed to be extensible, allowing easy addition of new edge cache providers while maintaining consistency and reliability across all operations.
