package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.warming.CacheWarmer
import io.cacheflow.spring.warming.CacheWarmupProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "cacheflow.warming", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class CacheFlowWarmingConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun cacheWarmer(
        properties: CacheFlowProperties,
        warmupProviders: List<CacheWarmupProvider>,
    ): CacheWarmer = CacheWarmer(properties, warmupProviders)
}
