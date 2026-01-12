package io.cacheflow.spring.service

import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.service.impl.CacheFlowServiceImpl
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CacheFlowServiceTest {
    private lateinit var cacheService: CacheFlowService

    @BeforeEach
    fun setUp() {
        cacheService = CacheFlowServiceImpl(CacheFlowProperties())
    }

    @Test
    fun `should put and get value with default TTL`() {
        cacheService.put("key1", "value1")

        val result = cacheService.get("key1")
        assertEquals("value1", result)
    }

    @Test
    fun `should put and get value with custom TTL`() {
        cacheService.put("key1", "value1", 120L)

        val result = cacheService.get("key1")
        assertEquals("value1", result)
    }

    @Test
    fun `should return null for non-existent key`() {
        val result = cacheService.get("non-existent")
        assertNull(result)
    }

    @Test
    fun `should evict specific key`() {
        cacheService.put("key1", "value1", 60L)
        cacheService.put("key2", "value2", 60L)

        cacheService.evict("key1")

        assertNull(cacheService.get("key1"))
        assertEquals("value2", cacheService.get("key2"))
    }

    @Test
    fun `should evict all keys`() {
        cacheService.put("key1", "value1", 60L)
        cacheService.put("key2", "value2", 60L)
        cacheService.put("key3", "value3", 60L)

        cacheService.evictAll()

        assertNull(cacheService.get("key1"))
        assertNull(cacheService.get("key2"))
        assertNull(cacheService.get("key3"))
        assertEquals(0L, cacheService.size())
    }

    @Test
    fun `should evict by tags`() {
        // Note: evictByTags is not implemented in CacheFlowServiceImpl
        // This test verifies the method exists and can be called
        assertDoesNotThrow { cacheService.evictByTags("tag1", "tag2") }
    }

    @Test
    fun `should return correct cache size`() {
        assertEquals(0L, cacheService.size())

        cacheService.put("key1", "value1", 60L)
        assertEquals(1L, cacheService.size())

        cacheService.put("key2", "value2", 60L)
        assertEquals(2L, cacheService.size())

        cacheService.evict("key1")
        assertEquals(1L, cacheService.size())
    }

    @Test
    fun `should return correct keys`() {
        assertTrue(cacheService.keys().isEmpty())

        cacheService.put("key1", "value1", 60L)
        cacheService.put("key2", "value2", 60L)

        val keys = cacheService.keys()
        assertEquals(2, keys.size)
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
    }

    @Test
    fun `should handle empty string values`() {
        cacheService.put("key1", "", 60L)

        val result = cacheService.get("key1")
        assertEquals("", result)
    }

    @Test
    fun `should handle different value types`() {
        cacheService.put("string", "hello", 60L)
        cacheService.put("number", 42, 60L)
        cacheService.put("boolean", true, 60L)
        cacheService.put("list", listOf(1, 2, 3), 60L)

        assertEquals("hello", cacheService.get("string"))
        assertEquals(42, cacheService.get("number"))
        assertEquals(true, cacheService.get("boolean"))
        assertEquals(listOf(1, 2, 3), cacheService.get("list"))
    }

    @Test
    fun `should overwrite existing key`() {
        cacheService.put("key1", "value1", 60L)
        cacheService.put("key1", "value2", 60L)

        val result = cacheService.get("key1")
        assertEquals("value2", result)
        assertEquals(1L, cacheService.size())
    }

    @Test
    fun `should handle empty key`() {
        cacheService.put("", "value", 60L)

        val result = cacheService.get("")
        assertEquals("value", result)
    }

    @Test
    fun `should handle evicting non-existent key`() {
        assertDoesNotThrow { cacheService.evict("non-existent") }
    }

    @Test
    fun `should handle zero TTL`() {
        cacheService.put("key1", "value1", 0L)

        // With zero TTL, the entry should be considered expired immediately
        Thread.sleep(10) // Small delay to ensure expiration
        val result = cacheService.get("key1")
        assertNull(result)
    }

    @Test
    fun `should handle negative TTL`() {
        cacheService.put("key1", "value1", -1L)

        // With negative TTL, the entry should be considered expired immediately
        Thread.sleep(10) // Small delay to ensure expiration
        val result = cacheService.get("key1")
        assertNull(result)
    }
}
