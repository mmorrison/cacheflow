# Edge Cache Usage Guide

This comprehensive guide explains how to use the generic edge caching functionality in the CacheFlow Spring Boot Starter.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Usage Patterns](#usage-patterns)
- [Advanced Features](#advanced-features)
- [Monitoring & Management](#monitoring--management)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Overview

The edge caching system provides a unified interface for purging content from multiple edge cache providers (Cloudflare, AWS CloudFront, Fastly) with built-in rate limiting, circuit breaking, and monitoring.

### Cache Hierarchy

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Edge Cache    │    │   Redis Cache   │    │  Local Cache    │
│  (Multi-Provider)│    │     (L2)        │    │     (L1)        │
│      (L3)       │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
    TTL: 1 hour         TTL: 30 minutes        TTL: 5 minutes
```

### Key Features

- **Multi-Provider Support**: Cloudflare, AWS CloudFront, Fastly
- **Rate Limiting**: Token bucket algorithm with configurable limits
- **Circuit Breaking**: Fault tolerance with automatic recovery
- **Cost Tracking**: Real-time cost monitoring and management
- **Health Monitoring**: Comprehensive health checks and metrics
- **Reactive Programming**: Full Kotlin Flow support for async operations

## Quick Start

### 1. Add Dependencies

```kotlin
dependencies {
    implementation("com.yourcompany:cacheflow-spring-boot-starter:0.1.0-alpha")

    // For Cloudflare support
    implementation("org.springframework:spring-webflux")

    // For AWS CloudFront support
    implementation("software.amazon.awssdk:cloudfront")

    // For Fastly support (uses WebClient)
    implementation("org.springframework:spring-webflux")
}
```

### 2. Basic Configuration

```yaml
cacheflow:
  enabled: true
  base-url: "https://yourdomain.com"

  # Cloudflare configuration
  cloudflare:
    enabled: true
    zone-id: "your-cloudflare-zone-id"
    api-token: "your-cloudflare-api-token"
    key-prefix: "rd-cache:"
    auto-purge: true
    purge-on-evict: true
```

### 3. Use in Your Service

```kotlin
@Service
class UserService {

    @CacheFlow(key = "user-#{#id}", ttl = "1800")
    suspend fun getUserById(id: Long): User {
        return userRepository.findById(id)
    }

    @CacheFlowEvict(key = "user-#{#user.id}")
    suspend fun updateUser(user: User): User {
        val updatedUser = userRepository.save(user)
        // Edge cache will be automatically purged
        return updatedUser
    }
}
```

## Configuration

### Complete Configuration Example

```yaml
cacheflow:
  enabled: true
  base-url: "https://yourdomain.com"
  default-ttl: 1800 # 30 minutes
  max-size: 10000
  storage: REDIS

  # Redis configuration
  redis:
    enabled: true
    key-prefix: "rd-cache:"
    database: 0
    timeout: 5000
    default-ttl: 1800 # 30 minutes

  # Cloudflare edge cache configuration
  cloudflare:
    enabled: true
    zone-id: "your-cloudflare-zone-id"
    api-token: "your-cloudflare-api-token"
    key-prefix: "rd-cache:"
    default-ttl: 3600 # 1 hour
    auto-purge: true
    purge-on-evict: true

  # AWS CloudFront edge cache configuration
  aws-cloud-front:
    enabled: false
    distribution-id: "your-cloudfront-distribution-id"
    key-prefix: "rd-cache:"
    default-ttl: 3600 # 1 hour
    auto-purge: true
    purge-on-evict: true

  # Fastly edge cache configuration
  fastly:
    enabled: false
    service-id: "your-fastly-service-id"
    api-token: "your-fastly-api-token"
    key-prefix: "rd-cache:"
    default-ttl: 3600 # 1 hour
    auto-purge: true
    purge-on-evict: true

  # Global edge cache settings
  rate-limit:
    requests-per-second: 10
    burst-size: 20
    window-size: 60 # seconds

  circuit-breaker:
    failure-threshold: 5
    recovery-timeout: 60 # seconds
    half-open-max-calls: 3

  batching:
    batch-size: 100
    batch-timeout: 5 # seconds
    max-concurrency: 10

  monitoring:
    enable-metrics: true
    enable-tracing: true
    log-level: "INFO"
```

### Configuration Properties Reference

#### Cloudflare Properties

| Property                                       | Default       | Description                                  |
| ---------------------------------------------- | ------------- | -------------------------------------------- |
| `cacheflow.cloudflare.enabled`        | `false`       | Enable Cloudflare edge cache                 |
| `cacheflow.cloudflare.zone-id`        | `""`          | Cloudflare zone ID                           |
| `cacheflow.cloudflare.api-token`      | `""`          | Cloudflare API token                         |
| `cacheflow.cloudflare.key-prefix`     | `"rd-cache:"` | Key prefix for cache entries                 |
| `cacheflow.cloudflare.auto-purge`     | `true`        | Automatically purge on cache eviction        |
| `cacheflow.cloudflare.purge-on-evict` | `true`        | Purge edge cache when local cache is evicted |

#### AWS CloudFront Properties

| Property                                             | Default       | Description                                  |
| ---------------------------------------------------- | ------------- | -------------------------------------------- |
| `cacheflow.aws-cloud-front.enabled`         | `false`       | Enable AWS CloudFront edge cache             |
| `cacheflow.aws-cloud-front.distribution-id` | `""`          | CloudFront distribution ID                   |
| `cacheflow.aws-cloud-front.key-prefix`      | `"rd-cache:"` | Key prefix for cache entries                 |
| `cacheflow.aws-cloud-front.auto-purge`      | `true`        | Automatically purge on cache eviction        |
| `cacheflow.aws-cloud-front.purge-on-evict`  | `true`        | Purge edge cache when local cache is evicted |

#### Fastly Properties

| Property                                   | Default       | Description                                  |
| ------------------------------------------ | ------------- | -------------------------------------------- |
| `cacheflow.fastly.enabled`        | `false`       | Enable Fastly edge cache                     |
| `cacheflow.fastly.service-id`     | `""`          | Fastly service ID                            |
| `cacheflow.fastly.api-token`      | `""`          | Fastly API token                             |
| `cacheflow.fastly.key-prefix`     | `"rd-cache:"` | Key prefix for cache entries                 |
| `cacheflow.fastly.auto-purge`     | `true`        | Automatically purge on cache eviction        |
| `cacheflow.fastly.purge-on-evict` | `true`        | Purge edge cache when local cache is evicted |

#### Global Edge Cache Properties

| Property                                                 | Default                    | Description                                 |
| -------------------------------------------------------- | -------------------------- | ------------------------------------------- |
| `cacheflow.base-url`                            | `"https://yourdomain.com"` | Base URL for edge cache operations          |
| `cacheflow.rate-limit.requests-per-second`      | `10`                       | Rate limit for edge cache operations        |
| `cacheflow.rate-limit.burst-size`               | `20`                       | Burst size for rate limiting                |
| `cacheflow.rate-limit.window-size`              | `60`                       | Rate limit window size in seconds           |
| `cacheflow.circuit-breaker.failure-threshold`   | `5`                        | Circuit breaker failure threshold           |
| `cacheflow.circuit-breaker.recovery-timeout`    | `60`                       | Circuit breaker recovery timeout in seconds |
| `cacheflow.circuit-breaker.half-open-max-calls` | `3`                        | Max calls in half-open state                |
| `cacheflow.batching.batch-size`                 | `100`                      | Batch size for bulk operations              |
| `cacheflow.batching.batch-timeout`              | `5`                        | Batch timeout in seconds                    |
| `cacheflow.batching.max-concurrency`            | `10`                       | Max concurrent operations                   |
| `cacheflow.monitoring.enable-metrics`           | `true`                     | Enable metrics collection                   |
| `cacheflow.monitoring.enable-tracing`           | `true`                     | Enable tracing                              |
| `cacheflow.monitoring.log-level`                | `"INFO"`                   | Log level for edge cache operations         |

## Usage Patterns

### Basic Caching with Automatic Edge Cache Purging

```kotlin
@Service
class UserService {

    @CacheFlow(key = "user-#{#id}", ttl = "1800")
    suspend fun getUserById(id: Long): User {
        return userRepository.findById(id)
    }

    @CacheFlowEvict(key = "user-#{#user.id}")
    suspend fun updateUser(user: User): User {
        val updatedUser = userRepository.save(user)
        // Edge cache will be automatically purged
        return updatedUser
    }
}
```

### Tag-Based Cache Eviction

```kotlin
@Service
class UserService {

    @CacheFlowEvict(tags = ["users", "user-#{#user.id}"])
    suspend fun updateUser(user: User): User {
        val updatedUser = userRepository.save(user)
        // All users with "users" tag will be purged from edge cache
        return updatedUser
    }

    @CacheFlowEvict(tags = ["users"])
    suspend fun updateAllUsers(users: List<User>): List<User> {
        val updatedUsers = userRepository.saveAll(users)
        // All users with "users" tag will be purged from edge cache
        return updatedUsers
    }
}
```

### Conditional Caching

```kotlin
@Service
class UserService {

    @CacheFlow(
        key = "user-#{#id}",
        condition = "#id > 0",
        unless = "#result == null"
    )
    suspend fun getUserByIdConditional(id: Long): User? {
        if (id <= 0) return null
        return userRepository.findById(id)
    }
}
```

### Manual Edge Cache Operations

```kotlin
@Service
class CacheManagementService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    suspend fun purgeUserFromEdgeCache(userId: Long) {
        val results = edgeCacheService.purgeUrl("/api/users/$userId").toList()
        results.forEach { result ->
            if (result.success) {
                logger.info("Successfully purged user $userId from ${result.provider}")
            } else {
                logger.error("Failed to purge user $userId from ${result.provider}: ${result.error}")
            }
        }
    }

    suspend fun purgeUsersFromEdgeCache(userIds: List<Long>) {
        val urls = userIds.map { "/api/users/$it" }
        val results = edgeCacheService.purgeUrls(urls).toList()
        // Process results...
    }

    suspend fun purgeByTag(tag: String) {
        val results = edgeCacheService.purgeByTag(tag).toList()
        // Process results...
    }

    suspend fun purgeAllFromEdgeCache() {
        val results = edgeCacheService.purgeAll().toList()
        // Process results...
    }
}
```

### Cache Key Operations

```kotlin
@Service
class CacheKeyService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    suspend fun purgeCacheKey(cacheKey: String) {
        val results = edgeCacheService.purgeCacheKey("https://api.example.com", cacheKey).toList()
        results.forEach { result ->
            logger.info("Purged cache key '$cacheKey': ${result.success}")
        }
    }

    suspend fun purgeCacheKeys(cacheKeys: List<String>) {
        val results = edgeCacheService.purgeCacheKeys("https://api.example.com", cacheKeys).toList()
        val successCount = results.count { it.success }
        logger.info("Purged $successCount/${cacheKeys.size} cache keys")
    }
}
```

## Advanced Features

### Rate Limiting

The system includes built-in rate limiting to prevent overwhelming edge cache APIs:

```kotlin
@Service
class RateLimitedService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    suspend fun safePurgeUrl(url: String) {
        try {
            val results = edgeCacheService.purgeUrl(url).toList()
            // Process results...
        } catch (e: RateLimitExceededException) {
            logger.warn("Rate limit exceeded, implementing backoff")
            // Implement exponential backoff
            delay(1000)
            safePurgeUrl(url) // Retry
        }
    }
}
```

### Circuit Breaker Pattern

Automatic circuit breaking prevents cascading failures:

```kotlin
@Service
class ResilientService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    suspend fun purgeWithFallback(url: String) {
        try {
            val results = edgeCacheService.purgeUrl(url).toList()
            // Process results...
        } catch (e: CircuitBreakerOpenException) {
            logger.warn("Circuit breaker open, using fallback")
            // Implement fallback strategy
            fallbackPurge(url)
        }
    }

    private suspend fun fallbackPurge(url: String) {
        // Fallback implementation
    }
}
```

### Batch Operations

Efficient bulk operations with Flow-based processing:

```kotlin
@Service
class BatchService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    suspend fun purgeUsersInBatches(userIds: List<Long>) {
        val urls = userIds.map { "/api/users/$it" }
        val results = edgeCacheService.purgeUrls(urls).toList()

