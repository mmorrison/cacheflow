package io.cacheflow.spring.warming

import io.cacheflow.spring.config.CacheFlowProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener

/**
 * Component responsible for executing cache warmup providers on application startup.
 */
class CacheWarmer(
    private val properties: CacheFlowProperties,
    private val warmupProviders: List<CacheWarmupProvider>,
) : ApplicationListener<ApplicationReadyEvent> {

    private val logger = LoggerFactory.getLogger(CacheWarmer::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        if (properties.warming.enabled) {
            logger.info("CacheFlow warming started. Found ${warmupProviders.size} providers.")
            warmupProviders.forEach { provider ->
                try {
                    logger.debug("Executing warmup provider: ${provider::class.simpleName}")
                    provider.warmup()
                } catch (e: Exception) {
                    logger.error("Error during cache warmup execution for provider ${provider::class.simpleName}", e)
                }
            }
            logger.info("CacheFlow warming completed.")
        } else {
            logger.debug("CacheFlow warming passed (disabled).")
        }
    }
}
