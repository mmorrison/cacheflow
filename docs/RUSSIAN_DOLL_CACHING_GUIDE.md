# Russian Doll Caching Guide

This guide explains how to use the Russian Doll Caching features in CacheFlow Spring Boot Starter. Russian Doll Caching is inspired by Rails' fragment caching pattern and provides advanced caching capabilities including nested fragment caching, dependency-based invalidation, and granular cache regeneration.

## Table of Contents

1. [Overview](#overview)
2. [Key Features](#key-features)
3. [Getting Started](#getting-started)
4. [Fragment Caching](#fragment-caching)
5. [Dependency Tracking](#dependency-tracking)
6. [Cache Key Versioning](#cache-key-versioning)
7. [Fragment Composition](#fragment-composition)
8. [Advanced Features](#advanced-features)
9. [Best Practices](#best-practices)
10. [Examples](#examples)

## Overview

Russian Doll Caching allows you to cache small, reusable pieces of content (fragments) independently and compose them together to form larger cached content. This approach provides several benefits:

- **Granular Caching**: Cache only the parts that change frequently
- **Automatic Invalidation**: Dependencies are tracked and caches are invalidated automatically
- **Composition**: Combine multiple fragments into complete pages
- **Versioning**: Use timestamps to create versioned cache keys
- **Performance**: Reduce cache misses and improve hit rates

## Key Features

### 1. Fragment Caching

Cache small, reusable pieces of content independently.

### 2. Dependency Tracking

Automatically track dependencies between cache entries and invalidate dependent caches when dependencies change.

### 3. Cache Key Versioning

Use timestamps to create versioned cache keys that automatically invalidate when data changes.

### 4. Fragment Composition

Combine multiple cached fragments into complete pages using templates.

### 5. Tag-based Eviction

Group related cache entries using tags for efficient bulk operations.

## Getting Started

### Prerequisites

- Spring Boot 2.7+
- Java 8+
- CacheFlow Spring Boot Starter

### Basic Configuration

Add CacheFlow to your Spring Boot application:

```yaml
# application.yml
cacheflow:
  enabled: true
  default-ttl: 3600
  local-cache:
    enabled: true
    max-size: 1000
  redis-cache:
    enabled: true
    host: localhost
    port: 6379
```

## Fragment Caching

Fragment caching allows you to cache small pieces of content that can be reused across different contexts.

### Basic Fragment Caching

```kotlin
@Service
class UserService {

    @CacheFlowFragment(
        key = "user:#{userId}:profile",
        dependsOn = ["userId"],
        tags = ["user-#{userId}", "profile"],
        ttl = 3600
    )
    fun getUserProfile(userId: Long): String {
        // Expensive database operation
        return buildUserProfile(userId)
    }
}
```

### Fragment Caching with Dependencies

```kotlin
@CacheFlowFragment(
    key = "user:#{userId}:settings",
    dependsOn = ["userId"],
    tags = ["user-#{userId}", "settings"],
    ttl = 1800
)
fun getUserSettings(userId: Long): String {
    return buildUserSettings(userId)
}
```

## Dependency Tracking

Dependency tracking ensures that when a dependency changes, all dependent caches are automatically invalidated.

### How It Works

1. When a method is called with `dependsOn` parameters, the system tracks the relationship
2. When a dependency changes (e.g., user data is updated), all dependent caches are invalidated
3. This ensures data consistency without manual cache management

### Example

```kotlin
@Service
class UserService {

    // This cache depends on userId
    @CacheFlow(
        key = "user:#{userId}:summary",
        dependsOn = ["userId"],
        ttl = 1800
    )
    fun getUserSummary(userId: Long): String {
        return buildUserSummary(userId)
    }

    // When this method is called, it will invalidate getUserSummary cache
    @CacheFlowEvict(key = "user:#{userId}")
    fun updateUser(userId: Long, name: String): String {
        return updateUserInDatabase(userId, name)
    }
}
```

## Cache Key Versioning

Versioned cache keys include timestamps, allowing automatic cache invalidation when data changes.

### Basic Versioning

```kotlin
@CacheFlow(
    key = "user:#{userId}:data",
    versioned = true,
    timestampField = "lastModified",
    ttl = 3600
)
fun getUserData(userId: Long, lastModified: Long): String {
    return buildUserData(userId, lastModified)
}
```

### Versioning with Custom Timestamp Field

```kotlin
@CacheFlow(
    key = "product:#{productId}:details",
    versioned = true,
    timestampField = "updatedAt",
    ttl = 1800
)
fun getProductDetails(productId: Long, updatedAt: Instant): String {
    return buildProductDetails(productId, updatedAt)
}
```

### Supported Timestamp Types

- `Long` (milliseconds since epoch)
- `Instant`
- `LocalDateTime`
- `ZonedDateTime`
- `OffsetDateTime`
- `Date`
- Objects with `updatedAt`, `createdAt`, or `modifiedAt` fields

## Fragment Composition

Fragment composition allows you to combine multiple cached fragments into complete pages.

### Basic Composition

```kotlin
@CacheFlowComposition(
    key = "user:#{userId}:page",
    template = """
        <!DOCTYPE html>
        <html>
        <head><title>User Page</title></head>
        <body>
            {{header}}
            <main>{{content}}</main>
            {{footer}}
        </body>
        </html>
    """,
    fragments = [
        "user:#{userId}:header",
        "user:#{userId}:content",
        "user:#{userId}:footer"
    ],
    ttl = 1800
)
fun getUserPage(userId: Long): String {
    // This method should not be called due to composition
    return "This should not be called"
}
```

### Dynamic Composition

```kotlin
@Service
class PageService {

    fun composeUserPage(userId: Long): String {
        val template = "<div>{{header}}</div><div>{{content}}</div><div>{{footer}}</div>"
        val fragments = mapOf(
            "header" to getUserHeader(userId),
            "content" to getUserContent(userId),
            "footer" to getUserFooter(userId)
        )
        return fragmentCacheService.composeFragments(template, fragments)
    }
}
```

## Advanced Features

### Tag-based Eviction

```kotlin
// Cache with tags
@CacheFlowFragment(
    key = "user:#{userId}:profile",
    tags = ["user-#{userId}", "profile"],
    ttl = 3600
)
fun getUserProfile(userId: Long): String {
    return buildUserProfile(userId)
}

// Invalidate by tag
fun invalidateUserFragments(userId: Long) {
    fragmentCacheService.invalidateFragmentsByTag("user-$userId")
}
```

### Conditional Caching

```kotlin
@CacheFlow(
    key = "user:#{userId}:data",
    condition = "#{userId > 0}",
    unless = "#{result == null}",
    ttl = 3600
)
fun getUserData(userId: Long): String? {
    return if (userId > 0) buildUserData(userId) else null
}
```

### Synchronous Caching

```kotlin
@CacheFlow(
    key = "user:#{userId}:critical",
    sync = true,
    ttl = 3600
)
fun getCriticalUserData(userId: Long): String {
    return buildCriticalUserData(userId)
}
```

## Best Practices

### 1. Use Appropriate TTL Values

- **Fragments**: 30 minutes to 2 hours
- **Compositions**: 15 minutes to 1 hour
- **Versioned caches**: 1 hour to 24 hours

### 2. Choose Meaningful Cache Keys

```kotlin
// Good
key = "user:#{userId}:profile:#{profileId}"

// Avoid
key = "data:#{id}"
```

### 3. Use Tags for Grouping

```kotlin
tags = ["user-#{userId}", "profile", "public"]
```

### 4. Leverage Dependencies

```kotlin
// Cache depends on user data
dependsOn = ["userId"]

// Cache depends on multiple parameters
dependsOn = ["userId", "profileId"]
```

### 5. Use Versioning for Frequently Changing Data

```kotlin
@CacheFlow(
    key = "product:#{productId}:price",
    versioned = true,
    timestampField = "lastPriceUpdate",
    ttl = 3600
)
fun getProductPrice(productId: Long, lastPriceUpdate: Instant): BigDecimal {
    return getCurrentPrice(productId, lastPriceUpdate)
}
```

## Examples

### Complete User Dashboard

```kotlin
@Service
class UserDashboardService {

    @CacheFlowFragment(
        key = "user:#{userId}:header",
        dependsOn = ["userId"],
        tags = ["user-#{userId}", "header"],
        ttl = 7200
    )
    fun getUserHeader(userId: Long): String {
        return buildUserHeader(userId)
    }

    @CacheFlowFragment(
        key = "user:#{userId}:profile",
        dependsOn = ["userId"],
        tags = ["user-#{userId}", "profile"],
        ttl = 3600
    )
    fun getUserProfile(userId: Long): String {
        return buildUserProfile(userId)
    }

    @CacheFlowFragment(
        key = "user:#{userId}:settings",
        dependsOn = ["userId"],
        tags = ["user-#{userId}", "settings"],
        ttl = 1800
    )
    fun getUserSettings(userId: Long): String {
        return buildUserSettings(userId)
    }

    @CacheFlowComposition(
        key = "user:#{userId}:dashboard",
        template = """
            <!DOCTYPE html>
            <html>
            <head><title>User Dashboard</title></head>
            <body>
                {{header}}
                <main>
                    {{profile}}
                    {{settings}}
                </main>
            </body>
            </html>
        """,
        fragments = [
            "user:#{userId}:header",
            "user:#{userId}:profile",
            "user:#{userId}:settings"
        ],
        ttl = 1800
    )
    fun getUserDashboard(userId: Long): String {
        return "This should not be called"
    }

    @CacheFlowEvict(key = "user:#{userId}")
    fun updateUser(userId: Long, name: String): String {
        return updateUserInDatabase(userId, name)
    }
}
```

### E-commerce Product Page

```kotlin
@Service
class ProductService {

    @CacheFlowFragment(
        key = "product:#{productId}:header",
        dependsOn = ["productId"],
        tags = ["product-#{productId}", "header"],
        ttl = 3600
    )
    fun getProductHeader(productId: Long): String {
        return buildProductHeader(productId)
    }

    @CacheFlowFragment(
        key = "product:#{productId}:details",
        dependsOn = ["productId"],
        tags = ["product-#{productId}", "details"],
        ttl = 1800
    )
    fun getProductDetails(productId: Long): String {
        return buildProductDetails(productId)
    }

    @CacheFlowFragment(
        key = "product:#{productId}:reviews",
        dependsOn = ["productId"],
        tags = ["product-#{productId}", "reviews"],
        ttl = 900
    )
    fun getProductReviews(productId: Long): String {
        return buildProductReviews(productId)
    }

    @CacheFlowComposition(
        key = "product:#{productId}:page",
        template = """
            <!DOCTYPE html>
            <html>
            <head><title>Product Page</title></head>
            <body>
                {{header}}
                <main>
                    {{details}}
                    {{reviews}}
                </main>
            </body>
            </html>
        """,
        fragments = [
            "product:#{productId}:header",
            "product:#{productId}:details",
            "product:#{productId}:reviews"
        ],
        ttl = 1800
    )
    fun getProductPage(productId: Long): String {
        return "This should not be called"
    }
}
```

## Monitoring and Debugging

### Cache Statistics

```kotlin
@Service
class CacheMonitoringService {

    @Autowired
    private lateinit var cacheService: CacheFlowService

    @Autowired
    private lateinit var fragmentCacheService: FragmentCacheService

    @Autowired
    private lateinit var dependencyResolver: DependencyResolver

    fun getCacheStatistics(): Map<String, Any> {
        return mapOf(
            "totalCacheEntries" to cacheService.size(),
            "totalFragments" to fragmentCacheService.getFragmentCount(),
            "totalDependencies" to dependencyResolver.getDependencyCount(),
            "cacheKeys" to cacheService.keys(),
            "fragmentKeys" to fragmentCacheService.getFragmentKeys()
        )
    }
}
```

### Debugging Dependencies

```kotlin
fun debugDependencies(cacheKey: String) {
    val dependencies = dependencyResolver.getDependencies(cacheKey)
    val dependents = dependencyResolver.getDependentCaches(cacheKey)

    println("Cache key: $cacheKey")
    println("Dependencies: $dependencies")
    println("Dependents: $dependents")
}
```

## Conclusion

Russian Doll Caching provides powerful tools for building efficient, scalable applications with sophisticated caching strategies. By leveraging fragment caching, dependency tracking, versioning, and composition, you can create applications that are both performant and maintainable.

For more examples and advanced usage patterns, see the [examples directory](examples/) and the [integration tests](../src/test/kotlin/io/cacheflow/spring/integration/).
