package io.cacheflow.spring.annotation

import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for managing CacheFlow configurations. Allows for complex configurations to be defined
 * separately from annotations.
 */
class CacheFlowConfigRegistry {
    private val configurations = ConcurrentHashMap<String, CacheFlowConfig>()

    /**
     * Registers a configuration with a given name.
     *
     * @param name The configuration name
     * @param config The configuration
     */
    fun register(
        name: String,
        config: CacheFlowConfig,
    ) {
        configurations[name] = config
    }

    /**
     * Gets a configuration by name.
     *
     * @param name The configuration name
     * @return The configuration or null if not found
     */
    fun get(name: String): CacheFlowConfig? = configurations[name]

    /**
     * Gets a configuration by name or returns a default configuration.
     *
     * @param name The configuration name
     * @param defaultConfig The default configuration to return if not found
     * @return The configuration or default
     */
    fun getOrDefault(
        name: String,
        defaultConfig: CacheFlowConfig,
    ): CacheFlowConfig = configurations[name] ?: defaultConfig

    /**
     * Checks if a configuration exists.
     *
     * @param name The configuration name
     * @return true if the configuration exists
     */
    fun exists(name: String): Boolean = configurations.containsKey(name)

    /**
     * Removes a configuration.
     *
     * @param name The configuration name
     * @return The removed configuration or null if not found
     */
    fun remove(name: String): CacheFlowConfig? = configurations.remove(name)

    /**
     * Gets all registered configuration names.
     *
     * @return Set of configuration names
     */
    fun getConfigurationNames(): Set<String> = configurations.keys.toSet()

    /** Clears all configurations. */
    fun clear() {
        configurations.clear()
    }

    /**
     * Gets the number of registered configurations.
     *
     * @return The number of configurations
     */
    fun size(): Int = configurations.size
}
