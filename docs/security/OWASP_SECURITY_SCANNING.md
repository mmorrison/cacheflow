# OWASP Security Scanning Strategy

## Overview

This project includes OWASP Dependency Check for security vulnerability scanning. Due to network connectivity issues with the National Vulnerability Database (NVD), we've implemented a flexible approach to handle various scenarios.

## Configuration

### Current Setup

- **Plugin**: OWASP Dependency Check 8.4.3
- **CVSS Threshold**: 7.0 (High/Critical vulnerabilities)
- **Data Directory**: `build/dependency-check-data`
- **Suppression File**: `config/dependency-check-suppressions.xml`
- **Retry Configuration**: 3 retries with 30-second timeouts

### Network Handling

The OWASP plugin is configured to:

- **Not fail the build** on network errors by default
- **Cache data locally** for 7 days to reduce network dependency
- **Retry failed requests** up to 3 times
- **Use local data** when network is unavailable

## Available Tasks

### Core Quality Tasks (No Network Required)

```bash
./gradlew qualityCheck          # Detekt + Tests + Coverage
./gradlew buildAndTest          # Build + Tests + Coverage
./gradlew fullCheck             # Quality + Documentation
```

### Security-Enhanced Tasks (Requires Network)

```bash
./gradlew securityCheck                    # OWASP only
./gradlew qualityCheckWithSecurity         # Quality + OWASP
./gradlew fullCheckWithSecurity            # All checks + OWASP
```

## Usage Scenarios

### 1. Development Environment

```bash
# Use standard quality checks (no network dependency)
./gradlew qualityCheck
./gradlew buildAndTest
```

### 2. CI/CD Pipeline

```bash
# Try security scanning, but don't fail if network issues
./gradlew qualityCheckWithSecurity
```

### 3. Security-Focused Environment

```bash
# Force security scanning (will fail on network issues)
./gradlew -Powasp.failOnError=true securityCheck
```

### 4. Offline Environment

```bash
# Use cached data only
./gradlew -Powasp.autoUpdate=false securityCheck
```

## Troubleshooting

### Common Issues

1. **403 Forbidden from NVD**

   - **Cause**: Rate limiting or network restrictions
   - **Solution**: Use `qualityCheck` instead of `qualityCheckWithSecurity`

2. **Connection Timeout**

   - **Cause**: Slow network or firewall restrictions
   - **Solution**: Increase timeout in build.gradle.kts or use offline mode

3. **Outdated Vulnerability Data**
   - **Cause**: Network unavailable for updates
   - **Solution**: Run with `-Powasp.autoUpdate=false` to use cached data

### Network Configuration

If you have proxy settings or need to configure network access:

```bash
# Set proxy (if needed)
export GRADLE_OPTS="-Dhttp.proxyHost=proxy.company.com -Dhttp.proxyPort=8080"

# Run security check
./gradlew securityCheck
```

## Suppression File

The `config/dependency-check-suppressions.xml` file allows you to suppress false positives:

```xml
<suppress>
    <notes><![CDATA[
    This is a false positive. The vulnerability is not applicable to our use case.
    ]]></notes>
    <cve>CVE-2023-12345</cve>
</suppress>
```

## Best Practices

1. **Regular Security Scans**: Run `securityCheck` weekly or before releases
2. **Monitor Suppressions**: Review and update suppression file regularly
3. **Update Dependencies**: Keep dependencies updated to reduce vulnerabilities
4. **CI/CD Integration**: Use `qualityCheckWithSecurity` in CI/CD with proper error handling

## Reports

OWASP generates reports in multiple formats:

- **HTML**: `build/reports/dependency-check-report.html`
- **JSON**: `build/reports/dependency-check-report.json`
- **XML**: `build/reports/dependency-check-report.xml`

## Integration with Other Tools

- **SonarQube**: OWASP reports are integrated with SonarQube analysis
- **GitHub Actions**: Can be configured to run security checks in CI/CD
- **IDE**: Reports can be viewed in any web browser

## Future Improvements

1. **Alternative Data Sources**: Consider using GitHub Security Advisories
2. **Scheduled Updates**: Set up automated vulnerability database updates
3. **Custom Rules**: Implement custom vulnerability detection rules
4. **Integration**: Better integration with package managers and dependency updates
