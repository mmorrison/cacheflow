package io.cacheflow.spring.autoconfigure

import io.cacheflow.spring.config.CacheFlowProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

class CacheFlowRedisConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheFlowRedisConfiguration::class.java))

    @Test
    fun `should create cacheFlowRedisTemplate when storage is REDIS`() {
        contextRunner
            .withPropertyValues("cacheflow.storage=REDIS")
            .withBean(CacheFlowProperties::class.java, { CacheFlowProperties() })
            .withBean(RedisConnectionFactory::class.java, { mock(RedisConnectionFactory::class.java) })
            .withBean(org.springframework.data.redis.core.StringRedisTemplate::class.java, {
                mock(org.springframework.data.redis.core.StringRedisTemplate::class.java)
            })
            .withBean(
                com.fasterxml.jackson.databind.ObjectMapper::class.java,
                { mock(com.fasterxml.jackson.databind.ObjectMapper::class.java) },
            ).withUserConfiguration(MockRedisContainerConfig::class.java) // Override the container with a mock
            .run { context ->
                assertThat(context).hasBean("cacheFlowRedisTemplate")
                val template = context.getBean("cacheFlowRedisTemplate", RedisTemplate::class.java)
                assertThat(template.keySerializer).isInstanceOf(StringRedisSerializer::class.java)
                assertThat(template.valueSerializer).isInstanceOf(GenericJackson2JsonRedisSerializer::class.java)
            }
    }

    @Test
    fun `should NOT create cacheFlowRedisTemplate when storage is NOT REDIS`() {
        contextRunner
            .withPropertyValues("cacheflow.storage=IN_MEMORY")
            .withBean(CacheFlowProperties::class.java, { CacheFlowProperties() })
            .withBean(RedisConnectionFactory::class.java, { mock(RedisConnectionFactory::class.java) })
            .withBean(org.springframework.data.redis.core.StringRedisTemplate::class.java, {
                mock(org.springframework.data.redis.core.StringRedisTemplate::class.java)
            })
            .withBean(
                com.fasterxml.jackson.databind.ObjectMapper::class.java,
                { mock(com.fasterxml.jackson.databind.ObjectMapper::class.java) },
            ).withUserConfiguration(MockRedisContainerConfig::class.java)
            .run { context ->
                assertThat(context).doesNotHaveBean("cacheFlowRedisTemplate")
            }
    }

    @Test
    fun `should NOT create cacheFlowRedisTemplate when RedisConnectionFactory is missing`() {
        contextRunner
            .withPropertyValues("cacheflow.storage=REDIS")
            .withBean(CacheFlowProperties::class.java, { CacheFlowProperties() })
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(
                    context,
                ).getFailure().hasRootCauseInstanceOf(org.springframework.beans.factory.NoSuchBeanDefinitionException::class.java)
            }
    }

    @Configuration
    class MockRedisContainerConfig {
        @Bean
        fun redisMessageListenerContainer(): RedisMessageListenerContainer = mock(RedisMessageListenerContainer::class.java)
    }
}
