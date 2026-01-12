package io.cacheflow.spring.aspect

import io.cacheflow.spring.annotation.CacheFlowUpdate
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import org.springframework.stereotype.Component

class TouchPropagationAspectTest {
    private lateinit var parentToucher: ParentToucher
    private lateinit var aspect: TouchPropagationAspect
    private lateinit var testService: TestService

    @BeforeEach
    fun setUp() {
        parentToucher = mock()
        aspect = TouchPropagationAspect(parentToucher)
        
        // Create proxy for testing aspect
        val target = TestServiceImpl()
        val factory = AspectJProxyFactory(target)
        factory.isProxyTargetClass = true // Force CGLIB/Target class proxy to match method annotations on implementation
        factory.addAspect(aspect)
        testService = factory.getProxy()
    }

    @Test
    fun `should touch parent when condition matches`() {
        // When
        testService.updateChild("child-1", "parent-1")

        // Then
        verify(parentToucher).touch("organization", "parent-1")
    }

    @Test
    fun `should not touch parent when condition fails`() {
        // When
        testService.updateChildCondition("child-1", "parent-1", false)

        // Then
        verify(parentToucher, never()).touch(any(), any())
    }
    
    @Test
    fun `should touch parent when condition passes`() {
        // When
        testService.updateChildCondition("child-1", "parent-1", true)

        // Then
        verify(parentToucher).touch("organization", "parent-1")
    }

    @Test
    fun `should handle missing parent ID gracefully`() {
        // When
        testService.updateChild("child-1", "")

        // Then
        verify(parentToucher, never()).touch(any(), any())
    }

    // Interface for testing AOP proxy
    interface TestService {
        fun updateChild(id: String, parentId: String)
        fun updateChildCondition(id: String, parentId: String, shouldUpdate: Boolean)
    }

    // Implementation for testing
    @Component
    open class TestServiceImpl : TestService {
        @CacheFlowUpdate(parent = "#parentId", entityType = "organization")
        override fun updateChild(id: String, parentId: String) {
            // No-op
        }
        
        @CacheFlowUpdate(parent = "#parentId", entityType = "organization", condition = "#shouldUpdate")
        override fun updateChildCondition(id: String, parentId: String, shouldUpdate: Boolean) {
            // No-op
        }
    }
}
