---
name: Bug report
about: Create a report to help us improve CacheFlow
title: "[BUG] "
labels: bug
assignees: ""
---

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:

1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

**Expected behavior**
A clear and concise description of what you expected to happen.

**Environment (please complete the following information):**

- CacheFlow version: [e.g. 1.0.0]
- Spring Boot version: [e.g. 3.2.0]
- Java version: [e.g. 17]
- Kotlin version: [e.g. 1.9.20]
- OS: [e.g. macOS, Linux, Windows]

**Configuration**

```yaml
# Please share your relevant configuration (remove sensitive information)
cacheflow:
  # your configuration here
```

**Code Sample**

```kotlin
// Please share relevant code that demonstrates the issue
@Service
class YourService {
    @CacheFlow(key = "test")
    fun yourMethod(): String {
        return "test"
    }
}
```

**Error Logs**

```
# Please share relevant error logs
```

**Additional context**
Add any other context about the problem here.

**Screenshots**
If applicable, add screenshots to help explain your problem.
