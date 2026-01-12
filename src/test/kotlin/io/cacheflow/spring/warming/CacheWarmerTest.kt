package io.cacheflow.spring.warming

import io.cacheflow.spring.config.CacheFlowProperties
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.context.event.ApplicationReadyEvent

class CacheWarmerTest {

    @Test
    fun `should execute warmup providers if enabled`() {
        // Given
        val properties = CacheFlowProperties(warming = CacheFlowProperties.WarmingProperties(enabled = true))
        val provider1 = mock<CacheWarmupProvider>()
        val provider2 = mock<CacheWarmupProvider>()
        val warmer = CacheWarmer(properties, listOf(provider1, provider2))
        val event = mock<ApplicationReadyEvent>()

        // When
        warmer.onApplicationEvent(event)

        // Then
        verify(provider1).warmup()
        verify(provider2).warmup()
    }

    @Test
    fun `should not execute warmup providers if disabled`() {
        // Given
        val properties = CacheFlowProperties(warming = CacheFlowProperties.WarmingProperties(enabled = false))
        val provider1 = mock<CacheWarmupProvider>()
        val warmer = CacheWarmer(properties, listOf(provider1))
        val event = mock<ApplicationReadyEvent>()

        // When
        warmer.onApplicationEvent(event)

        // Then
        verify(provider1, times(0)).warmup()
    }
    
    @Test
    fun `should handle provider exceptions gracefully`() {
        // Given
        val properties = CacheFlowProperties(warming = CacheFlowProperties.WarmingProperties(enabled = true))
        val provider1 = mock<CacheWarmupProvider>()
        val provider2 = mock<CacheWarmupProvider>()
        whenever(provider1.warmup()).thenThrow(RuntimeException("Warmup failed"))
        
        val warmer = CacheWarmer(properties, listOf(provider1, provider2))
        val event = mock<ApplicationReadyEvent>()

        // When
        warmer.onApplicationEvent(event)

        // Then
        verify(provider1).warmup()
        verify(provider2).warmup() // Should proceed to next provider
    }
}
