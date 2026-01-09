package io.cacheflow.spring.annotation

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheFlowConfigTest {

    @Test
    fun `should create config with default values`() {
        val config = CacheFlowConfig()

        assertEquals("", config.key)
        assertEquals("defaultKeyGenerator", config.keyGenerator)
        assertEquals(-1L, config.ttl)
        assertTrue(config.dependsOn.isEmpty())
        assertTrue(config.tags.isEmpty())
        assertEquals("", config.condition)
        assertEquals("", config.unless)
        assertFalse(config.sync)
    }

    @Test
    fun `should create config with custom values`() {
        val config =
            CacheFlowConfig(
                key = "test-key",
                keyGenerator = "customGenerator",
                ttl = 3600L,
                dependsOn = arrayOf("param1", "param2"),
                tags = arrayOf("tag1", "tag2"),
                condition = "true",
                unless = "false",
                sync = true
            )

        assertEquals("test-key", config.key)
        assertEquals("customGenerator", config.keyGenerator)
        assertEquals(3600L, config.ttl)
        assertArrayEquals(arrayOf("param1", "param2"), config.dependsOn)
        assertArrayEquals(arrayOf("tag1", "tag2"), config.tags)
        assertEquals("true", config.condition)
        assertEquals("false", config.unless)
        assertTrue(config.sync)
    }

    @Test
    fun `should be equal when all properties match`() {
        val config1 =
            CacheFlowConfig(
                key = "test-key",
                keyGenerator = "customGenerator",
                ttl = 3600L,
                dependsOn = arrayOf("param1", "param2"),
                tags = arrayOf("tag1", "tag2"),
                condition = "true",
                unless = "false",
                sync = true
            )

        val config2 =
            CacheFlowConfig(
                key = "test-key",
                keyGenerator = "customGenerator",
                ttl = 3600L,
                dependsOn = arrayOf("param1", "param2"),
                tags = arrayOf("tag1", "tag2"),
                condition = "true",
                unless = "false",
                sync = true
            )

        assertEquals(config1, config2)
        assertEquals(config1.hashCode(), config2.hashCode())
    }

    @Test
    fun `should not be equal when properties differ`() {
        val config1 = CacheFlowConfig(key = "key1")
        val config2 = CacheFlowConfig(key = "key2")

        assertNotEquals(config1, config2)
        assertNotEquals(config1.hashCode(), config2.hashCode())
    }

    @Test
    fun `should not be equal when dependsOn arrays differ`() {
        val config1 = CacheFlowConfig(dependsOn = arrayOf("param1"))
        val config2 = CacheFlowConfig(dependsOn = arrayOf("param2"))

        assertNotEquals(config1, config2)
    }

    @Test
    fun `should not be equal when tags arrays differ`() {
        val config1 = CacheFlowConfig(tags = arrayOf("tag1"))
        val config2 = CacheFlowConfig(tags = arrayOf("tag2"))

        assertNotEquals(config1, config2)
    }

    @Test
    fun `should not be equal to null`() {
        val config = CacheFlowConfig()
        assertNotEquals(config, null)
    }

    @Test
    fun `should not be equal to different class`() {
        val config = CacheFlowConfig()
        assertNotEquals(config, "not a config")
    }

    @Test
    fun `should be equal to itself`() {
        val config = CacheFlowConfig()
        assertEquals(config, config)
    }

    @Test
    fun `should have consistent hashCode`() {
        val config =
            CacheFlowConfig(
                key = "test-key",
                keyGenerator = "customGenerator",
                ttl = 3600L,
                dependsOn = arrayOf("param1", "param2"),
                tags = arrayOf("tag1", "tag2"),
                condition = "true",
                unless = "false",
                sync = true
            )

        val hashCode1 = config.hashCode()
        val hashCode2 = config.hashCode()
        assertEquals(hashCode1, hashCode2)
    }
}
