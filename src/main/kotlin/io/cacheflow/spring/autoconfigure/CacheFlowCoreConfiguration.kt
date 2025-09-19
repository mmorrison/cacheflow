package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.dependency.CacheDependencyTracker
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.service.impl.CacheFlowServiceImpl
import io.cacheflow.spring.versioning.CacheKeyVersioner
import io.cacheflow.spring.versioning.TimestampExtractor
import io.cacheflow.spring.versioning.impl.DefaultTimestampExtractor
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Core configuration for CacheFlow services.
 *
 * This configuration handles the basic cache services, dependency management, and versioning
 * components.
 */
@Configuration
class CacheFlowCoreConfiguration {

    /**
     * Creates the CacheFlow service bean.
     *
     * @return The CacheFlow service implementation
     */
    @Bean
    @ConditionalOnMissingBean
    fun cacheFlowService(): CacheFlowService = CacheFlowServiceImpl()

    /**
     * Creates the dependency resolver bean.
     *
     * @return The dependency resolver implementation
     */
    @Bean
    @ConditionalOnMissingBean
    fun dependencyResolver(): DependencyResolver = CacheDependencyTracker()

    /**
     * Creates the timestamp extractor bean.
     *
     * @return The timestamp extractor implementation
     */
    @Bean
    @ConditionalOnMissingBean
    fun timestampExtractor(): TimestampExtractor = DefaultTimestampExtractor()

    /**
     * Creates the cache key versioner bean.
     *
     * @param timestampExtractor The timestamp extractor
     * @return The cache key versioner
     */
    @Bean
    @ConditionalOnMissingBean
    fun cacheKeyVersioner(timestampExtractor: TimestampExtractor): CacheKeyVersioner =
            CacheKeyVersioner(timestampExtractor)
}
