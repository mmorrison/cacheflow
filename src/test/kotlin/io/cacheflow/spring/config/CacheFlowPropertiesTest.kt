package io.cacheflow.spring.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CacheFlowPropertiesTest {

    @Test
    fun `should create properties with default values`() {
        val properties = CacheFlowProperties()

        assertTrue(properties.enabled)
        assertEquals(3_600L, properties.defaultTtl)
        assertEquals(10_000L, properties.maxSize)
        assertEquals(CacheFlowProperties.StorageType.IN_MEMORY, properties.storage)
        assertEquals("https://yourdomain.com", properties.baseUrl)
        assertNotNull(properties.redis)
        assertNotNull(properties.cloudflare)
        assertNotNull(properties.awsCloudFront)
        assertNotNull(properties.fastly)
        assertNotNull(properties.metrics)
    }

    @Test
    fun `should create properties with custom values`() {
        val properties =
                CacheFlowProperties(
                        enabled = false,
                        defaultTtl = 1800L,
                        maxSize = 5000L,
                        storage = CacheFlowProperties.StorageType.REDIS,
                        baseUrl = "https://custom.com"
                )

        assertFalse(properties.enabled)
        assertEquals(1800L, properties.defaultTtl)
        assertEquals(5000L, properties.maxSize)
        assertEquals(CacheFlowProperties.StorageType.REDIS, properties.storage)
        assertEquals("https://custom.com", properties.baseUrl)
    }

    @Test
    fun `StorageType enum should have correct values`() {
        val values = CacheFlowProperties.StorageType.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(CacheFlowProperties.StorageType.IN_MEMORY))
        assertTrue(values.contains(CacheFlowProperties.StorageType.REDIS))
        assertTrue(values.contains(CacheFlowProperties.StorageType.CAFFEINE))
        assertTrue(values.contains(CacheFlowProperties.StorageType.CLOUDFLARE))
    }

    @Test
    fun `RedisProperties should have default values`() {
        val redisProps = CacheFlowProperties.RedisProperties()

        assertEquals("rd-cache:", redisProps.keyPrefix)
        assertEquals(0, redisProps.database)
        assertEquals(5_000L, redisProps.timeout)
    }

    @Test
    fun `RedisProperties should accept custom values`() {
        val redisProps =
                CacheFlowProperties.RedisProperties(
                        keyPrefix = "custom:",
                        database = 1,
                        timeout = 10_000L
                )

        assertEquals("custom:", redisProps.keyPrefix)
        assertEquals(1, redisProps.database)
        assertEquals(10_000L, redisProps.timeout)
    }

    @Test
    fun `CloudflareProperties should have default values`() {
        val cloudflareProps = CacheFlowProperties.CloudflareProperties()

        assertFalse(cloudflareProps.enabled)
        assertEquals("", cloudflareProps.zoneId)
        assertEquals("", cloudflareProps.apiToken)
        assertEquals("rd-cache:", cloudflareProps.keyPrefix)
        assertEquals(3_600L, cloudflareProps.defaultTtl)
        assertTrue(cloudflareProps.autoPurge)
        assertTrue(cloudflareProps.purgeOnEvict)
        assertNull(cloudflareProps.rateLimit)
        assertNull(cloudflareProps.circuitBreaker)
    }

    @Test
    fun `CloudflareProperties should accept custom values`() {
        val rateLimit = CacheFlowProperties.RateLimit(20, 40, 120)
        val circuitBreaker = CacheFlowProperties.CircuitBreakerConfig(10, 120, 5)

        val cloudflareProps =
                CacheFlowProperties.CloudflareProperties(
                        enabled = true,
                        zoneId = "zone123",
                        apiToken = "token123",
                        keyPrefix = "cf:",
                        defaultTtl = 7200L,
                        autoPurge = false,
                        purgeOnEvict = false,
                        rateLimit = rateLimit,
                        circuitBreaker = circuitBreaker
                )

        assertTrue(cloudflareProps.enabled)
        assertEquals("zone123", cloudflareProps.zoneId)
        assertEquals("token123", cloudflareProps.apiToken)
        assertEquals("cf:", cloudflareProps.keyPrefix)
        assertEquals(7200L, cloudflareProps.defaultTtl)
        assertFalse(cloudflareProps.autoPurge)
        assertFalse(cloudflareProps.purgeOnEvict)
        assertEquals(rateLimit, cloudflareProps.rateLimit)
        assertEquals(circuitBreaker, cloudflareProps.circuitBreaker)
    }

    @Test
    fun `AwsCloudFrontProperties should have default values`() {
        val awsProps = CacheFlowProperties.AwsCloudFrontProperties()

        assertFalse(awsProps.enabled)
        assertEquals("", awsProps.distributionId)
        assertEquals("rd-cache:", awsProps.keyPrefix)
        assertEquals(3_600L, awsProps.defaultTtl)
        assertTrue(awsProps.autoPurge)
        assertTrue(awsProps.purgeOnEvict)
        assertNull(awsProps.rateLimit)
        assertNull(awsProps.circuitBreaker)
    }

    @Test
    fun `AwsCloudFrontProperties should accept custom values`() {
        val rateLimit = CacheFlowProperties.RateLimit(15, 30, 90)
        val circuitBreaker = CacheFlowProperties.CircuitBreakerConfig(8, 90, 4)

        val awsProps =
                CacheFlowProperties.AwsCloudFrontProperties(
                        enabled = true,
                        distributionId = "dist123",
                        keyPrefix = "aws:",
                        defaultTtl = 1800L,
                        autoPurge = false,
                        purgeOnEvict = false,
                        rateLimit = rateLimit,
                        circuitBreaker = circuitBreaker
                )

        assertTrue(awsProps.enabled)
        assertEquals("dist123", awsProps.distributionId)
        assertEquals("aws:", awsProps.keyPrefix)
        assertEquals(1800L, awsProps.defaultTtl)
        assertFalse(awsProps.autoPurge)
        assertFalse(awsProps.purgeOnEvict)
        assertEquals(rateLimit, awsProps.rateLimit)
        assertEquals(circuitBreaker, awsProps.circuitBreaker)
    }

    @Test
    fun `FastlyProperties should have default values`() {
        val fastlyProps = CacheFlowProperties.FastlyProperties()

        assertFalse(fastlyProps.enabled)
        assertEquals("", fastlyProps.serviceId)
        assertEquals("", fastlyProps.apiToken)
        assertEquals("rd-cache:", fastlyProps.keyPrefix)
        assertEquals(3_600L, fastlyProps.defaultTtl)
        assertTrue(fastlyProps.autoPurge)
        assertTrue(fastlyProps.purgeOnEvict)
        assertNull(fastlyProps.rateLimit)
        assertNull(fastlyProps.circuitBreaker)
    }

    @Test
    fun `FastlyProperties should accept custom values`() {
        val rateLimit = CacheFlowProperties.RateLimit(25, 50, 180)
        val circuitBreaker = CacheFlowProperties.CircuitBreakerConfig(12, 180, 6)

        val fastlyProps =
                CacheFlowProperties.FastlyProperties(
                        enabled = true,
                        serviceId = "service123",
                        apiToken = "token123",
                        keyPrefix = "fastly:",
                        defaultTtl = 900L,
                        autoPurge = false,
                        purgeOnEvict = false,
                        rateLimit = rateLimit,
                        circuitBreaker = circuitBreaker
                )

        assertTrue(fastlyProps.enabled)
        assertEquals("service123", fastlyProps.serviceId)
        assertEquals("token123", fastlyProps.apiToken)
        assertEquals("fastly:", fastlyProps.keyPrefix)
        assertEquals(900L, fastlyProps.defaultTtl)
        assertFalse(fastlyProps.autoPurge)
        assertFalse(fastlyProps.purgeOnEvict)
        assertEquals(rateLimit, fastlyProps.rateLimit)
        assertEquals(circuitBreaker, fastlyProps.circuitBreaker)
    }

    @Test
    fun `RateLimit should have default values`() {
        val rateLimit = CacheFlowProperties.RateLimit()

        assertEquals(10, rateLimit.requestsPerSecond)
        assertEquals(20, rateLimit.burstSize)
        assertEquals(60L, rateLimit.windowSize)
    }

    @Test
    fun `RateLimit should accept custom values`() {
        val rateLimit = CacheFlowProperties.RateLimit(50, 100, 300)

        assertEquals(50, rateLimit.requestsPerSecond)
        assertEquals(100, rateLimit.burstSize)
        assertEquals(300L, rateLimit.windowSize)
    }

    @Test
    fun `CircuitBreakerConfig should have default values`() {
        val circuitBreaker = CacheFlowProperties.CircuitBreakerConfig()

        assertEquals(5, circuitBreaker.failureThreshold)
        assertEquals(60L, circuitBreaker.recoveryTimeout)
        assertEquals(3, circuitBreaker.halfOpenMaxCalls)
    }

    @Test
    fun `CircuitBreakerConfig should accept custom values`() {
        val circuitBreaker = CacheFlowProperties.CircuitBreakerConfig(15, 300, 8)

        assertEquals(15, circuitBreaker.failureThreshold)
        assertEquals(300L, circuitBreaker.recoveryTimeout)
        assertEquals(8, circuitBreaker.halfOpenMaxCalls)
    }

    @Test
    fun `MetricsProperties should have default values`() {
        val metrics = CacheFlowProperties.MetricsProperties()

        assertTrue(metrics.enabled)
        assertEquals(60L, metrics.exportInterval)
    }

    @Test
    fun `MetricsProperties should accept custom values`() {
        val metrics = CacheFlowProperties.MetricsProperties(false, 120L)

        assertFalse(metrics.enabled)
        assertEquals(120L, metrics.exportInterval)
    }
}
