package io.cacheflow.spring.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.service.CacheFlowService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.data.redis.core.StringRedisTemplate

class RedisCacheInvalidatorTest {
    private lateinit var properties: CacheFlowProperties
    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var cacheFlowService: CacheFlowService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var invalidator: RedisCacheInvalidator

    @BeforeEach
    fun setUp() {
        properties = CacheFlowProperties()
        redisTemplate = mock()
        cacheFlowService = mock()
        objectMapper = jacksonObjectMapper()
        invalidator = RedisCacheInvalidator(properties, redisTemplate, cacheFlowService, objectMapper)
    }

    @Test
    fun `publish should send message to redis topic`() {
        // Given
        val type = InvalidationType.EVICT
        val keys = setOf("key1", "key2")

        // When
        invalidator.publish(type, keys = keys)

        // Then
        verify(redisTemplate).convertAndSend(eq("cacheflow:invalidation"), any<String>())
    }

    @Test
    fun `handleMessage should ignore message from self`() {
        // Given
        val message = CacheInvalidationMessage(InvalidationType.EVICT, keys = setOf("key1"), origin = invalidator.instanceId)
        val json = objectMapper.writeValueAsString(message)

        // When
        invalidator.handleMessage(json)

        // Then
        verify(cacheFlowService, never()).evictLocal(any())
    }

    @Test
    fun `handleMessage should process EVICT message from other`() {
        // Given
        val message = CacheInvalidationMessage(InvalidationType.EVICT, keys = setOf("key1", "key2"), origin = "other-instance")
        val json = objectMapper.writeValueAsString(message)

        // When
        invalidator.handleMessage(json)

        // Then
        verify(cacheFlowService).evictLocal("key1")
        verify(cacheFlowService).evictLocal("key2")
    }

    @Test
    fun `handleMessage should process EVICT_BY_TAGS message from other`() {
        // Given
        val message = CacheInvalidationMessage(InvalidationType.EVICT_BY_TAGS, tags = setOf("tag1"), origin = "other-instance")
        val json = objectMapper.writeValueAsString(message)

        // When
        invalidator.handleMessage(json)

        // Then
        verify(cacheFlowService).evictLocalByTags("tag1")
    }

    @Test
    fun `handleMessage should process EVICT_ALL message from other`() {
        // Given
        val message = CacheInvalidationMessage(InvalidationType.EVICT_ALL, origin = "other-instance")
        val json = objectMapper.writeValueAsString(message)

        // When
        invalidator.handleMessage(json)

        // Then
        verify(cacheFlowService).evictLocalAll()
    }
}
