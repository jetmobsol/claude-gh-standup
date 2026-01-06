# Change: Refactor to Domain Driven Design with Ports & Adapters

## Why

The current codebase consists of monolithic JBang scripts with tightly coupled ProcessBuilder calls to external CLIs. This makes unit testing impossible without invoking actual `gh`, `git`, and `claude` commands. We need testable code with clear separation of concerns to enable proper TDD and maintainability.

## What Changes

- **BREAKING**: Internal code structure changes (public API/CLI unchanged)
- Extract domain entities from existing scripts into bounded contexts
- Introduce port interfaces for all external dependencies (GitHub, Git, Claude CLIs)
- Create infrastructure adapters implementing ports via ProcessBuilder
- Add service layer for orchestration with dependency injection
- Enable unit testing with mock ports (JUnit 5 + Mockito pattern)

## Impact

- **Affected specs**: slash-command-interface (internal architecture)
- **Affected code**: All scripts in `scripts/` directory
- **New capabilities**: domain-model, ports-adapters, testing-architecture
- **Backward compatibility**: 100% - CLI interface unchanged
- **File count**: ~27 new files (domain, ports, infrastructure, services, tests)

## Non-Goals

- Changing the CLI interface or flags
- Switching from JBang to Maven/Gradle
- Adding new user-facing features
- Changing prompt templates
