package io.cacheflow.spring.integration

import io.cacheflow.spring.annotation.CacheFlow
import io.cacheflow.spring.annotation.CacheFlowComposition
import io.cacheflow.spring.annotation.CacheFlowEvict
import io.cacheflow.spring.annotation.CacheFlowFragment
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.fragment.FragmentCacheService
import io.cacheflow.spring.service.CacheFlowService
import io.cacheflow.spring.versioning.CacheKeyVersioner
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Service
import java.time.Instant

@SpringBootTest(classes = [TestConfiguration::class])
class RussianDollCachingIntegrationTest {

    @Autowired private lateinit var cacheService: CacheFlowService

    @Autowired private lateinit var fragmentCacheService: FragmentCacheService

    @Autowired private lateinit var dependencyResolver: DependencyResolver

    @Autowired private lateinit var cacheKeyVersioner: CacheKeyVersioner

    @Autowired private lateinit var testService: RussianDollTestService

    @Test
    fun `should implement complete russian doll caching pattern`() {
        // Given
        val userId = 123L
        val profileId = 456L
        val settingsId = 789L

        // When - Call methods that create nested fragments
        val userProfile = testService.getUserProfile(userId, profileId)
        val userSettings = testService.getUserSettings(userId, settingsId)
        val userHeader = testService.getUserHeader(userId)
        val userFooter = testService.getUserFooter(userId)

        // Then - Verify fragments are cached
        assertNotNull(userProfile)
        assertNotNull(userSettings)
        assertNotNull(userHeader)
        assertNotNull(userFooter)

        // Verify fragments are cached individually
        assertTrue(fragmentCacheService.hasFragment("user:$userId:profile:$profileId"))
        assertTrue(fragmentCacheService.hasFragment("user:$userId:settings:$settingsId"))
        assertTrue(fragmentCacheService.hasFragment("user:$userId:header"))
        assertTrue(fragmentCacheService.hasFragment("user:$userId:footer"))

        // When - Compose fragments into a complete page
        val completePage = testService.getCompleteUserPage(userId, profileId, settingsId)

        // Then - Verify composition is cached
        assertNotNull(completePage)
        assertTrue(completePage.contains("User Profile Content"))
        assertTrue(completePage.contains("User Settings Content"))
        assertTrue(completePage.contains("User Header"))
        assertTrue(completePage.contains("User Footer"))
    }

    @Test
    fun `should handle dependency invalidation correctly`() {
        // Given
        val userId = 123L
        val profileId = 456L

        // When - Create cached content
        val userProfile = testService.getUserProfile(userId, profileId)
        val userHeader = testService.getUserHeader(userId)
        val completePage = testService.getCompleteUserPage(userId, profileId, 789L)

        // Then - Verify content is cached
        assertNotNull(userProfile)
        assertNotNull(userHeader)
        assertNotNull(completePage)

        // When - Update user (this should invalidate dependent caches)
        testService.updateUser(userId, "Updated Name")

        // Then - Verify dependent caches are invalidated
        assertNull(cacheService.get("user:$userId:profile:$profileId"))
        assertNull(cacheService.get("user:$userId:header"))
        assertNull(cacheService.get("user:$userId:page:$profileId:789"))

        // But fragments should still be cached
        assertTrue(fragmentCacheService.hasFragment("user:$userId:profile:$profileId"))
        assertTrue(fragmentCacheService.hasFragment("user:$userId:header"))
    }

    @Test
    fun `should handle versioned cache keys correctly`() {
        // Given
        val userId = 123L
        val timestamp = Instant.now().toEpochMilli()

        // When - Call method with versioned caching
        val versionedResult = testService.getVersionedUserData(userId, timestamp)

        // Then - Verify versioned key is used
        assertNotNull(versionedResult)
        val versionedKey = "user:$userId:versioned-v$timestamp"
        assertNotNull(cacheService.get(versionedKey))

        // When - Call with different timestamp
        val newTimestamp = timestamp + 1000
        val newVersionedResult = testService.getVersionedUserData(userId, newTimestamp)

        // Then - Verify new versioned key is used
        assertNotNull(newVersionedResult)
        val newVersionedKey = "user:$userId:versioned-v$newTimestamp"
        assertNotNull(cacheService.get(newVersionedKey))

        // Both versions should exist
        assertNotNull(cacheService.get(versionedKey))
        assertNotNull(cacheService.get(newVersionedKey))
    }

    @Test
    fun `should handle fragment composition with templates`() {
        // Given
        val userId = 123L
        val profileId = 456L

        // When - Create fragments
        val headerFragment = testService.getUserHeader(userId)
        val profileFragment = testService.getUserProfile(userId, profileId)
        val footerFragment = testService.getUserFooter(userId)

        // Then - Verify fragments are created
        assertNotNull(headerFragment)
        assertNotNull(profileFragment)
        assertNotNull(footerFragment)

        // When - Compose using template
        val composedPage = testService.composeUserPageWithTemplate(userId, profileId)

        // Then - Verify composition includes all fragments
        assertNotNull(composedPage)
        assertTrue(composedPage.contains("User Header"))
        assertTrue(composedPage.contains("User Profile Content"))
        assertTrue(composedPage.contains("User Footer"))
    }

