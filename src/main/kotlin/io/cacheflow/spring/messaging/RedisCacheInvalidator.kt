package io.cacheflow.spring.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import io.cacheflow.spring.config.CacheFlowProperties
import io.cacheflow.spring.service.CacheFlowService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service to handle distributed cache invalidation via Redis Pub/Sub.
 */
@Service
class RedisCacheInvalidator(
    private val property: CacheFlowProperties,
    private val redisTemplate: StringRedisTemplate?,
    private val cacheFlowService: CacheFlowService,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(RedisCacheInvalidator::class.java)
    val instanceId: String = UUID.randomUUID().toString()
    val topic = "cacheflow:invalidation"

    /**
     * Publishes an invalidation message to the Redis topic.
     *
     * @param type The type of invalidation
     * @param keys The keys to invalidate
     * @param tags The tags to invalidate
     */
    fun publish(
        type: InvalidationType,
        keys: Set<String> = emptySet(),
        tags: Set<String> = emptySet(),
    ) {
        if (redisTemplate == null) return

        try {
            val message = CacheInvalidationMessage(type, keys, tags, instanceId)
            val json = objectMapper.writeValueAsString(message)
            redisTemplate.convertAndSend(topic, json)
            logger.debug("Published invalidation message: {}", json)
        } catch (e: Exception) {
            logger.error("Error publishing invalidation message", e)
        }
    }

    /**
     * Handles incoming invalidation messages.
     *
     * @param messageJson The JSON string of the message
     */
    fun handleMessage(messageJson: String) {
        try {
            val message = objectMapper.readValue(messageJson, CacheInvalidationMessage::class.java)

            // Ignore messages from self
            if (message.origin == instanceId) return

            logger.debug("Received invalidation message from {}: {}", message.origin, message.type)

            when (message.type) {
                InvalidationType.EVICT -> {
                    message.keys.forEach { cacheFlowService.evictLocal(it) }
                }
                InvalidationType.EVICT_BY_TAGS -> {
                    if (message.tags.isNotEmpty()) {
                        cacheFlowService.evictLocalByTags(*message.tags.toTypedArray())
                    }
                }
                InvalidationType.EVICT_ALL -> {
                    cacheFlowService.evictLocalAll()
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling invalidation message", e)
        }
    }
}
