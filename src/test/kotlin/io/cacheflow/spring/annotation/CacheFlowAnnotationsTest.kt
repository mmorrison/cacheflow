package io.cacheflow.spring.annotation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CacheFlowAnnotationsTest {

    @Test
    fun `CacheFlow annotation should have correct target and retention`() {
        val annotation = CacheFlow::class.java
        val target = annotation.getAnnotation(Target::class.java)
        val retention = annotation.getAnnotation(Retention::class.java)

        assertNotNull(target)
        assertNotNull(retention)
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `CacheFlowCached annotation should have correct target and retention`() {
        val annotation = CacheFlowCached::class.java
        val target = annotation.getAnnotation(Target::class.java)
        val retention = annotation.getAnnotation(Retention::class.java)

        assertNotNull(target)
        assertNotNull(retention)
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `CacheFlowEvict annotation should have correct target and retention`() {
        val annotation = CacheFlowEvict::class.java
        val target = annotation.getAnnotation(Target::class.java)
        val retention = annotation.getAnnotation(Retention::class.java)

        assertNotNull(target)
        assertNotNull(retention)
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `CacheFlowEvictAlternative annotation should have correct target and retention`() {
        val annotation = CacheFlowEvictAlternative::class.java
        val target = annotation.getAnnotation(Target::class.java)
        val retention = annotation.getAnnotation(Retention::class.java)

        assertNotNull(target)
        assertNotNull(retention)
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `CacheEntity annotation should have correct target and retention`() {
        val annotation = CacheEntity::class.java
        val target = annotation.getAnnotation(Target::class.java)
        val retention = annotation.getAnnotation(Retention::class.java)

        assertNotNull(target)
        assertNotNull(retention)
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `CacheKey annotation should have correct target and retention`() {
        val annotation = CacheKey::class.java
        val target = annotation.getAnnotation(Target::class.java)
        val retention = annotation.getAnnotation(Retention::class.java)

        assertNotNull(target)
        assertNotNull(retention)
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `CacheVersion annotation should have correct target and retention`() {
        val annotation = CacheVersion::class.java
        val target = annotation.getAnnotation(Target::class.java)
        val retention = annotation.getAnnotation(Retention::class.java)

        assertNotNull(target)
        assertNotNull(retention)
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `CacheFlow annotation should have default values`() {
        val annotation = CacheFlow::class.java
        val method = TestClass::class.java.getDeclaredMethod("testMethod")
        val cacheFlow = method.getAnnotation(annotation)

        assertNotNull(cacheFlow)
        assertEquals("", cacheFlow.key)
        assertEquals("defaultKeyGenerator", cacheFlow.keyGenerator)
        assertEquals(-1L, cacheFlow.ttl)
        assertTrue(cacheFlow.dependsOn.isEmpty())
        assertTrue(cacheFlow.tags.isEmpty())
        assertEquals("", cacheFlow.condition)
        assertEquals("", cacheFlow.unless)
        assertFalse(cacheFlow.sync)
    }

    @Test
    fun `CacheFlowCached annotation should have default values`() {
        val annotation = CacheFlowCached::class.java
        val method = TestClass::class.java.getDeclaredMethod("testCachedMethod")
        val cacheFlowCached = method.getAnnotation(annotation)

        assertNotNull(cacheFlowCached)
        assertEquals("", cacheFlowCached.key)
        assertEquals("defaultKeyGenerator", cacheFlowCached.keyGenerator)
        assertEquals(-1L, cacheFlowCached.ttl)
        assertTrue(cacheFlowCached.dependsOn.isEmpty())
        assertTrue(cacheFlowCached.tags.isEmpty())
        assertEquals("", cacheFlowCached.condition)
        assertEquals("", cacheFlowCached.unless)
        assertFalse(cacheFlowCached.sync)
    }

    @Test
    fun `CacheFlowEvict annotation should have default values`() {
        val annotation = CacheFlowEvict::class.java
        val method = TestClass::class.java.getDeclaredMethod("testEvictMethod")
        val cacheFlowEvict = method.getAnnotation(annotation)

        assertNotNull(cacheFlowEvict)
        assertEquals("", cacheFlowEvict.key)
        assertTrue(cacheFlowEvict.tags.isEmpty())
        assertFalse(cacheFlowEvict.allEntries)
        assertFalse(cacheFlowEvict.beforeInvocation)
        assertEquals("", cacheFlowEvict.condition)
    }

    @Test
    fun `CacheFlowEvictAlternative annotation should have default values`() {
        val annotation = CacheFlowEvictAlternative::class.java
        val method = TestClass::class.java.getDeclaredMethod("testEvictAlternativeMethod")
        val cacheFlowEvictAlternative = method.getAnnotation(annotation)

        assertNotNull(cacheFlowEvictAlternative)
        assertEquals("", cacheFlowEvictAlternative.key)
        assertTrue(cacheFlowEvictAlternative.tags.isEmpty())
        assertFalse(cacheFlowEvictAlternative.allEntries)
        assertFalse(cacheFlowEvictAlternative.beforeInvocation)
        assertEquals("", cacheFlowEvictAlternative.condition)
    }

    @Test
    fun `CacheEntity annotation should have default values`() {
        val annotation = CacheEntity::class.java
        val cacheEntity = TestClass::class.java.getAnnotation(annotation)

        assertNotNull(cacheEntity)
        assertEquals("test:", cacheEntity.keyPrefix)
        assertEquals("version", cacheEntity.versionField)
    }

    // Test class with annotated methods
    @CacheEntity(keyPrefix = "test:", versionField = "version")
    class TestClass {
        @CacheFlow fun testMethod() {}

        @CacheFlowCached fun testCachedMethod() {}

        @CacheFlowEvict fun testEvictMethod() {}

        @CacheFlowEvictAlternative fun testEvictAlternativeMethod() {}
    }
}
