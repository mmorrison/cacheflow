package io.cacheflow.spring.annotation

private const val DEFAULT_KEY_GENERATOR = "defaultKeyGenerator"
private const val DEFAULT_TIMESTAMP_FIELD = "updatedAt"

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
 * @param versioned Whether to use versioned cache keys based on timestamps
 * @param timestampField The field name to extract timestamp from for versioning
 */
data class CacheFlowConfig(
    val key: String = "",
    val keyGenerator: String = DEFAULT_KEY_GENERATOR,
    val ttl: Long = -1,
    val dependsOn: Array<String> = emptyArray(),
    val tags: Array<String> = emptyArray(),
    val condition: String = "",
    val unless: String = "",
    val sync: Boolean = false,
    val versioned: Boolean = false,
    val timestampField: String = DEFAULT_TIMESTAMP_FIELD,
    /** Configuration name for complex setups using CacheFlowConfigBuilder. */
    val config: String = "",
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
        if (versioned != other.versioned) return false
        if (timestampField != other.timestampField) return false
        if (config != other.config) return false

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
        result = 31 * result + versioned.hashCode()
        result = 31 * result + timestampField.hashCode()
        result = 31 * result + config.hashCode()
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
 * @param versioned Whether to use versioned cache keys based on timestamps
 * @param timestampField The field name to extract timestamp from for versioning
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlow(
    /** The cache key expression (SpEL supported). */
    val key: String = "",
    /** Time to live for the cache entry in seconds. */
    val ttl: Long = -1,
    /** Array of parameter names that this cache depends on. */
    val dependsOn: Array<String> = [],
    /** Array of tags for group-based eviction. */
    val tags: Array<String> = [],
    /** Whether to use versioned cache keys based on timestamps. */
    val versioned: Boolean = false,
    /** The field name to extract timestamp from for versioning. */
    val timestampField: String = DEFAULT_TIMESTAMP_FIELD,
    /** Configuration name for complex setups using CacheFlowConfigBuilder. */
    val config: String = "",
)

/** Alternative annotation name for compatibility. */

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowCached(
    /** The cache key expression (SpEL supported). */
    val key: String = "",
    /** Time to live for the cache entry in seconds. */
    val ttl: Long = -1,
    /** Array of parameter names that this cache depends on. */
    val dependsOn: Array<String> = [],
    /** Array of tags for group-based eviction. */
    val tags: Array<String> = [],
    /** Whether to use versioned cache keys based on timestamps. */
    val versioned: Boolean = false,
    /** The field name to extract timestamp from for versioning. */
    val timestampField: String = DEFAULT_TIMESTAMP_FIELD,
    /** Configuration name for complex setups using CacheFlowConfigBuilder. */
    val config: String = "",
)
