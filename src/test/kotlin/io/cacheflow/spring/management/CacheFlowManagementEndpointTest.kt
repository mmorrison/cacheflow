package io.cacheflow.spring.management

import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.service.impl.CacheFlowServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class CacheFlowManagementEndpointTest {

    private lateinit var cacheService: CacheFlowService
    private lateinit var endpoint: CacheFlowManagementEndpoint

    @BeforeEach
    fun setUp() {
        cacheService = CacheFlowServiceImpl()
        endpoint = CacheFlowManagementEndpoint(cacheService)
    }

    @Test
    fun `should return cache info with size and keys`() {
        // Add some test data
        cacheService.put("key1", "value1", 60)
        cacheService.put("key2", "value2", 60)

        val result = endpoint.getCacheInfo()

        assertNotNull(result)
        assertEquals(2L, result["size"])
        assertTrue(result["keys"] is Set<*>)
        val keys = result["keys"] as Set<*>
        assertEquals(2, keys.size)
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
    }

    @Test
    fun `should return empty cache info when cache is empty`() {
        val result = endpoint.getCacheInfo()

        assertNotNull(result)
        assertEquals(0L, result["size"])
        assertTrue(result["keys"] is Set<*>)
        val keys = result["keys"] as Set<*>
        assertTrue(keys.isEmpty())
    }

    @Test
    fun `should evict by pattern`() {
        // Add test data
        cacheService.put("user:123", "userData", 60)
        cacheService.put("user:456", "userData2", 60)
        cacheService.put("product:789", "productData", 60)

        val result = endpoint.evictByPattern("user:")

        assertNotNull(result)
        assertEquals(2, result["evicted"])
        assertEquals("user:", result["pattern"])

        // Verify only user keys were evicted
        val remainingKeys = cacheService.keys()
        assertEquals(1, remainingKeys.size)
        assertTrue(remainingKeys.contains("product:789"))
    }

    @Test
    fun `should evict by pattern with no matches`() {
        cacheService.put("key1", "value1", 60)
        cacheService.put("key2", "value2", 60)

        val result = endpoint.evictByPattern("nonexistent")

        assertNotNull(result)
        assertEquals(0, result["evicted"])
        assertEquals("nonexistent", result["pattern"])

        // Verify no keys were evicted
        val remainingKeys = cacheService.keys()
        assertEquals(2, remainingKeys.size)
    }

    @Test
    fun `should evict by tags`() {
        // Note: evictByTags is not implemented in CacheFlowServiceImpl, so this tests the endpoint
        // logic
        val result = endpoint.evictByTags("tag1,tag2")

        assertNotNull(result)
        assertEquals("all", result["evicted"])
        assertTrue(result["tags"] is Array<*>)
        val tags = result["tags"] as Array<*>
        assertEquals(2, tags.size)
        assertTrue(tags.contains("tag1"))
        assertTrue(tags.contains("tag2"))
    }

    @Test
    fun `should evict by single tag`() {
        val result = endpoint.evictByTags("single-tag")

        assertNotNull(result)
        assertEquals("all", result["evicted"])
        assertTrue(result["tags"] is Array<*>)
        val tags = result["tags"] as Array<*>
        assertEquals(1, tags.size)
        assertTrue(tags.contains("single-tag"))
    }

    @Test
    fun `should evict all entries`() {
        // Add test data
        cacheService.put("key1", "value1", 60)
        cacheService.put("key2", "value2", 60)

        val result = endpoint.evictAll()

        assertNotNull(result)
        assertEquals("all", result["evicted"])

        // Verify all keys were evicted
        val remainingKeys = cacheService.keys()
        assertTrue(remainingKeys.isEmpty())
    }

    @Test
    fun `should handle empty cache when evicting all`() {
        val result = endpoint.evictAll()

        assertNotNull(result)
        assertEquals("all", result["evicted"])
    }

    @Test
    fun `should handle tags with extra whitespace`() {
        val result = endpoint.evictByTags(" tag1 , tag2 , tag3 ")

        assertNotNull(result)
        assertEquals("all", result["evicted"])
        assertTrue(result["tags"] is Array<*>)
        val tags = result["tags"] as Array<*>
        assertEquals(3, tags.size)
        assertTrue(tags.contains("tag1"))
        assertTrue(tags.contains("tag2"))
        assertTrue(tags.contains("tag3"))
    }

    @Test
    fun `should handle empty tags string`() {
        val result = endpoint.evictByTags("")

        assertNotNull(result)
        assertEquals("all", result["evicted"])
        assertTrue(result["tags"] is Array<*>)
        val tags = result["tags"] as Array<*>
        assertEquals(1, tags.size)
        assertTrue(tags.contains(""))
    }
}
