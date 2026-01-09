package io.cacheflow.spring.aspect

import io.cacheflow.spring.annotation.CacheFlowComposition
import io.cacheflow.spring.annotation.CacheFlowFragment
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.fragment.FragmentCacheService
import io.cacheflow.spring.fragment.FragmentTagManager
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.SimpleEvaluationContext
import org.springframework.stereotype.Component

/**
 * AOP Aspect for handling fragment caching annotations.
 *
 * This aspect provides support for caching fragments and composing them in the Russian Doll caching
 * pattern.
 */
@Aspect
@Component
class FragmentCacheAspect(
    private val fragmentCacheService: FragmentCacheService,
    private val dependencyResolver: DependencyResolver,
    private val tagManager: FragmentTagManager
) {

    private val expressionParser = SpelExpressionParser()
    private val defaultTtlSeconds = 3_600L

    /**
     * Around advice for CacheFlowFragment annotation.
     *
     * @param joinPoint The join point
     * @return The result of the method execution or cached fragment
     */
    @Around("@annotation(io.cacheflow.spring.annotation.CacheFlowFragment)")
    fun aroundFragment(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val fragment =
            method.getAnnotation(CacheFlowFragment::class.java) ?: return joinPoint.proceed()

        return processFragment(joinPoint, fragment)
    }

    /**
     * Around advice for CacheFlowComposition annotation.
     *
     * @param joinPoint The join point
     * @return The result of the method execution or cached composition
     */
    @Around("@annotation(io.cacheflow.spring.annotation.CacheFlowComposition)")
    fun aroundComposition(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val composition =
            method.getAnnotation(CacheFlowComposition::class.java) ?: return joinPoint.proceed()

        return processComposition(joinPoint, composition)
    }

    private fun processFragment(joinPoint: ProceedingJoinPoint, fragment: CacheFlowFragment): Any? {
        // Generate cache key
        val key = buildCacheKeyFromExpression(fragment.key, joinPoint)
        if (key.isBlank()) {
            return joinPoint.proceed()
        }

        // Track dependencies if specified
        registerFragmentDependencies(key, fragment.dependsOn, joinPoint)

        // Check cache first or execute and cache result
        return fragmentCacheService.getFragment(key)
            ?: executeAndCacheFragment(joinPoint, fragment, key)
    }

    private fun executeAndCacheFragment(
        joinPoint: ProceedingJoinPoint,
        fragment: CacheFlowFragment,
        key: String
    ): Any? {
        val result = joinPoint.proceed()
        if (result is String) {
            val ttl = if (fragment.ttl > 0) fragment.ttl else defaultTtlSeconds
            fragmentCacheService.cacheFragment(key, result, ttl)

            // Add tags if specified
            fragment.tags.forEach { tag ->
                val evaluatedTag = evaluateFragmentKeyExpression(tag, joinPoint)
                tagManager.addFragmentTag(key, evaluatedTag)
            }
        }
        return result
    }

    private fun processComposition(
        joinPoint: ProceedingJoinPoint,
        composition: CacheFlowComposition
    ): Any? {
        // Generate cache key
        val key = buildCacheKeyFromExpression(composition.key, joinPoint)
        if (key.isBlank()) {
            return joinPoint.proceed()
        }

        // Try to compose fragments if template and fragments are available
        val composedResult = tryComposeFragments(composition, key, joinPoint)
        return composedResult ?: joinPoint.proceed()
    }

    private fun tryComposeFragments(composition: CacheFlowComposition, key: String, joinPoint: ProceedingJoinPoint): String? {
        if (composition.template.isBlank() || composition.fragments.isEmpty()) {
            return null
        }

        // Evaluate SpEL expressions in fragment keys
        val evaluatedFragmentKeys = composition.fragments.map { fragmentKey ->
            evaluateFragmentKeyExpression(fragmentKey, joinPoint)
        }.filter { it.isNotBlank() }

        val composedResult =
            fragmentCacheService.composeFragmentsByKeys(
                composition.template,
                evaluatedFragmentKeys
            )

        return if (composedResult.isNotBlank()) {
            val ttl = if (composition.ttl > 0) composition.ttl else defaultTtlSeconds
            fragmentCacheService.cacheFragment(key, composedResult, ttl)
            composedResult
        } else {
            null
        }
    }

    private fun registerFragmentDependencies(
        fragmentKey: String,
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
                dependencyResolver.trackDependency(fragmentKey, dependencyKey)
            }
        }
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

    private fun evaluateFragmentKeyExpression(fragmentKey: String, joinPoint: ProceedingJoinPoint): String {
        if (fragmentKey.isBlank()) {
            return ""
        }

        return try {
            val context = SimpleEvaluationContext.forReadOnlyDataBinding().build()
            val method = joinPoint.signature as MethodSignature
            val parameterNames = method.parameterNames

            // Add method parameters to context
            joinPoint.args.forEachIndexed { index, arg ->
                context.setVariable(parameterNames[index], arg)
            }

            // Add method target to context
            context.setVariable("target", joinPoint.target)

            val expression = expressionParser.parseExpression(fragmentKey)
            expression.getValue(context, String::class.java) ?: ""
        } catch (e: org.springframework.expression.ParseException) {
            // Log the parsing exception for debugging but fall back to empty string
            println("FragmentCacheAspect: SpEL parse exception: ${e.message}")
            ""
        } catch (e: Exception) {
            // Log other exceptions and fall back to empty string
            println("FragmentCacheAspect: SpEL evaluation exception: ${e.message}")
            ""
        }
    }

    private fun buildCacheKeyFromExpression(
        keyExpression: String,
        joinPoint: ProceedingJoinPoint
    ): String {
        if (keyExpression.isBlank()) {
            return buildDefaultCacheKey(joinPoint)
        }

        return try {
            val context = SimpleEvaluationContext.forReadOnlyDataBinding().build()
            val method = joinPoint.signature as MethodSignature
            val parameterNames = method.parameterNames

            // Add method parameters to context
            joinPoint.args.forEachIndexed { index, arg ->
                context.setVariable(parameterNames[index], arg)
            }

            // Add method target to context
            context.setVariable("target", joinPoint.target)

            val expression = expressionParser.parseExpression(keyExpression)
            expression.getValue(context, String::class.java) ?: buildDefaultCacheKey(joinPoint)
        } catch (e: org.springframework.expression.ParseException) {
            // Log the parsing exception for debugging but fall back to default key generation
            println("Failed to parse fragment cache key expression '$keyExpression': ${e.message}")
            buildDefaultCacheKey(joinPoint)
        } catch (e: org.springframework.expression.EvaluationException) {
            // Log the evaluation exception for debugging but fall back to default key generation
            println(
                "Failed to evaluate fragment cache key expression '$keyExpression': ${e.message}"
            )
            buildDefaultCacheKey(joinPoint)
        }
    }

    private fun buildDefaultCacheKey(joinPoint: ProceedingJoinPoint): String {
        val method = joinPoint.signature as MethodSignature
        val className = method.declaringType.simpleName
        val methodName = method.name
        val args = joinPoint.args.joinToString(",") { it?.toString() ?: "null" }
        return "$className.$methodName($args)"
    }
}
