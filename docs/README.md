# Documentation

> Central hub for all claude-gh-standup documentation

## Overview

This directory contains comprehensive documentation for the claude-gh-standup project, an AI-powered GitHub standup report generator built with JBang and integrated with Claude Code.

## Documentation Sections

### 📐 [Architecture Documentation](./architecture/)

**Comprehensive architectural diagrams and documentation**

Dive deep into the system design with detailed Mermaid diagrams covering:
- System overview and context
- Component architecture
- Data flow and transformations
- Sequence diagrams (temporal interactions)
- Integration patterns (GitHub CLI, Git CLI, Claude CLI)

**Start here** if you want to understand how the system works internally.

**Key files**:
- [Architecture README](./architecture/README.md) - Overview and navigation
- [System Overview](./architecture/01-system-overview.md) - High-level context
- [Component Architecture](./architecture/02-component-architecture.md) - Internal structure
- [Data Flow](./architecture/03-data-flow.md) - Data transformation pipeline
- [Sequence Diagrams](./architecture/04-sequence-diagrams.md) - Temporal interactions
- [Integration Patterns](./architecture/05-integration-patterns.md) - External integrations

## Quick Links

### For Users
- [Main README](../README.md) - Getting started, installation, usage
- [Slash Command Definition](../claude-gh-standup.md) - Command documentation
- [Example Output](../examples/sample_output.md) - Sample standup reports

### For Contributors
- [CONTRIBUTING.md](../CONTRIBUTING.md) - Contribution guidelines
- [Architecture Documentation](./architecture/) - System design and patterns
- [OpenSpec](../openspec/) - Change proposals and specifications

### For Maintainers
- [Architecture Documentation](./architecture/) - Complete system design
- [OpenSpec Agents Guide](../openspec/AGENTS.md) - AI-assisted development workflow

## Documentation Philosophy

This project follows a **diagram-first documentation approach**:

1. **Visual First**: Complex systems are best understood through diagrams
2. **Mermaid Everywhere**: All diagrams use Mermaid for version control and easy updates
3. **Progressive Disclosure**: Start high-level, drill down as needed
4. **Context-Rich**: Diagrams include explanations and rationale
5. **Living Documentation**: Updated alongside code changes

## Viewing Mermaid Diagrams

### In GitHub
GitHub renders Mermaid diagrams automatically in `.md` files.

### In VS Code
Install the [Markdown Preview Mermaid Support](https://marketplace.visualstudio.com/items?itemName=bierner.markdown-mermaid) extension.

### In Claude Code
Claude Code renders Mermaid diagrams in the markdown preview.

### Online
Use [Mermaid Live Editor](https://mermaid.live/) to view and edit diagrams.

## Documentation Coverage

### Current Coverage

| Area | Status | Location |
|------|--------|----------|
| **User Guide** | ✅ Complete | [README.md](../README.md) |
| **Architecture** | ✅ Complete | [architecture/](./architecture/) |
| **API/CLI Reference** | ✅ Complete | [claude-gh-standup.md](../claude-gh-standup.md) |
| **Examples** | ⚠️ Basic | [examples/](../examples/) |
| **Contributing** | ✅ Complete | [CONTRIBUTING.md](../CONTRIBUTING.md) |

### Future Documentation

| Area | Priority | Notes |
|------|----------|-------|
| **Troubleshooting Guide** | Medium | Common issues and solutions |
| **Performance Tuning** | Low | Optimization tips |
| **Advanced Usage** | Medium | Power user scenarios |
| **API Integration Examples** | Low | Using as a library |

## How to Use This Documentation

### I want to...

**...use claude-gh-standup as a user**
- Start: [Main README](../README.md)
- Reference: [Slash Command Definition](../claude-gh-standup.md)

**...understand how it works**
- Start: [Architecture README](./architecture/README.md)
- Deep dive: [System Overview](./architecture/01-system-overview.md)

**...contribute code**
- Start: [CONTRIBUTING.md](../CONTRIBUTING.md)
- Architecture: [Component Architecture](./architecture/02-component-architecture.md)
- Workflow: [OpenSpec Agents](../openspec/AGENTS.md)

**...modify the architecture**
- Start: [Architecture README](./architecture/README.md)
- Understand: [Data Flow](./architecture/03-data-flow.md)
- Extend: [Integration Patterns](./architecture/05-integration-patterns.md)

**...debug an issue**
- Flow: [Sequence Diagrams](./architecture/04-sequence-diagrams.md)
- Integration: [Integration Patterns](./architecture/05-integration-patterns.md)
- Errors: [Component Architecture - Error Handling](./architecture/02-component-architecture.md#error-handling-strategy)

## Contributing to Documentation

### Adding Documentation

1. **Diagrams**: Use Mermaid syntax in markdown files
2. **Structure**: Follow existing organization (overview → details)
3. **Examples**: Include concrete examples with code snippets
4. **Context**: Explain the "why", not just the "what"

### Updating Diagrams

When code changes affect architecture:

1. Update relevant diagrams in `architecture/` directory
2. Verify Mermaid syntax renders correctly
3. Update related text explanations
4. Update the Architecture README if adding new concepts

### Documentation Standards

- **Clear headings**: Use hierarchical markdown headers
- **Code blocks**: Always specify language for syntax highlighting
- **Links**: Use relative links for internal docs
- **Diagrams**: Keep Mermaid diagrams focused and readable
- **Examples**: Include realistic, tested examples

## Documentation Maintenance

### Review Schedule

- **Architecture docs**: Review on major changes
- **User docs**: Review on feature releases
- **Examples**: Update with each release
- **API/CLI reference**: Update immediately on changes

### Staleness Indicators

If you notice:
- Diagrams don't match current code structure
- Examples produce errors
- Links are broken
- Instructions don't work

Please open an issue or submit a pull request to fix.

## Resources

### Mermaid Documentation
- [Mermaid Official Docs](https://mermaid.js.org/)
- [Mermaid Cheat Sheet](https://jojozhuang.github.io/tutorial/mermaid-cheat-sheet/)
- [Mermaid Live Editor](https://mermaid.live/)

### Related Projects
- [gh-standup](https://github.com/sgoedecke/gh-standup) - Original inspiration (Go-based)
- [Claude Code Docs](https://docs.anthropic.com/claude/docs/claude-code) - Official Claude Code documentation

## Feedback

Found an issue with the documentation? Have suggestions for improvement?

- Open an issue: [GitHub Issues](https://github.com/jetmobsol/claude-gh-standup/issues)
- Contribute: [CONTRIBUTING.md](../CONTRIBUTING.md)

---

**Last Updated**: 2026-01-03
**Documentation Version**: 1.0.0
