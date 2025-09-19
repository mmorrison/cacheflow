package io.cacheflow.spring.annotation

/**
 * Annotation to evict entries from Russian Doll cache.
 *
 * @param key The cache key expression (SpEL supported)
 * @param tags Array of tags for group-based eviction
 * @param allEntries Whether to evict all entries
 * @param beforeInvocation Whether to evict before method invocation
 * @param condition Condition to determine if eviction should be applied
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowEvict(
    /** The cache key expression (SpEL supported). */

    val key: String = "",
    /** Array of tags for group-based eviction. */

    val tags: Array<String> = [],
    /** Whether to evict all entries. */

    val allEntries: Boolean = false,
    /** Whether to evict before method invocation. */

    val beforeInvocation: Boolean = false,
    /** Condition to determine if eviction should be applied. */

    val condition: String = ""
)

/** Alternative annotation name for compatibility. */

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowEvictAlternative(
    /** The cache key expression (SpEL supported). */

    val key: String = "",
    /** Array of tags for group-based eviction. */

    val tags: Array<String> = [],
    /** Whether to evict all entries. */

    val allEntries: Boolean = false,
    /** Whether to evict before method invocation. */

    val beforeInvocation: Boolean = false,
    /** Condition to determine if eviction should be applied. */

    val condition: String = ""
)

/** Annotation to mark classes as cacheable entities. */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheEntity(
        /** Key prefix for cache entries. */
        val keyPrefix: String = "",
        /** Version field name for cache invalidation. */
        val versionField: String = "updatedAt"
)

/** Annotation to mark properties as cache keys. */



@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheKey

/** Annotation to mark properties as cache version fields. */

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheVersion
