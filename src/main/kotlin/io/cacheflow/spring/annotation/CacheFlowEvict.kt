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
        val key: String = "",
        val tags: Array<String> = [],
        val allEntries: Boolean = false,
        val beforeInvocation: Boolean = false,
        val condition: String = ""
)

/** Alternative annotation name for compatibility */
@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowEvictAlternative(
        val key: String = "",
        val tags: Array<String> = [],
        val allEntries: Boolean = false,
        val beforeInvocation: Boolean = false,
        val condition: String = ""
)

/** Annotation to mark classes as cacheable entities */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheEntity(val keyPrefix: String = "", val versionField: String = "updatedAt")

/** Annotation to mark properties as cache keys */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheKey

/** Annotation to mark properties as cache version fields */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheVersion
