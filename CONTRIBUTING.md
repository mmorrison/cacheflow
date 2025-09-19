# Contributing to CacheFlow

Thank you for your interest in contributing to CacheFlow! This document provides guidelines and information for contributors.

## 🚀 Getting Started

### Prerequisites

- JDK 17 or higher
- Gradle 7.0 or higher
- Git

### Development Setup

1. Fork the repository
2. Clone your fork: `git clone https://github.com/mmorrison/cacheflow-spring-boot-starter.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Run tests: `./gradlew test`
6. Commit your changes: `git commit -m "Add your feature"`
7. Push to your fork: `git push origin feature/your-feature-name`
8. Create a Pull Request

## 📝 Code Style

### Kotlin

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use `ktlint` for code formatting
- Write meaningful variable and function names
- Add KDoc comments for public APIs

### Testing

- Write unit tests for new features
- Maintain test coverage above 80%
- Use descriptive test names
- Follow AAA pattern (Arrange, Act, Assert)

### Documentation

- Update README.md for user-facing changes
- Add/update API documentation
- Include examples for new features

## 🐛 Bug Reports

When reporting bugs, please include:

- CacheFlow version
- Java/Kotlin version
- Spring Boot version
- Steps to reproduce
- Expected vs actual behavior
- Logs and stack traces

## ✨ Feature Requests

Before submitting feature requests:

1. Check existing issues and discussions
2. Describe the use case and benefits
3. Consider backward compatibility
4. Provide implementation ideas if possible

## 🔄 Pull Request Process

1. **Small, focused changes** - One feature/fix per PR
2. **Clear description** - Explain what and why
3. **Tests included** - New features need tests
4. **Documentation updated** - Update relevant docs
5. **Backward compatible** - Avoid breaking changes
6. **CI passes** - All checks must pass

### PR Template

```markdown
## Description

Brief description of changes

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Checklist

- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No breaking changes (or clearly documented)
```

## 🏷️ Release Process

Releases follow [Semantic Versioning](https://semver.org/):

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

## 📞 Getting Help

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Email**: [your-email@example.com]

## 📋 Development Guidelines

### Branch Naming

- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation updates
- `refactor/description` - Code refactoring

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add edge cache purging
fix: resolve Redis connection timeout
docs: update installation guide
refactor: simplify cache key generation
```

### Code Review

- Be constructive and respectful
- Focus on code quality and maintainability
- Ask questions if something is unclear
- Suggest improvements, don't just criticize

## 🎯 Areas for Contribution

- **Performance**: Optimize cache operations
- **Testing**: Improve test coverage
- **Documentation**: Examples and guides
- **Integrations**: New edge cache providers
- **Monitoring**: Enhanced metrics and observability

Thank you for contributing to CacheFlow! 🎉
