package io.cacheflow.spring.versioning.impl

import io.cacheflow.spring.versioning.HasCreatedAt
import io.cacheflow.spring.versioning.HasModifiedAt
import io.cacheflow.spring.versioning.HasUpdatedAt
import io.cacheflow.spring.versioning.TimestampExtractor
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAccessor
import java.util.Date
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import org.springframework.stereotype.Component

/**
 * Default implementation of TimestampExtractor that can extract timestamps from various object
 * types commonly used in Spring applications.
 */
@Component
class DefaultTimestampExtractor : TimestampExtractor {

    override fun extractTimestamp(obj: Any?): Long? {
        if (obj == null) return null

        return when (obj) {
            is TemporalAccessor -> extractFromTemporalAccessor(obj)
            is Date -> obj.time
            is Long -> obj
            is Number -> obj.toLong()
            is HasUpdatedAt -> obj.updatedAt?.let { extractFromTemporalAccessor(it) }
            is HasCreatedAt -> obj.createdAt?.let { extractFromTemporalAccessor(it) }
            is HasModifiedAt -> obj.modifiedAt?.let { extractFromTemporalAccessor(it) }
            else -> extractFromReflection(obj)
        }
    }

    override fun hasTimestamp(obj: Any?): Boolean {
        if (obj == null) return false

        return when (obj) {
            is TemporalAccessor -> true
            is Date -> true
            is Long -> true
            is Number -> true
            is HasUpdatedAt -> obj.updatedAt != null
            is HasCreatedAt -> obj.createdAt != null
            is HasModifiedAt -> obj.modifiedAt != null
            else -> extractFromReflection(obj) != null
        }
    }

    private fun extractFromTemporalAccessor(temporal: TemporalAccessor): Long? {
        return try {
            when (temporal) {
                is Instant -> temporal.toEpochMilli()
                is LocalDateTime ->
                        temporal.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                is ZonedDateTime -> temporal.toInstant().toEpochMilli()
                is OffsetDateTime -> temporal.toInstant().toEpochMilli()
                else -> extractFromGenericTemporal(temporal)
            }
        } catch (e: DateTimeException) {
            null
        }
    }

    private fun extractFromGenericTemporal(temporal: TemporalAccessor): Long? {
        return try {
            Instant.from(temporal).toEpochMilli()
        } catch (e: DateTimeException) {
            extractFromEpochSeconds(temporal)
        }
    }

    private fun extractFromEpochSeconds(temporal: TemporalAccessor): Long? {
        return try {
            temporal.getLong(java.time.temporal.ChronoField.INSTANT_SECONDS) * 1000
        } catch (e: DateTimeException) {
            null
        }
    }

    private fun extractFromReflection(obj: Any): Long? {
        return try {
            val properties = obj::class.memberProperties
            findTimestampInProperties(obj, properties)
        } catch (e: java.lang.SecurityException) {
            // Security manager prevented reflection access - this is expected in restricted
            // environments
            null
        } catch (e: java.lang.IllegalAccessException) {
            // Property access denied - this is expected for private fields
            null
        } catch (e: java.lang.Exception) {
            // Other reflection-related exceptions - this is expected for objects without timestamp
            // fields
            null
        }
    }

    private fun findTimestampInProperties(
            obj: Any,
            properties: Collection<kotlin.reflect.KProperty1<out Any, *>>
    ): Long? {
        val timestampFields = getTimestampFieldNames()

        for (fieldName in timestampFields) {
            val property = properties.find { it.name == fieldName }
            if (property != null) {
                val timestamp = extractTimestampFromProperty(obj, property)
                if (timestamp != null) {
                    return timestamp
                }
            }
        }
        return null
    }

    private fun getTimestampFieldNames(): List<String> {
        return listOf(
                "updatedAt",
                "updated_at",
                "updatedAtTimestamp",
                "lastModified",
                "createdAt",
                "created_at",
                "createdAtTimestamp",
                "created",
                "modifiedAt",
                "modified_at",
                "modifiedAtTimestamp",
                "modified",
                "timestamp",
                "ts",
                "time",
                "date"
        )
    }

    private fun extractTimestampFromProperty(
            obj: Any,
            property: kotlin.reflect.KProperty1<out Any, *>
    ): Long? {
        return try {
            property.isAccessible = true
            val value = property.getter.call(obj)
            extractTimestamp(value)
        } catch (e: java.lang.SecurityException) {
            // Security manager prevented property access - this is expected in restricted
            // environments
            null
        } catch (e: java.lang.IllegalAccessException) {
            // Property access denied - this is expected for private fields
            null
        } catch (e: java.lang.Exception) {
            // Other reflection-related exceptions - this is expected for objects without timestamp
            // fields
            null
        }
    }
}
