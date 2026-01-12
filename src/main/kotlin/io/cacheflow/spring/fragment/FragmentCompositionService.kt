package io.cacheflow.spring.fragment

/**
 * Service interface for fragment composition operations in Russian Doll caching.
 *
 * This interface handles the composition of multiple fragments into a single result using
 * template-based placeholders.
 */
interface FragmentCompositionService {
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
    ): String

    /**
     * Composes fragments by their keys using a template.
     *
     * @param template The template string with placeholders
     * @param fragmentKeys List of fragment keys to retrieve and compose
     * @return The composed result
     */
    fun composeFragmentsByKeys(
        template: String,
        fragmentKeys: List<String>,
    ): String
}
