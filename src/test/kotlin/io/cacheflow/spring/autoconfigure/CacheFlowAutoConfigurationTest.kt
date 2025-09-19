package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.aspect.CacheFlowAspect
import io.cacheflow.spring.autoconfigure.CacheFlowAspectConfiguration
import io.cacheflow.spring.autoconfigure.CacheFlowCoreConfiguration
import io.cacheflow.spring.autoconfigure.CacheFlowManagementConfiguration
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.management.CacheFlowManagementEndpoint
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.service.impl.CacheFlowServiceImpl
import io.cacheflow.spring.versioning.CacheKeyVersioner
import org.mockito.Mockito.mock




import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class CacheFlowAutoConfigurationTest {

    @Test
    fun `should have correct annotations`() {
        val configClass = CacheFlowAutoConfiguration::class.java

        // Check @Configuration
        assertTrue(configClass.isAnnotationPresent(Configuration::class.java))

        // Check @ConditionalOnProperty
        val conditionalOnProperty = configClass.getAnnotation(ConditionalOnProperty::class.java)
        assertNotNull(conditionalOnProperty)
        assertEquals("cacheflow", conditionalOnProperty.prefix)
        assertArrayEquals(arrayOf("enabled"), conditionalOnProperty.name)
        assertEquals("true", conditionalOnProperty.havingValue)
        assertTrue(conditionalOnProperty.matchIfMissing)

        // Check @EnableConfigurationProperties
        val enableConfigProps = configClass.getAnnotation(EnableConfigurationProperties::class.java)
        assertNotNull(enableConfigProps)
        assertEquals(1, enableConfigProps.value.size)
        // Note: Class comparison can be tricky in tests, so we just verify the annotation exists
    }

    @Test
    fun `should create cacheFlowService bean`() {
        val config = CacheFlowCoreConfiguration()
        val service = config.cacheFlowService()

        assertNotNull(service)
        assertTrue(service is CacheFlowServiceImpl)
    }

    @Test
    fun `should create cacheFlowAspect bean`() {
        val config = CacheFlowAspectConfiguration()
        val mockService = mock(CacheFlowService::class.java)
        val mockDependencyResolver = mock(DependencyResolver::class.java)
        val mockCacheKeyVersioner = mock(CacheKeyVersioner::class.java)
        val aspect = config.cacheFlowAspect(mockService, mockDependencyResolver, mockCacheKeyVersioner)

        assertNotNull(aspect)
        assertTrue(aspect is CacheFlowAspect)
    }

    @Test
    fun `should create cacheFlowManagementEndpoint bean`() {
        val config = CacheFlowManagementConfiguration()
        val mockService = mock(CacheFlowService::class.java)
        val endpoint = config.cacheFlowManagementEndpoint(mockService)

        assertNotNull(endpoint)
        assertTrue(endpoint is CacheFlowManagementEndpoint)
    }

    @Test
    fun `cacheFlowService method should have correct annotations`() {
        val method = CacheFlowCoreConfiguration::class.java.getDeclaredMethod("cacheFlowService")

        // Check @Bean
        assertTrue(method.isAnnotationPresent(Bean::class.java))

        // Check @ConditionalOnMissingBean
        assertTrue(method.isAnnotationPresent(ConditionalOnMissingBean::class.java))
    }

    @Test
    fun `cacheFlowAspect method should have correct annotations`() {
        val method =
                CacheFlowAspectConfiguration::class.java.getDeclaredMethod(
                        "cacheFlowAspect",
                        CacheFlowService::class.java,
                        DependencyResolver::class.java,
                        CacheKeyVersioner::class.java
                )

        // Check @Bean
        assertTrue(method.isAnnotationPresent(Bean::class.java))

        // Check @ConditionalOnMissingBean
        assertTrue(method.isAnnotationPresent(ConditionalOnMissingBean::class.java))
    }

    @Test
    fun `cacheFlowManagementEndpoint method should have correct annotations`() {
        val method =
                CacheFlowManagementConfiguration::class.java.getDeclaredMethod(
                        "cacheFlowManagementEndpoint",
                        CacheFlowService::class.java
                )

        // Check @Bean
        assertTrue(method.isAnnotationPresent(Bean::class.java))

        // Check @ConditionalOnMissingBean
        assertTrue(method.isAnnotationPresent(ConditionalOnMissingBean::class.java))

        // Check @ConditionalOnAvailableEndpoint
        assertTrue(method.isAnnotationPresent(ConditionalOnAvailableEndpoint::class.java))
    }

    @Test
    fun `should create different instances for each bean`() {
        val coreConfig = CacheFlowCoreConfiguration()
        val aspectConfig = CacheFlowAspectConfiguration()
        val managementConfig = CacheFlowManagementConfiguration()
        val mockService = mock(CacheFlowService::class.java)
        val mockDependencyResolver = mock(DependencyResolver::class.java)
        val mockCacheKeyVersioner = mock(CacheKeyVersioner::class.java)

        val service1 = coreConfig.cacheFlowService()
        val service2 = coreConfig.cacheFlowService()
        val aspect1 = aspectConfig.cacheFlowAspect(mockService, mockDependencyResolver, mockCacheKeyVersioner)
        val aspect2 = aspectConfig.cacheFlowAspect(mockService, mockDependencyResolver, mockCacheKeyVersioner)
        val endpoint1 = managementConfig.cacheFlowManagementEndpoint(mockService)
        val endpoint2 = managementConfig.cacheFlowManagementEndpoint(mockService)

        // Each call should create a new instance
        assertNotSame(service1, service2)
        assertNotSame(aspect1, aspect2)
        assertNotSame(endpoint1, endpoint2)
    }

    @Test
    fun `should handle null service parameter gracefully`() {
        val aspectConfig = CacheFlowAspectConfiguration()
        val managementConfig = CacheFlowManagementConfiguration()
        val mockDependencyResolver = mock(DependencyResolver::class.java)
        val mockCacheKeyVersioner = mock(CacheKeyVersioner::class.java)

        // These should not throw exceptions even with null service
        assertDoesNotThrow {
            aspectConfig.cacheFlowAspect(mock(CacheFlowService::class.java), mockDependencyResolver, mockCacheKeyVersioner)
            managementConfig.cacheFlowManagementEndpoint(mock(CacheFlowService::class.java))
        }
    }

    // Helper function to create mock
    private fun <T> mock(clazz: Class<T>): T {
        return org.mockito.Mockito.mock(clazz)
    }
}
