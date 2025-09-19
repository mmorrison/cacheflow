# Security Policy

## Supported Versions

We release patches for security vulnerabilities in the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security vulnerability in CacheFlow, please report it responsibly.

### How to Report

**Please do NOT report security vulnerabilities through public GitHub issues.**

Instead, please report them via:

2. **GitHub Security Advisories**: Use the "Report a vulnerability" button on the Security tab

### What to Include

When reporting a vulnerability, please include:

- **Description**: Clear description of the vulnerability
- **Impact**: Potential impact and affected components
- **Steps to Reproduce**: Detailed steps to reproduce the issue
- **Environment**: CacheFlow version, Java version, Spring Boot version
- **Proof of Concept**: If possible, provide a minimal reproduction case
- **Suggested Fix**: If you have ideas for fixing the issue

### Response Timeline

- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 1 week
- **Fix Development**: Depends on severity and complexity
- **Public Disclosure**: After fix is available and tested

### Severity Levels

We use the following severity levels:

- **Critical**: Remote code execution, authentication bypass
- **High**: Data exposure, privilege escalation
- **Medium**: Information disclosure, denial of service
- **Low**: Minor security improvements

## Security Best Practices

### For Users

1. **Keep Updated**: Always use the latest version of CacheFlow
2. **Secure Configuration**: Use secure configuration for cache storage
3. **Network Security**: Secure Redis and edge cache connections
4. **Access Control**: Implement proper access controls for management endpoints
5. **Monitoring**: Monitor cache operations for suspicious activity

### Configuration Security

```yaml
# Secure Redis configuration
cacheflow:
  redis:
    ssl: true
    password: ${REDIS_PASSWORD}
    timeout: 5000

# Secure management endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    cacheflow:
      enabled: true
      sensitive: true
```

### Edge Cache Security

- Use HTTPS for all edge cache communications
- Implement proper API key management
- Monitor edge cache usage for anomalies
- Use least-privilege access for edge cache providers

## Security Considerations

### Cache Storage

- **Redis**: Ensure Redis is properly secured with authentication and TLS
- **Local Cache**: Be aware of memory usage and potential data exposure
- **Edge Cache**: Validate and sanitize cache keys to prevent injection

### Management Endpoints

- **Authentication**: Secure management endpoints with proper authentication
- **Authorization**: Implement role-based access control
- **Network**: Restrict access to management endpoints

### Data Privacy

- **Sensitive Data**: Avoid caching sensitive information
- **Encryption**: Consider encrypting cached data for sensitive use cases
- **Retention**: Implement appropriate cache TTL for sensitive data

## Security Updates

Security updates will be released as:

- **Patch releases** for critical and high severity issues
- **Minor releases** for medium severity issues
- **Documentation updates** for low severity issues and best practices

## Credits

We thank all security researchers who responsibly disclose vulnerabilities to us.

## Contact

For security-related questions or concerns:

-- **GitHub**: Use the Security tab in the repository

---

**Note**: This security policy is subject to change. Please check back regularly for updates.
