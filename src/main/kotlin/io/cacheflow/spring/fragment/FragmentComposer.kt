package io.cacheflow.spring.fragment

import org.springframework.stereotype.Component

/**
 * Handles fragment composition logic for Russian Doll caching.
 *
 * This service manages the composition of multiple fragments into a single result using
 * template-based placeholders.
 */
@Component
class FragmentComposer {
    /**
     * Composes multiple fragments into a single result using a template.
     *
     * @param template The template string with placeholders
     * @param fragments Map of placeholder names to fragment content
     * @return The composed result
     */
    fun composeFragments(
        template: String,
        fragments: Map<String, String>,
    ): String {
        var result = template

        fragments.forEach { (placeholder, fragment) ->
            val placeholderPattern = "\\{\\{$placeholder\\}\\}"
            result = result.replace(placeholderPattern.toRegex(), fragment)
        }

        return result
    }

    /**
     * Composes fragments by their keys using a template.
     *
     * @param template The template string with placeholders
     * @param fragmentKeys List of fragment keys to retrieve and compose
     * @param fragmentRetriever Function to retrieve fragments by key
     * @return The composed result
     */
    fun composeFragmentsByKeys(
        template: String,
        fragmentKeys: List<String>,
        fragmentRetriever: (String) -> String?,
    ): String {
        // Extract placeholder names from template
        val placeholderPattern = "\\{\\{([^}]+)\\}\\}".toRegex()
        val placeholders = placeholderPattern.findAll(template).map { it.groupValues[1] }.toSet()

        // Map fragment keys to placeholder names
        val fragments = mutableMapOf<String, String>()

        for (fragmentKey in fragmentKeys) {
            val fragmentContent = fragmentRetriever(fragmentKey)
            if (fragmentContent != null) {
                // Try to find matching placeholder by extracting the last part of the key
                val keyParts = fragmentKey.split(":")
                val lastPart = keyParts.lastOrNull()

                // Check if this matches any placeholder
                for (placeholder in placeholders) {
                    if (lastPart == placeholder || fragmentKey.contains(placeholder)) {
                        fragments[placeholder] = fragmentContent
                        break
                    }
                }
            }
        }

        return composeFragments(template, fragments)
    }

    /**
     * Validates that all required placeholders in a template are provided.
     *
     * @param template The template string
     * @param fragments Map of available fragments
     * @return Set of missing placeholder names
     */
    fun findMissingPlaceholders(
        template: String,
        fragments: Map<String, String>,
    ): Set<String> {
        val placeholderPattern = "\\{\\{([^}]+)\\}\\}".toRegex()
        val placeholders = placeholderPattern.findAll(template).map { it.groupValues[1] }.toSet()

        return placeholders - fragments.keys
    }

    /**
     * Extracts all placeholders from a template.
     *
     * @param template The template string
     * @return Set of placeholder names
     */
    fun extractPlaceholders(template: String): Set<String> {
        val placeholderPattern = "\\{\\{([^}]+)\\}\\}".toRegex()
        return placeholderPattern.findAll(template).map { it.groupValues[1] }.toSet()
    }
}