    @Test
    fun `should handle tag-based invalidation`() {
        // Given
        val userId = 123L
        val profileId = 456L

        // When - Create tagged fragments
        val userProfile = testService.getUserProfile(userId, profileId)
        val userSettings = testService.getUserSettings(userId, 789L)

        // Then - Verify fragments are cached
        assertNotNull(userProfile)
        assertNotNull(userSettings)
        assertTrue(fragmentCacheService.hasFragment("user:$userId:profile:$profileId"))
        assertTrue(fragmentCacheService.hasFragment("user:$userId:settings:789"))

        // When - Invalidate by tag
        testService.invalidateUserFragments(userId)

        // Then - Verify tagged fragments are invalidated
        assertNull(fragmentCacheService.getFragment("user:$userId:profile:$profileId"))
        assertNull(fragmentCacheService.getFragment("user:$userId:settings:789"))
    }

    @Service
    class RussianDollTestService(
        private val fragmentCacheService: FragmentCacheService
    ) {

        @CacheFlowFragment(
            key = "'user:' + #userId + ':profile:' + #profileId",
            dependsOn = ["userId"],
            tags = ["'user-' + #userId"],
            ttl = 3600
        )
        fun getUserProfile(userId: Long, profileId: Long): String {
            return "User Profile Content for user $userId, profile $profileId"
        }

        @CacheFlowFragment(
            key = "'user:' + #userId + ':settings:' + #settingsId",
            dependsOn = ["userId"],
            tags = ["'user-' + #userId"],
            ttl = 3600
        )
        fun getUserSettings(userId: Long, settingsId: Long): String {
            return "User Settings Content for user $userId, settings $settingsId"
        }

        @CacheFlowFragment(
            key = "'user:' + #userId + ':header'",
            dependsOn = ["userId"],
            tags = ["'user-' + #userId"],
            ttl = 3600
        )
        fun getUserHeader(userId: Long): String {
            return "User Header for user $userId"
        }

        @CacheFlowFragment(
            key = "'user:' + #userId + ':footer'",
            dependsOn = ["userId"],
            tags = ["'user-' + #userId"],
            ttl = 3600
        )
        fun getUserFooter(userId: Long): String {
            return "User Footer for user $userId"
        }

        @CacheFlowComposition(
            key = "'user:' + #userId + ':page:' + #profileId + ':' + #settingsId",
            template =
            "<div>{{header}}</div><div>{{profile}}</div><div>{{settings}}</div><div>{{footer}}</div>",
            fragments =
            [
                "'user:' + #userId + ':header'",
                "'user:' + #userId + ':profile:' + #profileId",
                "'user:' + #userId + ':settings:' + #settingsId",
                "'user:' + #userId + ':footer'"
            ],
            ttl = 1800
        )
        fun getCompleteUserPage(userId: Long, profileId: Long, settingsId: Long): String {
            // This method should not be called due to composition
            return "This should not be called"
        }

        @CacheFlow(
            key = "'user:' + #userId + ':versioned'",
            versioned = true,
            timestampField = "timestamp",
            ttl = 3600
        )
        fun getVersionedUserData(userId: Long, timestamp: Long): String {
            return "Versioned data for user $userId at timestamp $timestamp"
        }

        @CacheFlow(key = "'user:' + #userId", dependsOn = ["userId"], ttl = 3600)
        fun getUser(userId: Long): String {
            return "User $userId"
        }

        @CacheFlowEvict(key = "'userId:' + #userId")
        fun updateUser(userId: Long, name: String): String {
            return "Updated user $userId with name $name"
        }

        fun composeUserPageWithTemplate(userId: Long, profileId: Long): String {
            val template =
                "<html><head><title>User Page</title></head><body>{{header}}{{profile}}{{footer}}</body></html>"
            val fragments =
                mapOf(
                    "header" to getUserHeader(userId),
                    "profile" to getUserProfile(userId, profileId),
                    "footer" to getUserFooter(userId)
                )
            return template.replace("{{header}}", fragments["header"]!!)
                .replace("{{profile}}", fragments["profile"]!!)
                .replace("{{footer}}", fragments["footer"]!!)
        }

        fun invalidateUserFragments(userId: Long) {
            // This would typically be called by a service that manages cache invalidation
            // For testing purposes, we'll simulate the invalidation by calling the fragment cache service
            // The actual implementation would be in a service, but for testing we'll call it
            // directly

            fragmentCacheService.invalidateFragmentsByTag("user-$userId")
        }
    }
}
