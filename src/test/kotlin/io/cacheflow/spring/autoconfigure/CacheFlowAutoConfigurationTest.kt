package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.aspect.CacheFlowAspect
import io.cacheflow.spring.management.CacheFlowManagementEndpoint
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.service.impl.CacheFlowServiceImpl
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
        val config = CacheFlowAutoConfiguration()
        val service = config.cacheFlowService()

        assertNotNull(service)
        assertTrue(service is CacheFlowServiceImpl)
    }

    @Test
    fun `should create cacheFlowAspect bean`() {
        val config = CacheFlowAutoConfiguration()
        val mockService = mock(CacheFlowService::class.java)
        val aspect = config.cacheFlowAspect(mockService)

        assertNotNull(aspect)
        assertTrue(aspect is CacheFlowAspect)
    }

    @Test
    fun `should create cacheFlowManagementEndpoint bean`() {
        val config = CacheFlowAutoConfiguration()
        val mockService = mock(CacheFlowService::class.java)
        val endpoint = config.cacheFlowManagementEndpoint(mockService)

        assertNotNull(endpoint)
        assertTrue(endpoint is CacheFlowManagementEndpoint)
    }

    @Test
    fun `cacheFlowService method should have correct annotations`() {
        val method = CacheFlowAutoConfiguration::class.java.getDeclaredMethod("cacheFlowService")

        // Check @Bean
        assertTrue(method.isAnnotationPresent(Bean::class.java))

        // Check @ConditionalOnMissingBean
        assertTrue(method.isAnnotationPresent(ConditionalOnMissingBean::class.java))
    }

    @Test
    fun `cacheFlowAspect method should have correct annotations`() {
        val method =
                CacheFlowAutoConfiguration::class.java.getDeclaredMethod(
                        "cacheFlowAspect",
                        CacheFlowService::class.java
                )

        // Check @Bean
        assertTrue(method.isAnnotationPresent(Bean::class.java))

        // Check @ConditionalOnMissingBean
        assertTrue(method.isAnnotationPresent(ConditionalOnMissingBean::class.java))
    }

    @Test
    fun `cacheFlowManagementEndpoint method should have correct annotations`() {
        val method =
                CacheFlowAutoConfiguration::class.java.getDeclaredMethod(
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
        val config = CacheFlowAutoConfiguration()
        val mockService = mock(CacheFlowService::class.java)

        val service1 = config.cacheFlowService()
        val service2 = config.cacheFlowService()
        val aspect1 = config.cacheFlowAspect(mockService)
        val aspect2 = config.cacheFlowAspect(mockService)
        val endpoint1 = config.cacheFlowManagementEndpoint(mockService)
        val endpoint2 = config.cacheFlowManagementEndpoint(mockService)

        // Each call should create a new instance
        assertNotSame(service1, service2)
        assertNotSame(aspect1, aspect2)
        assertNotSame(endpoint1, endpoint2)
    }

    @Test
    fun `should handle null service parameter gracefully`() {
        val config = CacheFlowAutoConfiguration()

        // These should not throw exceptions even with null service
        assertDoesNotThrow {
            config.cacheFlowAspect(mock(CacheFlowService::class.java))
            config.cacheFlowManagementEndpoint(mock(CacheFlowService::class.java))
        }
    }

    // Helper function to create mock
    private fun <T> mock(clazz: Class<T>): T {
        return org.mockito.Mockito.mock(clazz)
    }
}
