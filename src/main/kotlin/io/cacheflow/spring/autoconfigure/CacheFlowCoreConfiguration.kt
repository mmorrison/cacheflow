package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.annotation.CacheFlowConfigRegistry
import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.dependency.CacheDependencyTracker
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.edge.service.EdgeCacheIntegrationService
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.service.impl.CacheFlowServiceImpl
import io.cacheflow.spring.versioning.CacheKeyVersioner
import io.cacheflow.spring.versioning.TimestampExtractor
import io.cacheflow.spring.versioning.impl.DefaultTimestampExtractor
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate

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
     * @param properties Cache configuration properties
     * @param redisTemplate Optional Redis template for distributed caching
     * @param edgeCacheService Optional Edge cache service for edge integration
     * @param meterRegistry Optional MeterRegistry for metrics
     * @return The CacheFlow service implementation
     */
    @Bean
    @ConditionalOnMissingBean
    fun cacheFlowService(
        properties: CacheFlowProperties,
        @Autowired(required = false) @Qualifier("cacheFlowRedisTemplate") redisTemplate: RedisTemplate<String, Any>?,
        @Autowired(required = false) edgeCacheService: EdgeCacheIntegrationService?,
        @Autowired(required = false) meterRegistry: MeterRegistry?,
        @Autowired(required = false) redisCacheInvalidator: io.cacheflow.spring.messaging.RedisCacheInvalidator?,
    ): CacheFlowService = CacheFlowServiceImpl(properties, redisTemplate, edgeCacheService, meterRegistry, redisCacheInvalidator)

    /**
     * Creates the dependency resolver bean.
     *
     * @return The dependency resolver implementation
     */
    @Bean
    @ConditionalOnMissingBean
    fun dependencyResolver(
        properties: CacheFlowProperties,
        @Autowired(required = false) redisTemplate: org.springframework.data.redis.core.StringRedisTemplate?,
    ): DependencyResolver = CacheDependencyTracker(properties, redisTemplate)

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
    fun cacheKeyVersioner(timestampExtractor: TimestampExtractor): CacheKeyVersioner = CacheKeyVersioner(timestampExtractor)

    /**
     * Creates the CacheFlow configuration registry bean.
     *
     * @return The configuration registry
     */
    @Bean
    @ConditionalOnMissingBean
    fun cacheFlowConfigRegistry(): CacheFlowConfigRegistry = CacheFlowConfigRegistry()
}
