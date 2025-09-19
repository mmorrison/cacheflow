package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.fragment.FragmentCacheService
import io.cacheflow.spring.fragment.FragmentComposer
import io.cacheflow.spring.fragment.FragmentTagManager
import io.cacheflow.spring.fragment.impl.FragmentCacheServiceImpl
import io.cacheflow.spring.service.CacheFlowService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Fragment services configuration for CacheFlow.
 *
 * This configuration handles all fragment-related services including fragment caching, composition,
 * and tag management.
 */
@Configuration
class CacheFlowFragmentConfiguration {

    /**
     * Creates the fragment tag manager bean.
     *
     * @return The fragment tag manager
     */
    @Bean
    @ConditionalOnMissingBean
    fun fragmentTagManager(): FragmentTagManager = FragmentTagManager()

    /**
     * Creates the fragment composer bean.
     *
     * @return The fragment composer
     */
    @Bean @ConditionalOnMissingBean fun fragmentComposer(): FragmentComposer = FragmentComposer()

    /**
     * Creates the fragment cache service bean.
     *
     * @param cacheService The cache service
     * @param tagManager The fragment tag manager
     * @param composer The fragment composer
     * @return The fragment cache service
     */
    @Bean
    @ConditionalOnMissingBean
    fun fragmentCacheService(
            cacheService: CacheFlowService,
            tagManager: FragmentTagManager,
            composer: FragmentComposer
    ): FragmentCacheService = FragmentCacheServiceImpl(cacheService, tagManager, composer)
}
