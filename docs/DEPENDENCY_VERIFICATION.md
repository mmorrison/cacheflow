# Gradle Dependency Verification - Team Guide

## Overview

This project uses Gradle dependency verification to ensure the integrity and authenticity of all dependencies. This protects against supply chain attacks by verifying that dependencies haven't been tampered with.

## What It Means for You

Every time Gradle downloads a dependency, it will:
1. ✅ Verify the PGP signature (if available)
2. ✅ Verify the SHA256 checksum
3. ❌ Fail the build if verification fails

This adds security but requires a specific workflow when working with dependencies.

---

## Common Workflows

### Adding a New Dependency

**Step 1:** Add the dependency to `build.gradle.kts` as usual

```kotlin
dependencies {
    implementation("com.example:new-library:1.0.0")
}
```

**Step 2:** Regenerate verification metadata

```bash
./gradlew --write-verification-metadata pgp,sha256 --export-keys
```

This command will:
- Download the new dependency
- Verify and record its checksum and signature
- Update `gradle/verification-metadata.xml`
- Update keyring files if new PGP keys are found

**Step 3:** Commit all changes together

```bash
git add build.gradle.kts gradle/verification-metadata.xml gradle/verification-keyring.*
git commit -m "Add new-library dependency with verification"
```

> [!IMPORTANT]
> **Always commit verification files with dependency changes**
> 
> If you forget to regenerate verification metadata, the CI build will fail because the new dependency won't be verified.

---

### Updating an Existing Dependency

**Step 1:** Update the version in `build.gradle.kts`

```kotlin
dependencies {
    // Update from 1.0.0 to 1.1.0
    implementation("com.example:library:1.1.0")
}
```

**Step 2:** Regenerate verification metadata

```bash
./gradlew --write-verification-metadata pgp,sha256 --export-keys
```

**Step 3:** Commit changes

```bash
git add build.gradle.kts gradle/verification-metadata.xml gradle/verification-keyring.*
git commit -m "Update library to 1.1.0 with verification"
```

---

### Removing a Dependency

**Step 1:** Remove from `build.gradle.kts`

**Step 2:** Regenerate verification metadata (this cleans up unused entries)

```bash
./gradlew --write-verification-metadata pgp,sha256 --export-keys
```

**Step 3:** Commit changes

```bash
git add build.gradle.kts gradle/verification-metadata.xml gradle/verification-keyring.*
git commit -m "Remove unused dependency"
```

---

## Troubleshooting

### Build Fails with "Dependency verification failed"

**Symptoms:**
```
Dependency verification failed for configuration ':compileClasspath'
```

**Possible Causes & Solutions:**

1. **New dependency added without updating verification**
   - **Solution:** Run `./gradlew --write-verification-metadata pgp,sha256 --export-keys`

2. **Stale Gradle cache**
   - **Solution:** Clean and refresh dependencies
   ```bash
   ./gradlew clean --refresh-dependencies
   ```

3. **Network issues during download**
   - **Solution:** Retry the build. If persistent, check network connectivity

4. **Corrupted local cache**
   - **Solution:** Clear Gradle cache and rebuild
   ```bash
   rm -rf ~/.gradle/caches
   ./gradlew clean build
   ```

5. **Actual dependency tampering (RARE but serious)**
   - **Solution:** 
     - ⚠️ **DO NOT DISABLE VERIFICATION**
     - Report to security team immediately
     - Investigate the dependency source
     - Check for security advisories

---

### Merge Conflicts in verification-metadata.xml

**Symptoms:**
Git merge conflict in `gradle/verification-metadata.xml`

**Solution:**

After resolving dependency conflicts in `build.gradle.kts`:

```bash
# 1. Accept their version or yours for build.gradle.kts
# 2. Then regenerate verification metadata cleanly
./gradlew --write-verification-metadata pgp,sha256 --export-keys

# 3. Mark conflicts as resolved
git add gradle/verification-metadata.xml gradle/verification-keyring.*
git commit
```

> [!TIP]
> **Don't manually merge verification-metadata.xml**
> 
> Always regenerate it instead. The file is machine-generated and safe to replace.

---

### CI/CD Build Fails but Local Build Works

**Symptoms:**
- Local build passes
- CI build fails with verification errors

**Possible Causes:**

1. **Forgot to commit verification files**
   - **Solution:** Commit and push the verification files
   ```bash
   git add gradle/verification-metadata.xml gradle/verification-keyring.*
   git commit --amend --no-edit
   git push --force-with-lease
   ```

2. **Different dependency resolution in CI**
   - **Solution:** Check if CI uses different Gradle version or JDK version
   - Ensure `.mise.toml` or similar config is consistent

