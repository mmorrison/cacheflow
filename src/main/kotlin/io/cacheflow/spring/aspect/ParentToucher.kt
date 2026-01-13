package io.cacheflow.spring.aspect

/**
 * Interface to define how to "touch" a parent entity to update its timestamp.
 *
 * Implementations should update the 'updatedAt' (or equivalent) timestamp of the
 * specified entity, triggering a cache invalidation or refresh for any Russian Doll
 * caches that depend on that parent.
 */
interface ParentToucher {
    /**
     * Touches the specified parent entity.
     *
     * @param entityType The type string from @CacheFlowUpdate
     * @param parentId The ID of the parent entity
     */
    fun touch(
        entityType: String,
        parentId: String,
    )
}
