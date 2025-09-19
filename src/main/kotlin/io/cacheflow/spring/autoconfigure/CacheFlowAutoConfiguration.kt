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

    @Bean
    @ConditionalOnMissingBean
    fun cacheFlowService(properties: CacheFlowProperties): CacheFlowService {
        return CacheFlowServiceImpl(properties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun cacheFlowAspect(
        cacheService: CacheFlowService,
        applicationContext: org.springframework.context.ApplicationContext
    ): CacheFlowAspect {
        return CacheFlowAspect(cacheService, applicationContext)
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    fun cacheFlowManagementEndpoint(
        cacheService: CacheFlowService
    ): CacheFlowManagementEndpoint {
        return CacheFlowManagementEndpoint(cacheService)
    }
}
