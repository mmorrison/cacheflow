package io.cacheflow.spring.fragment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FragmentTagManagerTest {
    private lateinit var tagManager: FragmentTagManager

    @BeforeEach
    fun setUp() {
        tagManager = FragmentTagManager()
    }

    @Test
    fun `should add fragment tag correctly`() {
        // Given
        val key = "user:123:profile"
        val tag = "user-fragments"

        // When
        tagManager.addFragmentTag(key, tag)

        // Then
        val fragments = tagManager.getFragmentsByTag(tag)
        assertTrue(fragments.contains(key))
        assertEquals(1, fragments.size)
    }

    @Test
    fun `should add multiple fragments to same tag`() {
        // Given
        val key1 = "user:123:profile"
        val key2 = "user:456:profile"
        val tag = "user-fragments"

        // When
        tagManager.addFragmentTag(key1, tag)
        tagManager.addFragmentTag(key2, tag)

        // Then
        val fragments = tagManager.getFragmentsByTag(tag)
        assertTrue(fragments.contains(key1))
        assertTrue(fragments.contains(key2))
        assertEquals(2, fragments.size)
    }

    @Test
    fun `should add multiple tags to same fragment`() {
        // Given
        val key = "user:123:profile"
        val tag1 = "user-fragments"
        val tag2 = "profile-fragments"

        // When
        tagManager.addFragmentTag(key, tag1)
        tagManager.addFragmentTag(key, tag2)

        // Then
        val tags = tagManager.getFragmentTags(key)
        assertTrue(tags.contains(tag1))
        assertTrue(tags.contains(tag2))
        assertEquals(2, tags.size)
    }

    @Test
    fun `should remove fragment tag correctly`() {
        // Given
        val key = "user:123:profile"
        val tag = "user-fragments"
        tagManager.addFragmentTag(key, tag)

        // When
        tagManager.removeFragmentTag(key, tag)

        // Then
        val fragments = tagManager.getFragmentsByTag(tag)
        assertFalse(fragments.contains(key))
        assertTrue(fragments.isEmpty())
    }

    @Test
    fun `should remove tag when last fragment is removed`() {
        // Given
        val key = "user:123:profile"
        val tag = "user-fragments"
        tagManager.addFragmentTag(key, tag)

        // When
        tagManager.removeFragmentTag(key, tag)

        // Then
        val allTags = tagManager.getAllTags()
        assertFalse(allTags.contains(tag))
    }

    @Test
    fun `should not remove tag when other fragments remain`() {
        // Given
        val key1 = "user:123:profile"
        val key2 = "user:456:profile"
        val tag = "user-fragments"
        tagManager.addFragmentTag(key1, tag)
        tagManager.addFragmentTag(key2, tag)

        // When
        tagManager.removeFragmentTag(key1, tag)

        // Then
        val fragments = tagManager.getFragmentsByTag(tag)
        assertFalse(fragments.contains(key1))
        assertTrue(fragments.contains(key2))
        assertEquals(1, fragments.size)

        val allTags = tagManager.getAllTags()
        assertTrue(allTags.contains(tag))
    }

    @Test
    fun `should get fragments by tag correctly`() {
        // Given
        val key1 = "user:123:profile"
        val key2 = "user:456:profile"
        val tag = "user-fragments"
        tagManager.addFragmentTag(key1, tag)
        tagManager.addFragmentTag(key2, tag)

        // When
        val fragments = tagManager.getFragmentsByTag(tag)

        // Then
        assertEquals(setOf(key1, key2), fragments)
    }

    @Test
    fun `should return empty set for non-existent tag`() {
        // When
        val fragments = tagManager.getFragmentsByTag("non-existent")

        // Then
        assertTrue(fragments.isEmpty())
    }

    @Test
    fun `should return immutable set from getFragmentsByTag`() {
        // Given
        val key = "user:123:profile"
        val tag = "user-fragments"
        tagManager.addFragmentTag(key, tag)

        // When
        val fragments = tagManager.getFragmentsByTag(tag)

        // Then
        // Verify it's a different instance (defensive copy)
        val fragments2 = tagManager.getFragmentsByTag(tag)
        assertTrue(fragments !== fragments2)
        assertEquals(fragments, fragments2)
    }

    @Test
    fun `should get fragment tags correctly`() {
        // Given
        val key = "user:123:profile"
        val tag1 = "user-fragments"
        val tag2 = "profile-fragments"
        tagManager.addFragmentTag(key, tag1)
        tagManager.addFragmentTag(key, tag2)

        // When
        val tags = tagManager.getFragmentTags(key)

        // Then
        assertEquals(setOf(tag1, tag2), tags)
    }

    @Test
    fun `should return empty set for fragment with no tags`() {
        // When
        val tags = tagManager.getFragmentTags("non-existent")

        // Then
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `should return immutable set from getFragmentTags`() {
        // Given
        val key = "user:123:profile"
        val tag = "user-fragments"
        tagManager.addFragmentTag(key, tag)

        // When
        val tags = tagManager.getFragmentTags(key)

        // Then
        // Verify it's a different instance (defensive copy)
        val tags2 = tagManager.getFragmentTags(key)
        assertTrue(tags !== tags2)
        assertEquals(tags, tags2)
    }

    @Test
    fun `should remove fragment from all tags correctly`() {
        // Given
        val key = "user:123:profile"
        val tag1 = "user-fragments"
        val tag2 = "profile-fragments"
        tagManager.addFragmentTag(key, tag1)
        tagManager.addFragmentTag(key, tag2)

        // When
        tagManager.removeFragmentFromAllTags(key)

        // Then
        val tags = tagManager.getFragmentTags(key)
        assertTrue(tags.isEmpty())

        val fragments1 = tagManager.getFragmentsByTag(tag1)
        assertFalse(fragments1.contains(key))

        val fragments2 = tagManager.getFragmentsByTag(tag2)
        assertFalse(fragments2.contains(key))
    }

    @Test
    fun `should clear all tags correctly`() {
        // Given
        val key1 = "user:123:profile"
        val key2 = "user:456:profile"
        val tag1 = "user-fragments"
        val tag2 = "profile-fragments"
        tagManager.addFragmentTag(key1, tag1)
        tagManager.addFragmentTag(key2, tag2)

        // When
        tagManager.clearAllTags()

        // Then
        assertTrue(tagManager.getAllTags().isEmpty())
        assertTrue(tagManager.getFragmentsByTag(tag1).isEmpty())
        assertTrue(tagManager.getFragmentsByTag(tag2).isEmpty())
        assertEquals(0, tagManager.getTagCount())
    }

    @Test
    fun `should get all tags correctly`() {
        // Given
        val tag1 = "user-fragments"
        val tag2 = "profile-fragments"
        val tag3 = "post-fragments"
        tagManager.addFragmentTag("key1", tag1)
        tagManager.addFragmentTag("key2", tag2)
        tagManager.addFragmentTag("key3", tag3)

        // When
        val allTags = tagManager.getAllTags()

        // Then
        assertEquals(setOf(tag1, tag2, tag3), allTags)
    }

    @Test
    fun `should return empty set when no tags exist`() {
        // When
        val allTags = tagManager.getAllTags()

        // Then
        assertTrue(allTags.isEmpty())
    }

    @Test
    fun `should return immutable set from getAllTags`() {
        // Given
        tagManager.addFragmentTag("key1", "tag1")

        // When
        val tags = tagManager.getAllTags()

        // Then
        // Verify it's a different instance (defensive copy)
        val tags2 = tagManager.getAllTags()
        assertTrue(tags !== tags2)
        assertEquals(tags, tags2)
    }

    @Test
    fun `should get tag count correctly`() {
        // Given
        tagManager.addFragmentTag("key1", "tag1")
        tagManager.addFragmentTag("key2", "tag2")
        tagManager.addFragmentTag("key3", "tag3")

        // When
        val count = tagManager.getTagCount()

        // Then
        assertEquals(3, count)
    }

    @Test
    fun `should return zero count when no tags exist`() {
        // When
        val count = tagManager.getTagCount()

        // Then
        assertEquals(0, count)
    }

    @Test
    fun `should not duplicate fragment in tag`() {
        // Given
        val key = "user:123:profile"
        val tag = "user-fragments"

        // When
        tagManager.addFragmentTag(key, tag)
        tagManager.addFragmentTag(key, tag) // Add same combination again

        // Then
        val fragments = tagManager.getFragmentsByTag(tag)
        assertEquals(1, fragments.size)
        assertTrue(fragments.contains(key))
    }

    @Test
    fun `should handle concurrent modifications safely`() {
        // Given
        val key = "user:123:profile"
        val tag = "user-fragments"

        // When - Add while iterating
        tagManager.addFragmentTag(key, tag)
        tagManager.addFragmentTag("user:456:profile", tag)

        val fragments = tagManager.getFragmentsByTag(tag)

        // Add more while we have a reference to the previous set
        tagManager.addFragmentTag("user:789:profile", tag)

        // Then - Original set should not be affected
        assertEquals(2, fragments.size)

        // New query should show all fragments
        val newFragments = tagManager.getFragmentsByTag(tag)
        assertEquals(3, newFragments.size)
    }

    @Test
    fun `should handle empty tag name`() {
        // Given
        val key = "user:123:profile"
        val tag = ""

        // When
        tagManager.addFragmentTag(key, tag)

        // Then
        val fragments = tagManager.getFragmentsByTag(tag)
        assertTrue(fragments.contains(key))
    }

    @Test
    fun `should handle empty key name`() {
        // Given
        val key = ""
        val tag = "user-fragments"

        // When
        tagManager.addFragmentTag(key, tag)

        // Then
        val fragments = tagManager.getFragmentsByTag(tag)
        assertTrue(fragments.contains(key))
    }
}
