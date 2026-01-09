package io.cacheflow.spring.versioning

import io.cacheflow.spring.versioning.impl.DefaultTimestampExtractor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAccessor
import java.util.Date

class CacheKeyVersionerTest {

    companion object {
        private const val TEST_TIMESTAMP_1 = 1_640_995_200_000L // 2022-01-01 00:00:00 UTC
    }

    private lateinit var cacheKeyVersioner: CacheKeyVersioner
    private lateinit var timestampExtractor: TimestampExtractor

    @BeforeEach
    fun setUp() {
        timestampExtractor = DefaultTimestampExtractor()
        cacheKeyVersioner = CacheKeyVersioner(timestampExtractor)
    }

    @Test
    fun `should generate versioned key with timestamp`() {
        // Given
        val baseKey = "user:123"
        val timestamp = 1640995200000L // 2022-01-01 00:00:00 UTC
        val obj = timestamp

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, obj)

        // Then
        assertEquals("user:123-v$timestamp", versionedKey)
    }

    @Test
    fun `should return original key when no timestamp found`() {
        // Given
        val baseKey = "user:123"
        val obj = "some string"

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, obj)

        // Then
        assertEquals(baseKey, versionedKey)
    }

    @Test
    fun `should generate versioned key with specific timestamp`() {
        // Given
        val baseKey = "user:123"
        val timestamp = 1640995200000L

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, timestamp)

        // Then
        assertEquals("user:123-v$timestamp", versionedKey)
    }

    @Test
    fun `should generate versioned key with multiple objects using latest timestamp`() {
        // Given
        val baseKey = "user:123"
        val timestamp1 = 1640995200000L // 2022-01-01
        val timestamp2 = 1641081600000L // 2022-01-02
        val obj1 = timestamp1
        val obj2 = timestamp2

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, obj1, obj2)

        // Then
        assertEquals("user:123-v$timestamp2", versionedKey)
    }

    @Test
    fun `should generate versioned key with list of objects`() {
        // Given
        val baseKey = "user:123"
        val timestamps = listOf(1640995200000L, 1641081600000L, 1641168000000L)
        val objects = timestamps.map { it as Any? }

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, objects)

        // Then
        assertEquals("user:123-v1641168000000", versionedKey)
    }

    @Test
    fun `should extract base key from versioned key`() {
        // Given
        val versionedKey = "user:123-v1640995200000"

        // When
        val baseKey = cacheKeyVersioner.extractBaseKey(versionedKey)

        // Then
        assertEquals("user:123", baseKey)
    }

    @Test
    fun `should return original key when extracting base key from non-versioned key`() {
        // Given
        val key = "user:123"

        // When
        val baseKey = cacheKeyVersioner.extractBaseKey(key)

        // Then
        assertEquals(key, baseKey)
    }

    @Test
    fun `should extract timestamp from versioned key`() {
        // Given
        val versionedKey = "user:123-v1640995200000"
        val expectedTimestamp = 1640995200000L

        // When
        val timestamp = cacheKeyVersioner.extractTimestamp(versionedKey)

        // Then
        assertEquals(expectedTimestamp, timestamp)
    }

    @Test
    fun `should return null when extracting timestamp from non-versioned key`() {
        // Given
        val key = "user:123"

        // When
        val timestamp = cacheKeyVersioner.extractTimestamp(key)

        // Then
        assertNull(timestamp)
    }

    @Test
    fun `should identify versioned key correctly`() {
        // Given
        val versionedKey = "user:123-v1640995200000"
        val nonVersionedKey = "user:123"

        // When & Then
        assertTrue(cacheKeyVersioner.isVersionedKey(versionedKey))
        assertFalse(cacheKeyVersioner.isVersionedKey(nonVersionedKey))
    }

    @Test
    fun `should generate versioned key with custom format`() {
        // Given
        val baseKey = "user:123"
        // 1641038400000L is 2022-01-01 12:00:00 UTC. This ensures it's 2022-01-01 in both UTC-12 and UTC+12.
        val timestamp = 1641038400000L

        val obj = timestamp
        val format = "yyyyMMdd"

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKeyWithFormat(baseKey, obj, format)

        // Then
        assertTrue(versionedKey.startsWith("user:123-v"))
        assertTrue(versionedKey.contains("20220101"))
    }

    @Test
    fun `should handle temporal accessor objects`() {
        // Given
        val baseKey = "user:123"
        val instant = Instant.ofEpochMilli(1640995200000L)

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, instant)

        // Then
        assertEquals("user:123-v1640995200000", versionedKey)
    }

    @Test
    fun `should handle date objects`() {
        // Given
        val baseKey = "user:123"
        val date = Date(1640995200000L)

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, date)

        // Then
        assertEquals("user:123-v1640995200000", versionedKey)
    }

    @Test
    fun `should handle local date time objects`() {
        // Given
        val baseKey = "user:123"
        val localDateTime = LocalDateTime.of(2022, 1, 1, 0, 0, 0)
        val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, localDateTime)

        // Then
        assertTrue(versionedKey.startsWith("user:123-v"))
        assertTrue(versionedKey.contains(instant.toEpochMilli().toString()))
    }

    @Test
    fun `should handle objects with updatedAt field`() {
        // Given
        val baseKey = "user:123"
        val obj =
            object : HasUpdatedAt {
                override val updatedAt: TemporalAccessor? = Instant.ofEpochMilli(1640995200000L)
            }

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, obj)

        // Then
        assertEquals("user:123-v1640995200000", versionedKey)
    }

    @Test
    fun `should handle null objects`() {
        // Given
        val baseKey = "user:123"
        val obj: Any? = null

        // When
        val versionedKey = cacheKeyVersioner.generateVersionedKey(baseKey, obj)

        // Then
        assertEquals(baseKey, versionedKey)
    }
}
