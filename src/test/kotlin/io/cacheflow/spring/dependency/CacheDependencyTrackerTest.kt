package io.cacheflow.spring.dependency

import io.cacheflow.spring.config.CacheFlowProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.StringRedisTemplate

class CacheDependencyTrackerTest {
    private lateinit var dependencyTracker: CacheDependencyTracker
    private lateinit var properties: CacheFlowProperties

    @Nested
    inner class InMemoryTests {
        @BeforeEach
        fun setUp() {
            properties = CacheFlowProperties(storage = CacheFlowProperties.StorageType.IN_MEMORY)
            dependencyTracker = CacheDependencyTracker(properties)
        }

        @Test
        fun `should track dependency correctly`() {
            // Given
            val cacheKey = "user:123"
            val dependencyKey = "user:123:profile"

            // When
            dependencyTracker.trackDependency(cacheKey, dependencyKey)

            // Then
            assertTrue(dependencyTracker.getDependencies(cacheKey).contains(dependencyKey))
            assertTrue(dependencyTracker.getDependentCaches(dependencyKey).contains(cacheKey))
            assertEquals(1, dependencyTracker.getDependencyCount())
        }

        @Test
        fun `should not track self-dependency`() {
            // Given
            val key = "user:123"

            // When
            dependencyTracker.trackDependency(key, key)

            // Then
            assertTrue(dependencyTracker.getDependencies(key).isEmpty())
            assertTrue(dependencyTracker.getDependentCaches(key).isEmpty())
            assertEquals(0, dependencyTracker.getDependencyCount())
        }

        @Test
        fun `should track multiple dependencies for same cache key`() {
            // Given
            val cacheKey = "user:123"
            val dependency1 = "user:123:profile"
            val dependency2 = "user:123:settings"

            // When
            dependencyTracker.trackDependency(cacheKey, dependency1)
            dependencyTracker.trackDependency(cacheKey, dependency2)

            // Then
            val dependencies = dependencyTracker.getDependencies(cacheKey)
            assertTrue(dependencies.contains(dependency1))
            assertTrue(dependencies.contains(dependency2))
            assertEquals(2, dependencies.size)
            assertEquals(2, dependencyTracker.getDependencyCount())
        }

        @Test
        fun `should track multiple cache keys depending on same dependency`() {
            // Given
            val dependencyKey = "user:123"
            val cacheKey1 = "user:123:profile"
            val cacheKey2 = "user:123:settings"

            // When
            dependencyTracker.trackDependency(cacheKey1, dependencyKey)
            dependencyTracker.trackDependency(cacheKey2, dependencyKey)

            // Then
            val dependentCaches = dependencyTracker.getDependentCaches(dependencyKey)
            assertTrue(dependentCaches.contains(cacheKey1))
            assertTrue(dependentCaches.contains(cacheKey2))
            assertEquals(2, dependentCaches.size)
            assertEquals(2, dependencyTracker.getDependencyCount())
        }

        @Test
        fun `should invalidate dependent caches correctly`() {
            // Given
            val dependencyKey = "user:123"
            val cacheKey1 = "user:123:profile"
            val cacheKey2 = "user:123:settings"
            val cacheKey3 = "user:456:profile" // Different dependency

            dependencyTracker.trackDependency(cacheKey1, dependencyKey)
            dependencyTracker.trackDependency(cacheKey2, dependencyKey)
            dependencyTracker.trackDependency(cacheKey3, "user:456")

            // When
            val invalidatedKeys = dependencyTracker.invalidateDependentCaches(dependencyKey)

            // Then
            assertTrue(invalidatedKeys.contains(cacheKey1))
            assertTrue(invalidatedKeys.contains(cacheKey2))
            assertFalse(invalidatedKeys.contains(cacheKey3))
            assertEquals(2, invalidatedKeys.size)
        }

        @Test
        fun `should remove specific dependency`() {
            // Given
            val cacheKey = "user:123"
            val dependency1 = "user:123:profile"
            val dependency2 = "user:123:settings"

            dependencyTracker.trackDependency(cacheKey, dependency1)
            dependencyTracker.trackDependency(cacheKey, dependency2)

            // When
            dependencyTracker.removeDependency(cacheKey, dependency1)

            // Then
            val dependencies = dependencyTracker.getDependencies(cacheKey)
            assertFalse(dependencies.contains(dependency1))
            assertTrue(dependencies.contains(dependency2))
            assertEquals(1, dependencies.size)
            assertEquals(1, dependencyTracker.getDependencyCount())
        }

        @Test
        fun `should clear all dependencies for cache key`() {
            // Given
            val cacheKey = "user:123"
            val dependency1 = "user:123:profile"
            val dependency2 = "user:123:settings"

            dependencyTracker.trackDependency(cacheKey, dependency1)
            dependencyTracker.trackDependency(cacheKey, dependency2)

            // When
            dependencyTracker.clearDependencies(cacheKey)

            // Then
            assertTrue(dependencyTracker.getDependencies(cacheKey).isEmpty())
            assertTrue(dependencyTracker.getDependentCaches(dependency1).isEmpty())
            assertTrue(dependencyTracker.getDependentCaches(dependency2).isEmpty())
            assertEquals(0, dependencyTracker.getDependencyCount())
        }

        @Test
        fun `should return empty sets for non-existent keys`() {
            // Given
            val nonExistentKey = "non-existent"

            // When & Then
            assertTrue(dependencyTracker.getDependencies(nonExistentKey).isEmpty())
            assertTrue(dependencyTracker.getDependentCaches(nonExistentKey).isEmpty())
            assertTrue(dependencyTracker.invalidateDependentCaches(nonExistentKey).isEmpty())
        }

        @Test
        fun `should provide correct statistics`() {
            // Given
            dependencyTracker.trackDependency("key1", "dep1")
            dependencyTracker.trackDependency("key1", "dep2")
            dependencyTracker.trackDependency("key2", "dep1")

            // When
            val stats = dependencyTracker.getStatistics()

            // Then
            assertEquals(3, stats["totalDependencies"])
            assertEquals(2, stats["totalCacheKeys"])
            assertEquals(2, stats["totalDependencyKeys"])
            assertEquals(2, stats["maxDependenciesPerKey"])
            assertEquals(2, stats["maxDependentsPerKey"])
        }

        @Test
        fun `should detect circular dependencies`() {
            // Given - Create a circular dependency: key1 -> dep1 -> key1
            dependencyTracker.trackDependency("key1", "dep1")
            dependencyTracker.trackDependency("dep1", "key1")

            // When
            val hasCircular = dependencyTracker.hasCircularDependencies()

            // Then
            assertTrue(hasCircular)
        }

        @Test
        fun `should not detect circular dependencies when none exist`() {
            // Given - Create a linear dependency chain: key1 -> dep1 -> dep2
            dependencyTracker.trackDependency("key1", "dep1")
            dependencyTracker.trackDependency("dep1", "dep2")

            // When
            val hasCircular = dependencyTracker.hasCircularDependencies()

            // Then
            assertFalse(hasCircular)
        }

        @Test
        fun `should handle concurrent access safely`() {
            // Given
            val threads = mutableListOf<Thread>()
            val numThreads = 10
            val operationsPerThread = 100

            // When - Create multiple threads that add dependencies concurrently
            repeat(numThreads) { threadIndex ->
                val thread =
                    Thread {
                        repeat(operationsPerThread) { operationIndex ->
                            val cacheKey = "key$threadIndex:$operationIndex"
                            val dependencyKey = "dep$threadIndex:$operationIndex"
                            dependencyTracker.trackDependency(cacheKey, dependencyKey)
                        }
                    }
                threads.add(thread)
                thread.start()
            }

            // Wait for all threads to complete
            threads.forEach { it.join() }

            // Then - Verify no data corruption occurred
            val stats = dependencyTracker.getStatistics()
            val expectedTotalDependencies = numThreads * operationsPerThread
            assertEquals(expectedTotalDependencies, stats["totalDependencies"])
            assertFalse(dependencyTracker.hasCircularDependencies())
        }
    }

