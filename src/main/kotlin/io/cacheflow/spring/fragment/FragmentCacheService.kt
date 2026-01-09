package io.cacheflow.spring.fragment

/**
 * Main service interface for managing fragment caches in Russian Doll caching.
 *
 * This interface combines all fragment caching operations by extending the specialized service
 * interfaces. Fragments are small, reusable pieces of content that can be cached independently and
 * composed together to form larger cached content.
 */
interface FragmentCacheService :
    FragmentStorageService, FragmentCompositionService, FragmentManagementService
