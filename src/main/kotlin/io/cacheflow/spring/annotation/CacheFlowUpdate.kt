package io.cacheflow.spring.annotation

import java.lang.annotation.Inherited

/**
 * Annotation to trigger an update (touch) on a parent entity when a method is executed.
 *
 * This is useful for "Russian Doll" caching where updating a child entity should invalidate
 * or update the parent entity's cache key (e.g. by updating its updatedAt timestamp).
 *
 * @property parent SpEL expression to evaluate the parent ID (e.g., "#entity.parentId" or "#args[0]").
 * @property entityType The type of the parent entity (e.g., "user", "organization").
 * @property condition SpEL expression to verify if the update should proceed.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@MustBeDocumented
annotation class CacheFlowUpdate(
    val parent: String,
    val entityType: String,
    val condition: String = "",
)
