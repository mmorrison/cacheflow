package io.cacheflow.spring.annotation

/**
 * Builder class for CacheFlow configuration to reduce annotation parameter count. This allows for
 * more flexible configuration while keeping the annotation simple.
 */
class CacheFlowConfigBuilder {
    /** The cache key expression (SpEL supported). */
    var key: String = ""

    /** The key generator bean name. */
    var keyGenerator: String = ""

    /** Time to live for the cache entry in seconds. */
    var ttl: Long = -1

    /** Array of parameter names that this cache depends on. */
    var dependsOn: Array<out String> = emptyArray()

    /** Array of tags for group-based eviction. */
    var tags: Array<out String> = emptyArray()

    /** Condition to determine if caching should be applied. */
    var condition: String = ""

    /** Condition to determine if caching should be skipped. */
    var unless: String = ""

    /** Whether to use synchronous caching. */
    var sync: Boolean = false

    /** Whether to use versioned cache keys based on timestamps. */
    var versioned: Boolean = false

    /** The field name to extract timestamp from for versioning. */
    var timestampField: String = DEFAULT_TIMESTAMP_FIELD

    /** Builds the CacheFlowConfig with the configured values. */
    fun build(): CacheFlowConfig =
        CacheFlowConfig(
            key = key,
            keyGenerator = keyGenerator,
            ttl = ttl,
            dependsOn = dependsOn.toList().toTypedArray(),
            tags = tags.toList().toTypedArray(),
            condition = condition,
            unless = unless,
            sync = sync,
            versioned = versioned,
            timestampField = timestampField,
            config = ""
        )

    companion object {
        private const val DEFAULT_TIMESTAMP_FIELD = "updatedAt"

        /** Creates a builder with default values. */
        fun builder(): CacheFlowConfigBuilder = CacheFlowConfigBuilder()

        /** Creates a builder with a specific cache key. */
        fun withKey(key: String): CacheFlowConfigBuilder =
            CacheFlowConfigBuilder().apply { this.key = key }

        /** Creates a builder for versioned caching. */
        fun versioned(
            timestampField: String = DEFAULT_TIMESTAMP_FIELD
        ): CacheFlowConfigBuilder =
            CacheFlowConfigBuilder().apply {
                this.versioned = true
                this.timestampField = timestampField
            }

        /** Creates a builder with dependencies. */
        fun withDependencies(vararg dependsOn: String): CacheFlowConfigBuilder =
            CacheFlowConfigBuilder().apply { this.dependsOn = dependsOn }

        /** Creates a builder with tags. */
        fun withTags(vararg tags: String): CacheFlowConfigBuilder =
            CacheFlowConfigBuilder().apply { this.tags = tags }
    }
}
