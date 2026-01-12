# Russian Doll Caching Implementation Plan for CacheFlow

## Overview

This document outlines a comprehensive plan to implement true Russian Doll Caching functionality in the CacheFlow Spring Boot Starter, inspired by Rails' fragment caching pattern. The implementation will add nested fragment caching, dependency-based invalidation, and granular cache regeneration capabilities.

## Current State Analysis

### ✅ Existing Strengths
- Multi-level caching architecture (Local → Redis → Edge)
- Annotation-based approach with `@CacheFlow`
- SpEL support for dynamic cache keys
- Tag-based eviction system
- AOP integration

### ❌ Missing Russian Doll Features
- Nested fragment caching
- Dependency resolution and automatic invalidation
- Cache key versioning with timestamps
- Fragment composition
- Granular regeneration

## Implementation Phases

## Phase 1: Core Dependency Management (Weeks 1-2)

### 1.1 Implement Dependency Resolution Engine

**Files to Create/Modify:**
- `src/main/kotlin/io/cacheflow/spring/dependency/DependencyResolver.kt`
- `src/main/kotlin/io/cacheflow/spring/dependency/CacheDependencyTracker.kt`
- `src/main/kotlin/io/cacheflow/spring/aspect/CacheFlowAspect.kt` (modify)

**Key Components:**

```kotlin
// DependencyResolver.kt
interface DependencyResolver {
    fun trackDependency(cacheKey: String, dependencyKey: String)
    fun invalidateDependentCaches(dependencyKey: String): Set<String>
    fun getDependencies(cacheKey: String): Set<String>
}

// CacheDependencyTracker.kt
@Component
class CacheDependencyTracker : DependencyResolver {
    private val dependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()
    private val reverseDependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()
    
    override fun trackDependency(cacheKey: String, dependencyKey: String) {
        // Implementation for tracking cache dependencies
    }
    
    override fun invalidateDependentCaches(dependencyKey: String): Set<String> {
        // Implementation for cascading invalidation
    }
}
```

**Tasks:**
- [ ] Create dependency tracking data structures
- [ ] Implement dependency resolution logic
- [ ] Add dependency tracking to CacheFlowAspect
- [ ] Create unit tests for dependency management
- [ ] Add integration tests for cascading invalidation

### 1.2 Enhance CacheFlowAspect for Dependencies

**Modifications to `CacheFlowAspect.kt`:**

```kotlin
private fun processCacheFlow(joinPoint: ProceedingJoinPoint, cached: CacheFlow): Any? {
    val key = generateCacheKeyFromExpression(cached.key, joinPoint)
    if (key.isBlank()) return joinPoint.proceed()
    
    // Track dependencies
    trackDependencies(key, cached.dependsOn, joinPoint)
    
    val cachedValue = cacheService.get(key)
    return cachedValue ?: executeAndCache(joinPoint, key, cached)
}

private fun trackDependencies(cacheKey: String, dependsOn: Array<String>, joinPoint: ProceedingJoinPoint) {
    dependsOn.forEach { paramName ->
        val dependencyKey = generateDependencyKey(paramName, joinPoint)
        dependencyResolver.trackDependency(cacheKey, dependencyKey)
    }
}
```

## Phase 2: Fragment Caching System (Weeks 3-4)

### 2.1 Create Fragment Caching Annotations

**Files to Create:**
- `src/main/kotlin/io/cacheflow/spring/annotation/CacheFlowFragment.kt`
- `src/main/kotlin/io/cacheflow/spring/annotation/CacheFlowComposition.kt`

```kotlin
// CacheFlowFragment.kt
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowFragment(
    val key: String = "",
    val template: String = "",
    val versioned: Boolean = false,
    val dependsOn: Array<String> = [],
    val tags: Array<String> = [],
    val ttl: Long = -1
)

// CacheFlowComposition.kt
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowComposition(
    val fragments: Array<String> = [],
    val key: String = "",
    val ttl: Long = -1
)
```

### 2.2 Implement Fragment Cache Service

**Files to Create:**
- `src/main/kotlin/io/cacheflow/spring/fragment/FragmentCacheService.kt`
- `src/main/kotlin/io/cacheflow/spring/fragment/impl/FragmentCacheServiceImpl.kt`

```kotlin
// FragmentCacheService.kt
interface FragmentCacheService {
    fun cacheFragment(key: String, fragment: String, ttl: Long)
    fun getFragment(key: String): String?
    fun composeFragments(fragmentKeys: List<String>): String
    fun invalidateFragment(key: String)
    fun invalidateFragmentsByTag(tag: String)
}
```

