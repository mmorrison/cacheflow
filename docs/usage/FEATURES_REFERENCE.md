# Features Reference

This comprehensive reference covers all features available in the CacheFlow Spring Boot Starter.

## Table of Contents

- [Core Caching Features](#core-caching-features)
- [Edge Caching Features](#edge-caching-features)
- [Storage Implementations](#storage-implementations)
- [Annotation Reference](#annotation-reference)
- [Management Endpoints](#management-endpoints)
- [Metrics & Monitoring](#metrics--monitoring)
- [Configuration Reference](#configuration-reference)

## Core Caching Features

### Multi-Level Caching

The CacheFlow implements a hierarchical caching strategy:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Edge Cache    │    │   Redis Cache   │    │  Local Cache    │
│  (Multi-Provider)│    │     (L2)        │    │     (L1)        │
│      (L3)       │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
    TTL: 1 hour         TTL: 30 minutes        TTL: 5 minutes
```

### Storage Types

#### 1. In-Memory Storage (Default)

- **Type**: `IN_MEMORY`
- **Description**: Local JVM memory cache
- **Use Case**: Single-instance applications, development
- **Features**: Built-in statistics, tag support

```yaml
cacheflow:
  storage: IN_MEMORY
```

#### 2. Redis Storage

- **Type**: `REDIS`
- **Description**: Distributed cache using Redis
- **Use Case**: Multi-instance applications, production
- **Features**: Clustering, persistence, pub/sub

```yaml
cacheflow:
  storage: REDIS
  redis:
    enabled: true
    key-prefix: "rd-cache:"
    database: 0
    timeout: 5000
    default-ttl: 1800
```

#### 3. Caffeine Storage

- **Type**: `CAFFEINE`
- **Description**: High-performance local cache
- **Use Case**: High-throughput applications
- **Features**: Advanced eviction policies, statistics

```yaml
cacheflow:
  storage: CAFFEINE
```

#### 4. Cloudflare Storage

- **Type**: `CLOUDFLARE`
- **Description**: Edge cache using Cloudflare API
- **Use Case**: Global content distribution
- **Features**: Edge purging, global distribution

```yaml
cacheflow:
  storage: CLOUDFLARE
  cloudflare:
    enabled: true
    zone-id: "your-zone-id"
    api-token: "your-api-token"
```

### Cache Key Generation

#### Default Key Generator

- **Bean Name**: `defaultKeyGenerator`
- **Features**: SpEL support, parameter-based keys
- **Customization**: Implement `CacheKeyGenerator` interface

```kotlin
@Component
class CustomKeyGenerator : CacheKeyGenerator {
    override fun generateKey(method: Method, params: Array<Any?>): String {
        return "custom-${method.name}-${params.joinToString("-")}"
    }
}
```

#### SpEL Key Expressions

```kotlin
// Simple parameter reference
@CacheFlow(key = "#id")
fun getUserById(id: Long): User

// Method name and parameters
@CacheFlow(key = "#method.name + '-' + #id")
fun getUserById(id: Long): User

// Complex expression
@CacheFlow(key = "user-#{#user.id}-#{#user.version}")
fun updateUser(user: User): User

// Conditional key
@CacheFlow(key = "#id > 0 ? 'user-' + #id : 'invalid'")
fun getUserById(id: Long): User?
```

## Edge Caching Features

### Multi-Provider Support

#### Cloudflare Provider

- **Provider**: `cloudflare`
- **API**: Cloudflare Cache API
- **Features**: Zone-based purging, tag support, analytics

```yaml
cacheflow:
  cloudflare:
    enabled: true
    zone-id: "your-zone-id"
    api-token: "your-api-token"
    key-prefix: "rd-cache:"
    default-ttl: 3600
    auto-purge: true
    purge-on-evict: true
```

#### AWS CloudFront Provider

- **Provider**: `aws-cloudfront`
- **API**: AWS CloudFront API
- **Features**: Distribution invalidation, path patterns

```yaml
cacheflow:
  aws-cloud-front:
    enabled: true
    distribution-id: "your-distribution-id"
    key-prefix: "rd-cache:"
    default-ttl: 3600
    auto-purge: true
    purge-on-evict: true
```

#### Fastly Provider

- **Provider**: `fastly`
- **API**: Fastly API
- **Features**: Service-based purging, soft purging, tag support

```yaml
cacheflow:
  fastly:
    enabled: true
    service-id: "your-service-id"
    api-token: "your-api-token"
    key-prefix: "rd-cache:"
    default-ttl: 3600
    auto-purge: true
    purge-on-evict: true
```

### Rate Limiting

Token bucket algorithm with configurable limits:

```yaml
cacheflow:
  rate-limit:
    requests-per-second: 10
    burst-size: 20
    window-size: 60 # seconds
```

### Circuit Breaker

Fault tolerance with automatic recovery:

```yaml
cacheflow:
  circuit-breaker:
    failure-threshold: 5
    recovery-timeout: 60 # seconds
    half-open-max-calls: 3
```

### Batching

Efficient bulk operations:

```yaml
cacheflow:
  batching:
    batch-size: 100
    batch-timeout: 5 # seconds
    max-concurrency: 10
```

## Annotation Reference

### @CacheFlow

Caches method results with configurable options.

#### Parameters

| Parameter      | Type          | Default                 | Description                                         |
| -------------- | ------------- | ----------------------- | --------------------------------------------------- |
| `key`          | String        | `""`                    | Cache key expression (SpEL supported)               |
| `keyGenerator` | String        | `"defaultKeyGenerator"` | Key generator bean name                             |
| `ttl`          | Long          | `-1`                    | Time to live in seconds                             |
| `dependsOn`    | Array<String> | `[]`                    | Parameter names this cache depends on               |
| `tags`         | Array<String> | `[]`                    | Tags for group-based eviction                       |
| `condition`    | String        | `""`                    | Condition to determine if caching should be applied |
| `unless`       | String        | `""`                    | Condition to determine if caching should be skipped |
| `sync`         | Boolean       | `false`                 | Whether to use synchronous caching                  |

#### Examples

```kotlin
// Basic caching
@CacheFlow(key = "#id", ttl = 1800)
fun getUserById(id: Long): User

// Conditional caching
@CacheFlow(
    key = "user-#{#id}",
    condition = "#id > 0",
    unless = "#result == null"
)
fun getUserById(id: Long): User?

// Tagged caching
@CacheFlow(
    key = "user-#{#id}",
    tags = ["users", "user-#{#id}"]
)
fun getUserById(id: Long): User

// Dependency-based caching
@CacheFlow(
    key = "user-#{#id}",
    dependsOn = ["user"],
    ttl = 1800
)
fun getUserProfile(user: User): String

// Synchronous caching
@CacheFlow(key = "#id", sync = true)
fun getUserById(id: Long): User
```

### @CacheFlowEvict

Evicts entries from cache with various strategies.

#### Parameters

| Parameter          | Type          | Default | Description                                          |
| ------------------ | ------------- | ------- | ---------------------------------------------------- |
| `key`              | String        | `""`    | Cache key expression (SpEL supported)                |
| `tags`             | Array<String> | `[]`    | Tags for group-based eviction                        |
| `allEntries`       | Boolean       | `false` | Whether to evict all entries                         |
| `beforeInvocation` | Boolean       | `false` | Whether to evict before method invocation            |
| `condition`        | String        | `""`    | Condition to determine if eviction should be applied |

#### Examples

```kotlin
// Evict specific key
@CacheFlowEvict(key = "#user.id")
fun updateUser(user: User): User

// Evict by tags
@CacheFlowEvict(tags = ["users"])
fun updateAllUsers(users: List<User>): List<User>

// Evict all entries
@CacheFlowEvict(allEntries = true)
fun clearAllCache(): Unit

// Evict before invocation
@CacheFlowEvict(key = "#user.id", beforeInvocation = true)
fun updateUser(user: User): User
```

### @CacheFlowd

Alternative name for `@CacheFlow` for compatibility.

### @CacheFlowEvict

Alternative name for `@CacheFlowEvict` for compatibility.

### @CacheEntity

Marks classes as cacheable entities with metadata.

#### Parameters

| Parameter      | Type   | Default       | Description                     |
| -------------- | ------ | ------------- | ------------------------------- |
| `keyPrefix`    | String | `""`          | Prefix for cache keys           |
| `versionField` | String | `"updatedAt"` | Field name for version tracking |

#### Example

```kotlin
@CacheEntity(keyPrefix = "user", versionField = "updatedAt")
data class User(
    val id: Long,
    val name: String,
    @CacheKey val userId: Long = id,
    @CacheVersion val updatedAt: Long = System.currentTimeMillis()
)
```

### @CacheKey

Marks properties as cache keys for automatic key generation.

### @CacheVersion

Marks properties as version fields for cache invalidation.

## Management Endpoints

### Local Cache Endpoints

#### GET /actuator/russiandollcache

Get cache information and statistics.

**Response:**

```json
{
  "size": 150,
  "type": "InMemoryCacheStorage",
  "keys": ["user-1", "user-2", "product-123"]
}
```

#### POST /actuator/russiandollcache

Put a value in the cache.

**Request Body:**

```json
{
  "key": "user-123",
  "value": { "id": 123, "name": "John Doe" },
  "ttl": 1800
}
```

#### DELETE /actuator/russiandollcache/{key}

Evict a specific cache entry.

#### DELETE /actuator/russiandollcache

Evict all cache entries.

#### POST /actuator/russiandollcache/pattern/{pattern}

Evict entries matching a pattern.

#### POST /actuator/russiandollcache/tags/{tags}

Evict entries by tags (comma-separated).

### Edge Cache Endpoints

#### GET /actuator/edgecache

Get edge cache health status and metrics.

**Response:**

```json
{
  "providers": {
    "cloudflare": true,
    "aws-cloudfront": false,
    "fastly": true
  },
  "rateLimiter": {
    "availableTokens": 15,
    "timeUntilNextToken": "PT0S"
  },
  "circuitBreaker": {
    "state": "CLOSED",
    "failureCount": 0
  },
  "metrics": {
    "totalOperations": 1250,
    "successfulOperations": 1200,
    "failedOperations": 50,
    "totalCost": 12.5,
    "averageLatency": "PT0.1S",
    "successRate": 0.96
  }
}
```

#### GET /actuator/edgecache/stats

Get aggregated edge cache statistics.

#### POST /actuator/edgecache/purge/{url}

Purge a specific URL from all edge cache providers.

#### POST /actuator/edgecache/purge/tag/{tag}

Purge entries by tag from all edge cache providers.

#### POST /actuator/edgecache/purge/all

Purge all entries from all edge cache providers.

#### DELETE /actuator/edgecache/metrics

Reset edge cache metrics.

## Metrics & Monitoring

### Local Cache Metrics

| Metric                          | Type    | Description               |
| ------------------------------- | ------- | ------------------------- |
| `russian.doll.cache.hits`       | Counter | Number of cache hits      |
| `russian.doll.cache.misses`     | Counter | Number of cache misses    |
| `russian.doll.cache.evictions`  | Counter | Number of cache evictions |
| `russian.doll.cache.operations` | Timer   | Cache operation duration  |
| `russian.doll.cache.size`       | Gauge   | Current cache size        |

### Edge Cache Metrics

| Metric                                          | Type    | Description                   |
| ----------------------------------------------- | ------- | ----------------------------- |
| `russian.doll.cache.edge.operations`            | Counter | Edge cache operations         |
| `russian.doll.cache.edge.cost`                  | Gauge   | Total edge cache costs        |
| `russian.doll.cache.edge.latency`               | Timer   | Edge cache operation latency  |
| `russian.doll.cache.edge.rate_limiter.tokens`   | Gauge   | Available rate limiter tokens |
| `russian.doll.cache.edge.circuit_breaker.state` | Gauge   | Circuit breaker state         |

### Prometheus Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,russiandollcache,edgecache
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: "cacheflow"
```

## Configuration Reference

### Global Configuration

| Property                         | Default                    | Description                        |
| -------------------------------- | -------------------------- | ---------------------------------- |
| `cacheflow.enabled`     | `true`                     | Enable CacheFlow          |
| `cacheflow.default-ttl` | `3600`                     | Default TTL in seconds             |
| `cacheflow.max-size`    | `10000`                    | Maximum cache size                 |
| `cacheflow.storage`     | `IN_MEMORY`                | Storage type                       |
| `cacheflow.base-url`    | `"https://yourdomain.com"` | Base URL for edge cache operations |

### Redis Configuration

| Property                               | Default       | Description              |
| -------------------------------------- | ------------- | ------------------------ |
| `cacheflow.redis.enabled`     | `false`       | Enable Redis storage     |
| `cacheflow.redis.key-prefix`  | `"rd-cache:"` | Key prefix for Redis     |
| `cacheflow.redis.database`    | `0`           | Redis database number    |
| `cacheflow.redis.timeout`     | `5000`        | Connection timeout in ms |
| `cacheflow.redis.default-ttl` | `3600`        | Default TTL for Redis    |

### Edge Cache Configuration

#### Cloudflare

| Property                                       | Default       | Description                                  |
| ---------------------------------------------- | ------------- | -------------------------------------------- |
| `cacheflow.cloudflare.enabled`        | `false`       | Enable Cloudflare edge cache                 |
| `cacheflow.cloudflare.zone-id`        | `""`          | Cloudflare zone ID                           |
| `cacheflow.cloudflare.api-token`      | `""`          | Cloudflare API token                         |
| `cacheflow.cloudflare.key-prefix`     | `"rd-cache:"` | Key prefix for cache entries                 |
| `cacheflow.cloudflare.default-ttl`    | `3600`        | Default TTL in seconds                       |
| `cacheflow.cloudflare.auto-purge`     | `true`        | Automatically purge on cache eviction        |
| `cacheflow.cloudflare.purge-on-evict` | `true`        | Purge edge cache when local cache is evicted |

#### AWS CloudFront

| Property                                             | Default       | Description                                  |
| ---------------------------------------------------- | ------------- | -------------------------------------------- |
| `cacheflow.aws-cloud-front.enabled`         | `false`       | Enable AWS CloudFront edge cache             |
| `cacheflow.aws-cloud-front.distribution-id` | `""`          | CloudFront distribution ID                   |
| `cacheflow.aws-cloud-front.key-prefix`      | `"rd-cache:"` | Key prefix for cache entries                 |
| `cacheflow.aws-cloud-front.default-ttl`     | `3600`        | Default TTL in seconds                       |
| `cacheflow.aws-cloud-front.auto-purge`      | `true`        | Automatically purge on cache eviction        |
| `cacheflow.aws-cloud-front.purge-on-evict`  | `true`        | Purge edge cache when local cache is evicted |

#### Fastly

| Property                                   | Default       | Description                                  |
| ------------------------------------------ | ------------- | -------------------------------------------- |
| `cacheflow.fastly.enabled`        | `false`       | Enable Fastly edge cache                     |
| `cacheflow.fastly.service-id`     | `""`          | Fastly service ID                            |
| `cacheflow.fastly.api-token`      | `""`          | Fastly API token                             |
| `cacheflow.fastly.key-prefix`     | `"rd-cache:"` | Key prefix for cache entries                 |
| `cacheflow.fastly.default-ttl`    | `3600`        | Default TTL in seconds                       |
| `cacheflow.fastly.auto-purge`     | `true`        | Automatically purge on cache eviction        |
| `cacheflow.fastly.purge-on-evict` | `true`        | Purge edge cache when local cache is evicted |

### Rate Limiting Configuration

| Property                                            | Default | Description                          |
| --------------------------------------------------- | ------- | ------------------------------------ |
| `cacheflow.rate-limit.requests-per-second` | `10`    | Rate limit for edge cache operations |
| `cacheflow.rate-limit.burst-size`          | `20`    | Burst size for rate limiting         |
| `cacheflow.rate-limit.window-size`         | `60`    | Rate limit window size in seconds    |

### Circuit Breaker Configuration

| Property                                                 | Default | Description                                 |
| -------------------------------------------------------- | ------- | ------------------------------------------- |
| `cacheflow.circuit-breaker.failure-threshold`   | `5`     | Circuit breaker failure threshold           |
| `cacheflow.circuit-breaker.recovery-timeout`    | `60`    | Circuit breaker recovery timeout in seconds |
| `cacheflow.circuit-breaker.half-open-max-calls` | `3`     | Max calls in half-open state                |

### Batching Configuration

| Property                                      | Default | Description                    |
| --------------------------------------------- | ------- | ------------------------------ |
| `cacheflow.batching.batch-size`      | `100`   | Batch size for bulk operations |
| `cacheflow.batching.batch-timeout`   | `5`     | Batch timeout in seconds       |
| `cacheflow.batching.max-concurrency` | `10`    | Max concurrent operations      |

### Monitoring Configuration

| Property                                       | Default  | Description                         |
| ---------------------------------------------- | -------- | ----------------------------------- |
| `cacheflow.monitoring.enable-metrics` | `true`   | Enable metrics collection           |
| `cacheflow.monitoring.enable-tracing` | `true`   | Enable tracing                      |
| `cacheflow.monitoring.log-level`      | `"INFO"` | Log level for edge cache operations |

## SpEL Expression Reference

### Available Variables

| Variable             | Type   | Description             |
| -------------------- | ------ | ----------------------- |
| `#method`            | Method | The method being called |
| `#method.name`       | String | Method name             |
| `#method.returnType` | Class  | Method return type      |
| `#args`              | Array  | Method arguments        |
| `#result`            | Object | Method return value     |
| `#paramName`         | Object | Named parameter value   |

### Common Expressions

```kotlin
// Simple parameter reference
@CacheFlow(key = "#id")

// Method name with parameters
@CacheFlow(key = "#method.name + '-' + #id")

// Conditional expressions
@CacheFlow(
    key = "#id > 0 ? 'user-' + #id : 'invalid'",
    condition = "#id > 0"
)

// Complex object properties
@CacheFlow(key = "user-#{#user.id}-#{#user.version}")

// Array/List operations
@CacheFlow(key = "users-#{#userIds.size()}-#{#userIds.hashCode()}")

// String operations
@CacheFlow(key = "#name.toLowerCase() + '-' + #id")
```

## Best Practices

### 1. Cache Key Design

- Use descriptive, hierarchical keys
- Include version information for cache invalidation
- Avoid special characters that might cause issues

### 2. TTL Strategy

- Set appropriate TTLs for each cache level
- Consider data freshness requirements
- Use shorter TTLs for frequently changing data

### 3. Tag Usage

- Use tags for group-based eviction
- Keep tag names consistent and descriptive
- Avoid too many tags per entry

### 4. Error Handling

- Implement proper fallback strategies
- Monitor cache hit/miss ratios
- Handle edge cache failures gracefully

### 5. Performance

- Use appropriate storage types for your use case
- Monitor memory usage and cache size
- Implement proper eviction policies

This reference covers all available features in the CacheFlow Spring Boot Starter. For implementation examples and advanced usage patterns, see the [Edge Cache Usage Guide](EDGE_CACHE_USAGE_GUIDE.md).
