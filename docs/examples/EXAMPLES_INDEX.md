# Examples Index

This directory contains comprehensive examples demonstrating all features of the CacheFlow Spring Boot Starter.

## 📁 Example Files

### Configuration Examples

- **[application-edge-cache-example.yml](application-edge-cache-example.yml)** - Complete configuration example with all providers

### Code Examples

- **[Basic Usage Example](../src/main/kotlin/com/yourcompany/russiandollcache/example/ExampleUsage.kt)** - Simple annotation usage
- **[Edge Cache Example Application](../src/main/kotlin/com/yourcompany/russiandollcache/example/EdgeCacheExampleApplication.kt)** - Basic edge cache integration
- **[Comprehensive Edge Cache Example](../src/main/kotlin/com/yourcompany/russiandollcache/example/ComprehensiveEdgeCacheExample.kt)** - Advanced features demonstration

## 🚀 Quick Start Examples

### 1. Basic Caching

```kotlin
@Service
class UserService {

    @CacheFlow(key = "#id", ttl = 1800)
    suspend fun getUserById(id: Long): User {
        return userRepository.findById(id)
    }

    @CacheFlowEvict(key = "#user.id")
    suspend fun updateUser(user: User): User {
        return userRepository.save(user)
    }
}
```

### 2. Edge Cache Integration

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

### 3. Tag-Based Eviction

```kotlin
@Service
class UserService {

    @CacheFlow(
        key = "user-#{#id}",
        tags = ["users", "user-#{#id}"]
    )
    suspend fun getUserById(id: Long): User {
        return userRepository.findById(id)
    }

    @CacheFlowEvict(tags = ["users"])
    suspend fun updateAllUsers(users: List<User>): List<User> {
        return userRepository.saveAll(users)
    }
}
```

### 4. Conditional Caching

```kotlin
@Service
class UserService {

    @CacheFlow(
        key = "user-#{#id}",
        condition = "#id > 0",
        unless = "#result == null"
    )
    suspend fun getUserById(id: Long): User? {
        if (id <= 0) return null
        return userRepository.findById(id)
    }
}
```

### 5. Manual Edge Cache Operations

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
            }
        }
    }

    suspend fun purgeByTag(tag: String) {
        val results = edgeCacheService.purgeByTag(tag).toList()
        // Process results...
    }
}
```

## 🔧 Configuration Examples

### Basic Configuration

```yaml
cacheflow:
  enabled: true
  storage: REDIS
  default-ttl: 1800
  redis:
    enabled: true
    key-prefix: "rd-cache:"
```

### Edge Cache Configuration

```yaml
cacheflow:
  enabled: true
  base-url: "https://yourdomain.com"

  cloudflare:
    enabled: true
    zone-id: "your-zone-id"
    api-token: "your-api-token"
    auto-purge: true
    purge-on-evict: true

  aws-cloud-front:
    enabled: false
    distribution-id: "your-distribution-id"

  fastly:
    enabled: false
    service-id: "your-service-id"
    api-token: "your-api-token"
```

### Advanced Configuration

```yaml
cacheflow:
  enabled: true
  base-url: "https://yourdomain.com"
  storage: REDIS
  default-ttl: 1800
  max-size: 10000

  redis:
    enabled: true
    key-prefix: "rd-cache:"
    database: 0
    timeout: 5000
    default-ttl: 1800

  cloudflare:
    enabled: true
    zone-id: "your-zone-id"
    api-token: "your-api-token"
    key-prefix: "rd-cache:"
    default-ttl: 3600
    auto-purge: true
    purge-on-evict: true

  rate-limit:
    requests-per-second: 10
    burst-size: 20
    window-size: 60

  circuit-breaker:
    failure-threshold: 5
    recovery-timeout: 60
    half-open-max-calls: 3

  batching:
    batch-size: 100
    batch-timeout: 5
    max-concurrency: 10

  monitoring:
    enable-metrics: true
    enable-tracing: true
    log-level: "INFO"