### 2.3 Create Fragment Aspect

**Files to Create:**
- `src/main/kotlin/io/cacheflow/spring/aspect/FragmentCacheAspect.kt`

**Tasks:**
- [ ] Implement fragment caching annotations
- [ ] Create fragment cache service
- [ ] Add fragment composition logic
- [ ] Implement fragment aspect
- [ ] Add comprehensive tests

## Phase 3: Cache Key Versioning (Weeks 5-6)

### 3.1 Implement Versioned Cache Keys

**Files to Create/Modify:**
- `src/main/kotlin/io/cacheflow/spring/versioning/CacheKeyVersioner.kt`
- `src/main/kotlin/io/cacheflow/spring/versioning/TimestampExtractor.kt`
- `src/main/kotlin/io/cacheflow/spring/aspect/CacheFlowAspect.kt` (modify)

```kotlin
// CacheKeyVersioner.kt
@Component
class CacheKeyVersioner {
    fun generateVersionedKey(baseKey: String, obj: Any?): String {
        val timestamp = extractTimestamp(obj)
        return if (timestamp != null) {
            "$baseKey-v$timestamp"
        } else {
            baseKey
        }
    }
    
    private fun extractTimestamp(obj: Any?): Long? {
        // Extract updatedAt timestamp from objects
        return when (obj) {
            is TemporalAccessor -> obj.toEpochMilli()
            is HasUpdatedAt -> obj.updatedAt?.toEpochMilli()
            else -> null
        }
    }
}

// TimestampExtractor.kt
interface TimestampExtractor {
    fun extractTimestamp(obj: Any?): Long?
}

@Component
class DefaultTimestampExtractor : TimestampExtractor {
    override fun extractTimestamp(obj: Any?): Long? {
        // Implementation for extracting timestamps from various object types
    }
}
```

### 3.2 Add Versioning Support to Annotations

**Modifications to existing annotations:**

```kotlin
// Enhanced CacheFlow annotation
annotation class CacheFlow(
    val key: String = "",
    val versioned: Boolean = false,  // New parameter
    val timestampField: String = "updatedAt",  // New parameter
    // ... existing parameters
)
```

**Tasks:**
- [ ] Implement timestamp extraction logic
- [ ] Add versioning to cache key generation
- [ ] Create timestamp extractor interface
- [ ] Add versioning support to annotations
- [ ] Update aspect to use versioned keys

## Phase 4: Granular Invalidation System (Weeks 7-8)

### 4.1 Implement Granular Invalidation

**Files to Create:**
- `src/main/kotlin/io/cacheflow/spring/invalidation/GranularInvalidator.kt`
- `src/main/kotlin/io/cacheflow/spring/invalidation/InvalidationStrategy.kt`

```kotlin
// GranularInvalidator.kt
@Component
class GranularInvalidator(
    private val cacheService: CacheFlowService,
    private val dependencyResolver: DependencyResolver
) {
    fun invalidateGranularly(
        rootKey: String,
        strategy: InvalidationStrategy = InvalidationStrategy.CASCADE
    ) {
        when (strategy) {
            InvalidationStrategy.CASCADE -> invalidateCascade(rootKey)
            InvalidationStrategy.SELECTIVE -> invalidateSelective(rootKey)
            InvalidationStrategy.FRAGMENT_ONLY -> invalidateFragmentOnly(rootKey)
        }
    }
    
    private fun invalidateCascade(rootKey: String) {
        val dependentKeys = dependencyResolver.invalidateDependentCaches(rootKey)
        dependentKeys.forEach { cacheService.evict(it) }
    }
}

// InvalidationStrategy.kt
enum class InvalidationStrategy {
    CASCADE,      // Invalidate all dependent caches
    SELECTIVE,    // Invalidate only directly dependent caches
    FRAGMENT_ONLY // Invalidate only fragment caches
}
```

### 4.2 Enhanced CacheFlowEvict Annotation

**Modifications to `CacheFlowEvict.kt`:**

```kotlin
annotation class CacheFlowEvict(
    val key: String = "",
    val tags: Array<String> = [],
    val allEntries: Boolean = false,
    val beforeInvocation: Boolean = false,
    val condition: String = "",
    val strategy: InvalidationStrategy = InvalidationStrategy.CASCADE,  // New parameter
    val cascade: Array<String> = []  // New parameter for specific cascading
)
```

