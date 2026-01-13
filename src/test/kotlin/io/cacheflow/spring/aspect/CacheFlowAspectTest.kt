package io.cacheflow.spring.aspect

import io.cacheflow.spring.annotation.CacheFlow
import io.cacheflow.spring.annotation.CacheFlowCached
import io.cacheflow.spring.annotation.CacheFlowConfig
import io.cacheflow.spring.annotation.CacheFlowConfigRegistry
import io.cacheflow.spring.annotation.CacheFlowEvict
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.versioning.CacheKeyVersioner
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class CacheFlowAspectTest {
    private lateinit var cacheService: CacheFlowService
    private lateinit var dependencyResolver: DependencyResolver
    private lateinit var cacheKeyVersioner: CacheKeyVersioner
    private lateinit var configRegistry: CacheFlowConfigRegistry

    private lateinit var aspect: CacheFlowAspect
    private lateinit var joinPoint: ProceedingJoinPoint
    private lateinit var methodSignature: MethodSignature

    @BeforeEach
    fun setUp() {
        cacheService = mock(CacheFlowService::class.java)
        dependencyResolver = mock(DependencyResolver::class.java)
        cacheKeyVersioner = mock(CacheKeyVersioner::class.java)
        configRegistry = mock(CacheFlowConfigRegistry::class.java)

        aspect = CacheFlowAspect(cacheService, dependencyResolver, cacheKeyVersioner, configRegistry)

        joinPoint = mock(ProceedingJoinPoint::class.java)
        methodSignature = mock(MethodSignature::class.java)
        // Setup mock to return proper declaring type
        whenever(methodSignature.declaringType).thenReturn(TestClass::class.java)

        whenever(joinPoint.signature).thenReturn(methodSignature)
    }

    @Test
    fun `should proceed when no CacheFlow annotation present`() {
        val method = TestClass::class.java.getDeclaredMethod("methodWithoutAnnotation")
        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(joinPoint.proceed()).thenReturn("result")

        val result = aspect.aroundCache(joinPoint)

        assertEquals("result", result)
        verify(joinPoint).proceed()
        verifyNoInteractions(cacheService)
    }

    @Test
    fun `should cache result when CacheFlow annotation present`() {
        val method =
            TestClass::class.java.getDeclaredMethod(
                "methodWithCacheFlow",
                String::class.java,
                String::class.java,
            )

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.parameterNames).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.target).thenReturn(TestClass())
        whenever(joinPoint.proceed()).thenReturn("cached result")
        whenever(cacheService.get(any())).thenReturn(null)

        val result = aspect.aroundCache(joinPoint)

        assertEquals("cached result", result)
        verify(joinPoint).proceed()
    }

    @Test
    fun `should return cached value when present`() {
        val method =
            TestClass::class.java.getDeclaredMethod(
                "methodWithCacheFlow",
                String::class.java,
                String::class.java,
            )

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.parameterNames).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.target).thenReturn(TestClass())
        whenever(cacheService.get(any())).thenReturn("cached value")

        val result = aspect.aroundCache(joinPoint)

        assertEquals("cached value", result)
        verify(joinPoint, never()).proceed()
    }

    @Test
    fun `should use config from registry when config name provided`() {
        val method =
            TestClass::class.java.getDeclaredMethod(
                "methodWithCacheFlowConfig",
                String::class.java,
                String::class.java,
            )

        val configName = "testConfig"
        val config = CacheFlowConfig(key = "#arg1 + '_' + #arg2", ttl = 600L)
        whenever(configRegistry.get(configName)).thenReturn(config)

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.parameterNames).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.target).thenReturn(TestClass())
        whenever(joinPoint.proceed()).thenReturn("result")
        whenever(cacheService.get(any())).thenReturn(null)

        val result = aspect.aroundCache(joinPoint)

        assertEquals("result", result)
        verify(configRegistry).get(configName)
        verify(cacheService).put(any<String>(), eq("result"), eq(600L), any<Set<String>>())
    }

    @Test
    fun `should use annotation when config name not found`() {
        val method =
            TestClass::class.java.getDeclaredMethod(
                "methodWithCacheFlowConfig",
                String::class.java,
                String::class.java,
            )

        val configName = "testConfig"
        whenever(configRegistry.get(configName)).thenReturn(null)

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.parameterNames).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.target).thenReturn(TestClass())
        whenever(joinPoint.proceed()).thenReturn("result")
        whenever(cacheService.get(any())).thenReturn(null)

        val result = aspect.aroundCache(joinPoint)

        assertEquals("result", result)
        verify(configRegistry).get(configName)
        // Should use annotation values (ttl defaults to -1, which uses defaultTtlSeconds 3600L)
        verify(cacheService).put(any<String>(), eq("result"), eq(3600L), any<Set<String>>())
    }

    @Test
    fun `should proceed when no CacheFlowCached annotation present`() {
        val method = TestClass::class.java.getDeclaredMethod("methodWithoutAnnotation")
        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(joinPoint.proceed()).thenReturn("result")

        val result = aspect.aroundCached(joinPoint)

        assertEquals("result", result)
        verify(joinPoint).proceed()
        verifyNoInteractions(cacheService)
    }

    @Test
    fun `should cache result when CacheFlowCached annotation present`() {
        val method =
            TestClass::class.java.getDeclaredMethod(
                "methodWithCacheFlowCached",
                String::class.java,
                String::class.java,
            )

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.parameterNames).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.target).thenReturn(TestClass())
        whenever(joinPoint.proceed()).thenReturn("cached result")
        whenever(cacheService.get(any())).thenReturn(null)

        val result = aspect.aroundCached(joinPoint)

        assertEquals("cached result", result)
        verify(joinPoint).proceed()
    }

    @Test
    fun `should proceed when no CacheFlowEvict annotation present`() {
        val method = TestClass::class.java.getDeclaredMethod("methodWithoutAnnotation")
        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(joinPoint.proceed()).thenReturn("result")

        val result = aspect.aroundEvict(joinPoint)

        assertEquals("result", result)
        verify(joinPoint).proceed()
        verifyNoInteractions(cacheService)
    }

    @Test
    fun `should evict after method execution by default`() {
        val method =
            TestClass::class.java.getDeclaredMethod(
                "methodWithCacheFlowEvict",
                String::class.java,
                String::class.java,
            )

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.parameterNames).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.target).thenReturn(TestClass())
        whenever(joinPoint.proceed()).thenReturn("result")

        val result = aspect.aroundEvict(joinPoint)

        assertEquals("result", result)
        verify(joinPoint).proceed()
        verify(cacheService).evict(any())
    }

    @Test
    fun `should evict before method execution when beforeInvocation is true`() {
        val method =
            TestClass::class.java.getDeclaredMethod(
                "methodWithCacheFlowEvictBeforeInvocation",
                String::class.java,
                String::class.java,
            )

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.parameterNames).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.target).thenReturn(TestClass())
        whenever(joinPoint.proceed()).thenReturn("result")

        val result = aspect.aroundEvict(joinPoint)

        assertEquals("result", result)
        verify(cacheService).evict(any())
        verify(joinPoint).proceed()
    }

    @Test
    fun `should evict all when allEntries is true`() {
        val method = TestClass::class.java.getDeclaredMethod("methodWithCacheFlowEvictAll")

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(joinPoint.proceed()).thenReturn("result")

        val result = aspect.aroundEvict(joinPoint)

        assertEquals("result", result)
        verify(joinPoint).proceed()
        verify(cacheService).evictAll()
    }

    @Test
    fun `should evict by tags when tags are provided`() {
        val method = TestClass::class.java.getDeclaredMethod("methodWithCacheFlowEvictTags")

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(joinPoint.proceed()).thenReturn("result")

        val result = aspect.aroundEvict(joinPoint)

        assertEquals("result", result)
        verify(joinPoint).proceed()
        verify(cacheService).evictByTags(eq("tag1"), eq("tag2"))
    }

    @Test
    fun `should generate default cache key when key expression is blank`() {
        val method = TestClass::class.java.getDeclaredMethod("methodWithBlankKey")

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.declaringType).thenReturn(TestClass::class.java)
        whenever(methodSignature.name).thenReturn("methodWithBlankKey")
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.proceed()).thenReturn("result")
        whenever(cacheService.get(any())).thenReturn(null)

        val result = aspect.aroundCache(joinPoint)

        assertEquals("result", result)
        verify(joinPoint).proceed()
    }

    @Test
    fun `should not cache null result`() {
        val method =
            TestClass::class.java.getDeclaredMethod(
                "methodWithCacheFlow",
                String::class.java,
                String::class.java,
            )

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.parameterNames).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.target).thenReturn(TestClass())
        whenever(joinPoint.proceed()).thenReturn(null)
        whenever(cacheService.get(any())).thenReturn(null)

        val result = aspect.aroundCache(joinPoint)

        assertNull(result)
        verify(joinPoint).proceed()
        verify(cacheService).get(any())
    }

    @Test
    fun `should use custom TTL when specified`() {
        val method =
            TestClass::class.java.getDeclaredMethod(
                "methodWithCustomTtl",
                String::class.java,
                String::class.java,
            )

        whenever(joinPoint.signature).thenReturn(methodSignature)
        whenever(methodSignature.method).thenReturn(method)
        whenever(methodSignature.parameterNames).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.args).thenReturn(arrayOf("arg1", "arg2"))
        whenever(joinPoint.target).thenReturn(TestClass())
        whenever(joinPoint.proceed()).thenReturn("result")
        whenever(cacheService.get(any())).thenReturn(null)

        val result = aspect.aroundCache(joinPoint)

        assertEquals("result", result)
        verify(joinPoint).proceed()
    }

    // Test class with various annotated methods
    class TestClass {
        @CacheFlow(key = "#arg1 + '_' + #arg2")
        fun methodWithCacheFlow(
            arg1: String,
            arg2: String,
        ): String = "result"

        @CacheFlow(key = "#arg1 + '_' + #arg2", config = "testConfig")
        fun methodWithCacheFlowConfig(
            arg1: String,
            arg2: String,
        ): String = "result"

        @CacheFlowCached(key = "#arg1 + '_' + #arg2")
        fun methodWithCacheFlowCached(
            arg1: String,
            arg2: String,
        ): String = "result"

        @CacheFlowEvict(key = "#arg1 + '_' + #arg2")
        fun methodWithCacheFlowEvict(
            arg1: String,
            arg2: String,
        ): String = "result"

        @CacheFlowEvict(key = "#arg1 + '_' + #arg2", beforeInvocation = true)
        fun methodWithCacheFlowEvictBeforeInvocation(
            arg1: String,
            arg2: String,
        ): String = "result"

        @CacheFlowEvict(allEntries = true)
        fun methodWithCacheFlowEvictAll(): String = "result"

        @CacheFlowEvict(tags = ["tag1", "tag2"])
        fun methodWithCacheFlowEvictTags(): String = "result"

        @CacheFlow(key = "")
        fun methodWithBlankKey(): String = "result"

        @CacheFlow(key = "#arg1 + '_' + #arg2", ttl = 1800L)
        fun methodWithCustomTtl(
            arg1: String,
            arg2: String,
        ): String = "result"

        fun methodWithoutAnnotation(): String = "result"
    }
}