```

## 📊 Monitoring Examples

### Health Check Endpoint

```kotlin
@RestController
class CacheHealthController(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    @GetMapping("/health/cache")
    suspend fun getCacheHealth(): Map<String, Any> {
        val healthStatus = edgeCacheService.getHealthStatus()
        val metrics = edgeCacheService.getMetrics()

        return mapOf(
            "providers" to healthStatus,
            "metrics" to mapOf(
                "totalOperations" to metrics.getTotalOperations(),
                "successRate" to metrics.getSuccessRate(),
                "totalCost" to metrics.getTotalCost()
            )
        )
    }
}
```

### Prometheus Metrics

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

## 🧪 Testing Examples

### Unit Testing

```kotlin
@SpringBootTest
class UserServiceTest {

    @Autowired
    private lateinit var userService: UserService

    @Test
    fun `should cache user by id`() {
        val user = userService.getUserById(1L)
        val cachedUser = userService.getUserById(1L)

        assertThat(cachedUser).isEqualTo(user)
    }
}
```

### Integration Testing

```kotlin
@SpringBootTest
class EdgeCacheIntegrationTest {

    @Autowired
    private lateinit var edgeCacheService: EdgeCacheIntegrationService

    @Test
    fun `should purge edge cache on eviction`() {
        val results = edgeCacheService.purgeUrl("/api/users/1").toList()

        assertThat(results).isNotEmpty()
        assertThat(results.first().success).isTrue()
    }
}
```

## 🚨 Error Handling Examples

### Rate Limiting

```kotlin
@Service
class ResilientCacheService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    suspend fun safePurgeUrl(url: String) {
        try {
            val results = edgeCacheService.purgeUrl(url).toList()
            // Process results...
        } catch (e: RateLimitExceededException) {
            logger.warn("Rate limit exceeded, implementing backoff")
            delay(1000)
            safePurgeUrl(url) // Retry
        }
    }
}
```

### Circuit Breaker

```kotlin
@Service
class FaultTolerantCacheService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    suspend fun purgeWithFallback(url: String) {
        try {
            val results = edgeCacheService.purgeUrl(url).toList()
            // Process results...
        } catch (e: CircuitBreakerOpenException) {
            logger.warn("Circuit breaker open, using fallback")
            fallbackPurge(url)
        }
    }

    private suspend fun fallbackPurge(url: String) {
        // Fallback implementation
    }
}
```

## 📈 Performance Examples

### Batch Operations

```kotlin
@Service
class BatchCacheService(
    private val edgeCacheService: EdgeCacheIntegrationService
) {

    suspend fun purgeUsersInBatches(userIds: List<Long>) {
        val urls = userIds.map { "/api/users/$it" }
        val results = edgeCacheService.purgeUrls(urls).toList()

        val successCount = results.count { it.success }
        logger.info("Purged $successCount/${urls.size} users")
    }
}
```

### Cost Monitoring

```kotlin
@Service
class CostAwareCacheService(
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

## 🔗 Related Documentation

- **[Edge Cache Usage Guide](../usage/EDGE_CACHE_USAGE_GUIDE.md)** - Complete usage instructions
- **[Features Reference](../usage/FEATURES_REFERENCE.md)** - Comprehensive feature reference
- **[Testing Guide](../testing/EDGE_CACHE_TESTING_GUIDE.md)** - Testing strategies
- **[Troubleshooting Guide](../troubleshooting/EDGE_CACHE_TROUBLESHOOTING.md)** - Common issues and solutions

## 💡 Best Practices

1. **Start Simple**: Begin with basic caching and gradually add edge cache features
2. **Monitor Costs**: Set up cost monitoring for edge cache operations
3. **Handle Errors**: Implement proper error handling and fallback strategies
4. **Test Thoroughly**: Use both unit and integration tests
5. **Monitor Performance**: Set up comprehensive monitoring and alerting

## 🆘 Getting Help

If you need help with examples or have questions:

1. Check the [Troubleshooting Guide](../troubleshooting/EDGE_CACHE_TROUBLESHOOTING.md)
2. Review the [Features Reference](../usage/FEATURES_REFERENCE.md)
3. Look at the comprehensive examples in the source code
4. Check the [Edge Cache Usage Guide](../usage/EDGE_CACHE_USAGE_GUIDE.md) for detailed instructions
