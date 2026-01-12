package io.cacheflow.spring.dependency

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe implementation of DependencyResolver for tracking cache dependencies.
 *
 * This implementation uses concurrent data structures to ensure thread safety while maintaining
 * high performance for dependency tracking operations.
 */
@Component
class CacheDependencyTracker : DependencyResolver {
    // Maps cache key -> set of dependency keys
    private val dependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()

    // Maps dependency key -> set of cache keys that depend on it
    private val reverseDependencyGraph = ConcurrentHashMap<String, MutableSet<String>>()

    // Lock for atomic operations on both graphs
    private val lock = ReentrantReadWriteLock()

    override fun trackDependency(
        cacheKey: String,
        dependencyKey: String,
    ) {
        if (cacheKey == dependencyKey) {
            // Prevent self-dependency
            return
        }

        lock.write {
            // Add to dependency graph
            dependencyGraph
                .computeIfAbsent(cacheKey) { ConcurrentHashMap.newKeySet() }
                .add(dependencyKey)

            // Add to reverse dependency graph
            reverseDependencyGraph
                .computeIfAbsent(dependencyKey) { ConcurrentHashMap.newKeySet() }
                .add(cacheKey)
        }
    }

    override fun invalidateDependentCaches(dependencyKey: String): Set<String> =
        lock.read { reverseDependencyGraph[dependencyKey]?.toSet() ?: emptySet() }

    override fun getDependencies(cacheKey: String): Set<String> = lock.read { dependencyGraph[cacheKey]?.toSet() ?: emptySet() }

    override fun getDependentCaches(dependencyKey: String): Set<String> =
        lock.read { reverseDependencyGraph[dependencyKey]?.toSet() ?: emptySet() }

    override fun removeDependency(
        cacheKey: String,
        dependencyKey: String,
    ) {
        lock.write {
            // Remove from dependency graph
            dependencyGraph[cacheKey]?.remove(dependencyKey)

            // Remove from reverse dependency graph
            reverseDependencyGraph[dependencyKey]?.remove(cacheKey)

            // Clean up empty sets
            if (dependencyGraph[cacheKey]?.isEmpty() == true) {
                dependencyGraph.remove(cacheKey)
            }
            if (reverseDependencyGraph[dependencyKey]?.isEmpty() == true) {
                reverseDependencyGraph.remove(dependencyKey)
            }
        }
    }

    override fun clearDependencies(cacheKey: String) {
        lock.write {
            val dependencies = dependencyGraph.remove(cacheKey) ?: return

            // Remove from reverse dependency graph
            dependencies.forEach { dependencyKey ->
                reverseDependencyGraph[dependencyKey]?.remove(cacheKey)
                if (reverseDependencyGraph[dependencyKey]?.isEmpty() == true) {
                    reverseDependencyGraph.remove(dependencyKey)
                }
            }
        }
    }

    override fun getDependencyCount(): Int = lock.read { dependencyGraph.values.sumOf { it.size } }

    /**
     * Gets statistics about the dependency graph.
     *
     * @return Map containing various statistics
     */
    fun getStatistics(): Map<String, Any> =
        lock.read {
            mapOf(
                "totalDependencies" to dependencyGraph.values.sumOf { it.size },
                "totalCacheKeys" to dependencyGraph.size,
                "totalDependencyKeys" to reverseDependencyGraph.size,
                "maxDependenciesPerKey" to
                    (dependencyGraph.values.maxOfOrNull { it.size } ?: 0),
                "maxDependentsPerKey" to
                    (reverseDependencyGraph.values.maxOfOrNull { it.size } ?: 0),
            )
        }

    /**
     * Checks if there are any circular dependencies in the graph.
     *
     * @return true if circular dependencies exist, false otherwise
     */
    fun hasCircularDependencies(): Boolean =
        lock.read {
            val cycleDetector = CycleDetector(dependencyGraph)
            cycleDetector.hasCircularDependencies()
        }

    /**
     * Internal class to handle cycle detection logic. Separated to reduce complexity of the main
     * class.
     */
    private class CycleDetector(
        private val dependencyGraph: Map<String, Set<String>>,
    ) {
        private val visited = mutableSetOf<String>()
        private val recursionStack = mutableSetOf<String>()

        fun hasCircularDependencies(): Boolean =
            dependencyGraph.keys.any { key ->
                if (!visited.contains(key)) {
                    hasCycleFromNode(key)
                } else {
                    false
                }
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
