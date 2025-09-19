package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.aspect.CacheFlowAspect
import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.management.CacheFlowManagementEndpoint
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.service.impl.CacheFlowServiceImpl
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Auto-configuration for CacheFlow. */
@Configuration
@ConditionalOnProperty(
    prefix = "cacheflow",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(CacheFlowProperties::class)
class CacheFlowAutoConfiguration {

    /**
     * Creates the CacheFlow service bean.
     *
     * @return The CacheFlow service implementation
     */
    @Bean
    @ConditionalOnMissingBean
    fun cacheFlowService(): CacheFlowService = CacheFlowServiceImpl()

    /**
     * Creates the CacheFlow aspect bean.
     *
     * @param cacheService The cache service
     * @param applicationContext The Spring application context
     * @return The CacheFlow aspect
     */
    @Bean
    @ConditionalOnMissingBean
    fun cacheFlowAspect(
        cacheService: CacheFlowService
    ): CacheFlowAspect = CacheFlowAspect(cacheService)

    /**
     * Creates the CacheFlow management endpoint bean.
     *
     * @param cacheService The cache service
     * @return The management endpoint
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    fun cacheFlowManagementEndpoint(
        cacheService: CacheFlowService
    ): CacheFlowManagementEndpoint = CacheFlowManagementEndpoint(cacheService)
}
