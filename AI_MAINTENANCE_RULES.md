# 🤖 AI Maintenance Rules for CacheFlow Spring Boot Starter

> Comprehensive rules to maintain technical and documentation excellence

## 📋 Overview

This document provides AI assistants with specific rules and guidelines to maintain the CacheFlow project's high standards for code quality, testing, documentation, and architecture. These rules ensure consistency, reliability, and maintainability across all contributions.

## 🎯 Core Principles

### 1. **Quality First**

- All code must pass Detekt analysis with zero violations
- Maintain 90%+ test coverage for all components
- Follow Kotlin best practices and Spring Boot conventions
- Ensure all public APIs are fully documented

### 2. **Russian Doll Caching Focus**

- Preserve the core Russian Doll caching pattern integrity
- Maintain fragment-based caching capabilities
- Ensure dependency tracking and invalidation work correctly
- Keep the multi-level cache hierarchy (Local → Redis → Edge)

### 3. **Documentation Excellence**

- Every public API must have comprehensive KDoc
- All examples must be executable and tested
- Documentation must be kept in sync with code changes
- Use progressive disclosure from quick start to advanced topics

## 🏗️ Architecture Rules

### Code Organization

```
src/main/kotlin/io/cacheflow/spring/
├── annotation/          # Cache annotations (@CacheFlow, @CacheFlowEvict)
├── aspect/             # AOP aspects for caching
├── autoconfigure/      # Spring Boot auto-configuration
├── config/             # Configuration properties
├── dependency/         # Dependency tracking and resolution
├── edge/              # Edge cache providers (Cloudflare, AWS, Fastly)
├── fragment/          # Fragment caching implementation
├── management/        # Actuator endpoints
├── service/           # Core cache services
└── util/              # Utility classes
```

### Naming Conventions

- **Classes**: PascalCase with descriptive names (`CacheFlowServiceImpl`)
- **Functions**: camelCase with verb-noun pattern (`cacheFragment`, `invalidateByTags`)
- **Constants**: UPPER_SNAKE_CASE (`DEFAULT_TTL_SECONDS`)
- **Packages**: lowercase with dots (`io.cacheflow.spring.fragment`)
- **Test Classes**: `*Test.kt` suffix (`CacheFlowServiceTest`)

### Interface Design

```kotlin
// ✅ Good: Clear, focused interface
interface FragmentCacheService {
    fun cacheFragment(key: String, fragment: String, ttl: Long)
    fun getFragment(key: String): String?
    fun invalidateFragment(key: String)
}

// ❌ Bad: Too many responsibilities
interface CacheService {
    fun cacheFragment(...)
    fun cacheUser(...)
    fun cacheProduct(...)
    fun sendEmail(...)
}
```

## 🧪 Testing Rules

### Test Structure Requirements

1. **Unit Tests** (60-70% of tests)

   - Test individual components in isolation
   - Use Mockito for dependencies
   - Cover all public methods and edge cases
   - Test both success and failure scenarios

2. **Integration Tests** (20-30% of tests)

   - Test Spring Boot context integration
   - Test component interactions
   - Use `@SpringBootTest` for full context

3. **Performance Tests** (5-10% of tests)
   - Benchmark critical operations
   - Test under load conditions
   - Validate response time requirements

### Test Naming Convention

```kotlin
// ✅ Good: Descriptive test names
@Test
fun `should cache fragment with custom TTL when valid input provided`() {
    // Test implementation
}

@Test
fun `should return null when fragment key does not exist`() {
    // Test implementation
}

// ❌ Bad: Vague test names
@Test
fun testCacheFragment() {
    // Test implementation
}
```

### Test Coverage Requirements

- **Minimum Coverage**: 90% for all components
- **Critical Paths**: 100% coverage for cache operations
- **Edge Cases**: Test null inputs, empty strings, boundary values
- **Error Handling**: Test all exception scenarios

### Test Data Management

```kotlin
// ✅ Good: Use test data builders
class FragmentTestDataBuilder {
    private var key: String = "test-fragment"
    private var content: String = "Hello World"
    private var ttl: Long = 3600L

    fun withKey(key: String) = apply { this.key = key }
    fun withContent(content: String) = apply { this.content = content }
    fun withTtl(ttl: Long) = apply { this.ttl = ttl }

    fun build() = Fragment(key = key, content = content, ttl = ttl)
}

// Usage in tests
val fragment = FragmentTestDataBuilder()
    .withKey("user-profile")
    .withContent("<div>User Profile</div>")
    .withTtl(1800L)
    .build()
```

## 📚 Documentation Rules

### KDoc Requirements

Every public API must include:

```kotlin
/**
 * Caches a fragment with the specified key and TTL.
 *
 * @param key The unique identifier for the fragment
 * @param fragment The HTML content to cache
 * @param ttl Time to live in seconds (must be positive)
 * @throws IllegalArgumentException if key is blank or ttl is negative
 * @since 0.1.0
 * @see [getFragment] for retrieving cached fragments
 * @see [invalidateFragment] for removing cached fragments
 */
fun cacheFragment(key: String, fragment: String, ttl: Long)
```

