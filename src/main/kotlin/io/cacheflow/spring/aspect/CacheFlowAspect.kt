package io.cacheflow.spring.aspect

import io.cacheflow.spring.annotation.*
import io.cacheflow.spring.service.CacheFlowService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.*
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.ApplicationContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

/** AOP Aspect for handling CacheFlow annotations. */
@Aspect
@Component
class CacheFlowAspect(
        private val cacheService: CacheFlowService,
        private val applicationContext: ApplicationContext
) {
    private val expressionParser = SpelExpressionParser()

    @Around("@annotation(com.yourcompany.cacheflow.annotation.CacheFlow)")
    fun aroundCache(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val cached = method.getAnnotation(CacheFlow::class.java) ?: return joinPoint.proceed()

        // Generate cache key
        val key = generateCacheKey(cached.key, joinPoint)
        if (key.isBlank()) return joinPoint.proceed()

        // Check cache first
        val cachedValue = cacheService.get(key)
        if (cachedValue != null) {
            return cachedValue
        }

        // Execute method and cache result
        val result = joinPoint.proceed()
        if (result != null) {
            val ttl = if (cached.ttl > 0) cached.ttl else 3600L
            cacheService.put(key, result, ttl)
        }

        return result
    }

    @Around("@annotation(com.yourcompany.cacheflow.annotation.CacheFlowCached)")
    fun aroundCached(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val cached = method.getAnnotation(CacheFlowCached::class.java) ?: return joinPoint.proceed()

        // Generate cache key
        val key = generateCacheKey(cached.key, joinPoint)
        if (key.isBlank()) return joinPoint.proceed()

        // Check cache first
        val cachedValue = cacheService.get(key)
        if (cachedValue != null) {
            return cachedValue
        }

        // Execute method and cache result
        val result = joinPoint.proceed()
        if (result != null) {
            val ttl = if (cached.ttl > 0) cached.ttl else 3600L
            cacheService.put(key, result, ttl)
        }

        return result
    }

    @Around("@annotation(com.yourcompany.cacheflow.annotation.CacheFlowEvict)")
    fun aroundEvict(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val evict = method.getAnnotation(CacheFlowEvict::class.java) ?: return joinPoint.proceed()

        // Execute method first if beforeInvocation is false
        val result =
                if (evict.beforeInvocation) {
                    performEviction(evict, joinPoint)
                    joinPoint.proceed()
                } else {
                    val methodResult = joinPoint.proceed()
                    performEviction(evict, joinPoint)
                    methodResult
                }

        return result
    }

    private fun generateCacheKey(keyExpression: String, joinPoint: ProceedingJoinPoint): String {
        if (keyExpression.isBlank()) {
            return generateDefaultKey(joinPoint)
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
            expression.getValue(context, String::class.java) ?: generateDefaultKey(joinPoint)
        } catch (e: Exception) {
            generateDefaultKey(joinPoint)
        }
    }

    private fun generateDefaultKey(joinPoint: ProceedingJoinPoint): String {
        val method = joinPoint.signature as MethodSignature
        val className = method.declaringType.simpleName
        val methodName = method.name
        val args = joinPoint.args.joinToString(",") { it?.toString() ?: "null" }
        return "$className.$methodName($args)"
    }

    private fun performEviction(evict: CacheFlowEvict, joinPoint: ProceedingJoinPoint) {
        when {
            evict.allEntries -> {
                cacheService.evictAll()
            }
            evict.key.isNotBlank() -> {
                val key = generateCacheKey(evict.key, joinPoint)
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
