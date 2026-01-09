# Java 24 Target Configuration

## Current Configuration

The project is configured to target **Java 24** for compilation:

- **Gradle**: 9.0 (required to run on Java 25 runtime)
- **Kotlin**: 2.2.0 (supports JVM_24 compilation target)
- **Java Source Compatibility**: 24
- **Kotlin JVM Target**: JVM_24
- **Runtime**: Can run on Java 24 or Java 25 (Java 25 can execute Java 24 bytecode)

## Known Issue: Gradle 9.0 + Kotlin 2.2.0 Compatibility

There is a known compatibility issue between Gradle 9.0 and Kotlin 2.2.0 that prevents compilation:

```
Failed to notify dependency resolution listener.
> 'java.util.Set org.gradle.api.artifacts.LenientConfiguration.getArtifacts(org.gradle.api.specs.Spec)'
```

This is due to API changes in Gradle 9.0's dependency resolution system that Kotlin 2.2.0 hasn't been updated for yet.

### Workaround

Until Kotlin releases a version compatible with Gradle 9.0, you have two options:

1. **Use Java 24 Runtime** (Recommended)
   - Install Java 24
   - Use Gradle 8.10.2 (supports Java 23, can work with Java 24)
   - All plugins will work

2. **Wait for Kotlin Update**
   - Monitor Kotlin releases for Gradle 9.0 compatibility
   - Expected in Kotlin 2.3.0+ or a patch release

## Temporarily Disabled

- **Detekt**: Waiting for Gradle 9.0 compatible version

## Status

The build configuration is correct for Java 24 targeting. The compilation issue is a toolchain compatibility problem that requires updates from the Kotlin team.

