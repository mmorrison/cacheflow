package io.cacheflow.spring.aspect

import io.cacheflow.spring.dependency.DependencyResolver
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature

/** Service for managing cache dependencies. Extracted from CacheFlowAspect to reduce complexity. */
class DependencyManager(private val dependencyResolver: DependencyResolver) {

    /**
     * Tracks dependencies for a cache key based on the dependsOn parameter names.
     *
     * @param cacheKey The cache key to track dependencies for
     * @param dependsOn Array of parameter names that this cache depends on
     * @param joinPoint The join point containing method parameters
     */
    fun trackDependencies(
            cacheKey: String,
            dependsOn: Array<String>,
            joinPoint: ProceedingJoinPoint
    ) {
        if (dependsOn.isEmpty()) return

        val method = joinPoint.signature as MethodSignature
        val parameterNames = method.parameterNames

        dependsOn.forEach { paramName ->
            val paramIndex = parameterNames.indexOf(paramName)
            if (paramIndex >= 0 && paramIndex < joinPoint.args.size) {
                val paramValue = joinPoint.args[paramIndex]
                val dependencyKey = buildDependencyKey(paramName, paramValue)
                dependencyResolver.trackDependency(cacheKey, dependencyKey)
            }
        }
    }

    /**
     * Evicts a cache key and all its dependent caches.
     *
     * @param key The cache key to evict
     * @param cacheService The cache service to use for eviction
     */
    fun evictWithDependencies(
            key: String,
            cacheService: io.cacheflow.spring.service.CacheFlowService
    ) {
        // Evict the main key
        cacheService.evict(key)

        // Get and evict all dependent caches
        val dependentKeys = dependencyResolver.invalidateDependentCaches(key)
dependentKeys.forEach { dependentKey -> cacheService.evict(dependentKey) }


        // Clear dependencies for the evicted key
        dependencyResolver.clearDependencies(key)
    }

    private fun buildDependencyKey(paramName: String, paramValue: Any?): String {
        val prefix = "$paramName:"
        return when (paramValue) {
            null -> "${prefix}null"
            is String, is Number, is Boolean -> createDependencyKey(prefix, paramValue)
            else -> "$prefix${paramValue.hashCode()}"
        }
    }

    private fun createDependencyKey(prefix: String, value: Any): String = "$prefix$value"
}
