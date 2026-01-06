package io.cacheflow.spring.fragment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FragmentComposerTest {

    private val composer = FragmentComposer()

    @Test
    fun `should extract placeholders`() {
        val template = "<div>{{header}}</div><div>{{content}}</div>"

        val placeholders = composer.extractPlaceholders(template)

        assertEquals(2, placeholders.size)
        assertTrue(placeholders.contains("header"))
        assertTrue(placeholders.contains("content"))
    }

    @Test
    fun `should find missing placeholders`() {
        val template = "<div>{{header}}</div><div>{{content}}</div>"
        val fragments = mapOf("header" to "Header Content")

        val missing = composer.findMissingPlaceholders(template, fragments)

        assertEquals(1, missing.size)
        assertTrue(missing.contains("content"))
    }

    @Test
    fun `should compose fragments`() {
        val template = "Start {{part1}} End"
        val fragments = mapOf("part1" to "Middle")

        val result = composer.composeFragments(template, fragments)

        assertEquals("Start Middle End", result)
    }
}
