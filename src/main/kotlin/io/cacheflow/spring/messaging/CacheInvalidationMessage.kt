package io.cacheflow.spring.messaging

/**
 * Message payload for distributed cache invalidation.
 *
 * @property type The type of invalidation operation
 * @property keys Specific keys to invalidate (for EVICT type)
 * @property tags Tags to invalidate (for EVICT_BY_TAGS type)
 * @property origin The unique instance ID of the publisher to prevent self-eviction loops
 */
data class CacheInvalidationMessage(
    val type: InvalidationType,
    val keys: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val origin: String,
)

/**
 * Type of invalidation operation.
 */
enum class InvalidationType {
    EVICT,
    EVICT_ALL,
    EVICT_BY_TAGS,
}
