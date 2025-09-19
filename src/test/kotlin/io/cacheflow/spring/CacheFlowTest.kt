package io.cacheflow.spring

import io.cacheflow.spring.service.impl.CacheFlowServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CacheFlowTest {

    @Test
    fun `should cache and retrieve`() {
        val cacheService = CacheFlowServiceImpl()

        // Put a value
        cacheService.put("test-key", "test-value", 60)

        // Get the value
        val result = cacheService.get("test-key")
        assertEquals("test-value", result)
    }

    @Test
    fun `should evict cached values`() {
        val cacheService = CacheFlowServiceImpl()

        // Put a value
        cacheService.put("test-key", "test-value", 60)

        // Verify it's cached
        val cached = cacheService.get("test-key")
        assertEquals("test-value", cached)

        // Evict it
        cacheService.evict("test-key")

        // Verify it's evicted
        val evicted = cacheService.get("test-key")
        assertNull(evicted)
    }

    @Test
    fun `testReturnNull`() {
        val cacheService = CacheFlowServiceImpl()

        val result = cacheService.get("non-existent-key")
        assertNull(result)
    }

    @Test
    fun `should handle cache size`() {
        val cacheService = CacheFlowServiceImpl()

        // Initially empty
        assertEquals(0L, cacheService.size())
        assertEquals(0, cacheService.keys().size)

        // Add some values
        cacheService.put("key1", "value1", 60)
        cacheService.put("key2", "value2", 60)

        // Check size and keys
        assertEquals(2L, cacheService.size())
        assertEquals(2, cacheService.keys().size)
        assertEquals(setOf("key1", "key2"), cacheService.keys())

        // Evict all
        cacheService.evictAll()
        assertEquals(0L, cacheService.size())
        assertEquals(0, cacheService.keys().size)
    }
}
