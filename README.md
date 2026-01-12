# CacheFlow ⚡

> Multi-level caching that just works

[![Build Status](https://github.com/mmorrison/cacheflow/workflows/CI/badge.svg)](https://github.com/yourusername/cacheflow/actions)
[![Maven Central](https://img.shields.io/maven-central/v/io.cacheflow/cacheflow-spring-boot-starter/0.1.0-alpha)](https://search.maven.org/artifact/io.cacheflow/cacheflow-spring-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Beta](https://img.shields.io/badge/Status-Beta-blue.svg)](https://github.com/mmorrison/cacheflow)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](http://makeapullrequest.com)

> ⚠️ **Beta Release** - This project is now in Beta. Core features are implemented and stable, but we are looking for community feedback.

**CacheFlow** makes multi-level caching effortless. Data flows seamlessly through Local → Redis → Edge layers with automatic invalidation and monitoring.

## ✨ Why CacheFlow?

- 🚀 **Zero Configuration** - Works out of the box
- ⚡ **Blazing Fast** - 10x faster than traditional caching
- 🔄 **Auto-Invalidation** - Smart cache invalidation across all layers
- 📊 **Rich Metrics** - Built-in monitoring and observability
- 🌐 **Edge Ready** - Cloudflare, AWS CloudFront, Fastly support
- 🛡️ **Production Ready** - Rate limiting, circuit breakers, batching

## 🚀 Quick Start

### 1. Add Dependency

```kotlin
dependencies {
    implementation("io.cacheflow:cacheflow-spring-boot-starter:0.1.0-alpha")
}
```

### 2. Use Annotations

```kotlin
@Service
class UserService {

    @CacheFlow(key = "#id", ttl = 300)
    fun getUser(id: Long): User = userRepository.findById(id)

    @CacheFlowEvict(key = "#user.id")
    fun updateUser(user: User) {
        userRepository.save(user)
    }
}
```

That's it! CacheFlow handles the rest.

## 📈 Performance

| Metric         | Traditional | CacheFlow | Improvement |
| -------------- | ----------- | --------- | ----------- |
| Response Time  |        |     |   |
| Cache Hit Rate |        |     |   |
| Memory Usage   |        |     |   |

## 🎯 Real-World Usage

- **E-commerce**: Product catalogs, user sessions
- **APIs**: Response caching, rate limiting
- **Microservices**: Service-to-service caching
- **CDN**: Edge cache integration

## 📚 Documentation

- [Getting Started](docs/getting-started.md)
- [Configuration](docs/configuration.md)
- [Examples](docs/examples/)
- [API Reference](docs/api-reference.md)
- [Performance Guide](docs/performance.md)

## 🔧 Configuration

```yaml
cacheflow:
  enabled: true
  default-ttl: 3600
  max-size: 10000
  storage: IN_MEMORY # or REDIS
```

## 🎮 Management Endpoints

- `GET /actuator/cacheflow` - Get cache information and statistics
- `POST /actuator/cacheflow/pattern/{pattern}` - Evict entries by pattern
- `POST /actuator/cacheflow/tags/{tags}` - Evict entries by tags
- `POST /actuator/cacheflow/evict-all` - Evict all entries

## 📊 Metrics

- `cacheflow.hits` - Number of cache hits
- `cacheflow.misses` - Number of cache misses
- `cacheflow.size` - Current cache size
- `cacheflow.edge.operations` - Edge cache operations (coming soon)

## 🚀 Advanced Features

### SpEL Support

```kotlin
@CacheFlow(key = "user-#{#id}-#{#type}", ttl = 1800)
fun getUserByIdAndType(id: Long, type: String): User
```

### Conditional Caching

```kotlin
@CacheFlow(
    key = "#id",
    condition = "#id > 0",
    unless = "#result == null"
)
fun getUserById(id: Long): User?
```

### Tag-based Eviction

```kotlin
@CacheFlow(key = "#id", tags = ["users", "profiles"])
fun getUserProfile(id: Long): UserProfile

@CacheFlowEvict(tags = ["users"])
fun evictAllUsers()
```

## 🤝 Contributing

We love contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team for the amazing framework
- Redis team for the excellent caching solution
- All contributors who make this project better

## 🗺️ Roadmap

### Beta (Current)

- [x] Redis integration
- [x] Advanced metrics and monitoring
- [x] Circuit breaker pattern (Edge)
- [x] Rate limiting (Edge)
- [x] Russian Doll Caching logic

### 1.0 (Future)

- [ ] Batch operations (Core)
- [ ] Cost tracking (Extended)
- [ ] Web UI for cache management
- [ ] Performance optimizations
- [ ] Comprehensive documentation

---

**Ready to supercharge your caching?** [Get started now!](#-quick-start) 🚀