        val successCount = results.count { it.success }
        val totalCost = results.sumOf { it.cost?.totalCost ?: 0.0 }

        logger.info("Purged $successCount/${urls.size} users, Total cost: $${String.format("%.4f", totalCost)}")
    }
}
```

### Cost Tracking

Monitor and manage edge cache costs:

```kotlin
@Service
class CostAwareService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    suspend fun monitorCosts() {
        val metrics = edgeCacheService.getMetrics()
        val totalCost = metrics.getTotalCost()

        if (totalCost > MAX_DAILY_COST) {
            logger.error("Edge cache costs exceeded: $${String.format("%.2f", totalCost)}")
            // Send alert or implement cost-based circuit breaker
        }
    }
}
```

## Monitoring & Management

### Health Monitoring

```kotlin
@RestController
class EdgeCacheHealthController(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    @GetMapping("/health/edge-cache")
    suspend fun getHealthStatus(): Map<String, Any> {
        val healthStatus = edgeCacheService.getHealthStatus()
        val rateLimiterStatus = edgeCacheService.getRateLimiterStatus()
        val circuitBreakerStatus = edgeCacheService.getCircuitBreakerStatus()
        val metrics = edgeCacheService.getMetrics()

        return mapOf(
            "providers" to healthStatus,
            "rateLimiter" to mapOf(
                "availableTokens" to rateLimiterStatus.availableTokens,
                "timeUntilNextToken" to rateLimiterStatus.timeUntilNextToken.toString()
            ),
            "circuitBreaker" to mapOf(
                "state" to circuitBreakerStatus.state.name,
                "failureCount" to circuitBreakerStatus.failureCount
            ),
            "metrics" to mapOf(
                "totalOperations" to metrics.getTotalOperations(),
                "successfulOperations" to metrics.getSuccessfulOperations(),
                "failedOperations" to metrics.getFailedOperations(),
                "totalCost" to metrics.getTotalCost(),
                "averageLatency" to metrics.getAverageLatency().toString(),
                "successRate" to metrics.getSuccessRate()
            )
        )
    }

    @GetMapping("/stats/edge-cache")
    suspend fun getStatistics(): EdgeCacheStatistics {
        return edgeCacheService.getStatistics()
    }
}
```

### Management Endpoints

The system provides Actuator endpoints for management:

- `GET /actuator/edgecache` - Get health status and metrics
- `GET /actuator/edgecache/stats` - Get aggregated statistics
- `POST /actuator/edgecache/purge/{url}` - Purge specific URL
- `POST /actuator/edgecache/purge/tag/{tag}` - Purge by tag
- `POST /actuator/edgecache/purge/all` - Purge all cache entries
- `DELETE /actuator/edgecache/metrics` - Reset metrics

### Metrics Integration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,russiandollcache,edgecache
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: "cacheflow"
```