### Documentation Structure

```
docs/
├── README.md                           # Main project overview
├── EDGE_CACHE_OVERVIEW.md             # Feature overview
├── usage/
│   ├── EDGE_CACHE_USAGE_GUIDE.md      # Complete usage guide
│   └── FEATURES_REFERENCE.md          # API reference
├── testing/
│   ├── COMPREHENSIVE_TESTING_GUIDE.md # Testing strategies
│   └── EDGE_CACHE_TESTING_GUIDE.md    # Edge cache testing
├── troubleshooting/
│   └── EDGE_CACHE_TROUBLESHOOTING.md  # Common issues
└── examples/
    ├── EXAMPLES_INDEX.md              # Examples overview
    └── application-edge-cache-example.yml
```

### Code Examples

All examples must be:

- **Executable**: Can be run without modification
- **Tested**: Included in test suite
- **Commented**: Explain key concepts
- **Complete**: Include all necessary imports and configuration

```kotlin
// ✅ Good: Complete, executable example
@RestController
class UserController(
    private val userService: UserService,
    private val fragmentCacheService: FragmentCacheService
) {

    @GetMapping("/users/{id}")
    fun getUserProfile(@PathVariable id: Long): String {
        // Check cache first
        val cachedProfile = fragmentCacheService.getFragment("user-profile-$id")
        if (cachedProfile != null) {
            return cachedProfile
        }

        // Generate profile HTML
        val user = userService.findById(id)
        val profileHtml = generateUserProfileHtml(user)

        // Cache for 30 minutes
        fragmentCacheService.cacheFragment("user-profile-$id", profileHtml, 1800L)

        return profileHtml
    }
}
```

## 🔧 Code Quality Rules

### Detekt Configuration Compliance

All code must pass these Detekt rules:

- **Complexity**: Max 15 for methods, 4 for conditions
- **Naming**: Follow Kotlin conventions strictly
- **Documentation**: All public APIs must be documented
- **Performance**: Avoid unnecessary allocations
- **Style**: Consistent formatting and structure

### Error Handling

```kotlin
// ✅ Good: Specific error handling
fun cacheFragment(key: String, fragment: String, ttl: Long) {
    require(key.isNotBlank()) { "Fragment key cannot be blank" }
    require(ttl > 0) { "TTL must be positive, got: $ttl" }

    try {
        cacheService.put("fragment:$key", fragment, ttl)
    } catch (e: CacheException) {
        logger.error("Failed to cache fragment with key: $key", e)
        throw FragmentCacheException("Unable to cache fragment", e)
    }
}

// ❌ Bad: Generic error handling
fun cacheFragment(key: String, fragment: String, ttl: Long) {
    cacheService.put("fragment:$key", fragment, ttl)
}
```

### Performance Considerations

- **Cache Key Generation**: Use efficient key generation algorithms
- **Memory Usage**: Monitor and limit cache size
- **Concurrent Access**: Use thread-safe collections
- **TTL Management**: Implement efficient expiration checking

```kotlin
// ✅ Good: Efficient cache key generation
private fun generateCacheKey(prefix: String, params: Map<String, Any>): String {
    return params.entries
        .sortedBy { it.key }
        .joinToString(":") { "${it.key}=${it.value}" }
        .let { "$prefix:$it" }
}
```

## 🚀 Build and CI/CD Rules

### Gradle Configuration

- **Dependencies**: Use exact versions, no dynamic versions
- **Plugins**: Keep all plugins up to date
- **Tasks**: Configure all quality gates properly
- **Reports**: Generate comprehensive reports

### Quality Gates

```kotlin
// Required quality checks
tasks.register("qualityCheck") {
    dependsOn("detekt", "test", "jacocoTestReport")
}

// Security checks
tasks.register("securityCheck") {
    dependsOn("dependencyCheckAnalyze")
}
```

### CI/CD Pipeline

- **Test Execution**: Run all tests on every commit
- **Coverage Reporting**: Track coverage trends
- **Security Scanning**: OWASP dependency check
- **Documentation**: Generate and validate docs

## 🔒 Security Rules

### Input Validation

```kotlin
// ✅ Good: Comprehensive input validation
fun cacheFragment(key: String, fragment: String, ttl: Long) {
    validateFragmentKey(key)
    validateFragmentContent(fragment)
    validateTtl(ttl)

    // Safe to proceed
}

private fun validateFragmentKey(key: String) {
    require(key.isNotBlank()) { "Fragment key cannot be blank" }
    require(key.length <= MAX_KEY_LENGTH) { "Fragment key too long" }
    require(key.matches(SAFE_KEY_PATTERN)) { "Fragment key contains invalid characters" }
}
```

### Security Best Practices

