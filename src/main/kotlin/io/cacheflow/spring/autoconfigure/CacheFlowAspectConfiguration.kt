package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.annotation.CacheFlowConfigRegistry
import io.cacheflow.spring.aspect.CacheFlowAspect
import io.cacheflow.spring.aspect.CacheKeyGenerator
import io.cacheflow.spring.aspect.DependencyManager
import io.cacheflow.spring.aspect.FragmentCacheAspect
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.fragment.FragmentCacheService
import io.cacheflow.spring.fragment.FragmentTagManager
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.versioning.CacheKeyVersioner
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Aspect configuration for CacheFlow.
 *
 * This configuration handles all AOP aspects including the main CacheFlow aspect, fragment cache
 * aspect, and their supporting services.
 */
@Configuration
class CacheFlowAspectConfiguration {

    /**
     * Creates the cache key generator bean.
     *
     * @param cacheKeyVersioner The cache key versioner
     * @return The cache key generator
     */
    @Bean
    @ConditionalOnMissingBean
    fun cacheKeyGenerator(cacheKeyVersioner: CacheKeyVersioner): CacheKeyGenerator =
        CacheKeyGenerator(cacheKeyVersioner)

    /**
     * Creates the dependency manager bean.
     *
     * @param dependencyResolver The dependency resolver
     * @return The dependency manager
     */
    @Bean
    @ConditionalOnMissingBean
    fun dependencyManager(dependencyResolver: DependencyResolver): DependencyManager =
        DependencyManager(dependencyResolver)

    /**
     * Creates the CacheFlow aspect bean.
     *
     * @param cacheService The cache service
     * @param dependencyResolver The dependency resolver
     * @param cacheKeyVersioner The cache key versioner
     * @param configRegistry The configuration registry
     * @return The CacheFlow aspect
     */
    @Bean
    @ConditionalOnMissingBean
    fun cacheFlowAspect(
        cacheService: CacheFlowService,
        dependencyResolver: DependencyResolver,
        cacheKeyVersioner: CacheKeyVersioner,
        configRegistry: CacheFlowConfigRegistry
    ): CacheFlowAspect = CacheFlowAspect(cacheService, dependencyResolver, cacheKeyVersioner, configRegistry)

    /**
     * Creates the fragment cache aspect bean.
     *
     * @param fragmentCacheService The fragment cache service
     * @param dependencyResolver The dependency resolver
     * @param tagManager The fragment tag manager
     * @return The fragment cache aspect
     */
    @Bean
    @ConditionalOnMissingBean
    fun fragmentCacheAspect(
        fragmentCacheService: FragmentCacheService,
        dependencyResolver: DependencyResolver,
        tagManager: FragmentTagManager
    ): FragmentCacheAspect =
        FragmentCacheAspect(fragmentCacheService, dependencyResolver, tagManager)
}
