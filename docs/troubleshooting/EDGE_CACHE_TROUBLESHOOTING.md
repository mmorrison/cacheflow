# Edge Cache Troubleshooting Guide

This guide helps you diagnose and resolve common issues with the edge caching functionality.

## Common Issues

### 1. Edge Cache Not Purging

**Symptoms:**

- Cache eviction works locally but edge cache still serves old content
- No edge cache purge operations in logs

**Diagnosis:**

```bash
# Check if edge caching is enabled
curl http://localhost:8080/actuator/edgecache

# Check configuration
curl http://localhost:8080/actuator/configprops | grep -A 20 "cacheflow"
```

**Solutions:**

1. **Verify Configuration:**

   ```yaml
   cacheflow:
     base-url: "https://yourdomain.com" # Must be set
     cloudflare:
       enabled: true # Must be enabled
       zone-id: "your-zone-id" # Must be valid
       api-token: "your-api-token" # Must be valid
   ```

2. **Check Base URL:**

   ```kotlin
   // Ensure base URL is accessible
   @Value("\${cacheflow.base-url}")
   private lateinit var baseUrl: String

   @PostConstruct
   fun validateBaseUrl() {
       require(baseUrl.startsWith("http")) { "Base URL must start with http" }
   }
   ```

3. **Enable Debug Logging:**
   ```yaml
   logging:
     level:
       com.yourcompany.russiandollcache.edge: DEBUG
   ```

### 2. Rate Limiting Issues

**Symptoms:**

- `RateLimitExceededException` in logs
- Edge cache operations failing intermittently
- High latency for cache operations

**Diagnosis:**

```bash
# Check rate limiter status
curl http://localhost:8080/actuator/edgecache | jq '.rateLimiter'
```

**Solutions:**

1. **Adjust Rate Limits:**

   ```yaml
   cacheflow:
     rate-limit:
       requests-per-second: 5 # Reduce if hitting limits
       burst-size: 10
       window-size: 60
   ```

2. **Implement Exponential Backoff:**

   ```kotlin
   @Retryable(
       value = [RateLimitExceededException::class],
       maxAttempts = 3,
       backoff = Backoff(delay = 1000, multiplier = 2.0)
   )
   suspend fun purgeWithRetry(url: String) {
       edgeCacheService.purgeUrl(url)
   }
   ```

3. **Monitor Rate Limiter:**
   ```kotlin
   @Scheduled(fixedRate = 30000) // Every 30 seconds
   fun monitorRateLimiter() {
       val status = edgeCacheService.getRateLimiterStatus()
       if (status.availableTokens < 2) {
           logger.warn("Rate limiter running low: ${status.availableTokens} tokens")
       }
   }
   ```

### 3. Circuit Breaker Open

**Symptoms:**

- `CircuitBreakerOpenException` in logs
- All edge cache operations failing
- Service appears "down" but is actually healthy

**Diagnosis:**

```bash
# Check circuit breaker status
curl http://localhost:8080/actuator/edgecache | jq '.circuitBreaker'
```

**Solutions:**

1. **Check Provider Health:**

   ```bash
   # Test provider connectivity
   curl -H "Authorization: Bearer $API_TOKEN" \
        "https://api.cloudflare.com/client/v4/zones/$ZONE_ID/health"
   ```

2. **Adjust Circuit Breaker Settings:**

   ```yaml
   cacheflow:
     circuit-breaker:
       failure-threshold: 10 # Increase tolerance
       recovery-timeout: 300 # 5 minutes
       half-open-max-calls: 5
   ```

3. **Implement Fallback:**

   ```kotlin
   @CircuitBreaker(name = "edge-cache", fallbackMethod = "fallbackPurge")
   suspend fun purgeUrl(url: String): Flow<EdgeCacheResult> {
       return edgeCacheService.purgeUrl(url)
   }

   suspend fun fallbackPurge(url: String): Flow<EdgeCacheResult> {
       logger.warn("Edge cache unavailable, using fallback for $url")
       return flowOf(EdgeCacheResult.failure("fallback", EdgeCacheOperation.PURGE_URL,
           RuntimeException("Circuit breaker open")))
   }
   ```

### 4. High Costs

**Symptoms:**

- Unexpected charges from edge cache providers
- High `totalCost` in metrics
- Budget alerts

**Diagnosis:**

```bash
# Check current costs
curl http://localhost:8080/actuator/edgecache | jq '.metrics.totalCost'
```

**Solutions:**

1. **Implement Cost Monitoring:**

   ```kotlin
   @Scheduled(fixedRate = 300000) // Every 5 minutes
   fun monitorCosts() {
       val metrics = edgeCacheService.getMetrics()
       val totalCost = metrics.getTotalCost()

       if (totalCost > MAX_DAILY_COST) {
           logger.error("Edge cache costs exceeded: $${String.format("%.2f", totalCost)}")
           // Send alert
       }
   }
   ```

2. **Implement Cost-Based Circuit Breaker:**

   ```kotlin
   @Component
   class CostBasedCircuitBreaker {
       private var dailyCost = 0.0
       private var lastReset = LocalDate.now()

       fun shouldAllowOperation(cost: Double): Boolean {
           resetIfNewDay()
           return dailyCost + cost <= MAX_DAILY_COST
       }

       private fun resetIfNewDay() {
           if (lastReset != LocalDate.now()) {
               dailyCost = 0.0
               lastReset = LocalDate.now()
           }
       }
   }
   ```

