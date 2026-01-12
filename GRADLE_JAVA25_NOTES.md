# Java 25 Target Configuration Notes

## Current Status

The project has been configured to target Java 25 with the following updates:

- **Gradle**: Upgraded to 9.0 (supports running on Java 25)
- **Kotlin**: Upgraded to 2.2.0 (supports Java 24 compilation target)
- **Java Toolchain**: Configured for Java 25
- **Kotlin JVM Target**: Set to JVM_24 (Kotlin 2.2.0 doesn't support JVM_25 yet, but Java 25 can run Java 24 bytecode)

## Known Compatibility Issues

### Gradle 9.0 + Kotlin 2.2.0 Dependency Resolution Issue

There is a known compatibility issue between Gradle 9.0 and Kotlin 2.2.0 that causes a dependency resolution listener error:

```
Failed to notify dependency resolution listener.
> 'java.util.Set org.gradle.api.artifacts.LenientConfiguration.getArtifacts(org.gradle.api.specs.Spec)'
```

This is due to API changes in Gradle 9.0 that Kotlin 2.2.0's dependency resolution listener hasn't been updated for yet.

### Temporarily Disabled Plugins

The following plugins have been temporarily disabled due to Gradle 9.0 compatibility issues:

- **Detekt** (1.23.1) - API incompatibility
- **SonarQube** (4.4.1.3373) - Compatibility issues  
- **OWASP Dependency Check** (8.4.3) - Compatibility issues
- **ktlint** (11.6.1) - Testing compatibility

## Workarounds

### Option 1: Use Java 24 for Compilation (Recommended)

Java 25 can run Java 24 bytecode, so you can:
- Keep Java 25 as the runtime
- Use JVM_24 as the Kotlin compilation target (already configured)
- Wait for Kotlin/Gradle plugin updates

### Option 2: Wait for Updates

Wait for:
- Kotlin 2.3.0+ (which should have better Gradle 9.0 compatibility)
- Gradle 9.1+ (if it addresses these issues)
- Plugin updates for Detekt, SonarQube, etc.

### Option 3: Use Gradle 8.10 with Java 24

If you need all plugins working immediately:
- Use Gradle 8.10.2 (supports Java 23)
- Use Java 24 as the target
- Re-enable all plugins

## Current Configuration

- **Java Source Compatibility**: 25
- **Java Toolchain**: 25
- **Kotlin JVM Target**: 24 (highest supported by Kotlin 2.2.0)
- **Gradle**: 9.0
- **Kotlin**: 2.2.0

## Next Steps

1. Monitor Kotlin releases for Gradle 9.0 compatibility fixes
2. Monitor plugin updates for Gradle 9.0 support
3. Consider using Java 24 compilation target until full Java 25 support is available

