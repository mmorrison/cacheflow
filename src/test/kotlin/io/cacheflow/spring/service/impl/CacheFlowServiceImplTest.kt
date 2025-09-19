package io.cacheflow.spring.service.impl






import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class CacheFlowServiceImplTest {

    private lateinit var cacheService: CacheFlowServiceImpl

    @BeforeEach
    fun setUp() {
        cacheService = CacheFlowServiceImpl()
    }

    @Test
    fun `should cache and retrieve value`() {
        cacheService.put("test-key", "test-value", 60)

        val result = cacheService.get("test-key")
        assertEquals("test-value", result)
    }

    @Test
    fun `should return null for non-existent key`() {
        val result = cacheService.get("non-existent")
        assertNull(result)
    }

    @Test
    fun `should evict specific key`() {
        cacheService.put("key1", "value1", 60)
        cacheService.put("key2", "value2", 60)

        cacheService.evict("key1")

        assertNull(cacheService.get("key1"))
        assertEquals("value2", cacheService.get("key2"))
    }

    @Test
    fun `should evict all keys`() {
        cacheService.put("key1", "value1", 60)
        cacheService.put("key2", "value2", 60)
        cacheService.put("key3", "value3", 60)

        cacheService.evictAll()

        assertNull(cacheService.get("key1"))
        assertNull(cacheService.get("key2"))
        assertNull(cacheService.get("key3"))
        assertEquals(0L, cacheService.size())
    }

    @Test
    fun `should return correct cache size`() {
        assertEquals(0L, cacheService.size())

        cacheService.put("key1", "value1", 60)
        assertEquals(1L, cacheService.size())

        cacheService.put("key2", "value2", 60)
        assertEquals(2L, cacheService.size())

        cacheService.evict("key1")
        assertEquals(1L, cacheService.size())
    }

    @Test
    fun `should return correct keys`() {
        assertTrue(cacheService.keys().isEmpty())

        cacheService.put("key1", "value1", 60)
        cacheService.put("key2", "value2", 60)

        val keys = cacheService.keys()
        assertEquals(2, keys.size)
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
    }

    @Test
    fun `should handle empty string values`() {
        cacheService.put("key1", "", 60)

        val result = cacheService.get("key1")
        assertEquals("", result)
    }

    @Test
    fun `should handle different value types`() {
        cacheService.put("string", "hello", 60)
        cacheService.put("number", 42, 60)
        cacheService.put("boolean", true, 60)
        cacheService.put("list", listOf(1, 2, 3), 60)

        assertEquals("hello", cacheService.get("string"))
        assertEquals(42, cacheService.get("number"))
        assertEquals(true, cacheService.get("boolean"))
        assertEquals(listOf(1, 2, 3), cacheService.get("list"))
    }

    @Test
    fun `should overwrite existing key`() {
        cacheService.put("key1", "value1", 60)
        cacheService.put("key1", "value2", 60)

        val result = cacheService.get("key1")
        assertEquals("value2", result)
        assertEquals(1L, cacheService.size())
    }

    @Test
    fun `should handle empty key`() {
        cacheService.put("", "value", 60)

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

    @Test
    fun `should expire entries after TTL`() {
        cacheService.put("key1", "value1", 1L) // 1 second TTL

        // Should be available immediately
        assertEquals("value1", cacheService.get("key1"))

        // Wait for expiration
        Thread.sleep(1100)

        // Should be expired now
        assertNull(cacheService.get("key1"))
    }

    @Test
    fun `should not expire entries before TTL`() {
        cacheService.put("key1", "value1", 5L) // 5 second TTL

        // Should be available immediately
        assertEquals("value1", cacheService.get("key1"))

        // Wait a bit but not enough to expire
        Thread.sleep(2000)

        // Should still be available
        assertEquals("value1", cacheService.get("key1"))
    }

    @Test
    fun `should handle evictByTags method`() {
        // Note: evictByTags is not implemented in CacheFlowServiceImpl
        // This test verifies the method exists and can be called
        assertDoesNotThrow { cacheService.evictByTags("tag1", "tag2") }
    }

    @Test
    fun `should handle concurrent access`() {
        val threads = mutableListOf<Thread>()
        val results = mutableListOf<Any?>()

        // Add some initial data
        cacheService.put("key1", "value1", 60)
        cacheService.put("key2", "value2", 60)

        // Create multiple threads that read and write
        repeat(10) { i ->
            val thread = Thread {
                cacheService.put("key$i", "value$i", 60)
                results.add(cacheService.get("key$i"))
            }
            threads.add(thread)
            thread.start()
        }

        // Wait for all threads to complete
        threads.forEach { it.join() }

        // Verify all values were stored and retrieved
        assertEquals(10, results.size)
        results.forEach { assertNotNull(it) }
    }

    @Test
    fun `should handle large number of entries`() {
        val entryCount = 1000

        // Add many entries
        repeat(entryCount) { i -> cacheService.put("key$i", "value$i", 60) }

        assertEquals(entryCount.toLong(), cacheService.size())
        assertEquals(entryCount, cacheService.keys().size)

        // Verify random entries
        repeat(10) {
            val randomKey = "key${(0 until entryCount).random()}"
            val expectedValue = "value${randomKey.substring(3)}"
            assertEquals(expectedValue, cacheService.get(randomKey))
        }
    }

    @Test
    fun `should handle special characters in keys and values`() {
        val specialKey = "key with spaces!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val specialValue = "value with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"

        cacheService.put(specialKey, specialValue, 60)

        val result = cacheService.get(specialKey)
        assertEquals(specialValue, result)
    }

    @Test
    fun `should handle very long keys and values`() {
        val longKey = "a".repeat(1000)
        val longValue = "b".repeat(1000)

        cacheService.put(longKey, longValue, 60)

        val result = cacheService.get(longKey)
        assertEquals(longValue, result)
    }

    @Test
    fun `should handle evictAll on empty cache`() {
        assertDoesNotThrow { cacheService.evictAll() }
        assertEquals(0L, cacheService.size())
    }

    @Test
    fun `should handle evict on empty cache`() {
        assertDoesNotThrow { cacheService.evict("any-key") }
        assertEquals(0L, cacheService.size())
    }

    @Test
    fun `should maintain keys set consistency`() {
        cacheService.put("key1", "value1", 60)
        cacheService.put("key2", "value2", 60)

        val keys1 = cacheService.keys()
        val keys2 = cacheService.keys()

        assertEquals(keys1, keys2)
        assertEquals(2, keys1.size)

        cacheService.evict("key1")

        val keys3 = cacheService.keys()
        assertEquals(1, keys3.size)
        assertTrue(keys3.contains("key2"))
        assertFalse(keys3.contains("key1"))
    }
}