    @Nested
    inner class RedisTests {
        private lateinit var redisTemplate: StringRedisTemplate
        private lateinit var setOperations: SetOperations<String, String>

        @BeforeEach
        fun setUp() {
            properties =
                CacheFlowProperties(
                    storage = CacheFlowProperties.StorageType.REDIS,
                    redis = CacheFlowProperties.RedisProperties(keyPrefix = "test-prefix:"),
                )
            redisTemplate = mock()
            setOperations = mock()
            whenever(redisTemplate.opsForSet()).thenReturn(setOperations)
            dependencyTracker = CacheDependencyTracker(properties, redisTemplate)
        }

        @Test
        fun `should track dependency in Redis`() {
            // Given
            val cacheKey = "user:123"
            val dependencyKey = "user:123:profile"

            // When
            dependencyTracker.trackDependency(cacheKey, dependencyKey)

            // Then
            verify(setOperations).add("test-prefix:deps:$cacheKey", dependencyKey)
            verify(setOperations).add("test-prefix:rev-deps:$dependencyKey", cacheKey)
        }

        @Test
        fun `should get dependencies from Redis`() {
            // Given
            val cacheKey = "user:123"
            val dependencies = setOf("dep1", "dep2")
            whenever(setOperations.members("test-prefix:deps:$cacheKey")).thenReturn(dependencies)

            // When
            val result = dependencyTracker.getDependencies(cacheKey)

            // Then
            assertEquals(dependencies, result)
        }

        @Test
        fun `should get dependent caches from Redis`() {
            // Given
            val dependencyKey = "dep1"
            val dependents = setOf("cache1", "cache2")
            whenever(setOperations.members("test-prefix:rev-deps:$dependencyKey")).thenReturn(dependents)

            // When
            val result = dependencyTracker.getDependentCaches(dependencyKey)

            // Then
            assertEquals(dependents, result)
        }

        @Test
        fun `should remove dependency from Redis`() {
            // Given
            val cacheKey = "user:123"
            val dependencyKey = "dep1"

            // When
            dependencyTracker.removeDependency(cacheKey, dependencyKey)

            // Then
            verify(setOperations).remove("test-prefix:deps:$cacheKey", dependencyKey)
            verify(setOperations).remove("test-prefix:rev-deps:$dependencyKey", cacheKey)
        }

        @Test
        fun `should clear dependencies from Redis`() {
            // Given
            val cacheKey = "user:123"
            val dependencies = setOf("dep1")
            whenever(setOperations.members("test-prefix:deps:$cacheKey")).thenReturn(dependencies)

            // When
            dependencyTracker.clearDependencies(cacheKey)

            // Then
            verify(redisTemplate).delete("test-prefix:deps:$cacheKey")
            verify(setOperations).remove("test-prefix:rev-deps:dep1", cacheKey)
        }

        @Test
        fun `should fallback to empty set on Redis error`() {
            // Given
            val cacheKey = "user:123"
            whenever(setOperations.members(anyString())).thenThrow(RuntimeException("Redis error"))

            // When
            val result = dependencyTracker.getDependencies(cacheKey)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        fun `should handle missing redisTemplate gracefully (fallback to local)`() {
            // Given - Redis enabled in config but template is null (misconfiguration safety check)
            // Although the code checks for redisTemplate != null, let's verify if we pass null
            // expecting it to fall back to local
            properties = CacheFlowProperties(storage = CacheFlowProperties.StorageType.REDIS)
            dependencyTracker = CacheDependencyTracker(properties, null) // Explicit null

            // When
            dependencyTracker.trackDependency("key1", "dep1")

            // Then
            // Verify it stored locally by checking local stats which only exist in local mode
            val stats = dependencyTracker.getStatistics()
            assertEquals(1, stats["totalDependencies"])
        }
    }
}
