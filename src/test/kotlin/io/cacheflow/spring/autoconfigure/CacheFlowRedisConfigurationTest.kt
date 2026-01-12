package io.cacheflow.spring.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.mockito.Mockito.mock

class CacheFlowRedisConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CacheFlowRedisConfiguration::class.java))

    @Test
    fun `should create cacheFlowRedisTemplate when storage is REDIS`() {
        contextRunner
            .withPropertyValues("cacheflow.storage=REDIS")
            .withBean(RedisConnectionFactory::class.java, { mock(RedisConnectionFactory::class.java) })
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
            .withBean(RedisConnectionFactory::class.java, { mock(RedisConnectionFactory::class.java) })
            .run { context ->
                assertThat(context).doesNotHaveBean("cacheFlowRedisTemplate")
            }
    }

    @Test
    fun `should NOT create cacheFlowRedisTemplate when RedisConnectionFactory is missing`() {
        contextRunner
            .withPropertyValues("cacheflow.storage=REDIS")
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context).getFailure().hasRootCauseInstanceOf(org.springframework.beans.factory.NoSuchBeanDefinitionException::class.java)
            }
    }
}
