# Edge Cache Overview

This document provides a comprehensive overview of the edge caching functionality in the CacheFlow Spring Boot Starter.

## 🎯 What is Edge Caching?

Edge caching extends the CacheFlow pattern to include content delivery networks (CDNs) and edge locations, creating a three-tier caching hierarchy:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Edge Cache    │    │   Redis Cache   │    │  Local Cache    │
│  (Multi-Provider)│    │     (L2)        │    │     (L1)        │
│      (L3)       │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
    TTL: 1 hour         TTL: 30 minutes        TTL: 5 minutes
```

## 🚀 Key Features

### Multi-Provider Support

- **Cloudflare** - Global CDN with powerful caching capabilities
- **AWS CloudFront** - Amazon's content delivery network
- **Fastly** - High-performance edge cloud platform
- **Extensible** - Easy to add new providers

### Production-Ready Features

- **Rate Limiting** - Token bucket algorithm with configurable limits
- **Circuit Breaking** - Fault tolerance with automatic recovery
- **Cost Tracking** - Real-time cost monitoring and management
- **Health Monitoring** - Comprehensive health checks and metrics
- **Reactive Programming** - Full Kotlin Flow support for async operations

### Developer Experience

- **Zero Configuration** - Works out of the box with sensible defaults
- **Annotation-Based** - Simple `@CacheFlow` and `@CacheFlowEvict` annotations
- **Management Endpoints** - Built-in Actuator endpoints for monitoring
- **Comprehensive Testing** - Full test suite with mocking support

## 📚 Documentation Structure

### Core Documentation

- **[README.md](README.md)** - Main project documentation with quick start
- **[Edge Cache Usage Guide](EDGE_CACHE_USAGE_GUIDE.md)** - Complete usage instructions and configuration
- **[Generic Edge Caching Architecture](GENERIC_EDGE_CACHING_ARCHITECTURE.md)** - Technical architecture details

### Advanced Topics

- **[Edge Cache Testing Guide](EDGE_CACHE_TESTING_GUIDE.md)** - Comprehensive testing strategies
- **[Edge Cache Troubleshooting](EDGE_CACHE_TROUBLESHOOTING.md)** - Common issues and solutions
- **[Edge Caching Guide](EDGE_CACHING_GUIDE.md)** - Original edge caching concepts

### Examples

- **[Edge Cache Example Application](src/main/kotlin/com/yourcompany/russiandollcache/example/EdgeCacheExampleApplication.kt)** - Basic usage example
- **[Comprehensive Edge Cache Example](src/main/kotlin/com/yourcompany/russiandollcache/example/ComprehensiveEdgeCacheExample.kt)** - Advanced features demonstration
- **[Example Configuration](src/main/resources/application-edge-cache-example.yml)** - Complete configuration example

## 🏗️ Architecture Components

### Core Interfaces

- **`EdgeCacheProvider`** - Generic interface for all edge cache providers
- **`EdgeCacheManager`** - Orchestrates multiple providers with rate limiting and circuit breaking
- **`EdgeCacheIntegrationService`** - High-level service for easy integration

### Provider Implementations

- **`CloudflareEdgeCacheProvider`** - Cloudflare API integration
- **`AwsCloudFrontEdgeCacheProvider`** - AWS CloudFront integration
- **`FastlyEdgeCacheProvider`** - Fastly API integration

### Supporting Components

- **`EdgeCacheRateLimiter`** - Token bucket rate limiting
- **`EdgeCacheCircuitBreaker`** - Circuit breaker pattern implementation
- **`EdgeCacheBatcher`** - Batch processing for bulk operations
- **`EdgeCacheMetrics`** - Comprehensive metrics collection

## 🔧 Quick Start

### 1. Add Dependencies

```kotlin
dependencies {
    implementation("com.yourcompany:cacheflow-spring-boot-starter:0.1.0-alpha")
    implementation("org.springframework:spring-webflux")
    implementation("software.amazon.awssdk:cloudfront")
}
```

### 2. Configure Edge Cache

```yaml
cacheflow:
  base-url: "https://yourdomain.com"
  cloudflare:
    enabled: true
    zone-id: "your-zone-id"
    api-token: "your-api-token"
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
        // Automatically purges from all enabled edge cache providers
        return updatedUser
    }
}
```

## 📊 Monitoring & Management

### Health Endpoints

- `GET /actuator/edgecache` - Health status and metrics
- `GET /actuator/edgecache/stats` - Detailed statistics
- `POST /actuator/edgecache/purge/{url}` - Manual URL purging
- `POST /actuator/edgecache/purge/tag/{tag}` - Tag-based purging
- `POST /actuator/edgecache/purge/all` - Purge all cache

### Metrics

- **Operations**: Total, successful, failed operations
- **Costs**: Real-time cost tracking per provider
- **Latency**: Average operation latency
- **Rate Limiting**: Available tokens and wait times
- **Circuit Breaker**: State and failure counts

## 🧪 Testing

### Unit Testing

```kotlin
@ExtendWith(MockitoExtension::class)
class EdgeCacheServiceTest {
    @Mock private lateinit var edgeCacheManager: EdgeCacheManager
    @InjectMocks private lateinit var edgeCacheService: EdgeCacheIntegrationService

    @Test
    fun `should purge URL successfully`() = runTest {
        // Test implementation
    }
}
```

### Integration Testing

```kotlin
@SpringBootTest
@Testcontainers
class EdgeCacheIntegrationTest {
    @Container
    static val redis = GenericContainer("redis:7-alpine")

    @Test
    fun `should integrate with edge cache providers`() = runTest {
        // Integration test implementation
    }
}
```

## 🚨 Troubleshooting

### Common Issues

1. **Edge Cache Not Purging** - Check configuration and base URL
2. **Rate Limiting Issues** - Adjust rate limits or implement backoff
3. **Circuit Breaker Open** - Check provider health and credentials
4. **High Costs** - Monitor costs and optimize purge patterns
5. **Authentication Issues** - Verify API tokens and permissions

### Debug Tools

- Health check endpoints
- Prometheus metrics
- Debug logging
- Management endpoints

## 🎯 Best Practices

### Configuration

- Start with conservative rate limits
- Use environment variables for sensitive data
- Enable monitoring and alerting
- Test in staging before production

### Performance

- Use batching for bulk operations
- Implement proper error handling
- Monitor costs and optimize patterns
- Use async operations where possible

### Reliability

- Implement circuit breakers
- Use fallback strategies
- Monitor health continuously
- Test failure scenarios

## 🔮 Future Enhancements

### Planned Features

- **Additional Providers** - Azure CDN, Google Cloud CDN
- **Advanced Analytics** - Cache hit rate analysis
- **Cost Optimization** - Intelligent purge strategies
- **Multi-Region Support** - Geographic distribution

### Community Contributions

- New edge cache providers
- Performance optimizations
- Additional monitoring features
- Documentation improvements

## 📞 Support

### Getting Help

1. Check the [Troubleshooting Guide](EDGE_CACHE_TROUBLESHOOTING.md)
2. Review the [Usage Guide](EDGE_CACHE_USAGE_GUIDE.md)
3. Examine the [Test Examples](EDGE_CACHE_TESTING_GUIDE.md)
4. Create an issue in the project repository

### Contributing

- Fork the repository
- Create a feature branch
- Add tests for new functionality
- Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Ready to get started?** Check out the [Edge Cache Usage Guide](EDGE_CACHE_USAGE_GUIDE.md) for detailed instructions and examples!
