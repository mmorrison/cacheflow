package io.cacheflow.spring.aspect

import io.cacheflow.spring.annotation.CacheFlowConfig
import io.cacheflow.spring.versioning.CacheKeyVersioner
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.expression.EvaluationContext
import org.springframework.expression.Expression
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

/**
 * Service for generating cache keys from SpEL expressions and method parameters. Extracted from
 * CacheFlowAspect to reduce complexity.
 */
class CacheKeyGenerator(
    private val cacheKeyVersioner: CacheKeyVersioner,
) {
    private val parser: ExpressionParser = SpelExpressionParser()

    /**
     * Generates a cache key from a SpEL expression.
     *
     * @param keyExpression The SpEL expression for the cache key
     * @param joinPoint The join point containing method parameters
     * @return The generated cache key, or empty string if expression is invalid
     */
    fun generateCacheKeyFromExpression(
        keyExpression: String,
        joinPoint: ProceedingJoinPoint,
    ): String {
        if (keyExpression.isBlank()) return ""

        return try {
            val expression: Expression = parser.parseExpression(keyExpression)
            val context = buildEvaluationContext(joinPoint)
            val result = expression.getValue(context)
            result?.toString() ?: ""
        } catch (e: org.springframework.expression.ParseException) {
            // Fallback to method name and parameters if SpEL parsing fails
            // Log at debug level as this is expected behavior for invalid expressions
            buildDefaultCacheKey(joinPoint)
        } catch (e: org.springframework.expression.EvaluationException) {
            // Fallback to method name and parameters if SpEL evaluation fails
            // Log at debug level as this is expected behavior for invalid expressions
            buildDefaultCacheKey(joinPoint)
        }
    }

    /**
     * Generates a versioned cache key based on the configuration.
     *
     * @param baseKey The base cache key
     * @param config The cache configuration
     * @param joinPoint The join point
     * @return The versioned cache key
     */
    fun generateVersionedKey(
        baseKey: String,
        config: CacheFlowConfig,
        joinPoint: ProceedingJoinPoint,
    ): String {
        val method = joinPoint.signature as MethodSignature
        val parameterNames = method.parameterNames

        // Try to find the timestamp field in method parameters
        val timestampField = config.timestampField
        val paramIndex = parameterNames.indexOf(timestampField)

        return if (paramIndex >= 0 && paramIndex < joinPoint.args.size) {
            val timestampValue = joinPoint.args[paramIndex]
            cacheKeyVersioner.generateVersionedKey(baseKey, timestampValue)
        } else {
            // Fall back to using all parameters
            cacheKeyVersioner.generateVersionedKey(baseKey, joinPoint.args.toList())
        }
    }

    private fun buildEvaluationContext(joinPoint: ProceedingJoinPoint): EvaluationContext {
        val context = StandardEvaluationContext()
        val method = joinPoint.signature as MethodSignature
        val parameterNames = method.parameterNames

        // Add method parameters to context
        joinPoint.args.forEachIndexed { index, arg ->
            if (index < parameterNames.size) {
                context.setVariable(parameterNames[index], arg)
            }
        }

        // Add method name and class name
        context.setVariable("methodName", method.name)
        context.setVariable("className", method.declaringType.simpleName)

        return context
    }

    private fun buildDefaultCacheKey(joinPoint: ProceedingJoinPoint): String {
        val method = joinPoint.signature as MethodSignature
        val className = method.declaringType.simpleName
        val methodName = method.name
        val args = joinPoint.args.joinToString(",") { it?.toString() ?: "null" }
        return "$className.$methodName($args)"
    }
}
