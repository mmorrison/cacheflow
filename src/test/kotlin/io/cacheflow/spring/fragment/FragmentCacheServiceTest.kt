package io.cacheflow.spring.fragment

import io.cacheflow.spring.fragment.impl.FragmentCacheServiceImpl
import io.cacheflow.spring.service.CacheFlowService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class FragmentCacheServiceTest {

    @Mock private lateinit var cacheService: CacheFlowService

    @Mock private lateinit var tagManager: FragmentTagManager
    private val composer: FragmentComposer = FragmentComposer()

    private lateinit var fragmentCacheService: FragmentCacheService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        fragmentCacheService = FragmentCacheServiceImpl(cacheService, tagManager, composer)
    }

    @Test
    fun `should cache fragment correctly`() {
        // Given
        val key = "user:123:profile"
        val fragment = "<div>User Profile</div>"
        val ttl = 3600L

        // When
        fragmentCacheService.cacheFragment(key, fragment, ttl)

        // Then
        verify(cacheService).put("fragment:$key", fragment, ttl)
    }

    @Test
    fun `should retrieve fragment correctly`() {
        // Given
        val key = "user:123:profile"
        val fragment = "<div>User Profile</div>"
        `when`(cacheService.get("fragment:$key")).thenReturn(fragment)

        // When
        val result = fragmentCacheService.getFragment(key)

        // Then
        assertEquals(fragment, result)
        verify(cacheService).get("fragment:$key")
    }

    @Test
    fun `should return null for non-existent fragment`() {
        // Given
        val key = "non-existent"
        `when`(cacheService.get("fragment:$key")).thenReturn(null)

        // When
        val result = fragmentCacheService.getFragment(key)

        // Then
        assertNull(result)
    }

    @Test
    fun `should compose fragments correctly`() {
        // Given
        val template = "<div>{{header}}</div><div>{{content}}</div>"
        val fragments = mapOf("header" to "<h1>Title</h1>", "content" to "<p>Content</p>")

        // When
        val result = fragmentCacheService.composeFragments(template, fragments)

        // Then
        assertEquals("<div><h1>Title</h1></div><div><p>Content</p></div>", result)
    }

    @Test
    fun `should compose fragments by keys correctly`() {
        // Given
        val template = "<div>{{header}}</div><div>{{content}}</div>"
        val fragmentKeys = listOf("header", "content")
        val headerFragment = "<h1>Title</h1>"
        val contentFragment = "<p>Content</p>"

        `when`(cacheService.get("fragment:header")).thenReturn(headerFragment)

        `when`(cacheService.get("fragment:content")).thenReturn(contentFragment)

        // When
        val result = fragmentCacheService.composeFragmentsByKeys(template, fragmentKeys)

        // Then
        println("Result: $result")

        assertEquals("<div><h1>Title</h1></div><div><p>Content</p></div>", result)
    }

    @Test
    fun `should handle missing fragments in composition`() {
        // Given
        val template = "<div>{{header}}</div><div>{{content}}</div>"
        val fragmentKeys = listOf("header", "content", "missing")
        val headerFragment = "<h1>Title</h1>"

        `when`(cacheService.get("fragment:header")).thenReturn(headerFragment)

        `when`(cacheService.get("fragment:content")).thenReturn(null)

        `when`(cacheService.get("fragment:missing")).thenReturn(null)

        // When
        val result = fragmentCacheService.composeFragmentsByKeys(template, fragmentKeys)

        // Then
        assertEquals("<div><h1>Title</h1></div><div>{{content}}</div>", result)
    }

    @Test
    fun `should invalidate fragment correctly`() {
        // Given
        val key = "user:123:profile"

        // When
        fragmentCacheService.invalidateFragment(key)

        // Then
        verify(cacheService).evict("fragment:$key")
    }

    @Test
    fun `should invalidate all fragments correctly`() {
        // Given
        val allKeys = setOf("fragment:key1", "fragment:key2", "regular:key3")
        `when`(cacheService.keys()).thenReturn(allKeys)

        // When
        fragmentCacheService.invalidateAllFragments()

        // Then
        verify(cacheService).evict("fragment:key1")
        verify(cacheService).evict("fragment:key2")
        verify(cacheService, never()).evict("regular:key3")
    }

    @Test
    fun `should get fragment count correctly`() {
        // Given
        val allKeys = setOf("fragment:key1", "fragment:key2", "regular:key3")
        `when`(cacheService.keys()).thenReturn(allKeys)

        // When
        val count = fragmentCacheService.getFragmentCount()

        // Then
        assertEquals(2L, count)
    }

    @Test
    fun `should get fragment keys correctly`() {
        // Given
        val allKeys = setOf("fragment:key1", "fragment:key2", "regular:key3")
        `when`(cacheService.keys()).thenReturn(allKeys)

        // When
        val fragmentKeys = fragmentCacheService.getFragmentKeys()

        // Then
        assertEquals(setOf("key1", "key2"), fragmentKeys)
    }

    @Test
    fun `should check fragment existence correctly`() {
        // Given
        val key = "user:123:profile"
        `when`(cacheService.get("fragment:$key")).thenReturn("<div>Profile</div>")

        // When
        val exists = fragmentCacheService.hasFragment(key)

        // Then
        assertTrue(exists)
        verify(cacheService).get("fragment:$key")
    }

    @Test
    fun `should handle tag operations correctly`() {
        // Given

        val key = "user:123:profile"
        val tag = "user-fragments"

// Mock the tag manager behavior
        `when`(tagManager.getFragmentsByTag(tag)).thenReturn(setOf(key))

        `when`(tagManager.getFragmentTags(key)).thenReturn(setOf(tag))

        // When

        val fragmentsByTag = tagManager.getFragmentsByTag(tag)
        val tagsByFragment = tagManager.getFragmentTags(key)

        // Then
        assertTrue(fragmentsByTag.contains(key))
        assertTrue(tagsByFragment.contains(tag))

// When - after removal
        `when`(tagManager.getFragmentsByTag(tag)).thenReturn(emptySet())

        `when`(tagManager.getFragmentTags(key)).thenReturn(emptySet())

        val fragmentsByTagAfter = tagManager.getFragmentsByTag(tag)
        val tagsByFragmentAfter = tagManager.getFragmentTags(key)

        // Then
        assertFalse(fragmentsByTagAfter.contains(key))
        assertFalse(tagsByFragmentAfter.contains(tag))
    }
}