**Tasks:**
- [ ] Implement granular invalidation logic
- [ ] Create invalidation strategies
- [ ] Enhance CacheFlowEvict annotation
- [ ] Add cascade invalidation support
- [ ] Create invalidation tests

## Phase 5: Fragment Composition Engine (Weeks 9-10)

### 5.1 Implement Fragment Composition

**Files to Create:**
- `src/main/kotlin/io/cacheflow/spring/composition/FragmentComposer.kt`
- `src/main/kotlin/io/cacheflow/spring/composition/CompositionTemplate.kt`

```kotlin
// FragmentComposer.kt
@Component
class FragmentComposer(
    private val fragmentCacheService: FragmentCacheService
) {
    fun composeFragments(
        template: String,
        fragments: Map<String, String>
    ): String {
        var result = template
        fragments.forEach { (placeholder, fragment) ->
            result = result.replace("{{$placeholder}}", fragment)
        }
        return result
    }
    
    fun composeWithCaching(
        compositionKey: String,
        template: String,
        fragmentKeys: List<String>
    ): String {
        val fragments = fragmentKeys.mapNotNull { key ->
            fragmentCacheService.getFragment(key)
        }
        return composeFragments(template, fragments.associateByIndexed { i, _ -> "fragment$i" to it })
    }
}

// CompositionTemplate.kt
data class CompositionTemplate(
    val name: String,
    val template: String,
    val fragmentPlaceholders: List<String>
)
```

### 5.2 Add Composition Support to Aspect

**Modifications to `CacheFlowAspect.kt`:**

```kotlin
@Around("@annotation(io.cacheflow.spring.annotation.CacheFlowComposition)")
fun aroundComposition(joinPoint: ProceedingJoinPoint): Any? {
    val method = (joinPoint.signature as MethodSignature).method
    val composition = method.getAnnotation(CacheFlowComposition::class.java) ?: return joinPoint.proceed()
    
    return processComposition(joinPoint, composition)
}

private fun processComposition(joinPoint: ProceedingJoinPoint, composition: CacheFlowComposition): Any? {
    val key = generateCacheKeyFromExpression(composition.key, joinPoint)
    if (key.isBlank()) return joinPoint.proceed()
    
    val cachedValue = cacheService.get(key)
    return cachedValue ?: executeAndCompose(joinPoint, key, composition)
}
```

**Tasks:**
- [ ] Implement fragment composition logic
- [ ] Create composition templates
- [ ] Add composition aspect support
- [ ] Create composition caching
- [ ] Add composition tests

## Phase 6: Integration and Testing (Weeks 11-12)

### 6.1 Integration Testing

**Files to Create:**
- `src/test/kotlin/io/cacheflow/spring/integration/RussianDollCachingIntegrationTest.kt`
- `src/test/kotlin/io/cacheflow/spring/integration/FragmentCachingIntegrationTest.kt`

```kotlin
// RussianDollCachingIntegrationTest.kt
@SpringBootTest
class RussianDollCachingIntegrationTest {
    
    @Test
    fun `should implement russian doll caching pattern`() {
        // Test nested fragment caching
        // Test dependency invalidation
        // Test granular regeneration
        // Test fragment composition
    }
    
    @Test
    fun `should handle cascading invalidation correctly`() {
        // Test that changing a user invalidates user fragments
        // but not unrelated fragments
    }
}
```

### 6.2 Performance Testing

**Files to Create:**
- `src/test/kotlin/io/cacheflow/spring/performance/RussianDollPerformanceTest.kt`

```kotlin
@SpringBootTest
class RussianDollPerformanceTest {
    
    @Test
    fun `should demonstrate performance benefits of russian doll caching`() {
        // Benchmark traditional caching vs Russian Doll caching
        // Measure cache hit rates
        // Measure invalidation performance
    }
}
```

### 6.3 Documentation and Examples

**Files to Create:**
- `docs/RUSSIAN_DOLL_CACHING_GUIDE.md`
- `docs/examples/RussianDollCachingExamples.kt`
- `docs/examples/application-russian-doll-example.yml`

**Tasks:**
- [ ] Create comprehensive integration tests
- [ ] Add performance benchmarks
- [ ] Write detailed documentation
- [ ] Create practical examples
- [ ] Update README with Russian Doll features

## Phase 7: Advanced Features (Weeks 13-14)

### 7.1 Smart Invalidation

**Files to Create:**
- `src/main/kotlin/io/cacheflow/spring/smart/SmartInvalidator.kt`
- `src/main/kotlin/io/cacheflow/spring/smart/InvalidationRule.kt`

