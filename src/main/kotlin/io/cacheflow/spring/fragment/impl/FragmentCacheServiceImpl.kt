package io.cacheflow.spring.fragment.impl

import io.cacheflow.spring.fragment.FragmentCacheService
import io.cacheflow.spring.fragment.FragmentComposer
import io.cacheflow.spring.fragment.FragmentTagManager
import io.cacheflow.spring.service.CacheFlowService
import org.springframework.stereotype.Service

/**
 * Implementation of FragmentCacheService using the underlying CacheFlowService.
 *
 * This implementation provides fragment-specific caching operations while leveraging the existing
 * cache infrastructure.
 */
@Service
class FragmentCacheServiceImpl(
        private val cacheService: CacheFlowService,
        private val tagManager: FragmentTagManager,
        private val composer: FragmentComposer
) : FragmentCacheService {

    private val fragmentPrefix = "fragment:"

    override fun cacheFragment(key: String, fragment: String, ttl: Long) {
        val fragmentKey = buildFragmentKey(key)
        cacheService.put(fragmentKey, fragment, ttl)
    }

    override fun getFragment(key: String): String? {
        val fragmentKey = buildFragmentKey(key)
        return cacheService.get(fragmentKey) as? String
    }

    override fun composeFragments(template: String, fragments: Map<String, String>): String =
            composer.composeFragments(template, fragments)

    override fun composeFragmentsByKeys(template: String, fragmentKeys: List<String>): String =
            composer.composeFragmentsByKeys(template, fragmentKeys) { key -> getFragment(key) }

    override fun invalidateFragment(key: String) {
        val fragmentKey = buildFragmentKey(key)
        cacheService.evict(fragmentKey)
        tagManager.removeFragmentFromAllTags(key)
    }

    override fun invalidateFragmentsByTag(tag: String) {
        val fragmentKeys = tagManager.getFragmentsByTag(tag).toList()
        fragmentKeys.forEach { key -> invalidateFragment(key) }
    }

    override fun invalidateAllFragments() {
        val allKeys = cacheService.keys().filter { it.startsWith(fragmentPrefix) }
        allKeys.forEach { key -> cacheService.evict(key) }
        tagManager.clearAllTags()
    }

    override fun getFragmentCount(): Long =
            cacheService.keys().count { it.startsWith(fragmentPrefix) }.toLong()

    override fun getFragmentKeys(): Set<String> {
        return cacheService
                .keys()
                .filter { it.startsWith(fragmentPrefix) }
                .map { it.removePrefix(fragmentPrefix) }
                .toSet()
    }

    override fun hasFragment(key: String): Boolean {
        val fragmentKey = "$fragmentPrefix$key"
        return cacheService.get(fragmentKey) != null
    }

    private fun buildFragmentKey(key: String): String = "$fragmentPrefix$key"
}