---

## PR Review Guidelines

When reviewing pull requests that change dependencies:

### ✅ Check these things:

- [ ] `gradle/verification-metadata.xml` is updated
- [ ] `gradle/verification-keyring.gpg` and `.keys` files are updated (if new dependencies)
- [ ] CI build passes
- [ ] Dependency version makes sense (semantic versioning)
- [ ] New dependencies are from trusted sources

### ❌ Red flags:

- ⚠️ Dependency change without verification metadata update
- ⚠️ Verification metadata deleted or disabled
- ⚠️ Dependencies from unknown or untrusted sources
- ⚠️ Large number of ignored keys added without explanation

---

## Advanced Topics

### Understanding the Verification Metadata

The `gradle/verification-metadata.xml` file contains:

```xml
<configuration>
   <verify-metadata>true</verify-metadata>
   <verify-signatures>true</verify-signatures>
   <ignored-keys>
      <!-- Keys that couldn't be downloaded from key servers -->
   </ignored-keys>
   <trusted-keys>
      <!-- Known good publisher keys -->
   </trusted-keys>
</configuration>
<components>
   <!-- Checksums for each dependency artifact -->
</components>
```

- **trusted-keys**: PGP keys from known publishers (Spring, Apache, Google, etc.)
- **ignored-keys**: Dependencies without downloadable keys (fallback to checksum only)
- **components**: SHA256 checksums for every JAR, POM, and module file

### Verifying a Specific Dependency Manually

If you want to manually verify a dependency's publisher:

```bash
# 1. Find the key ID in verification-metadata.xml
# 2. Look up the key on a keyserver
gpg --keyserver hkps://keys.openpgp.org --recv-keys <KEY_ID>
gpg --list-keys <KEY_ID>

# 3. Verify against official sources
# Check the project's website, GitHub repo, etc.
```

### Dealing with Unsigned Dependencies

Some dependencies don't provide PGP signatures. For these:
- Gradle uses SHA256 checksum verification only
- The key is added to `<ignored-keys>` section
- This is still secure as long as you trust the initial checksum

If you're concerned about a specific unsigned dependency:
1. Check the dependency's official documentation
2. Verify the checksum against official sources
3. Consider alternatives if no verification method exists

---

## Quick Reference

### Essential Commands

```bash
# Regenerate verification metadata (use this most often)
./gradlew --write-verification-metadata pgp,sha256 --export-keys

# Clean build with verification
./gradlew clean build

# Refresh dependencies and rebuild
./gradlew clean --refresh-dependencies build

# Run tests with verification
./gradlew test
```

### Files Involved

| File | Purpose | Commit? |
|------|---------|---------|
| `gradle/verification-metadata.xml` | Main verification config | ✅ Yes |
| `gradle/verification-keyring.gpg` | Binary PGP keyring | ✅ Yes |
| `gradle/verification-keyring.keys` | ASCII PGP keyring | ✅ Yes |
| `build.gradle.kts` | Dependency declarations | ✅ Yes |

---

## FAQ

**Q: Can I disable verification for local development?**  
A: No, and you shouldn't. Verification runs quickly and provides important security guarantees.

**Q: What if verification is too slow?**  
A: Initial verification downloads keys, but subsequent builds use cache and are fast. If it's consistently slow, check network connectivity.

**Q: Can I manually edit verification-metadata.xml?**  
A: Not recommended. Always regenerate it using the Gradle command.

**Q: What happens if a dependency is compromised?**  
A: Gradle will detect the checksum/signature mismatch and fail the build, protecting you.

**Q: Do I need to regenerate for transitive dependencies?**  
A: No, transitive dependencies are automatically included when you regenerate for direct dependencies.

**Q: How do I know which dependencies are trusted?**  
A: Check the `<trusted-keys>` section in verification-metadata.xml. Major publishers like Spring, Apache, Google, etc. are included.

---

## Getting Help

If you encounter issues not covered here:

1. **Check CI logs** - Often provides specific error messages
2. **Clean and retry** - Many issues are resolved with `./gradlew clean --refresh-dependencies`
3. **Ask the team** - Someone may have encountered the issue before
4. **Security concerns** - Report dependency verification bypasses or suspicious failures to the security team

---

## Additional Resources

- [Gradle Dependency Verification Documentation](https://docs.gradle.org/current/userguide/dependency_verification.html)
- [OWASP Top 10 - A08: Software and Data Integrity Failures](https://owasp.org/Top10/A08_2021-Software_and_Data_Integrity_Failures/)
- Project walkthrough: See `walkthrough.md` in artifacts directory for implementation details

---

**Last Updated:** 2026-01-11  
**Maintained By:** Development Team
