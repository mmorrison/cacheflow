package io.cacheflow.spring.example

import io.cacheflow.spring.annotation.CacheFlow
import io.cacheflow.spring.annotation.CacheFlowComposition
import io.cacheflow.spring.annotation.CacheFlowEvict
import io.cacheflow.spring.annotation.CacheFlowFragment
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Example service demonstrating Russian Doll Caching features.
 *
 * This service shows how to use fragment caching, dependency tracking, versioned cache keys, and
 * composition in a real-world scenario.
 */
@Service
class RussianDollCachingExample {
    companion object {
        private const val DEFAULT_TTL_SECONDS = 3600L
        private const val SHORT_TTL_SECONDS = 1800L
        private const val SIMULATION_DELAY_MS = 100L
        private const val SETTINGS_DELAY_MS = 50L
        private const val HEADER_DELAY_MS = 25L
        private const val FOOTER_DELAY_MS = 30L
        private const val SUMMARY_EXTRA_DELAY_MS = 50L
    }

    /**
     * Example of fragment caching with dependency tracking. This fragment depends on the userId
     * parameter and will be invalidated when the user data changes.
     */
    @CacheFlowFragment(
        key = "user:#{userId}:profile",
        dependsOn = ["userId"],
        tags = ["user-#{userId}", "profile"],
        ttl = DEFAULT_TTL_SECONDS,
    )
    fun getUserProfile(userId: Long): String {
        // Simulate expensive database operation
        Thread.sleep(SIMULATION_DELAY_MS)
        return """
            <div class="user-profile">
                <h2>User Profile</h2>
                <p>User ID: $userId</p>
                <p>Last updated: ${Instant.now()}</p>
            </div>
            """.trimIndent()
    }

    /** Example of fragment caching for user settings. */
    @CacheFlowFragment(
        key = "user:#{userId}:settings",
        dependsOn = ["userId"],
        tags = ["user-#{userId}", "settings"],
        ttl = SHORT_TTL_SECONDS,
    )
    @Suppress("UNUSED_PARAMETER")
    fun getUserSettings(userId: Long): String {
        // Simulate expensive database operation
        Thread.sleep(SETTINGS_DELAY_MS)
        return """
            <div class="user-settings">
                <h3>Settings</h3>
                <ul>
                    <li>Theme: Dark</li>
                    <li>Language: English</li>
                    <li>Notifications: Enabled</li>
                </ul>
            </div>
            """.trimIndent()
    }

    /** Example of fragment caching for user header. */
    @CacheFlowFragment(
        key = "user:#{userId}:header",
        dependsOn = ["userId"],
        tags = ["user-#{userId}", "header"],
        ttl = 7200,
    )
    fun getUserHeader(userId: Long): String {
        // Simulate expensive database operation
        Thread.sleep(FOOTER_DELAY_MS)
        return """
            <header class="user-header">
                <h1>Welcome, User $userId!</h1>
                <nav>
                    <a href="/profile">Profile</a>
                    <a href="/settings">Settings</a>
                    <a href="/logout">Logout</a>
                </nav>
            </header>
            """.trimIndent()
    }

    /** Example of fragment caching for user footer. */
    @CacheFlowFragment(
        key = "user:#{userId}:footer",
        dependsOn = ["userId"],
        tags = ["user-#{userId}", "footer"],
        ttl = 7200,
    )
    fun getUserFooter(userId: Long): String {
        // Simulate expensive database operation
        Thread.sleep(HEADER_DELAY_MS)
        return """
            <footer class="user-footer">
                <p>&copy; 2024 User $userId. All rights reserved.</p>
                <p>Last login: ${Instant.now()}</p>
            </footer>
            """.trimIndent()
    }

    /**
     * Example of composition using multiple fragments. This method composes multiple cached
     * fragments into a complete page.
     */
    @CacheFlowComposition(
        key = "user:#{userId}:page",
        template =
            """
            <!DOCTYPE html>
            <html>
            <head>
                <title>User Dashboard</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
                    .container { max-width: 800px; margin: 0 auto; }
                    .user-profile, .user-settings { margin: 20px 0; padding: 15px; border: 1px solid #ddd; }
                </style>
            </head>
            <body>
                <div class="container">
                    {{header}}
                    <main>
                        {{profile}}
                        {{settings}}
                    </main>
                    {{footer}}
                </div>
            </body>
            </html>
        """,
        fragments =
            [
                "user:#{userId}:header",
                "user:#{userId}:profile",
                "user:#{userId}:settings",
                "user:#{userId}:footer",
            ],
        ttl = SHORT_TTL_SECONDS,
    )
    @Suppress("UNUSED_PARAMETER")
    fun getUserDashboard(userId: Long): String =
        // This method should not be called due to composition
        // The fragments will be retrieved from cache and composed
        "This should not be called"

    /**
     * Example of versioned caching. The cache key will include a timestamp version, so the cache
     * will be automatically invalidated when the data changes.
     */
    @CacheFlow(
        key = "user:#{userId}:data",
        versioned = true,
        timestampField = "lastModified",
        ttl = DEFAULT_TTL_SECONDS,
    )
    fun getUserData(
        userId: Long,
        lastModified: Long,
    ): String {
        // Simulate expensive database operation
        Thread.sleep(SIMULATION_DELAY_MS * 2)
        return """
            {
                "userId": $userId,
                "name": "User $userId",
                "email": "user$userId@example.com",
                "lastModified": $lastModified,
                "data": "Some user data that changes over time"
            }
            """.trimIndent()
    }

    /**
     * Example of dependency-based caching. This cache depends on the userId parameter and will be
     * invalidated when the user data changes.
     */
    @CacheFlow(
        key = "user:#{userId}:summary",
        dependsOn = ["userId"],
        tags = ["user-#{userId}", "summary"],
        ttl = SHORT_TTL_SECONDS,
    )
    fun getUserSummary(userId: Long): String {
        // Simulate expensive database operation
        Thread.sleep(SIMULATION_DELAY_MS + SUMMARY_EXTRA_DELAY_MS)
        return """
            <div class="user-summary">
                <h2>User Summary</h2>
                <p>User ID: $userId</p>
                <p>Status: Active</p>
                <p>Member since: 2024-01-01</p>
            </div>
            """.trimIndent()
    }

    /** Example of cache eviction. This method will invalidate all caches related to the user. */
    @CacheFlowEvict(key = "user:#{userId}")
    fun updateUser(
        userId: Long,
        name: String,
        email: String,
    ): String {
        // Simulate database update
        Thread.sleep(SIMULATION_DELAY_MS)
        return "Updated user $userId with name '$name' and email '$email'"
    }

    /**
     * Example of tag-based cache eviction. This method will invalidate all caches with the
     * specified tag.
     */
    fun invalidateUserFragments(userId: Long) {
        // This would typically be called by a cache management service
        // For demonstration purposes, we'll just return a message
        println("Invalidating all fragments for user $userId")
    }

    /** Example of getting cache statistics. This method demonstrates how to check cache status. */
    fun getCacheStatistics(): Map<String, Any> =
        mapOf(
            "message" to "Cache statistics would be available through the CacheFlowService",
            "features" to
                listOf(
                    "Fragment caching",
                    "Dependency tracking",
                    "Versioned cache keys",
                    "Composition",
                    "Tag-based eviction",
                ),
        )
}
