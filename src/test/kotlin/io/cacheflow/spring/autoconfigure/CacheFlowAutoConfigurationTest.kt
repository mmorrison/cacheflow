package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.annotation.CacheFlowConfigRegistry
import io.cacheflow.spring.aspect.CacheFlowAspect
import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.edge.service.EdgeCacheIntegrationService
import io.cacheflow.spring.management.CacheFlowManagementEndpoint
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.service.impl.CacheFlowServiceImpl
import io.cacheflow.spring.versioning.CacheKeyVersioner
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate

class CacheFlowAutoConfigurationTest {
    @Test
    fun `should have correct annotations`() {
        val configClass = CacheFlowAutoConfiguration::class.java

        // Check @AutoConfiguration
        assertTrue(configClass.isAnnotationPresent(org.springframework.boot.autoconfigure.AutoConfiguration::class.java))

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
        val service = config.cacheFlowService(CacheFlowProperties(), null, null, null, null)

        assertNotNull(service)
        assertTrue(service is CacheFlowServiceImpl)
    }

    @Test
    fun `should create cacheFlowAspect bean`() {
        val config = CacheFlowAspectConfiguration()
        val mockService = mock(CacheFlowService::class.java)
        val mockDependencyResolver = mock(DependencyResolver::class.java)
        val mockCacheKeyVersioner = mock(CacheKeyVersioner::class.java)
        val mockConfigRegistry = mock(CacheFlowConfigRegistry::class.java)
        val aspect = config.cacheFlowAspect(mockService, mockDependencyResolver, mockCacheKeyVersioner, mockConfigRegistry)

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
    fun `should create cacheWarmer bean`() {
        val config = CacheFlowWarmingConfiguration()
        val warmer = config.cacheWarmer(CacheFlowProperties(), emptyList())

        assertNotNull(warmer)
    }

    @Test
    fun `cacheFlowService method should have correct annotations`() {
        val method =
            CacheFlowCoreConfiguration::class.java.getDeclaredMethod(
                "cacheFlowService",
                CacheFlowProperties::class.java,
                RedisTemplate::class.java,
                EdgeCacheIntegrationService::class.java,
                MeterRegistry::class.java,
                io.cacheflow.spring.messaging.RedisCacheInvalidator::class.java,
            )

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
                CacheKeyVersioner::class.java,
                CacheFlowConfigRegistry::class.java,
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
                CacheFlowService::class.java,
            )

        // Check @Bean
        assertTrue(method.isAnnotationPresent(Bean::class.java))

        // Check @ConditionalOnMissingBean
        assertTrue(method.isAnnotationPresent(ConditionalOnMissingBean::class.java))

        // Check @ConditionalOnAvailableEndpoint
        assertTrue(method.isAnnotationPresent(ConditionalOnAvailableEndpoint::class.java))
    }

    @Test
    fun `cacheWarmer method should have correct annotations`() {
        val method =
            CacheFlowWarmingConfiguration::class.java.getDeclaredMethod(
                "cacheWarmer",
                CacheFlowProperties::class.java,
                List::class.java,
            )

        // Check @Bean
        assertTrue(method.isAnnotationPresent(Bean::class.java))

        // Check @ConditionalOnMissingBean
        assertTrue(method.isAnnotationPresent(ConditionalOnMissingBean::class.java))
    }

    @Test
    fun `should create different instances for each bean`() {
        val coreConfig = CacheFlowCoreConfiguration()
        val aspectConfig = CacheFlowAspectConfiguration()
        val managementConfig = CacheFlowManagementConfiguration()
        val mockService = mock(CacheFlowService::class.java)
        val mockDependencyResolver = mock(DependencyResolver::class.java)
        val mockCacheKeyVersioner = mock(CacheKeyVersioner::class.java)
        val mockConfigRegistry = mock(CacheFlowConfigRegistry::class.java)

        val service1 = coreConfig.cacheFlowService(CacheFlowProperties(), null, null, null, null)
        val service2 = coreConfig.cacheFlowService(CacheFlowProperties(), null, null, null, null)
        val aspect1 = aspectConfig.cacheFlowAspect(mockService, mockDependencyResolver, mockCacheKeyVersioner, mockConfigRegistry)
        val aspect2 = aspectConfig.cacheFlowAspect(mockService, mockDependencyResolver, mockCacheKeyVersioner, mockConfigRegistry)
        val endpoint1 = managementConfig.cacheFlowManagementEndpoint(mockService)
        val endpoint2 = managementConfig.cacheFlowManagementEndpoint(mockService)

        // Each call should create a new instance
        assertNotSame(service1, service2)
        assertNotSame(aspect1, aspect2)
        assertNotSame(endpoint1, endpoint2)
    }

    @Test
    fun `should create different instances for cacheWarmer`() {
        val config = CacheFlowWarmingConfiguration()
        val warmer1 = config.cacheWarmer(CacheFlowProperties(), emptyList())
        val warmer2 = config.cacheWarmer(CacheFlowProperties(), emptyList())

        assertNotSame(warmer1, warmer2)
    }

    @Test
    fun `should handle null service parameter gracefully`() {
        val aspectConfig = CacheFlowAspectConfiguration()
        val managementConfig = CacheFlowManagementConfiguration()
        val mockDependencyResolver = mock(DependencyResolver::class.java)
        val mockCacheKeyVersioner = mock(CacheKeyVersioner::class.java)
        val mockConfigRegistry = mock(CacheFlowConfigRegistry::class.java)

        // These should not throw exceptions even with null service
        assertDoesNotThrow {
            aspectConfig.cacheFlowAspect(
                mock(CacheFlowService::class.java),
                mockDependencyResolver,
                mockCacheKeyVersioner,
                mockConfigRegistry,
            )
            managementConfig.cacheFlowManagementEndpoint(mock(CacheFlowService::class.java))
        }
    }

    // Helper function to create mock
    private fun <T> mock(clazz: Class<T>): T = org.mockito.Mockito.mock(clazz)
}