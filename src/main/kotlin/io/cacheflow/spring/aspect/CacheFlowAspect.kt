package io.cacheflow.spring.aspect

import io.cacheflow.spring.annotation.CacheFlow
import io.cacheflow.spring.annotation.CacheFlowCached
import io.cacheflow.spring.annotation.CacheFlowEvict
import io.cacheflow.spring.service.CacheFlowService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

/** AOP Aspect for handling CacheFlow annotations. */
@Aspect
@Component
class CacheFlowAspect(
    private val cacheService: CacheFlowService
) {
    private val expressionParser = SpelExpressionParser()
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
        // Generate cache key
        val key = generateCacheKeyFromExpression(cached.key, joinPoint)
        if (key.isBlank()) return joinPoint.proceed()

        // Check cache first
        val cachedValue = cacheService.get(key)
        return cachedValue ?: executeAndCache(joinPoint, key, cached)
    }

    private fun executeAndCache(joinPoint: ProceedingJoinPoint, key: String, cached: CacheFlow): Any? {
        val result = joinPoint.proceed()
        if (result != null) {
            val ttl = if (cached.ttl > 0) cached.ttl else defaultTtlSeconds
            cacheService.put(key, result, ttl)
        }
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
        // Generate cache key
        val key = generateCacheKeyFromExpression(cached.key, joinPoint)
        if (key.isBlank()) return joinPoint.proceed()

        // Check cache first
        val cachedValue = cacheService.get(key)
        return cachedValue ?: executeAndCacheCached(joinPoint, key, cached)
    }

    private fun executeAndCacheCached(joinPoint: ProceedingJoinPoint, key: String, cached: CacheFlowCached): Any? {
        val result = joinPoint.proceed()
        if (result != null) {
            val ttl = if (cached.ttl > 0) cached.ttl else defaultTtlSeconds
            cacheService.put(key, result, ttl)
        }
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

    private fun generateCacheKeyFromExpression(keyExpression: String, joinPoint: ProceedingJoinPoint): String {
        if (keyExpression.isBlank()) {
            return generateDefaultCacheKey(joinPoint)
        }

        return try {
            val context = StandardEvaluationContext()
            val method = joinPoint.signature as MethodSignature
            val parameterNames = method.parameterNames

            // Add method parameters to context
            joinPoint.args.forEachIndexed { index, arg ->
                context.setVariable(parameterNames[index], arg)
            }

            // Add method target to context
            context.setVariable("target", joinPoint.target)

            val expression = expressionParser.parseExpression(keyExpression)
            expression.getValue(context, String::class.java) ?: generateDefaultCacheKey(joinPoint)
        } catch (e: org.springframework.expression.ParseException) {
            // Log the parsing exception for debugging but fall back to default key generation
            println("Failed to parse cache key expression '$keyExpression': ${e.message}")
            generateDefaultCacheKey(joinPoint)
        } catch (e: org.springframework.expression.EvaluationException) {
            // Log the evaluation exception for debugging but fall back to default key generation
            println("Failed to evaluate cache key expression '$keyExpression': ${e.message}")
            generateDefaultCacheKey(joinPoint)
        }
    }

    private fun generateDefaultCacheKey(joinPoint: ProceedingJoinPoint): String {
        val method = joinPoint.signature as MethodSignature
        val className = method.declaringType.simpleName
        val methodName = method.name
        val args = joinPoint.args.joinToString(",") { it?.toString() ?: "null" }
        return "$className.$methodName($args)"
    }

    private fun evictCacheEntries(evict: CacheFlowEvict, joinPoint: ProceedingJoinPoint) {
        when {
            evict.allEntries -> {
                cacheService.evictAll()
            }
            evict.key.isNotBlank() -> {
                val key = generateCacheKeyFromExpression(evict.key, joinPoint)
                if (key.isNotBlank()) {
                    cacheService.evict(key)
                }
            }
            evict.tags.isNotEmpty() -> {
                cacheService.evictByTags(*evict.tags)
            }
        }
    }
}
