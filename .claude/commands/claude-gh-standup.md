---
description: Generate AI-powered GitHub standup reports via Claude AI (Java/JBang implementation)
argument-hint: [--days N] [--user USERNAME] [--format markdown|json|html] [--team USER1 USER2...] [--output FILE]
---

# claude-gh-standup

Generate professional standup reports from GitHub activity using Claude AI.

## Overview

Analyzes GitHub commits, pull requests, issues, and code reviews to generate AI-powered standup reports **for the current repository**. Includes file change analysis for better context and supports team aggregation.

**Key Features**:
- **Repository-aware**: Automatically detects and analyzes the current git repository
- Collects GitHub activity using `gh` CLI (no API keys needed)
- Analyzes file diffs for commits and PRs
- Generates natural language standup reports via `claude -p`
- Supports team aggregation across multiple developers
- Exports to Markdown, JSON, or HTML formats

## Prerequisites

Before using this command, ensure you have:

1. **GitHub CLI** (`gh`) installed and authenticated:
   ```bash
   gh auth login
   gh auth status
   ```

2. **JBang** installed:
   ```bash
   curl -Ls https://sh.jbang.dev | bash -s - app setup
   # OR via package manager (brew, sdkman, etc.)
   ```

3. **Claude Code CLI** (`claude`) available in PATH

4. **Java 11+** (automatically managed by JBang if not present)

## Usage Examples

### Basic Usage

```bash
# Yesterday's activity in current repository
/claude-gh-standup

# Last 3 days in current repository
/claude-gh-standup --days 3

# Specific user in current repository
/claude-gh-standup --user octocat --days 7
```

**Note**: The command automatically detects the current git repository and analyzes activity only in that repository. If you're not in a git repository, it will search across all repositories (with a warning).

### Team Reports

```bash
# Team standup for multiple developers
/claude-gh-standup --team alice bob charlie --days 7
```

### Repository Filtering

```bash
# Activity in specific repository
/claude-gh-standup --repo owner/repo-name --days 3
```

## Invocation

```bash

jbang $COMMAND_DIR/scripts/Main.java --no-claude $ARGUMENTS
```

## Arguments Reference

| Flag | Short | Type | Default | Description |
|------|-------|------|---------|-------------|
| `--days` | `-d` | Integer | `1` | Number of days to look back for activity |
| `--user` | `-u` | String | Authenticated user | GitHub username to analyze |
| `--repo` | `-r` | String | All repos | Filter activity to specific repository (owner/repo) |
| `--format` | `-f` | String | `markdown` | Output format: `markdown`, `json`, or `html` |
| `--team` | | String[] | None | Space-separated list of team members for aggregated report |
| `--output` | `-o` | String | stdout | File path to save report (otherwise prints to console) |
| `--no-claude` | | Boolean | `false` | Skip claude -p call and output prompt directly (auto-enabled in Claude Code) |

## Troubleshooting

### Command Not Found
If `/claude-gh-standup` is not recognized:
- Ensure repository is cloned to correct path:
  - User-level: `~/.claude/commands/claude-gh-standup/`
  - Project-level: `.claude/commands/claude-gh-standup/`
- Restart Claude Code to reload commands

### GitHub Authentication Errors
```bash
gh auth login
gh auth refresh -h github.com -s read:org
```

### JBang Not Found
```bash
# Install JBang
curl -Ls https://sh.jbang.dev | bash -s - app setup

# Verify installation
jbang version
```

### No Activity Found
- Check date range with `--days` flag
- Verify user has public activity in period
- Try different `--user` if checking others

## Credits

Inspired by and adapted from [gh-standup](https://github.com/sgoedecke/gh-standup) by Sean Goedecke.

Original gh-standup is a Go-based GitHub CLI extension using GitHub Models API. This reimplementation uses Java/JBang and integrates with Claude Code via `claude -p`.
