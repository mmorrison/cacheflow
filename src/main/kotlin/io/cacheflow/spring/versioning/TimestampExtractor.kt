package io.cacheflow.spring.versioning

import java.time.temporal.TemporalAccessor

/**
 * Interface for extracting timestamps from objects for cache key versioning.
 *
 * This interface provides methods to extract timestamps from various object types to enable
 * versioned cache keys in Russian Doll caching.
 */
interface TimestampExtractor {

    /**
     * Extracts a timestamp from an object.
     *
     * @param obj The object to extract timestamp from
     * @return The timestamp in milliseconds since epoch, or null if no timestamp found
     */
    fun extractTimestamp(obj: Any?): Long?

    /**
     * Checks if an object has a timestamp that can be extracted.
     *
     * @param obj The object to check
     * @return true if the object has an extractable timestamp, false otherwise
     */
    fun hasTimestamp(obj: Any?): Boolean
}

/** Interface for objects that have an updatedAt timestamp. */
interface HasUpdatedAt {
    /** The timestamp when the object was last updated. */
    val updatedAt: TemporalAccessor?
}

/** Interface for objects that have a createdAt timestamp. */
interface HasCreatedAt {
    /** The timestamp when the object was created. */
    val createdAt: TemporalAccessor?
}

/** Interface for objects that have a modifiedAt timestamp. */
interface HasModifiedAt {
    /** The timestamp when the object was last modified. */
    val modifiedAt: TemporalAccessor?
}
