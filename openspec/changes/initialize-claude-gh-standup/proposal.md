# Change: Initialize claude-gh-standup - Claude Code Slash Command for AI-Powered GitHub Standup Reports

## Why

Teams and individual developers need efficient ways to generate standup reports from GitHub activity without manually reviewing commits, PRs, issues, and reviews. Existing tools like gh-standup (Go-based) rely on GitHub Models LLM API, requiring API keys and external dependencies. This project creates a Claude Code slash command that:

- Uses `gh` CLI for GitHub data (no GitHub API keys needed)
- Uses `claude -p` for AI generation (no Anthropic API keys needed)
- Analyzes file diffs for better context than commit messages alone
- Supports team aggregation for multi-developer standups
- Provides multiple export formats (Markdown, JSON, HTML)

This fills a gap in the Claude Code ecosystem by providing a production-ready standup generation tool using the Java/JBang pattern for single-file executable scripts.

## What Changes

This is a **new project initialization** creating a Claude Code slash command from scratch:

- **NEW**: GitHub Activity Collection capability (commits, PRs, issues, reviews via `gh` CLI)
- **NEW**: Diff Analysis capability (file changes, LOC stats via `gh pr diff` and `git diff`)
- **NEW**: Report Generation capability (AI-powered standup via `claude -p`)
- **NEW**: Export Formats capability (Markdown, JSON, HTML formatters)
- **NEW**: Team Aggregation capability (multi-user report consolidation)
- **NEW**: Slash Command Interface capability (command definition and argument parsing)

**Implementation Stack**:
- Java 11+ with JBang (single-file executable scripts, no compilation)
- ProcessBuilder pattern for CLI integration (`gh`, `claude`)
- Gson for JSON parsing
- Markdown prompt templates with variable injection

**Reference Projects**:
- Pattern source: `/Users/garden/projects/tac/tac-1` (JBang + `claude -p` integration)
- Prompt source: `/Users/garden/projects/ai/sgoedecke/gh-standup` (Go-based GitHub CLI extension)

## Impact

**Affected specs**: ALL NEW (no existing specs to modify)
- `github-activity-collection` - NEW capability
- `diff-analysis` - NEW capability
- `report-generation` - NEW capability
- `export-formats` - NEW capability
- `team-aggregation` - NEW capability
- `slash-command-interface` - NEW capability

**Affected code**: ALL NEW (greenfield implementation)
- `claude-gh-standup.md` - Slash command definition
- `scripts/Main.java` - Entry point with argument parsing
- `scripts/CollectActivity.java` - GitHub activity collection
- `scripts/AnalyzeDiffs.java` - File diff analysis
- `scripts/GenerateReport.java` - Claude AI integration
- `scripts/ExportUtils.java` - Format conversion
- `scripts/TeamAggregator.java` - Multi-user consolidation
- `prompts/*.md` - Prompt templates for Claude

**User-facing changes**:
- NEW command: `/claude-gh-standup` available in Claude Code CLI
- Installation: Users clone repo to `~/.claude/commands/claude-gh-standup/` or `.claude/commands/claude-gh-standup/`

**Dependencies**:
- `gh` CLI (GitHub authentication)
- `claude` CLI (Claude Code)
- JBang (auto-installs Java 11+ if needed)
- Gson library (via JBang `//DEPS`)

**Breaking changes**: None (new project)

**Migration path**: N/A (new installation)
