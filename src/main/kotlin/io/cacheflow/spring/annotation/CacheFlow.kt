package io.cacheflow.spring.annotation

private const val DEFAULT_KEY_GENERATOR = "defaultKeyGenerator"

/**
 * Data class to hold cache configuration parameters.
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
data class CacheFlowConfig(
    val key: String = "",
    val keyGenerator: String = DEFAULT_KEY_GENERATOR,
    val ttl: Long = -1,
    val dependsOn: Array<String> = emptyArray(),
    val tags: Array<String> = emptyArray(),
    val condition: String = "",
    val unless: String = "",
    val sync: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheFlowConfig

        if (key != other.key) return false
        if (keyGenerator != other.keyGenerator) return false
        if (ttl != other.ttl) return false
        if (!dependsOn.contentEquals(other.dependsOn)) return false
        if (!tags.contentEquals(other.tags)) return false
        if (condition != other.condition) return false
        if (unless != other.unless) return false
        if (sync != other.sync) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + keyGenerator.hashCode()
        result = 31 * result + ttl.hashCode()
        result = 31 * result + dependsOn.contentHashCode()
        result = 31 * result + tags.contentHashCode()
        result = 31 * result + condition.hashCode()
        result = 31 * result + unless.hashCode()
        result = 31 * result + sync.hashCode()
        return result
    }
}

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
    /** The cache key expression (SpEL supported). */
    val key: String = "",
    /** The key generator bean name. */
    val keyGenerator: String = DEFAULT_KEY_GENERATOR,
    /** Time to live for the cache entry in seconds. */
    val ttl: Long = -1,
    /** Array of parameter names that this cache depends on. */
    val dependsOn: Array<String> = [],
    /** Array of tags for group-based eviction. */
    val tags: Array<String> = [],
    /** Condition to determine if caching should be applied. */
    val condition: String = "",
    /** Condition to determine if caching should be skipped. */
    val unless: String = "",
    /** Whether to use synchronous caching. */
    val sync: Boolean = false
)

/** Alternative annotation name for compatibility. */

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowCached(
    /** The cache key expression (SpEL supported). */

    val key: String = "",
    /** The key generator bean name. */

    val keyGenerator: String = DEFAULT_KEY_GENERATOR,
    /** Time to live for the cache entry in seconds. */

    val ttl: Long = -1,
    /** Array of parameter names that this cache depends on. */

    val dependsOn: Array<String> = [],
    /** Array of tags for group-based eviction. */

    val tags: Array<String> = [],
    /** Condition to determine if caching should be applied. */

    val condition: String = "",
    /** Condition to determine if caching should be skipped. */

    val unless: String = "",
    /** Whether to use synchronous caching. */

    val sync: Boolean = false
)
