package io.cacheflow.spring.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import com.fasterxml.jackson.databind.ObjectMapper

@Configuration
@ConditionalOnClass(RedisTemplate::class, ObjectMapper::class)
@ConditionalOnProperty(prefix = "cacheflow", name = ["storage"], havingValue = "REDIS")
class CacheFlowRedisConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["cacheFlowRedisTemplate"])
    fun cacheFlowRedisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = GenericJackson2JsonRedisSerializer()
        template.afterPropertiesSet()
        return template
    }

    @Bean
    @ConditionalOnMissingBean
    fun redisCacheInvalidator(
        properties: io.cacheflow.spring.config.CacheFlowProperties,
        redisTemplate: org.springframework.data.redis.core.StringRedisTemplate,
        @org.springframework.context.annotation.Lazy cacheFlowService: io.cacheflow.spring.service.CacheFlowService,
        objectMapper: ObjectMapper,
    ): io.cacheflow.spring.messaging.RedisCacheInvalidator {
        return io.cacheflow.spring.messaging.RedisCacheInvalidator(
            properties,
            redisTemplate,
            cacheFlowService,
            objectMapper
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun cacheInvalidationListenerAdapter(
        redisCacheInvalidator: io.cacheflow.spring.messaging.RedisCacheInvalidator
    ): org.springframework.data.redis.listener.adapter.MessageListenerAdapter {
        return org.springframework.data.redis.listener.adapter.MessageListenerAdapter(
            redisCacheInvalidator,
            "handleMessage"
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        cacheInvalidationListenerAdapter: org.springframework.data.redis.listener.adapter.MessageListenerAdapter
    ): org.springframework.data.redis.listener.RedisMessageListenerContainer {
        val container = org.springframework.data.redis.listener.RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(
            cacheInvalidationListenerAdapter,
            org.springframework.data.redis.listener.ChannelTopic("cacheflow:invalidation")
        )
        return container
    }
}
