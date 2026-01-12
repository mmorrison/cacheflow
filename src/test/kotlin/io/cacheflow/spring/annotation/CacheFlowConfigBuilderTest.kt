package io.cacheflow.spring.annotation

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheFlowConfigBuilderTest {
    @Test
    fun `should create builder with default values`() {
        val builder = CacheFlowConfigBuilder()

        assertEquals("", builder.key)
        assertEquals("", builder.keyGenerator)
        assertEquals(-1L, builder.ttl)
        assertTrue(builder.dependsOn.isEmpty())
        assertTrue(builder.tags.isEmpty())
        assertEquals("", builder.condition)
        assertEquals("", builder.unless)
        assertFalse(builder.sync)
        assertFalse(builder.versioned)
        assertEquals("updatedAt", builder.timestampField)
    }

    @Test
    fun `should build config with default values`() {
        val config = CacheFlowConfigBuilder().build()

        assertEquals("", config.key)
        assertEquals("", config.keyGenerator)
        assertEquals(-1L, config.ttl)
        assertTrue(config.dependsOn.isEmpty())
        assertTrue(config.tags.isEmpty())
        assertEquals("", config.condition)
        assertEquals("", config.unless)
        assertFalse(config.sync)
        assertFalse(config.versioned)
        assertEquals("updatedAt", config.timestampField)
        assertEquals("", config.config)
    }

    @Test
    fun `should set key via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.key = "test-key"

        val config = builder.build()
        assertEquals("test-key", config.key)
    }

    @Test
    fun `should set keyGenerator via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.keyGenerator = "customGenerator"

        val config = builder.build()
        assertEquals("customGenerator", config.keyGenerator)
    }

    @Test
    fun `should set ttl via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.ttl = 3600L

        val config = builder.build()
        assertEquals(3600L, config.ttl)
    }

    @Test
    fun `should set dependsOn via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.dependsOn = arrayOf("param1", "param2")

        val config = builder.build()
        assertArrayEquals(arrayOf("param1", "param2"), config.dependsOn)
    }

    @Test
    fun `should set tags via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.tags = arrayOf("tag1", "tag2")

        val config = builder.build()
        assertArrayEquals(arrayOf("tag1", "tag2"), config.tags)
    }

    @Test
    fun `should set condition via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.condition = "#result != null"

        val config = builder.build()
        assertEquals("#result != null", config.condition)
    }

    @Test
    fun `should set unless via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.unless = "#result == null"

        val config = builder.build()
        assertEquals("#result == null", config.unless)
    }

    @Test
    fun `should set sync via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.sync = true

        val config = builder.build()
        assertTrue(config.sync)
    }

    @Test
    fun `should set versioned via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.versioned = true

        val config = builder.build()
        assertTrue(config.versioned)
    }

    @Test
    fun `should set timestampField via property`() {
        val builder = CacheFlowConfigBuilder()
        builder.timestampField = "createdAt"

        val config = builder.build()
        assertEquals("createdAt", config.timestampField)
    }

    @Test
    fun `should create builder using companion object builder method`() {
        val builder = CacheFlowConfigBuilder.builder()

        val config = builder.build()
        assertEquals("", config.key)
    }

    @Test
    fun `should create builder with key using withKey factory method`() {
        val builder = CacheFlowConfigBuilder.withKey("test-key")

        assertEquals("test-key", builder.key)

        val config = builder.build()
        assertEquals("test-key", config.key)
    }

    @Test
    fun `should create versioned builder with default timestamp field`() {
        val builder = CacheFlowConfigBuilder.versioned()

        assertTrue(builder.versioned)
        assertEquals("updatedAt", builder.timestampField)

        val config = builder.build()
        assertTrue(config.versioned)
        assertEquals("updatedAt", config.timestampField)
    }

    @Test
    fun `should create versioned builder with custom timestamp field`() {
        val builder = CacheFlowConfigBuilder.versioned("createdAt")

        assertTrue(builder.versioned)
        assertEquals("createdAt", builder.timestampField)

        val config = builder.build()
        assertTrue(config.versioned)
        assertEquals("createdAt", config.timestampField)
    }

    @Test
    fun `should create builder with dependencies`() {
        val builder = CacheFlowConfigBuilder.withDependencies("param1", "param2", "param3")

        assertArrayEquals(arrayOf("param1", "param2", "param3"), builder.dependsOn)

        val config = builder.build()
        assertArrayEquals(arrayOf("param1", "param2", "param3"), config.dependsOn)
    }

    @Test
    fun `should create builder with tags`() {
        val builder = CacheFlowConfigBuilder.withTags("tag1", "tag2")

        assertArrayEquals(arrayOf("tag1", "tag2"), builder.tags)

        val config = builder.build()
        assertArrayEquals(arrayOf("tag1", "tag2"), config.tags)
    }

    @Test
    fun `should support method chaining with apply block`() {
        val config =
            CacheFlowConfigBuilder
                .withKey("test-key")
                .apply {
                    ttl = 3600L
                    sync = true
                    versioned = true
                    timestampField = "modifiedAt"
                }.build()

        assertEquals("test-key", config.key)
        assertEquals(3600L, config.ttl)
        assertTrue(config.sync)
        assertTrue(config.versioned)
        assertEquals("modifiedAt", config.timestampField)
    }

    @Test
    fun `should build complex configuration`() {
        val builder = CacheFlowConfigBuilder()
        builder.key = "complex-key"
        builder.keyGenerator = "customGenerator"
        builder.ttl = 7200L
        builder.dependsOn = arrayOf("param1", "param2")
        builder.tags = arrayOf("tag1", "tag2", "tag3")
        builder.condition = "#result != null"
        builder.unless = "#result.empty"
        builder.sync = true
        builder.versioned = true
        builder.timestampField = "lastModified"

        val config = builder.build()

        assertEquals("complex-key", config.key)
        assertEquals("customGenerator", config.keyGenerator)
        assertEquals(7200L, config.ttl)
        assertArrayEquals(arrayOf("param1", "param2"), config.dependsOn)
        assertArrayEquals(arrayOf("tag1", "tag2", "tag3"), config.tags)
        assertEquals("#result != null", config.condition)
        assertEquals("#result.empty", config.unless)
        assertTrue(config.sync)
        assertTrue(config.versioned)
        assertEquals("lastModified", config.timestampField)
    }

    @Test
    fun `should handle empty dependencies array`() {
        val builder = CacheFlowConfigBuilder.withDependencies()

        assertTrue(builder.dependsOn.isEmpty())

        val config = builder.build()
        assertTrue(config.dependsOn.isEmpty())
    }

    @Test
    fun `should handle empty tags array`() {
        val builder = CacheFlowConfigBuilder.withTags()

        assertTrue(builder.tags.isEmpty())

        val config = builder.build()
        assertTrue(config.tags.isEmpty())
    }

    @Test
    fun `should create multiple independent builders`() {
        val builder1 = CacheFlowConfigBuilder.withKey("key1")
        val builder2 = CacheFlowConfigBuilder.withKey("key2")

        builder1.ttl = 1800L
        builder2.ttl = 3600L

        val config1 = builder1.build()
        val config2 = builder2.build()

        assertEquals("key1", config1.key)
        assertEquals(1800L, config1.ttl)

        assertEquals("key2", config2.key)
        assertEquals(3600L, config2.ttl)
    }

    @Test
    fun `should build multiple configs from same builder`() {
        val builder = CacheFlowConfigBuilder.withKey("shared-key")

        val config1 = builder.build()
        builder.ttl = 3600L
        val config2 = builder.build()

        // First config should not be affected by later changes
        assertEquals(-1L, config1.ttl)
        assertEquals(3600L, config2.ttl)

        // Both should have the same key
        assertEquals("shared-key", config1.key)
        assertEquals("shared-key", config2.key)
    }

    @Test
    fun `should combine multiple factory methods`() {
        val config =
            CacheFlowConfigBuilder
                .withKey("combined-key")
                .apply {
                    dependsOn = arrayOf("dep1", "dep2")
                    tags = arrayOf("tag1")
                    versioned = true
                    timestampField = "updatedAt"
                }.build()

        assertEquals("combined-key", config.key)
        assertArrayEquals(arrayOf("dep1", "dep2"), config.dependsOn)
        assertArrayEquals(arrayOf("tag1"), config.tags)
        assertTrue(config.versioned)
        assertEquals("updatedAt", config.timestampField)
    }
}
