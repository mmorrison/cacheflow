package io.cacheflow.spring.annotation

/**
 * Annotation for marking methods that return cacheable fragments in Russian Doll caching.
 *
 * Fragments are small, reusable pieces of content that can be cached independently and composed
 * together to form larger cached content.
 *
 * @param key The cache key expression (SpEL supported)
 * @param template The template string for fragment composition
 * @param versioned Whether to use versioned cache keys based on timestamps
 * @param dependsOn Array of parameter names that this fragment depends on
 * @param tags Array of tags for group-based eviction
 * @param ttl Time to live for the fragment in seconds
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowFragment(
    /** The cache key expression (SpEL supported). */
    val key: String = "",
    /** The template string for fragment composition. */
    val template: String = "",
    /** Whether to use versioned cache keys based on timestamps. */
    val versioned: Boolean = false,
    /** Array of parameter names that this fragment depends on. */
    val dependsOn: Array<String> = [],
    /** Array of tags for group-based eviction. */
    val tags: Array<String> = [],
    /** Time to live for the fragment in seconds. */
    val ttl: Long = -1,
)
