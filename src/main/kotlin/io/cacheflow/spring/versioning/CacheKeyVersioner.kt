package io.cacheflow.spring.versioning

import java.time.DateTimeException
import org.springframework.stereotype.Component

/**
 * Service for generating versioned cache keys based on timestamps.
 *
 * This service provides methods to create versioned cache keys that include timestamps, enabling
 * automatic cache invalidation when underlying data changes.
 */
@Component
class CacheKeyVersioner(private val timestampExtractor: TimestampExtractor) {

    /**
     * Generates a versioned cache key from a base key and an object.
     *
     * @param baseKey The base cache key
     * @param obj The object to extract timestamp from
     * @return The versioned cache key, or the original key if no timestamp found
     */
    fun generateVersionedKey(baseKey: String, obj: Any?): String {
        val timestamp = timestampExtractor.extractTimestamp(obj)
        return if (timestamp != null) {
            "$baseKey-v$timestamp"
        } else {
            baseKey
        }
    }

    /**
     * Generates a versioned cache key from a base key and a specific timestamp.
     *
     * @param baseKey The base cache key
     * @param timestamp The timestamp in milliseconds since epoch
     * @return The versioned cache key
     */
    fun generateVersionedKey(baseKey: String, timestamp: Long): String = "$baseKey-v$timestamp"

    /**
     * Generates a versioned cache key from a base key and multiple objects.
     *
     * @param baseKey The base cache key
     * @param objects The objects to extract timestamps from
     * @return The versioned cache key with the latest timestamp
     */
    fun generateVersionedKey(baseKey: String, vararg objects: Any?): String {
        val timestamps = objects.mapNotNull { timestampExtractor.extractTimestamp(it) }
        return if (timestamps.isNotEmpty()) {
            val latestTimestamp = timestamps.maxOrNull()!!
            "$baseKey-v$latestTimestamp"
        } else {
            baseKey
        }
    }

    /**
     * Generates a versioned cache key from a base key and a list of objects.
     *
     * @param baseKey The base cache key
     * @param objects The list of objects to extract timestamps from
     * @return The versioned cache key with the latest timestamp
     */
    fun generateVersionedKey(baseKey: String, objects: List<Any?>): String {
        val timestamps = objects.mapNotNull { timestampExtractor.extractTimestamp(it) }
        return if (timestamps.isNotEmpty()) {
            val latestTimestamp = timestamps.maxOrNull()!!
            "$baseKey-v$latestTimestamp"
        } else {
            baseKey
        }
    }

    /**
     * Extracts the base key from a versioned key.
     *
     * @param versionedKey The versioned cache key
     * @return The base key without the version suffix
     */
    fun extractBaseKey(versionedKey: String): String {
        val lastDashIndex = versionedKey.lastIndexOf("-v")
        return if (lastDashIndex > 0) {
            versionedKey.substring(0, lastDashIndex)
        } else {
            versionedKey
        }
    }

    /**
     * Extracts the timestamp from a versioned key.
     *
     * @param versionedKey The versioned cache key
     * @return The timestamp in milliseconds since epoch, or null if not found
     */
    fun extractTimestamp(versionedKey: String): Long? {
        val lastDashIndex = versionedKey.lastIndexOf("-v")
        return if (lastDashIndex > 0) {
            try {
                versionedKey.substring(lastDashIndex + 2).toLong()
            } catch (e: NumberFormatException) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Checks if a key is versioned.
     *
     * @param key The cache key to check
     * @return true if the key is versioned, false otherwise
     */
    fun isVersionedKey(key: String): Boolean = key.contains("-v") && extractTimestamp(key) != null

    /**
     * Generates a versioned key with a custom version format.
     *
     * @param baseKey The base cache key
     * @param obj The object to extract timestamp from
     * @param versionFormat The format for the version (e.g., "yyyyMMddHHmmss")
     * @return The versioned cache key with custom format
     */
    fun generateVersionedKeyWithFormat(baseKey: String, obj: Any?, versionFormat: String): String {
        val timestamp = timestampExtractor.extractTimestamp(obj)
        return if (timestamp != null) {
            val formattedVersion = formatTimestamp(timestamp, versionFormat)
            "$baseKey-v$formattedVersion"
        } else {
            baseKey
        }
    }

    private fun formatTimestamp(timestamp: Long, format: String): String {
        return try {
            val instant = java.time.Instant.ofEpochMilli(timestamp)
            val dateTime =
                    java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            val formatter = java.time.format.DateTimeFormatter.ofPattern(format)
            dateTime.format(formatter)
        } catch (e: DateTimeException) {
            // Fallback to simple timestamp string if formatting fails
            timestamp.toString()
        }
    }
}