```kotlin
// SmartInvalidator.kt
@Component
class SmartInvalidator {
    fun shouldInvalidate(
        changedObject: Any,
        cacheKey: String,
        rules: List<InvalidationRule>
    ): Boolean {
        return rules.any { rule -> rule.matches(changedObject, cacheKey) }
    }
}

// InvalidationRule.kt
data class InvalidationRule(
    val condition: String,  // SpEL expression
    val action: InvalidationAction
)

enum class InvalidationAction {
    INVALIDATE_IMMEDIATELY,
    INVALIDATE_ON_NEXT_ACCESS,
    SKIP_INVALIDATION
}
```

### 7.2 Cache Warming

**Files to Create:**
- `src/main/kotlin/io/cacheflow/spring/warming/CacheWarmer.kt`
- `src/main/kotlin/io/cacheflow/spring/warming/WarmingStrategy.kt`

```kotlin
// CacheWarmer.kt
@Component
class CacheWarmer(
    private val cacheService: CacheFlowService,
    private val fragmentCacheService: FragmentCacheService
) {
    fun warmCache(warmingStrategy: WarmingStrategy) {
        when (warmingStrategy) {
            is WarmingStrategy.Preload -> preloadCache(warmingStrategy.keys)
            is WarmingStrategy.OnDemand -> setupOnDemandWarming(warmingStrategy.triggers)
        }
    }
}
```

**Tasks:**
- [ ] Implement smart invalidation rules
- [ ] Add cache warming capabilities
- [ ] Create advanced invalidation strategies
- [ ] Add cache warming tests

## Implementation Timeline

| Phase | Duration | Key Deliverables |
|-------|----------|------------------|
| Phase 1 | 2 weeks | Dependency resolution engine |
| Phase 2 | 2 weeks | Fragment caching system |
| Phase 3 | 2 weeks | Cache key versioning |
| Phase 4 | 2 weeks | Granular invalidation |
| Phase 5 | 2 weeks | Fragment composition |
| Phase 6 | 2 weeks | Integration & testing |
| Phase 7 | 2 weeks | Advanced features |

**Total Duration: 14 weeks (3.5 months)**

## Success Metrics

### Functional Requirements
- [ ] Support for nested fragment caching
- [ ] Automatic dependency-based invalidation
- [ ] Cache key versioning with timestamps
- [ ] Granular cache regeneration
- [ ] Fragment composition capabilities

### Performance Requirements
- [ ] 95%+ cache hit rate for nested fragments
- [ ] <10ms invalidation time for dependent caches
- [ ] 50% reduction in cache misses compared to traditional caching
- [ ] Support for 10,000+ concurrent fragment operations

### Quality Requirements
- [ ] 90%+ test coverage for new features
- [ ] Comprehensive documentation
- [ ] Backward compatibility with existing CacheFlow features
- [ ] Performance benchmarks and monitoring

## Risk Mitigation

### Technical Risks
1. **Complexity**: Russian Doll caching is inherently complex
   - *Mitigation*: Implement in phases, extensive testing
2. **Performance**: Dependency tracking overhead
   - *Mitigation*: Optimize data structures, lazy evaluation
3. **Memory Usage**: Fragment storage requirements
   - *Mitigation*: Implement TTL, compression, cleanup strategies

### Implementation Risks
1. **Breaking Changes**: Modifying existing APIs
   - *Mitigation*: Maintain backward compatibility, deprecation strategy
2. **Testing Complexity**: Complex dependency scenarios
   - *Mitigation*: Comprehensive test suite, integration tests
3. **Documentation**: Complex feature documentation
   - *Mitigation*: Examples, tutorials, step-by-step guides

## Next Steps

1. **Review and Approve Plan**: Get stakeholder approval for the implementation plan
2. **Set Up Development Environment**: Prepare development and testing infrastructure
3. **Begin Phase 1**: Start with dependency resolution engine implementation
4. **Regular Reviews**: Weekly progress reviews and adjustments
5. **Community Feedback**: Early feedback from users and contributors

## Conclusion

This implementation plan provides a comprehensive roadmap for adding true Russian Doll Caching functionality to CacheFlow. The phased approach ensures manageable development cycles while building toward a robust, production-ready feature set that matches the spirit and functionality of Rails' fragment caching.

The plan balances ambitious feature goals with practical implementation considerations, ensuring that CacheFlow becomes a leading caching solution for Spring Boot applications with advanced fragment caching capabilities.
