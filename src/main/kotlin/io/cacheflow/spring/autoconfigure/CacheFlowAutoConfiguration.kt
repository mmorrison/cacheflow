package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.config.CacheFlowProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Main auto-configuration for CacheFlow.
 *
 * This configuration imports all the specialized configuration classes and provides the main
 * configuration properties.
 */

@Configuration
@ConditionalOnProperty(
    prefix = "cacheflow",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(CacheFlowProperties::class)
@Import(
    CacheFlowCoreConfiguration::class,
    CacheFlowFragmentConfiguration::class,
    CacheFlowAspectConfiguration::class,
    CacheFlowManagementConfiguration::class,
    CacheFlowRedisConfiguration::class,
)
class CacheFlowAutoConfiguration
