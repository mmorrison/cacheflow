# Changelog

All notable changes to CacheFlow will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0-beta] - 2026-01-12

### Added
- **Redis Integration**: Distributed caching support via `CacheFlowRedisConfiguration`.
- **Edge Cache Orchestration**: Automatic purging of Cloudflare, AWS CloudFront, and Fastly caches.
- **Russian Doll Pattern**: Local → Redis → Edge multi-level cache flow.
- **Advanced Metrics**: Micrometer integration for tracking hits, misses, and evictions per layer.
- **Async Operations**: Non-blocking Edge Cache purges using Kotlin Coroutines.

### Changed
- Refactored `CacheFlowServiceImpl` to support tiered storage.
- Updated `CacheFlowCoreConfiguration` to inject optional Redis and Edge dependencies.

### Fixed
- Improved test stability and added mock-based verification for distributed paths.

## [0.1.0-alpha] - 2024-12-19

### Added

- Initial alpha release of CacheFlow Spring Boot Starter
- Basic in-memory caching implementation
- AOP-based annotations (@CacheFlow, @CacheFlowEvict)
- SpEL support for dynamic cache keys and conditions
- Basic management endpoints via Spring Boot Actuator
- Spring Boot auto-configuration
- Comprehensive documentation and examples
- Unit tests for core functionality

### Features

- **Core Caching**: In-memory caching with TTL support
- **AOP Integration**: Seamless annotation-based caching
- **SpEL Support**: Dynamic cache keys and conditions
- **Management**: Actuator endpoints for cache operations
- **Configuration**: Flexible TTL and cache settings
- **Testing**: Comprehensive unit test coverage

### Dependencies

- Spring Boot 3.2.0+
- Kotlin 1.9.20+
- Java 17+
- Spring AOP
- Spring Expression Language
- Micrometer for metrics

---

## Release Notes

### Version 0.1.0-alpha

This is the initial alpha release of CacheFlow, providing a solid foundation for multi-level caching in Spring Boot applications. The library offers:

- **Easy Integration**: Simple Spring Boot starter with auto-configuration
- **Annotation-Based**: Intuitive @CacheFlow and @CacheFlowEvict annotations
- **SpEL Support**: Dynamic cache keys and conditions using Spring Expression Language
- **Management**: Built-in actuator endpoints for cache monitoring and control
- **Alpha Ready**: Comprehensive testing and documentation

### Breaking Changes

- None in this initial release

### Deprecations

- None in this initial release
