package io.cacheflow.spring.annotation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CacheFlowConfigRegistryTest {
    private lateinit var registry: CacheFlowConfigRegistry

    @BeforeEach
    fun setUp() {
        registry = CacheFlowConfigRegistry()
    }

    @Test
    fun `should register and retrieve configuration`() {
        val config = CacheFlowConfig(key = "test-key", ttl = 3600L)
        registry.register("testConfig", config)

        val retrieved = registry.get("testConfig")
        assertNotNull(retrieved)
        assertEquals("test-key", retrieved?.key)
        assertEquals(3600L, retrieved?.ttl)
    }

    @Test
    fun `should return null for non-existent configuration`() {
        val retrieved = registry.get("nonExistent")
        assertNull(retrieved)
    }

    @Test
    fun `should return default configuration when not found`() {
        val defaultConfig = CacheFlowConfig(key = "default-key", ttl = 1800L)
        val retrieved = registry.getOrDefault("nonExistent", defaultConfig)

        assertNotNull(retrieved)
        assertEquals("default-key", retrieved.key)
        assertEquals(1800L, retrieved.ttl)
    }

    @Test
    fun `should return registered configuration instead of default`() {
        val registeredConfig = CacheFlowConfig(key = "registered-key", ttl = 3600L)
        val defaultConfig = CacheFlowConfig(key = "default-key", ttl = 1800L)

        registry.register("testConfig", registeredConfig)
        val retrieved = registry.getOrDefault("testConfig", defaultConfig)

        assertEquals("registered-key", retrieved.key)
        assertEquals(3600L, retrieved.ttl)
    }

    @Test
    fun `should check if configuration exists`() {
        assertFalse(registry.exists("testConfig"))

        val config = CacheFlowConfig(key = "test-key")
        registry.register("testConfig", config)

        assertTrue(registry.exists("testConfig"))
    }

    @Test
    fun `should remove configuration`() {
        val config = CacheFlowConfig(key = "test-key", ttl = 3600L)
        registry.register("testConfig", config)

        assertTrue(registry.exists("testConfig"))

        val removed = registry.remove("testConfig")
        assertNotNull(removed)
        assertEquals("test-key", removed?.key)

        assertFalse(registry.exists("testConfig"))
    }

    @Test
    fun `should return null when removing non-existent configuration`() {
        val removed = registry.remove("nonExistent")
        assertNull(removed)
    }

    @Test
    fun `should get all configuration names`() {
        assertTrue(registry.getConfigurationNames().isEmpty())

        registry.register("config1", CacheFlowConfig(key = "key1"))
        registry.register("config2", CacheFlowConfig(key = "key2"))
        registry.register("config3", CacheFlowConfig(key = "key3"))

        val names = registry.getConfigurationNames()
        assertEquals(3, names.size)
        assertTrue(names.contains("config1"))
        assertTrue(names.contains("config2"))
        assertTrue(names.contains("config3"))
    }

    @Test
    fun `should clear all configurations`() {
        registry.register("config1", CacheFlowConfig(key = "key1"))
        registry.register("config2", CacheFlowConfig(key = "key2"))

        assertEquals(2, registry.size())

        registry.clear()

        assertEquals(0, registry.size())
        assertTrue(registry.getConfigurationNames().isEmpty())
        assertFalse(registry.exists("config1"))
        assertFalse(registry.exists("config2"))
    }

    @Test
    fun `should return correct size`() {
        assertEquals(0, registry.size())

        registry.register("config1", CacheFlowConfig(key = "key1"))
        assertEquals(1, registry.size())

        registry.register("config2", CacheFlowConfig(key = "key2"))
        assertEquals(2, registry.size())

        registry.remove("config1")
        assertEquals(1, registry.size())

        registry.clear()
        assertEquals(0, registry.size())
    }

    @Test
    fun `should overwrite existing configuration`() {
        val config1 = CacheFlowConfig(key = "key1", ttl = 1800L)
        val config2 = CacheFlowConfig(key = "key2", ttl = 3600L)

        registry.register("testConfig", config1)
        assertEquals("key1", registry.get("testConfig")?.key)
        assertEquals(1800L, registry.get("testConfig")?.ttl)

        registry.register("testConfig", config2)
        assertEquals("key2", registry.get("testConfig")?.key)
        assertEquals(3600L, registry.get("testConfig")?.ttl)
        assertEquals(1, registry.size())
    }

    @Test
    fun `should handle concurrent access safely`() {
        val threadCount = 10
        val operationsPerThread = 100
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    repeat(operationsPerThread) { iteration ->
                        val configName = "config-$threadId-$iteration"
                        val config = CacheFlowConfig(key = "key-$threadId-$iteration")

                        // Register
                        registry.register(configName, config)

                        // Verify exists
                        assertTrue(registry.exists(configName))

                        // Retrieve
                        assertNotNull(registry.get(configName))

                        // Remove
                        if (iteration % 2 == 0) {
                            registry.remove(configName)
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()

        // Verify size is consistent (should have roughly half of the entries since we remove every other one)
        val expectedSize = threadCount * operationsPerThread / 2
        assertEquals(expectedSize, registry.size())
    }

    @Test
    fun `should return immutable snapshot of configuration names`() {
        registry.register("config1", CacheFlowConfig(key = "key1"))
        registry.register("config2", CacheFlowConfig(key = "key2"))

        val names1 = registry.getConfigurationNames()
        registry.register("config3", CacheFlowConfig(key = "key3"))
        val names2 = registry.getConfigurationNames()

        // Original snapshot should not be affected
        assertEquals(2, names1.size)
        assertEquals(3, names2.size)
    }

    @Test
    fun `should handle complex configuration with all parameters`() {
        val config =
            CacheFlowConfig(
                key = "complex-key",
                keyGenerator = "customGenerator",
                ttl = 7200L,
                dependsOn = arrayOf("param1", "param2"),
                tags = arrayOf("tag1", "tag2"),
                condition = "#result != null",
                unless = "#result == null",
                sync = true,
                versioned = true,
                timestampField = "updatedAt",
                config = "complexConfig",
            )

        registry.register("complexConfig", config)
        val retrieved = registry.get("complexConfig")

        assertNotNull(retrieved)
        assertEquals("complex-key", retrieved?.key)
        assertEquals("customGenerator", retrieved?.keyGenerator)
        assertEquals(7200L, retrieved?.ttl)
        assertEquals(2, retrieved?.dependsOn?.size)
        assertEquals(2, retrieved?.tags?.size)
        assertEquals("#result != null", retrieved?.condition)
        assertEquals("#result == null", retrieved?.unless)
        assertTrue(retrieved?.sync == true)
        assertTrue(retrieved?.versioned == true)
        assertEquals("updatedAt", retrieved?.timestampField)
    }
}
