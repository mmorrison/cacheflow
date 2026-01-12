package io.cacheflow.spring.edge.config

import io.cacheflow.spring.edge.*
import io.cacheflow.spring.edge.impl.AwsCloudFrontEdgeCacheProvider
import io.cacheflow.spring.edge.impl.CloudflareEdgeCacheProvider
import io.cacheflow.spring.edge.impl.FastlyEdgeCacheProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import software.amazon.awssdk.services.cloudfront.CloudFrontClient

/** Auto-configuration for edge cache providers */
@Configuration
@EnableConfigurationProperties(EdgeCacheProperties::class)
class EdgeCacheAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun edgeCacheCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(WebClient::class)
    fun edgeWebClient(): WebClient = WebClient.builder().build()

    @Bean
    @ConditionalOnProperty(
        prefix = "cacheflow.edge.cloudflare",
        name = ["enabled"],
        havingValue = "true",
    )
    @ConditionalOnClass(WebClient::class)
    fun cloudflareEdgeCacheProvider(
        webClient: WebClient,
        properties: EdgeCacheProperties,
        scope: CoroutineScope,
    ): CloudflareEdgeCacheProvider {
        val cloudflareProps = properties.cloudflare
        return CloudflareEdgeCacheProvider(
            webClient = webClient,
            zoneId = cloudflareProps.zoneId,
            apiToken = cloudflareProps.apiToken,
            keyPrefix = cloudflareProps.keyPrefix,
        )
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "cacheflow.edge.aws-cloud-front",
        name = ["enabled"],
        havingValue = "true",
    )
    @ConditionalOnClass(CloudFrontClient::class)
    fun awsCloudFrontEdgeCacheProvider(
        cloudFrontClient: CloudFrontClient,
        properties: EdgeCacheProperties,
    ): AwsCloudFrontEdgeCacheProvider {
        val awsProps = properties.awsCloudFront
        return AwsCloudFrontEdgeCacheProvider(
            cloudFrontClient = cloudFrontClient,
            distributionId = awsProps.distributionId,
            keyPrefix = awsProps.keyPrefix,
        )
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "cacheflow.edge.fastly",
        name = ["enabled"],
        havingValue = "true",
    )
    @ConditionalOnClass(WebClient::class)
    fun fastlyEdgeCacheProvider(
        webClient: WebClient,
        properties: EdgeCacheProperties,
    ): FastlyEdgeCacheProvider {
        val fastlyProps = properties.fastly
        return FastlyEdgeCacheProvider(
            webClient = webClient,
            serviceId = fastlyProps.serviceId,
            apiToken = fastlyProps.apiToken,
            keyPrefix = fastlyProps.keyPrefix,
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun edgeCacheManager(
        providers: List<EdgeCacheProvider>,
        properties: EdgeCacheProperties,
        scope: CoroutineScope,
    ): EdgeCacheManager {
        val configuration =
            EdgeCacheConfiguration(
                provider = "multi-provider",
                enabled = properties.enabled,
                rateLimit =
                    properties.rateLimit?.let {
                        RateLimit(
                            it.requestsPerSecond,
                            it.burstSize,
                            java.time.Duration.ofSeconds(it.windowSize),
                        )
                    },
                circuitBreaker =
                    properties.circuitBreaker?.let {
                        CircuitBreakerConfig(
                            failureThreshold = it.failureThreshold,
                            recoveryTimeout =
                                java.time.Duration.ofSeconds(
                                    it.recoveryTimeout,
                                ),
                            halfOpenMaxCalls = it.halfOpenMaxCalls,
                        )
                    },
                batching =
                    properties.batching?.let {
                        BatchingConfig(
                            batchSize = it.batchSize,
                            batchTimeout =
                                java.time.Duration.ofSeconds(it.batchTimeout),
                            maxConcurrency = it.maxConcurrency,
                        )
                    },
                monitoring =
                    properties.monitoring?.let {
                        MonitoringConfig(
                            enableMetrics = it.enableMetrics,
                            enableTracing = it.enableTracing,
                            logLevel = it.logLevel,
                        )
                    },
            )

        return EdgeCacheManager(providers, configuration, scope)
    }
}
