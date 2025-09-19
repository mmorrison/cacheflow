package io.cacheflow.spring.edge.config





import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class EdgeCachePropertiesTest {

    @Test
    fun `should create properties with default values`() {
        val properties = EdgeCacheProperties()

        assertTrue(properties.enabled)
        assertNotNull(properties.cloudflare)
        assertNotNull(properties.awsCloudFront)
        assertNotNull(properties.fastly)
        assertNull(properties.rateLimit)
        assertNull(properties.circuitBreaker)
        assertNull(properties.batching)
        assertNull(properties.monitoring)
    }

    @Test
    fun `should create properties with custom values`() {
        val properties =
                EdgeCacheProperties(
                        enabled = false,
                        cloudflare =
                                EdgeCacheProperties.CloudflareEdgeCacheProperties(
                                        enabled = true,
                                        zoneId = "zone123",
                                        apiToken = "token123",
                                        keyPrefix = "cf:",
                                        defaultTtl = 7200L,
                                        autoPurge = false,
                                        purgeOnEvict = false
                                )
                )

        assertFalse(properties.enabled)
        assertTrue(properties.cloudflare.enabled)
        assertEquals("zone123", properties.cloudflare.zoneId)
        assertEquals("token123", properties.cloudflare.apiToken)
        assertEquals("cf:", properties.cloudflare.keyPrefix)
        assertEquals(7200L, properties.cloudflare.defaultTtl)
        assertFalse(properties.cloudflare.autoPurge)
        assertFalse(properties.cloudflare.purgeOnEvict)
    }

    @Test
    fun `CloudflareEdgeCacheProperties should have default values`() {
        val cloudflare = EdgeCacheProperties.CloudflareEdgeCacheProperties()

        assertFalse(cloudflare.enabled)
        assertEquals("", cloudflare.zoneId)
        assertEquals("", cloudflare.apiToken)
        assertEquals("rd-cache:", cloudflare.keyPrefix)
        assertEquals(3_600L, cloudflare.defaultTtl)
        assertTrue(cloudflare.autoPurge)
        assertTrue(cloudflare.purgeOnEvict)
    }

    @Test
    fun `CloudflareEdgeCacheProperties should accept custom values`() {
        val cloudflare =
                EdgeCacheProperties.CloudflareEdgeCacheProperties(
                        enabled = true,
                        zoneId = "zone123",
                        apiToken = "token123",
                        keyPrefix = "cf:",
                        defaultTtl = 3600L,
                        autoPurge = true,
                        purgeOnEvict = true
                )

        assertTrue(cloudflare.enabled)
        assertEquals("zone123", cloudflare.zoneId)
        assertEquals("token123", cloudflare.apiToken)
        assertEquals("cf:", cloudflare.keyPrefix)
        assertEquals(3600L, cloudflare.defaultTtl)
        assertTrue(cloudflare.autoPurge)
        assertTrue(cloudflare.purgeOnEvict)
    }

    @Test
    fun `AwsCloudFrontEdgeCacheProperties should have default values`() {
        val aws = EdgeCacheProperties.AwsCloudFrontEdgeCacheProperties()

        assertFalse(aws.enabled)
        assertEquals("", aws.distributionId)
        assertEquals("rd-cache:", aws.keyPrefix)
        assertEquals(3_600L, aws.defaultTtl)
        assertTrue(aws.autoPurge)
        assertTrue(aws.purgeOnEvict)
    }

    @Test
    fun `AwsCloudFrontEdgeCacheProperties should accept custom values`() {
        val aws =
                EdgeCacheProperties.AwsCloudFrontEdgeCacheProperties(
                        enabled = true,
                        distributionId = "dist123",
                        keyPrefix = "aws:",
                        defaultTtl = 1800L,
                        autoPurge = true,
                        purgeOnEvict = true
                )

        assertTrue(aws.enabled)
        assertEquals("dist123", aws.distributionId)
        assertEquals("aws:", aws.keyPrefix)
        assertEquals(1800L, aws.defaultTtl)
        assertTrue(aws.autoPurge)
        assertTrue(aws.purgeOnEvict)
    }

    @Test
    fun `FastlyEdgeCacheProperties should have default values`() {
        val fastly = EdgeCacheProperties.FastlyEdgeCacheProperties()

        assertFalse(fastly.enabled)
        assertEquals("", fastly.serviceId)
        assertEquals("", fastly.apiToken)
        assertEquals("rd-cache:", fastly.keyPrefix)
        assertEquals(3_600L, fastly.defaultTtl)
        assertTrue(fastly.autoPurge)
        assertTrue(fastly.purgeOnEvict)
    }

    @Test
    fun `FastlyEdgeCacheProperties should accept custom values`() {
        val fastly =
                EdgeCacheProperties.FastlyEdgeCacheProperties(
                        enabled = true,
                        serviceId = "service123",
                        apiToken = "token123",
                        keyPrefix = "fastly:",
                        defaultTtl = 900L,
                        autoPurge = true,
                        purgeOnEvict = true
                )

        assertTrue(fastly.enabled)
        assertEquals("service123", fastly.serviceId)
        assertEquals("token123", fastly.apiToken)
        assertEquals("fastly:", fastly.keyPrefix)
        assertEquals(900L, fastly.defaultTtl)
        assertTrue(fastly.autoPurge)
        assertTrue(fastly.purgeOnEvict)
    }

    @Test
    fun `EdgeCacheRateLimitProperties should have default values`() {
        val rateLimit = EdgeCacheProperties.EdgeCacheRateLimitProperties()

        assertEquals(10, rateLimit.requestsPerSecond)
        assertEquals(20, rateLimit.burstSize)
        assertEquals(60L, rateLimit.windowSize)
    }

    @Test
    fun `EdgeCacheRateLimitProperties should accept custom values`() {
        val rateLimit =
                EdgeCacheProperties.EdgeCacheRateLimitProperties(
                        requestsPerSecond = 100,
                        burstSize = 200,
                        windowSize = 60L
                )

        assertEquals(100, rateLimit.requestsPerSecond)
        assertEquals(200, rateLimit.burstSize)
        assertEquals(60L, rateLimit.windowSize)
    }

    @Test
    fun `EdgeCacheCircuitBreakerProperties should have default values`() {
        val circuitBreaker = EdgeCacheProperties.EdgeCacheCircuitBreakerProperties()

        assertEquals(5, circuitBreaker.failureThreshold)
        assertEquals(60L, circuitBreaker.recoveryTimeout)
        assertEquals(3, circuitBreaker.halfOpenMaxCalls)
    }

    @Test
    fun `EdgeCacheCircuitBreakerProperties should accept custom values`() {
        val circuitBreaker =
                EdgeCacheProperties.EdgeCacheCircuitBreakerProperties(
                        failureThreshold = 10,
                        recoveryTimeout = 120L,
                        halfOpenMaxCalls = 5
                )

        assertEquals(10, circuitBreaker.failureThreshold)
        assertEquals(120L, circuitBreaker.recoveryTimeout)
        assertEquals(5, circuitBreaker.halfOpenMaxCalls)
    }

    @Test
    fun `EdgeCacheBatchingProperties should have default values`() {
        val batching = EdgeCacheProperties.EdgeCacheBatchingProperties()

        assertEquals(100, batching.batchSize)
        assertEquals(5L, batching.batchTimeout)
        assertEquals(10, batching.maxConcurrency)
    }

    @Test
    fun `EdgeCacheBatchingProperties should accept custom values`() {
        val batching =
                EdgeCacheProperties.EdgeCacheBatchingProperties(
                        batchSize = 50,
                        batchTimeout = 5000L,
                        maxConcurrency = 10
                )

        assertEquals(50, batching.batchSize)
        assertEquals(5000L, batching.batchTimeout)
        assertEquals(10, batching.maxConcurrency)
    }

    @Test
    fun `EdgeCacheMonitoringProperties should have default values`() {
        val monitoring = EdgeCacheProperties.EdgeCacheMonitoringProperties()

        assertTrue(monitoring.enableMetrics)
        assertTrue(monitoring.enableTracing)
        assertEquals("INFO", monitoring.logLevel)
    }

    @Test
    fun `EdgeCacheMonitoringProperties should accept custom values`() {
        val monitoring =
                EdgeCacheProperties.EdgeCacheMonitoringProperties(
                        enableMetrics = true,
                        enableTracing = true,
                        logLevel = "DEBUG"
                )

        assertTrue(monitoring.enableMetrics)
        assertTrue(monitoring.enableTracing)
        assertEquals("DEBUG", monitoring.logLevel)
    }
}
