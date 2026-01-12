package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.management.CacheFlowManagementEndpoint
import io.cacheflow.spring.service.CacheFlowService
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Management configuration for CacheFlow.
 *
 * This configuration handles management and monitoring endpoints for CacheFlow services.
 */
@Configuration
class CacheFlowManagementConfiguration {
    /**
     * Creates the CacheFlow management endpoint bean.
     *
     * @param cacheService The cache service
     * @return The management endpoint
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    fun cacheFlowManagementEndpoint(cacheService: CacheFlowService): CacheFlowManagementEndpoint = CacheFlowManagementEndpoint(cacheService)
}
