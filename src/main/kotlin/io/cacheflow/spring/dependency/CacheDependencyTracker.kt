package io.cacheflow.spring.dependency

import io.cacheflow.spring.config.CacheFlowProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe implementation of DependencyResolver for tracking cache dependencies.
 *
 * Supports distributed caching via Redis sets when configured, falling back to in-memory
 * ConcurrentHashMap for local caching or when Redis is unavailable.
 */
@Component
class CacheDependencyTracker(
    private val properties: CacheFlowProperties,
    private val redisTemplate: StringRedisTemplate? = null,
) : DependencyResolver {
    private val logger = LoggerFactory.getLogger(CacheDependencyTracker::class.java)

    // Maps cache key -> set of dependency keys (L1 fallback)
    private val dependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()

    // Maps dependency key -> set of cache keys that depend on it (L1 fallback)
    private val reverseDependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()

    // Lock for atomic operations on local graphs
    private val lock = ReentrantReadWriteLock()

    private val isRedisEnabled: Boolean
        get() = properties.storage == CacheFlowProperties.StorageType.REDIS && redisTemplate != null

    private fun getRedisDependencyKey(cacheKey: String): String =
        "${properties.redis.keyPrefix}deps:$cacheKey"

    private fun getRedisReverseDependencyKey(dependencyKey: String): String =
        "${properties.redis.keyPrefix}rev-deps:$dependencyKey"

    override fun trackDependency(
        cacheKey: String,
        dependencyKey: String,
    ) {
        if (cacheKey == dependencyKey) return

        if (isRedisEnabled) {
            try {
                redisTemplate!!.opsForSet().add(getRedisDependencyKey(cacheKey), dependencyKey)
                redisTemplate.opsForSet().add(getRedisReverseDependencyKey(dependencyKey), cacheKey)
            } catch (e: Exception) {
                logger.error("Error tracking dependency in Redis", e)
            }
        } else {
            lock.write {
                dependencyGraph
                    .computeIfAbsent(cacheKey) { ConcurrentHashMap.newKeySet() }
                    .add(dependencyKey)
                reverseDependencyGraph
                    .computeIfAbsent(dependencyKey) { ConcurrentHashMap.newKeySet() }
                    .add(cacheKey)
            }
        }
    }

    override fun invalidateDependentCaches(dependencyKey: String): Set<String> {
        if (isRedisEnabled) {
            return try {
                redisTemplate!!.opsForSet().members(getRedisReverseDependencyKey(dependencyKey)) ?: emptySet()
            } catch (e: Exception) {
                logger.error("Error retrieving dependent caches from Redis", e)
                emptySet()
            }
        }
        return lock.read { reverseDependencyGraph[dependencyKey]?.toSet() ?: emptySet() }
    }

    override fun getDependencies(cacheKey: String): Set<String> {
        if (isRedisEnabled) {
            return try {
                redisTemplate!!.opsForSet().members(getRedisDependencyKey(cacheKey)) ?: emptySet()
            } catch (e: Exception) {
                logger.error("Error retrieving dependencies from Redis", e)
                emptySet()
            }
        }
        return lock.read { dependencyGraph[cacheKey]?.toSet() ?: emptySet() }
    }

    override fun getDependentCaches(dependencyKey: String): Set<String> {
        if (isRedisEnabled) {
            return try {
                redisTemplate!!.opsForSet().members(getRedisReverseDependencyKey(dependencyKey)) ?: emptySet()
            } catch (e: Exception) {
                logger.error("Error retrieving dependent caches from Redis", e)
                emptySet()
            }
        }
        return lock.read { reverseDependencyGraph[dependencyKey]?.toSet() ?: emptySet() }
    }

    override fun removeDependency(
        cacheKey: String,
        dependencyKey: String,
    ) {
        if (isRedisEnabled) {
            try {
                redisTemplate!!.opsForSet().remove(getRedisDependencyKey(cacheKey), dependencyKey)
                redisTemplate.opsForSet().remove(getRedisReverseDependencyKey(dependencyKey), cacheKey)
            } catch (e: Exception) {
                logger.error("Error removing dependency from Redis", e)
            }
        } else {
            lock.write {
                dependencyGraph[cacheKey]?.remove(dependencyKey)
                reverseDependencyGraph[dependencyKey]?.remove(cacheKey)
                if (dependencyGraph[cacheKey]?.isEmpty() == true) {
                    dependencyGraph.remove(cacheKey)
                }
                if (reverseDependencyGraph[dependencyKey]?.isEmpty() == true) {
                    reverseDependencyGraph.remove(dependencyKey)
                }
            }
        }
    }

    override fun clearDependencies(cacheKey: String) {
        if (isRedisEnabled) {
            try {
                val depsKey = getRedisDependencyKey(cacheKey)
                val dependencies = redisTemplate!!.opsForSet().members(depsKey)
                if (!dependencies.isNullOrEmpty()) {
                    redisTemplate.delete(depsKey)
                    dependencies.forEach { dependencyKey ->
                        val revKey = getRedisReverseDependencyKey(dependencyKey)
                        redisTemplate.opsForSet().remove(revKey, cacheKey)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error clearing dependencies from Redis", e)
            }
        } else {
            lock.write {
                val dependencies = dependencyGraph.remove(cacheKey) ?: return
                dependencies.forEach { dependencyKey ->
                    reverseDependencyGraph[dependencyKey]?.remove(cacheKey)
                    if (reverseDependencyGraph[dependencyKey]?.isEmpty() == true) {
                        reverseDependencyGraph.remove(dependencyKey)
                    }
                }
            }
        }
    }

    override fun getDependencyCount(): Int {
        if (isRedisEnabled) {
            // Note: This is expensive in Redis as it requires scanning keys.
            // Using KEYS or SCAN which should be used with caution in production.
            // For now, returning -1 or unsupported might be better, or standard implementation
            // matching local behavior using SCAN (simulated here safely or skipped).
            // Simplest safe approach for now: return local count if using mixed mode, otherwise 0/unknown.
            // But to adhere to interface, we'll implement a safe count if possible or just log warning.
            // Let's defer full implementation to avoid blocking scans and return 0 for now with log.
            // Real implementation would ideally require a separate counter or HyperLogLog.
            return 0
        }
        return lock.read { dependencyGraph.values.sumOf { it.size } }
    }

    /**
     * Gets statistics about the dependency graph.
     */
    fun getStatistics(): Map<String, Any> =
        if (isRedisEnabled) {
            mapOf("info" to "Distributed statistics not fully implemented for performance reasons")
        } else {
            lock.read {
                mapOf(
                    "totalDependencies" to dependencyGraph.values.sumOf { it.size },
                    "totalCacheKeys" to dependencyGraph.size,
                    "totalDependencyKeys" to reverseDependencyGraph.size,
                    "maxDependenciesPerKey" to (dependencyGraph.values.maxOfOrNull { it.size } ?: 0),
                    "maxDependentsPerKey" to (reverseDependencyGraph.values.maxOfOrNull { it.size } ?: 0),
                )
            }
        }

    /**
     * Checks if there are any circular dependencies.
     * Note: Full circular check in distributed graph is very expensive.
     */
    fun hasCircularDependencies(): Boolean =
        if (isRedisEnabled) {
            false // Not implemented for distributed graph due to complexity/cost
        } else {
            lock.read {
                val cycleDetector = CycleDetector(dependencyGraph)
                cycleDetector.hasCircularDependencies()
            }
        }

    private class CycleDetector(
        private val dependencyGraph: Map<String, Set<String>>,
    ) {
        private val visited = mutableSetOf<String>()
        private val recursionStack = mutableSetOf<String>()

        fun hasCircularDependencies(): Boolean =
            dependencyGraph.keys.any { key ->
                if (!visited.contains(key)) hasCycleFromNode(key) else false
            }

        private fun hasCycleFromNode(node: String): Boolean =
            when {
                isInRecursionStack(node) -> true
                isAlreadyVisited(node) -> false
                else -> {
                    markNodeAsVisited(node)
                    addToRecursionStack(node)
                    val hasCycle = checkDependenciesForCycle(node)
                    removeFromRecursionStack(node)
                    hasCycle
                }
            }

        private fun isInRecursionStack(node: String): Boolean = recursionStack.contains(node)

        private fun isAlreadyVisited(node: String): Boolean = visited.contains(node)

        private fun markNodeAsVisited(node: String) {
            visited.add(node)
        }

        private fun addToRecursionStack(node: String) {
            recursionStack.add(node)
        }

        private fun removeFromRecursionStack(node: String) {
            recursionStack.remove(node)
        }

        private fun checkDependenciesForCycle(node: String): Boolean {
            val dependencies = dependencyGraph[node] ?: emptySet()
            return dependencies.any { dependency -> hasCycleFromNode(dependency) }
        }
    }
}
