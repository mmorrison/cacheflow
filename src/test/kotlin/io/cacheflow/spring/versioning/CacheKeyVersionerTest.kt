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
        private const val TEST_TIMESTAMP_2 = 1_640_995_230_000L // 2022-01-01 00:00:30 UTC
        private const val TEST_TIMESTAMP_3 = 1_640_995_260_000L // 2022-01-01 00:01:00 UTC
        private const val TEST_TIMESTAMP_4 = 1_640_995_290_000L // 2022-01-01 00:01:30 UTC
        private const val TEST_TIMESTAMP_5 = 1_640_995_320_000L // 2022-01-01 00:02:00 UTC
        private const val TEST_TIMESTAMP_6 = 1_640_995_350_000L // 2022-01-01 00:02:30 UTC
        private const val TEST_TIMESTAMP_7 = 1_640_995_380_000L // 2022-01-01 00:03:00 UTC
        private const val TEST_TIMESTAMP_8 = 1_640_995_410_000L // 2022-01-01 00:03:30 UTC
        private const val TEST_TIMESTAMP_9 = 1_640_995_440_000L // 2022-01-01 00:04:00 UTC
        private const val TEST_TIMESTAMP_10 = 1_640_995_470_000L // 2022-01-01 00:04:30 UTC
        private const val TEST_TIMESTAMP_11 = 1_640_995_500_000L // 2022-01-01 00:05:00 UTC
        private const val TEST_TIMESTAMP_12 = 1_640_995_530_000L // 2022-01-01 00:05:30 UTC
        private const val TEST_TIMESTAMP_13 = 1_640_995_560_000L // 2022-01-01 00:06:00 UTC
        private const val TEST_TIMESTAMP_14 = 1_640_995_590_000L // 2022-01-01 00:06:30 UTC
        private const val TEST_TIMESTAMP_15 = 1_640_995_620_000L // 2022-01-01 00:07:00 UTC
        private const val TEST_TIMESTAMP_16 = 1_640_995_650_000L // 2022-01-01 00:07:30 UTC
        private const val TEST_TIMESTAMP_17 = 1_640_995_680_000L // 2022-01-01 00:08:00 UTC
        private const val TEST_TIMESTAMP_18 = 1_640_995_710_000L // 2022-01-01 00:08:30 UTC
        private const val TEST_TIMESTAMP_19 = 1_640_995_740_000L // 2022-01-01 00:09:00 UTC
        private const val TEST_TIMESTAMP_20 = 1_640_995_770_000L // 2022-01-01 00:09:30 UTC
        private const val TEST_TIMESTAMP_21 = 1_640_995_800_000L // 2022-01-01 00:10:00 UTC
        private const val TEST_TIMESTAMP_22 = 1_640_995_830_000L // 2022-01-01 00:10:30 UTC
        private const val TEST_TIMESTAMP_23 = 1_640_995_860_000L // 2022-01-01 00:11:00 UTC
        private const val TEST_TIMESTAMP_24 = 1_640_995_890_000L // 2022-01-01 00:11:30 UTC
        private const val TEST_TIMESTAMP_25 = 1_640_995_920_000L // 2022-01-01 00:12:00 UTC
        private const val TEST_TIMESTAMP_26 = 1_640_995_950_000L // 2022-01-01 00:12:30 UTC
        private const val TEST_TIMESTAMP_27 = 1_640_995_980_000L // 2022-01-01 00:13:00 UTC
        private const val TEST_TIMESTAMP_28 = 1_640_996_010_000L // 2022-01-01 00:13:30 UTC
        private const val TEST_TIMESTAMP_29 = 1_640_996_040_000L // 2022-01-01 00:14:00 UTC
        private const val TEST_TIMESTAMP_30 = 1_640_996_070_000L // 2022-01-01 00:14:30 UTC
        private const val TEST_TIMESTAMP_31 = 1_640_996_100_000L // 2022-01-01 00:15:00 UTC
        private const val TEST_TIMESTAMP_32 = 1_640_996_130_000L // 2022-01-01 00:15:30 UTC
        private const val TEST_TIMESTAMP_33 = 1_640_996_160_000L // 2022-01-01 00:16:00 UTC
        private const val TEST_TIMESTAMP_34 = 1_640_996_190_000L // 2022-01-01 00:16:30 UTC
        private const val TEST_TIMESTAMP_35 = 1_640_996_220_000L // 2022-01-01 00:17:00 UTC
        private const val TEST_TIMESTAMP_36 = 1_640_996_250_000L // 2022-01-01 00:17:30 UTC
        private const val TEST_TIMESTAMP_37 = 1_640_996_280_000L // 2022-01-01 00:18:00 UTC
        private const val TEST_TIMESTAMP_38 = 1_640_996_310_000L // 2022-01-01 00:18:30 UTC
        private const val TEST_TIMESTAMP_39 = 1_640_996_340_000L // 2022-01-01 00:19:00 UTC
        private const val TEST_TIMESTAMP_40 = 1_640_996_370_000L // 2022-01-01 00:19:30 UTC
        private const val TEST_TIMESTAMP_41 = 1_640_996_400_000L // 2022-01-01 00:20:00 UTC
        private const val TEST_TIMESTAMP_42 = 1_640_996_430_000L // 2022-01-01 00:20:30 UTC
        private const val TEST_TIMESTAMP_43 = 1_640_996_460_000L // 2022-01-01 00:21:00 UTC
        private const val TEST_TIMESTAMP_44 = 1_640_996_490_000L // 2022-01-01 00:21:30 UTC
        private const val TEST_TIMESTAMP_45 = 1_640_996_520_000L // 2022-01-01 00:22:00 UTC
        private const val TEST_TIMESTAMP_46 = 1_640_996_550_000L // 2022-01-01 00:22:30 UTC
        private const val TEST_TIMESTAMP_47 = 1_640_996_580_000L // 2022-01-01 00:23:00 UTC
        private const val TEST_TIMESTAMP_48 = 1_640_996_610_000L // 2022-01-01 00:23:30 UTC
        private const val TEST_TIMESTAMP_49 = 1_640_996_640_000L // 2022-01-01 00:24:00 UTC
        private const val TEST_TIMESTAMP_50 = 1_640_996_670_000L // 2022-01-01 00:24:30 UTC
        private const val TEST_TIMESTAMP_51 = 1_640_996_700_000L // 2022-01-01 00:25:00 UTC
        private const val TEST_TIMESTAMP_52 = 1_640_996_730_000L // 2022-01-01 00:25:30 UTC
        private const val TEST_TIMESTAMP_53 = 1_640_996_760_000L // 2022-01-01 00:26:00 UTC
        private const val TEST_TIMESTAMP_54 = 1_640_996_790_000L // 2022-01-01 00:26:30 UTC
        private const val TEST_TIMESTAMP_55 = 1_640_996_820_000L // 2022-01-01 00:27:00 UTC
        private const val TEST_TIMESTAMP_56 = 1_640_996_850_000L // 2022-01-01 00:27:30 UTC
        private const val TEST_TIMESTAMP_57 = 1_640_996_880_000L // 2022-01-01 00:28:00 UTC
        private const val TEST_TIMESTAMP_58 = 1_640_996_910_000L // 2022-01-01 00:28:30 UTC
        private const val TEST_TIMESTAMP_59 = 1_640_996_940_000L // 2022-01-01 00:29:00 UTC
        private const val TEST_TIMESTAMP_60 = 1_640_996_970_000L // 2022-01-01 00:29:30 UTC
        private const val TEST_TIMESTAMP_61 = 1_640_997_000_000L // 2022-01-01 00:30:00 UTC
        private const val TEST_TIMESTAMP_62 = 1_640_997_030_000L // 2022-01-01 00:30:30 UTC
        private const val TEST_TIMESTAMP_63 = 1_640_997_060_000L // 2022-01-01 00:31:00 UTC
        private const val TEST_TIMESTAMP_64 = 1_640_997_090_000L // 2022-01-01 00:31:30 UTC
        private const val TEST_TIMESTAMP_65 = 1_640_997_120_000L // 2022-01-01 00:32:00 UTC
        private const val TEST_TIMESTAMP_66 = 1_640_997_150_000L // 2022-01-01 00:32:30 UTC
        private const val TEST_TIMESTAMP_67 = 1_640_997_180_000L // 2022-01-01 00:33:00 UTC
        private const val TEST_TIMESTAMP_68 = 1_640_997_210_000L // 2022-01-01 00:33:30 UTC
        private const val TEST_TIMESTAMP_69 = 1_640_997_240_000L // 2022-01-01 00:34:00 UTC
        private const val TEST_TIMESTAMP_70 = 1_640_997_270_000L // 2022-01-01 00:34:30 UTC
        private const val TEST_TIMESTAMP_71 = 1_640_997_300_000L // 2022-01-01 00:35:00 UTC
        private const val TEST_TIMESTAMP_72 = 1_640_997_330_000L // 2022-01-01 00:35:30 UTC
        private const val TEST_TIMESTAMP_73 = 1_640_997_360_000L // 2022-01-01 00:36:00 UTC
        private const val TEST_TIMESTAMP_74 = 1_640_997_390_000L // 2022-01-01 00:36:30 UTC
        private const val TEST_TIMESTAMP_75 = 1_640_997_420_000L // 2022-01-01 00:37:00 UTC
        private const val TEST_TIMESTAMP_76 = 1_640_997_450_000L // 2022-01-01 00:37:30 UTC
        private const val TEST_TIMESTAMP_77 = 1_640_997_480_000L // 2022-01-01 00:38:00 UTC
        private const val TEST_TIMESTAMP_78 = 1_640_997_510_000L // 2022-01-01 00:38:30 UTC
        private const val TEST_TIMESTAMP_79 = 1_640_997_540_000L // 2022-01-01 00:39:00 UTC
        private const val TEST_TIMESTAMP_80 = 1_640_997_570_000L // 2022-01-01 00:39:30 UTC
        private const val TEST_TIMESTAMP_81 = 1_640_997_600_000L // 2022-01-01 00:40:00 UTC
        private const val TEST_TIMESTAMP_82 = 1_640_997_630_000L // 2022-01-01 00:40:30 UTC
        private const val TEST_TIMESTAMP_83 = 1_640_997_660_000L // 2022-01-01 00:41:00 UTC
        private const val TEST_TIMESTAMP_84 = 1_640_997_690_000L // 2022-01-01 00:41:30 UTC
        private const val TEST_TIMESTAMP_85 = 1_640_997_720_000L // 2022-01-01 00:42:00 UTC
        private const val TEST_TIMESTAMP_86 = 1_640_997_750_000L // 2022-01-01 00:42:30 UTC
        private const val TEST_TIMESTAMP_87 = 1_640_997_780_000L // 2022-01-01 00:43:00 UTC
        private const val TEST_TIMESTAMP_88 = 1_640_997_810_000L // 2022-01-01 00:43:30 UTC
        private const val TEST_TIMESTAMP_89 = 1_640_997_840_000L // 2022-01-01 00:44:00 UTC
        private const val TEST_TIMESTAMP_90 = 1_640_997_870_000L // 2022-01-01 00:44:30 UTC
        private const val TEST_TIMESTAMP_91 = 1_640_997_900_000L // 2022-01-01 00:45:00 UTC
        private const val TEST_TIMESTAMP_92 = 1_640_997_930_000L // 2022-01-01 00:45:30 UTC
        private const val TEST_TIMESTAMP_93 = 1_640_997_960_000L // 2022-01-01 00:46:00 UTC
        private const val TEST_TIMESTAMP_94 = 1_640_997_990_000L // 2022-01-01 00:46:30 UTC
        private const val TEST_TIMESTAMP_95 = 1_640_998_020_000L // 2022-01-01 00:47:00 UTC
        private const val TEST_TIMESTAMP_96 = 1_640_998_050_000L // 2022-01-01 00:47:30 UTC
        private const val TEST_TIMESTAMP_97 = 1_640_998_080_000L // 2022-01-01 00:48:00 UTC
        private const val TEST_TIMESTAMP_98 = 1_640_998_110_000L // 2022-01-01 00:48:30 UTC
        private const val TEST_TIMESTAMP_99 = 1_640_998_140_000L // 2022-01-01 00:49:00 UTC
        private const val TEST_TIMESTAMP_100 = 1_640_998_170_000L // 2022-01-01 00:49:30 UTC
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
        val timestamp =
            1641081600000L // 2022-01-01 12:00:00 UTC (to ensure it's 2022-01-01 in most timezones)

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
