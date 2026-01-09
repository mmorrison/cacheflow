package io.cacheflow.spring.aspect

import io.cacheflow.spring.annotation.CacheFlow
import io.cacheflow.spring.annotation.CacheFlowCached
import io.cacheflow.spring.annotation.CacheFlowEvict
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.versioning.CacheKeyVersioner
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

/** AOP Aspect for handling CacheFlow annotations. */
@Aspect
@Component
class CacheFlowAspect(
    private val cacheService: CacheFlowService,
    private val dependencyResolver: DependencyResolver,
    private val cacheKeyVersioner: CacheKeyVersioner
) {
    private val cacheKeyGenerator = CacheKeyGenerator(cacheKeyVersioner)
    private val dependencyManager = DependencyManager(dependencyResolver)
    private val defaultTtlSeconds = 3_600L

    /**
     * Around advice for CacheFlow annotation.
     *
     * @param joinPoint The join point
     * @return The result of the method execution or cached value
     */
    @Around("@annotation(io.cacheflow.spring.annotation.CacheFlow)")
    fun aroundCache(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val cached = method.getAnnotation(CacheFlow::class.java) ?: return joinPoint.proceed()

        return processCacheFlow(joinPoint, cached)
    }

    private fun processCacheFlow(joinPoint: ProceedingJoinPoint, cached: CacheFlow): Any? {
        // Get configuration - use config registry if config name is provided
        val config = cached.takeIf { it.config.isNotBlank() } ?: cached
        // TODO: Inject CacheFlowConfigRegistry to get complex configuration
        // For now, use the annotation parameters directly

        // Generate cache key
        val baseKey = cacheKeyGenerator.generateCacheKeyFromExpression(config.key, joinPoint)
        if (baseKey.isBlank()) return joinPoint.proceed()

        // Apply versioning if enabled
        val key = if (config.versioned) {
            cacheKeyGenerator.generateVersionedKey(baseKey, config, joinPoint)
        } else {
            baseKey
        }

        // Track dependencies if specified
        dependencyManager.trackDependencies(key, config.dependsOn, joinPoint)

        // Check cache first
        val cachedValue = cacheService.get(key)
        return cachedValue ?: executeAndCache(joinPoint, key, config)
    }

    private fun executeAndCache(joinPoint: ProceedingJoinPoint, key: String, cached: CacheFlow): Any? {
        val result = joinPoint.proceed() ?: return null
        val ttl = if (cached.ttl > 0) cached.ttl else defaultTtlSeconds
        cacheService.put(key, result, ttl)
        return result
    }

    /**
     * Around advice for CacheFlowCached annotation.
     *
     * @param joinPoint The join point
     * @return The result of the method execution or cached value
     */
    @Around("@annotation(io.cacheflow.spring.annotation.CacheFlowCached)")
    fun aroundCached(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val cached = method.getAnnotation(CacheFlowCached::class.java) ?: return joinPoint.proceed()

        return processCacheFlowCached(joinPoint, cached)
    }

    private fun processCacheFlowCached(joinPoint: ProceedingJoinPoint, cached: CacheFlowCached): Any? {
        // Get configuration - use config registry if config name is provided
        val config = cached.takeIf { it.config.isNotBlank() } ?: cached
        // TODO: Inject CacheFlowConfigRegistry to get complex configuration
        // For now, use the annotation parameters directly

        // Generate cache key
        val baseKey = cacheKeyGenerator.generateCacheKeyFromExpression(config.key, joinPoint)
        if (baseKey.isBlank()) return joinPoint.proceed()

        // Apply versioning if enabled
        val key = if (config.versioned) {
            cacheKeyGenerator.generateVersionedKey(baseKey, config, joinPoint)
        } else {
            baseKey
        }

        // Track dependencies if specified
        dependencyManager.trackDependencies(key, config.dependsOn, joinPoint)

        // Check cache first
        val cachedValue = cacheService.get(key)
        return cachedValue ?: executeAndCacheCached(joinPoint, key, config)
    }

    private fun executeAndCacheCached(joinPoint: ProceedingJoinPoint, key: String, cached: CacheFlowCached): Any? {
        val result = joinPoint.proceed() ?: return null
        val ttl = if (cached.ttl > 0) cached.ttl else defaultTtlSeconds
        cacheService.put(key, result, ttl)
        return result
    }

    /**
     * Around advice for CacheFlowEvict annotation.
     *
     * @param joinPoint The join point
     * @return The result of the method execution
     */
    @Around("@annotation(io.cacheflow.spring.annotation.CacheFlowEvict)")
    fun aroundEvict(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val evict = method.getAnnotation(CacheFlowEvict::class.java) ?: return joinPoint.proceed()

        // Execute method first if beforeInvocation is false
        val result =
            if (evict.beforeInvocation) {
                evictCacheEntries(evict, joinPoint)
                joinPoint.proceed()
            } else {
                val methodResult = joinPoint.proceed()
                evictCacheEntries(evict, joinPoint)
                methodResult
            }

        return result
    }

    private fun evictCacheEntries(evict: CacheFlowEvict, joinPoint: ProceedingJoinPoint) {
        when {
            evict.allEntries -> {
                cacheService.evictAll()
            }
            evict.key.isNotBlank() -> {
                val key = cacheKeyGenerator.generateCacheKeyFromExpression(evict.key, joinPoint)
                if (key.isNotBlank()) {
                    dependencyManager.evictWithDependencies(key, cacheService)
                }
            }
            evict.tags.isNotEmpty() -> {
                cacheService.evictByTags(*evict.tags)
            }
        }
    }
}
