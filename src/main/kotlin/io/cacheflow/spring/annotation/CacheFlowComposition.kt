package io.cacheflow.spring.annotation

/**
 * Annotation for marking methods that compose multiple fragments into a complete cached result.
 *
 * Composition methods combine multiple cached fragments using templates to create larger, more
 * complex cached content in the Russian Doll caching pattern.
 *
 * @param fragments Array of fragment keys to compose
 * @param key The cache key expression (SpEL supported)
 * @param template The template string for composition
 * @param ttl Time to live for the composed result in seconds
 */
@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheFlowComposition(
        /** Array of fragment keys to compose. */
        val fragments: Array<String> = [],

        /** The cache key expression (SpEL supported). */
        val key: String = "",

        /** The template string for composition. */
        val template: String = "",

        /** Time to live for the composed result in seconds. */
        val ttl: Long = -1
)
