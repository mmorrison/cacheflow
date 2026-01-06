package io.cacheflow.spring.annotation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CacheFlowConfigRegistryTest {

    private lateinit var registry: CacheFlowConfigRegistry

    @BeforeEach
    fun setUp() {
        registry = CacheFlowConfigRegistry()
    }

    @Test
    fun `should register and retrieve config`() {
        val config = CacheFlowConfigBuilder.withKey("testKey").build()
        registry.register("testConfig", config)

        assertEquals(config, registry.get("testConfig"))
        assertTrue(registry.exists("testConfig"))
    }

    @Test
    fun `should return default when config not found`() {
        val defaultConfig = CacheFlowConfigBuilder.withKey("defaultKey").build()
        val config = registry.getOrDefault("nonExistent", defaultConfig)

        assertEquals(defaultConfig, config)
    }

    @Test
    fun `should remove config`() {
        val config = CacheFlowConfigBuilder.withKey("testKey").build()
        registry.register("testConfig", config)

        val removed = registry.remove("testConfig")

        assertEquals(config, removed)
        assertFalse(registry.exists("testConfig"))
    }

    @Test
    fun `should clear all configs`() {
        registry.register("c1", CacheFlowConfigBuilder.withKey("k1").build())
        registry.register("c2", CacheFlowConfigBuilder.withKey("k2").build())

        assertEquals(2, registry.size())

        registry.clear()

        assertEquals(0, registry.size())
    }

    @Test
    fun `should return configuration names`() {
        registry.register("c1", CacheFlowConfigBuilder.withKey("k1").build())
        registry.register("c2", CacheFlowConfigBuilder.withKey("k2").build())

        val names = registry.getConfigurationNames()

        assertEquals(2, names.size)
        assertTrue(names.contains("c1"))
        assertTrue(names.contains("c2"))
    }

    @Test
    fun `should verify builder usage`() {
        val builder = CacheFlowConfigBuilder.versioned("updatedAt")
        builder.sync = true
        builder.unless = "#result == null"
        builder.condition = "#id > 10"
        builder.keyGenerator = "customGen"

        val config = builder.build()

        assertTrue(config.versioned)
        assertEquals("updatedAt", config.timestampField)
        assertTrue(config.sync)
        assertEquals("#result == null", config.unless)
        assertEquals("#id > 10", config.condition)
        assertEquals("customGen", config.keyGenerator)
    }

    @Test
    fun `should verify builder static methods`() {
        val withTags = CacheFlowConfigBuilder.withTags("tag1", "tag2").build()
        assertEquals(2, withTags.tags.size)

        val withDeps = CacheFlowConfigBuilder.withDependencies("dep1").build()
        assertEquals(1, withDeps.dependsOn.size)
    }
}
