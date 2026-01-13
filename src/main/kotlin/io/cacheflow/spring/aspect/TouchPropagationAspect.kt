package io.cacheflow.spring.aspect

import io.cacheflow.spring.annotation.CacheFlowUpdate
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.stereotype.Component

/**
 * Aspect to handle [CacheFlowUpdate] annotations.
 *
 * This aspect intercepts methods annotated with @CacheFlowUpdate and executes the
 * [ParentToucher.touch] method for the resolved parent entity.
 */
@Aspect
@Component
class TouchPropagationAspect(
    private val parentToucher: ParentToucher?,
) {
    private val logger = LoggerFactory.getLogger(TouchPropagationAspect::class.java)
    private val parser: ExpressionParser = SpelExpressionParser()
    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    @AfterReturning("@annotation(io.cacheflow.spring.annotation.CacheFlowUpdate)")
    fun handleUpdate(joinPoint: JoinPoint) {
        if (parentToucher == null) {
            logger.debug("No ParentToucher bean found. Skipping @CacheFlowUpdate processing.")
            return
        }

        val signature = joinPoint.signature as MethodSignature
        var method = signature.method
        var annotation = method.getAnnotation(CacheFlowUpdate::class.java)

        // If annotation is not on the interface method, check the implementation class
        if (annotation == null && joinPoint.target != null) {
            try {
                val targetMethod =
                    joinPoint.target.javaClass.getMethod(method.name, *method.parameterTypes)
                annotation = targetMethod.getAnnotation(CacheFlowUpdate::class.java)
                method = targetMethod // Use the target method for context evaluation
            } catch (e: NoSuchMethodException) {
                // Ignore, keep original method
            }
        }

        if (annotation == null) return

        try {
            val context =
                MethodBasedEvaluationContext(
                    joinPoint.target,
                    method,
                    joinPoint.args,
                    parameterNameDiscoverer,
                )

            // Check condition if present
            if (annotation.condition.isNotBlank()) {
                val conditionMet =
                    parser.parseExpression(annotation.condition).getValue(context, Boolean::class.java)
                if (conditionMet != true) return
            }

            // Resolve parent ID
            val parentId =
                parser.parseExpression(annotation.parent).getValue(context, String::class.java)

            if (!parentId.isNullOrBlank()) {
                parentToucher.touch(annotation.entityType, parentId)
            }
        } catch (e: Exception) {
            logger.error("Error processing @CacheFlowUpdate", e)
        }
    }
}
