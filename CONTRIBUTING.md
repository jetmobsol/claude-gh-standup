# Contributing to claude-gh-standup

Thank you for your interest in contributing to claude-gh-standup!

## Development Setup

1. **Clone the repository**:
   ```bash
   git clone <repo-url>
   cd claude-gh-standup
   ```

2. **Install prerequisites**:
   - GitHub CLI: `brew install gh` (macOS) or see https://github.com/cli/cli#installation
   - JBang: `curl -Ls https://sh.jbang.dev | bash -s - app setup`
   - Claude Code CLI

3. **Authenticate with GitHub**:
   ```bash
   gh auth login
   ```

## Testing

### Test Individual Scripts

Each Java script can be tested independently:

```bash
# Test activity collection
jbang scripts/CollectActivity.java <your-username> 3

# Test with specific repository
jbang scripts/CollectActivity.java <your-username> 7 owner/repo
```

### Test Full Workflow

```bash
# Install to project-level commands
mkdir -p .claude/commands
ln -s $(pwd) .claude/commands/claude-gh-standup

# Run via Claude Code
/claude-gh-standup --days 3
```

## Code Style

### Java Conventions
- **File naming**: PascalCase (e.g., `CollectActivity.java`)
- **Method naming**: camelCase (e.g., `getUserCommits()`)
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Soft limit of 120 characters

### JBang Requirements
All `.java` files must start with:
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
```

Dependencies via comments:
```java
//DEPS com.google.code.gson:gson:2.10.1
```

## Git Workflow

### Commit Messages

Follow Conventional Commits format:

```
type(scope): description

feat(activity-collection): add PR collection via gh search prs
fix(diff-analysis): handle empty diff gracefully
docs(readme): add team aggregation examples
```

**Types**: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`
**Scopes**: Match capability names (activity-collection, diff-analysis, etc.)

### Pull Requests

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make your changes
3. Test thoroughly
4. Commit with conventional commit messages
5. Push and create PR
6. Reference any related issues

## Adding New Features

### New Capability
1. Create spec in `openspec/specs/<capability>/spec.md`
2. Create corresponding Java script in `scripts/`
3. Update `Main.java` to orchestrate new capability
4. Add tests and documentation
5. Update README.md and command definition

### New Export Format
1. Add export method in `ExportUtils.java`
2. Update format validation
3. Add example output in `examples/`
4. Document in README.md

## OpenSpec Workflow

This project uses OpenSpec for specification-driven development:

1. **Create Proposal**: `openspec create <change-id>`
2. **Write Specs**: Define requirements and scenarios
3. **Validate**: `openspec validate <change-id> --strict`
4. **Implement**: Follow `tasks.md` checklist
5. **Archive**: `openspec archive <change-id>` after deployment

## Questions?

- Open an issue for bugs or feature requests
- Start a discussion for questions or ideas

## License

By contributing, you agree that your contributions will be licensed under the MIT License with attribution to the original gh-standup project.
