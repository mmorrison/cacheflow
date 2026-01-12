package io.cacheflow.spring.warming

/**
 * Interface to be implemented by beans that provide cache warmup logic.
 * These beans will be automatically detected and executed by CacheWarmer if warming is enabled.
 */
interface CacheWarmupProvider {
    /**
     * Executes the warmup logic.
     * This method is called during application startup.
     */
    fun warmup()
}