### Prometheus Alerts

```yaml
# prometheus-alerts.yml
groups:
  - name: edge-cache
    rules:
      - alert: EdgeCacheHighErrorRate
        expr: rate(russian_doll_cache_edge_operations_total{success="false"}[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High edge cache error rate"

      - alert: EdgeCacheCircuitBreakerOpen
        expr: russian_doll_cache_edge_circuit_breaker_state == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Edge cache circuit breaker is open"

      - alert: EdgeCacheHighCost
        expr: russian_doll_cache_edge_cost_total > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Edge cache costs are high"
```

## Best Practices

### 1. TTL Strategy

```yaml
# Recommended TTL hierarchy
cacheflow:
  default-ttl: 1800 # 30 minutes (application cache)
  redis:
    default-ttl: 3600 # 1 hour (Redis cache)
  cloudflare:
    default-ttl: 3600 # 1 hour (edge cache)
```

### 2. Rate Limiting

```yaml
# Conservative rate limits for production
cacheflow:
  rate-limit:
    requests-per-second: 5 # Start conservative
    burst-size: 10
    window-size: 60
```

### 3. Circuit Breaker

```yaml
# Aggressive circuit breaker for cost control
cacheflow:
  circuit-breaker:
    failure-threshold: 3
    recovery-timeout: 300 # 5 minutes
    half-open-max-calls: 2
```

