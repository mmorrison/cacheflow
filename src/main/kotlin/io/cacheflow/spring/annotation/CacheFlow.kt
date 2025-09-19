package io.cacheflow.spring.annotation

/**
 * Annotation to mark methods for Russian Doll caching.
 *
 * @param key The cache key expression (SpEL supported)
 * @param keyGenerator The key generator bean name
 * @param ttl Time to live for the cache entry in seconds
 * @param dependsOn Array of parameter names that this cache depends on
 * @param tags Array of tags for group-based eviction
 * @param condition Condition to determine if caching should be applied
 * @param unless Condition to determine if caching should be skipped
 * @param sync Whether to use synchronous caching
 */
@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlow(
        val key: String = "",
        val keyGenerator: String = "defaultKeyGenerator",
        val ttl: Long = -1,
        val dependsOn: Array<String> = [],
        val tags: Array<String> = [],
        val condition: String = "",
        val unless: String = "",
        val sync: Boolean = false
)

/** Alternative annotation name for compatibility */
@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowCached(
        val key: String = "",
        val keyGenerator: String = "defaultKeyGenerator",
        val ttl: Long = -1,
        val dependsOn: Array<String> = [],
        val tags: Array<String> = [],
        val condition: String = "",
        val unless: String = "",
        val sync: Boolean = false
)
