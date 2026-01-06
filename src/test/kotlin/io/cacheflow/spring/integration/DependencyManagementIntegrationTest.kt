package io.cacheflow.spring.integration

import io.cacheflow.spring.annotation.CacheFlow
import io.cacheflow.spring.annotation.CacheFlowEvict
import io.cacheflow.spring.dependency.DependencyResolver
import io.cacheflow.spring.service.CacheFlowService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Service

@SpringBootTest(classes = [TestConfiguration::class])
class DependencyManagementIntegrationTest {

    @Autowired private lateinit var cacheService: CacheFlowService

    @Autowired private lateinit var dependencyResolver: DependencyResolver

    @Autowired private lateinit var testService: TestService

    @Test
    fun `should track and invalidate dependencies correctly`() {
        // Given
        val userId = 123L
        val profileId = 456L

        println("Starting test - testService: $testService")
        println("Cache service: $cacheService")
        println("Dependency resolver: $dependencyResolver")

        // When - Call method that depends on userId
        val result1 = testService.getUserProfile(userId, profileId)

        // Then - Verify cache is populated
        println("Result1: $result1")
        println("Cache service: $cacheService")
        println("Cache service type: ${cacheService::class.java}")
        assertNotNull(result1)
        assertNotNull(cacheService.get("user:$userId:profile:$profileId"))

        // Verify dependency is tracked
        val dependencies = dependencyResolver.getDependencies("user:$userId:profile:$profileId")
        assert(dependencies.contains("userId:$userId"))

        // When - Update user (this should invalidate dependent caches)
        testService.updateUser(userId, "Updated Name")

        // Then - Verify dependent cache is invalidated
        assertNull(cacheService.get("user:$userId:profile:$profileId"))
    }

    @Test
    fun `should handle multiple dependencies correctly`() {
        // Given
        val userId = 789L
        val profileId = 101L
        val settingsId = 202L

        // When - Call methods that depend on userId
        val profile = testService.getUserProfile(userId, profileId)
        val settings = testService.getUserSettings(userId, settingsId)

        // Then - Verify both caches are populated
        assertNotNull(profile)
        assertNotNull(settings)
        assertNotNull(cacheService.get("user:$userId:profile:$profileId"))
        assertNotNull(cacheService.get("user:$userId:settings:$settingsId"))

        // When - Update user
        testService.updateUser(userId, "New Name")

        // Then - Verify both dependent caches are invalidated
        assertNull(cacheService.get("user:$userId:profile:$profileId"))
        assertNull(cacheService.get("user:$userId:settings:$settingsId"))
    }

    @Test
    fun `should not invalidate unrelated caches`() {
        // Given
        val userId1 = 111L
        val userId2 = 222L
        val profileId = 333L

        // When - Create caches for different users
        val profile1 = testService.getUserProfile(userId1, profileId)
        val profile2 = testService.getUserProfile(userId2, profileId)

        // Then - Verify both caches are populated
        assertNotNull(profile1)
        assertNotNull(profile2)
        assertNotNull(cacheService.get("user:$userId1:profile:$profileId"))
        assertNotNull(cacheService.get("user:$userId2:profile:$profileId"))

        // When - Update only user1
        testService.updateUser(userId1, "Updated Name")

        // Then - Verify only user1's cache is invalidated
        assertNull(cacheService.get("user:$userId1:profile:$profileId"))
        assertNotNull(cacheService.get("user:$userId2:profile:$profileId"))
    }

    @Service
    class TestService {

        @CacheFlow(key = "'user:' + #userId + ':profile:' + #profileId", dependsOn = ["userId"], ttl = 3600)
        fun getUserProfile(userId: Long, profileId: Long): String {
            return "Profile for user $userId, profile $profileId"
        }

        @CacheFlow(
            key = "'user:' + #userId + ':settings:' + #settingsId",
            dependsOn = ["userId"],
            ttl = 3600
        )
        fun getUserSettings(userId: Long, settingsId: Long): String {
            return "Settings for user $userId, settings $settingsId"
        }

        @CacheFlowEvict(key = "'userId:' + #userId")
        fun updateUser(userId: Long, name: String): String {
            return "Updated user $userId with name $name"
        }
    }
}