- **Input Sanitization**: Validate all inputs
- **Key Injection Prevention**: Sanitize cache keys
- **Memory Limits**: Prevent memory exhaustion attacks
- **Access Control**: Implement proper authorization

## 📊 Monitoring and Observability

### Metrics Requirements

```kotlin
// Required metrics for all cache operations
@Component
class CacheMetrics {
    private val cacheHits = Counter.builder("cache.hits").register(meterRegistry)
    private val cacheMisses = Counter.builder("cache.misses").register(meterRegistry)
    private val cacheSize = Gauge.builder("cache.size").register(meterRegistry)

    fun recordCacheHit() = cacheHits.increment()
    fun recordCacheMiss() = cacheMisses.increment()
    fun recordCacheSize(size: Long) = cacheSize.set(size)
}
```

### Logging Standards

```kotlin
// ✅ Good: Structured logging
logger.info("Fragment cached successfully") {
    "key" to key
    "ttl" to ttl
    "size" to fragment.length
}

// ❌ Bad: Unstructured logging
logger.info("Fragment cached: $key")
```

## 🎯 Russian Doll Caching Specific Rules

### Fragment Management

- **Dependency Tracking**: Always track fragment dependencies
- **Invalidation Cascade**: Implement proper cascade invalidation
- **Composition**: Support fragment composition and templating
- **Versioning**: Use timestamps for cache versioning

### Cache Key Patterns

```kotlin
// Fragment cache keys
"fragment:user-profile:123"
"fragment:product-list:category:electronics"

// Dependency tracking
"dependency:user-profile:123:user:123"
"dependency:product-list:category:electronics:product:456"
```

### Performance Requirements

- **Fragment Retrieval**: < 1ms for cache hits
- **Composition**: < 5ms for complex fragment composition
- **Invalidation**: < 10ms for dependency-based invalidation
- **Memory Usage**: < 50MB for 10,000 fragments

## 🔄 Maintenance Workflow

### Code Review Checklist

- [ ] All tests pass with 90%+ coverage
- [ ] Detekt analysis passes with zero violations
- [ ] Documentation is updated and accurate
- [ ] Performance requirements are met
- [ ] Security best practices are followed
- [ ] Russian Doll caching patterns are preserved
- [ ] Examples are executable and tested

### Release Process

1. **Quality Gates**: All quality checks must pass
2. **Documentation**: Update all relevant documentation
3. **Version Bump**: Update version numbers consistently
4. **Changelog**: Document all changes
5. **Testing**: Run full test suite
6. **Security**: Complete security scan

## 🚨 Common Anti-Patterns to Avoid

### Code Anti-Patterns

```kotlin
// ❌ Bad: Generic exception handling
try {
    // cache operation
} catch (Exception e) {
    // handle all exceptions the same way
}

// ❌ Bad: Missing input validation
fun cacheFragment(key: String, fragment: String, ttl: Long) {
    cacheService.put(key, fragment, ttl) // No validation
}

// ❌ Bad: Hardcoded values
val ttl = 3600L // Should be configurable
```

### Documentation Anti-Patterns

```kotlin
// ❌ Bad: Missing or poor documentation
fun cacheFragment(key: String, fragment: String, ttl: Long) {
    // Implementation
}

// ❌ Bad: Outdated examples
// This example uses the old API
@CacheFlow(key = "user")
fun getUser(id: Long) = userService.findById(id)
```

## 📈 Success Metrics

### Quality Metrics

- **Test Coverage**: Maintain 90%+ coverage
- **Code Quality**: Zero Detekt violations
- **Documentation**: 100% public API coverage
- **Performance**: Meet all performance requirements
- **Security**: Zero high-severity vulnerabilities

### Maintenance Metrics

- **Build Time**: < 2 minutes for full build
- **Test Execution**: < 1 minute for test suite
- **Documentation Generation**: < 30 seconds
- **Deployment**: < 5 minutes for releases

---

## 🎯 Quick Reference

### Before Making Changes

1. Read and understand the Russian Doll caching architecture
2. Review existing tests and documentation
3. Check Detekt configuration and quality gates
4. Ensure all examples are executable

### During Development

1. Write tests first (TDD approach)
2. Follow naming conventions strictly
3. Document all public APIs comprehensively
4. Validate all inputs and handle errors properly

### After Implementation

1. Run full test suite and quality checks
2. Update all relevant documentation
3. Verify examples still work
4. Check performance requirements are met

### Code Review Focus

1. **Architecture**: Does it fit the Russian Doll pattern?
2. **Quality**: Does it pass all quality gates?
3. **Testing**: Are all scenarios covered?
4. **Documentation**: Is it complete and accurate?
5. **Performance**: Does it meet requirements?
6. **Security**: Are inputs validated and secure?

---

_These rules ensure CacheFlow maintains its high standards for technical excellence, comprehensive documentation, and reliable Russian Doll caching functionality._