3. **Optimize Purge Strategy:**
   ```kotlin
   // Batch purges to reduce API calls
   @CacheFlowEvict(tags = ["users"])
   suspend fun updateUsers(users: List<User>) {
       // Update all users
       userRepository.saveAll(users)

       // Single tag-based purge instead of individual purges
       edgeCacheService.purgeByTag("users")
   }
   ```

### 5. Authentication Issues

**Symptoms:**

- `401 Unauthorized` errors
- `403 Forbidden` errors
- Edge cache operations failing with auth errors

**Diagnosis:**

```bash
# Test API credentials
curl -H "Authorization: Bearer $API_TOKEN" \
     "https://api.cloudflare.com/client/v4/user/tokens/verify"
```

**Solutions:**

1. **Verify API Tokens:**

   ```yaml
   cacheflow:
     cloudflare:
       api-token: "${CLOUDFLARE_API_TOKEN:}" # Use environment variables
     fastly:
       api-token: "${FASTLY_API_TOKEN:}"
   ```

2. **Check Token Permissions:**

   - Cloudflare: Zone:Edit, Zone:Read
   - Fastly: Purge, Read
   - AWS CloudFront: cloudfront:CreateInvalidation

3. **Implement Token Rotation:**
   ```kotlin
   @Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
   fun rotateTokens() {
       // Implement token rotation logic
   }
   ```

### 6. Performance Issues

**Symptoms:**

- Slow edge cache operations
- High latency in metrics
- Timeout errors

**Diagnosis:**

```bash
# Check latency metrics
curl http://localhost:8080/actuator/edgecache | jq '.metrics.averageLatency'
```

**Solutions:**

1. **Optimize Batch Sizes:**

   ```yaml
   cacheflow:
     batching:
       batch-size: 50 # Reduce if operations are slow
       batch-timeout: 10 # Increase timeout
       max-concurrency: 5 # Reduce concurrency
   ```

2. **Implement Timeout Handling:**

   ```kotlin
   suspend fun purgeWithTimeout(url: String) {
       try {
           withTimeout(5000) { // 5 second timeout
               edgeCacheService.purgeUrl(url).toList()
           }
       } catch (e: TimeoutCancellationException) {
           logger.warn("Edge cache purge timed out for $url")
       }
   }
   ```

3. **Use Async Operations:**
   ```kotlin
   @Async
   fun purgeAsync(url: String) {
       runBlocking {
           edgeCacheService.purgeUrl(url)
       }
   }
   ```

## Debugging Tools

### 1. Health Check Endpoint

```bash
# Comprehensive health check
curl http://localhost:8080/actuator/edgecache | jq '.'

# Specific provider health
curl http://localhost:8080/actuator/edgecache | jq '.providers'

# Rate limiter status
curl http://localhost:8080/actuator/edgecache | jq '.rateLimiter'

# Circuit breaker status
curl http://localhost:8080/actuator/edgecache | jq '.circuitBreaker'
```

### 2. Metrics Monitoring

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep edge

# Custom metrics endpoint
curl http://localhost:8080/actuator/metrics/russian.doll.cache.edge.operations
```

### 3. Log Analysis

```bash
# Filter edge cache logs
grep "edge-cache" application.log | tail -100

# Monitor specific operations
grep "purgeUrl" application.log | grep ERROR

# Check rate limiting
grep "RateLimitExceeded" application.log
```

## Monitoring Setup

### 1. Prometheus Alerts

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

### 2. Grafana Dashboard

```json
{
  "dashboard": {
    "title": "Edge Cache Monitoring",
    "panels": [
      {
        "title": "Edge Cache Operations",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(russian_doll_cache_edge_operations_total[5m])",
            "legendFormat": "{{provider}} - {{operation}}"
          }
        ]
      },
      {
        "title": "Edge Cache Costs",
        "type": "singlestat",
        "targets": [
          {
            "expr": "russian_doll_cache_edge_cost_total",
            "legendFormat": "Total Cost ($)"
          }
        ]
      }
    ]
  }
}
```

## Best Practices

### 1. Proactive Monitoring

- Set up alerts for all critical metrics
- Monitor costs daily
- Track success rates and latency trends

### 2. Graceful Degradation

- Always have fallback strategies
- Don't let edge cache failures break your application
- Implement retry logic with exponential backoff

### 3. Cost Management

- Set daily/monthly cost limits
- Use batching to reduce API calls
- Monitor and optimize purge patterns

### 4. Testing

- Test failure scenarios regularly
- Use chaos engineering to test resilience
- Monitor performance under load

## Getting Help

If you're still experiencing issues:

1. **Check the logs** for specific error messages
2. **Verify configuration** using the health endpoints
3. **Test connectivity** to edge cache providers
4. **Review metrics** for patterns and trends
5. **Consult documentation** for your specific edge cache provider

For additional support, please refer to the [Edge Cache Usage Guide](EDGE_CACHE_USAGE_GUIDE.md) or create an issue in the project repository.