### 4. Monitoring

```yaml
# Comprehensive monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,edgecache
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 5. Error Handling

```kotlin
@Service
class RobustCacheService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    suspend fun safePurgeUrl(url: String) {
        try {
            val results = edgeCacheService.purgeUrl(url).toList()

            results.forEach { result ->
                when {
                    result.success -> {
                        logger.info("Successfully purged $url from ${result.provider}")
                    }
                    result.error is RateLimitExceededException -> {
                        logger.warn("Rate limit exceeded for ${result.provider}, retrying later...")
                        // Implement retry logic
                    }
                    result.error is CircuitBreakerOpenException -> {
                        logger.warn("Circuit breaker open for ${result.provider}, skipping...")
                        // Implement fallback logic
                    }
                    else -> {
                        logger.error("Failed to purge $url from ${result.provider}: ${result.error}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Unexpected error during edge cache purge: ${e.message}", e)
        }
    }
}
```

## Troubleshooting

### Common Issues

1. **Edge Cache Not Purging**

   - Check if edge caching is enabled in configuration
   - Verify base URL is set correctly
   - Check API credentials and permissions

2. **Rate Limit Exceeded**

   - Reduce `requests-per-second` in configuration
   - Implement exponential backoff in your code
   - Use batching for bulk operations

3. **Circuit Breaker Open**

   - Check edge cache provider health
   - Verify API credentials and permissions
   - Increase `recovery-timeout` if needed

4. **High Costs**
   - Monitor `totalCost` in metrics
   - Implement cost-based circuit breakers
   - Use batching to reduce API calls

### Debug Configuration

```yaml
# Enable debug logging
logging:
  level:
    com.yourcompany.russiandollcache.edge: DEBUG

# Check health status
curl http://localhost:8080/actuator/edgecache

# Check metrics
curl http://localhost:8080/actuator/edgecache/stats
```

## Conclusion

The edge caching system provides a robust, scalable solution for managing edge cache invalidation across multiple providers. With built-in rate limiting, circuit breaking, and monitoring, it's production-ready for high-traffic applications.

For more advanced usage patterns and examples, see the [Generic Edge Caching Architecture](../GENERIC_EDGE_CACHING_ARCHITECTURE.md) document.
