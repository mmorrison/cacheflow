package io.cacheflow.spring.integration

import io.cacheflow.spring.autoconfigure.CacheFlowAutoConfiguration
import io.cacheflow.spring.fragment.FragmentCacheService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import(CacheFlowAutoConfiguration::class)
class TestConfiguration {

        @Bean
        fun testService(): DependencyManagementIntegrationTest.TestService =
                DependencyManagementIntegrationTest.TestService()

        @Bean
        fun russianDollTestService(@Autowired fragmentCacheService: FragmentCacheService): RussianDollCachingIntegrationTest.RussianDollTestService =
                RussianDollCachingIntegrationTest.RussianDollTestService(fragmentCacheService)
}
