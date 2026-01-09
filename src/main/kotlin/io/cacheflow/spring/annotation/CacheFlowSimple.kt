package io.cacheflow.spring.annotation

/**
 * Simplified CacheFlow annotation with reduced parameters. Use CacheFlowConfigBuilder for complex
 * configurations.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowSimple(
    /** The cache key expression (SpEL supported). */
    val key: String = "",
    /** Time to live for the cache entry in seconds. */
    val ttl: Long = -1,
    /** Whether to use versioned cache keys based on timestamps. */
    val versioned: Boolean = false,
    /** Array of parameter names that this cache depends on. */
    val dependsOn: Array<String> = [],
    /** Array of tags for group-based eviction. */
    val tags: Array<String> = []
)

/**
 * Advanced CacheFlow annotation for complex configurations. Use this when you need more control
 * over caching behavior.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowAdvanced(
    /** Configuration name for complex setups using CacheFlowConfigBuilder. */
    val config: String = "",
    /** The cache key expression (SpEL supported). */
    val key: String = "",
    /** Time to live for the cache entry in seconds. */
    val ttl: Long = -1
)
