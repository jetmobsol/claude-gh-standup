---
description: Generate AI-powered GitHub standup reports via Claude AI (Java/JBang implementation)
argument-hint: [--yesterday|--last-week] [--config-add PATH] [--days N] [--user USERNAME] [--format markdown|json|html]
---

# claude-gh-standup

Generate professional standup reports from GitHub activity using Claude AI.

## Overview

Analyzes GitHub commits, pull requests, issues, and code reviews to generate AI-powered standup reports. Supports both single-repository mode and multi-directory tracking across multiple branches/repos.

**Key Features**:
- **Multi-directory support**: Track multiple branches/repos simultaneously with local WIP detection
- **Smart date shortcuts**: `--yesterday` (Friday-aware on Monday), `--last-week`
- **Local change detection**: Shows uncommitted and unpushed work alongside GitHub activity
- **Repository-aware**: Automatically detects current repository or uses configured directories
- **GitHub activity deduplication**: Fetches activity once per unique repository
- **Report auto-save**: Saves to `~/.claude-gh-standup/reports/` by default
- Collects GitHub activity using `gh` CLI (no API keys needed)
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

### Basic Usage (Legacy Mode)

```bash
# Yesterday's activity in current repository
/claude-gh-standup

# Last 3 days in current repository
/claude-gh-standup --days 3

# Specific user in current repository
/claude-gh-standup --user octocat --days 7
```

**Note**: The command automatically detects the current git repository and analyzes activity only in that repository. If you're not in a git repository, it will search across all repositories (with a warning).

### Date Shortcuts

```bash
# Yesterday (smart: Friday if Monday, otherwise previous day)
/claude-gh-standup --yesterday

# Last week
/claude-gh-standup --last-week

# Combine with other flags
/claude-gh-standup --yesterday --format json
```

**Monday-aware behavior**: Running `--yesterday` on Monday returns Friday's activity (3 days) to cover the weekend gap.

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

## Multi-Directory Mode

Track multiple branches/repositories simultaneously with local work-in-progress detection.

### Initial Setup

```bash
# 1. Initialize configuration (creates ~/.claude-gh-standup/config.json)
/claude-gh-standup --config-init

# 2. Add your first directory (auto-detects git info)
cd ~/projects/myapp
/claude-gh-standup --config-add .

# 3. Add more directories (different branches, same or different repos)
cd ~/projects/myapp-feature
/claude-gh-standup --config-add .

# 4. List configured directories
/claude-gh-standup --config-list
```

### Configuration Management

```bash
# Add directory with custom ID
/claude-gh-standup --config-add ~/projects/app --id myapp-main

# Remove directory by ID
/claude-gh-standup --config-remove myapp-main

# List all directories
/claude-gh-standup --config-list
```

### Multi-Directory Reports

Once directories are configured, reports automatically include:
- **GitHub activity**: Deduplicated across all unique repositories
- **Local changes**: Per-directory uncommitted and unpushed work
- **Context switching**: Highlights work across multiple branches/areas

```bash
# Generate multi-directory report for yesterday
/claude-gh-standup --yesterday

# Last week across all tracked directories
/claude-gh-standup --last-week

# Reports auto-save to ~/.claude-gh-standup/reports/ by default
```

### Local Change Detection

Multi-directory mode shows work-in-progress per directory:
- **Uncommitted changes**: Unstaged and staged files
- **Unpushed commits**: Commits not yet pushed to remote
- **Branch context**: Which directory/branch each change belongs to

Example output structure:
```markdown
## Work in Progress

### Main Branch (~/projects/myapp/main)
- 3 uncommitted files (2 staged, 1 unstaged)
- Database migration script, API endpoint changes

### Feature Branch (~/projects/myapp-feature/feature/new-ui)
- 2 unpushed commits
- UI component refactoring, style updates
```

### Migration from Legacy Mode

**Backward Compatibility**: Empty configuration = legacy mode (current directory only)

```bash
# Option 1: Initialize fresh config and add directories manually
/claude-gh-standup --config-init
cd ~/projects/myapp && /claude-gh-standup --config-add .

# Option 2: Keep using legacy mode (don't initialize config)
# Command works exactly as before when config.json is empty

# Option 3: Mix both modes
# - Use --repo flag to override multi-directory mode for one-off reports
/claude-gh-standup --repo owner/specific-repo --yesterday
```

**Config File Location**: `~/.claude-gh-standup/config.json`

**Example Configuration**:
```json
{
  "version": "1.0",
  "directories": [
    {
      "id": "myapp-main",
      "path": "~/projects/myapp",
      "branch": "main",
      "enabled": true,
      "remoteUrl": "git@github.com:owner/myapp.git",
      "repoName": "owner/myapp"
    },
    {
      "id": "myapp-feature",
      "path": "~/projects/myapp-feature",
      "branch": "feature/new-ui",
      "enabled": true,
      "remoteUrl": "git@github.com:owner/myapp.git",
      "repoName": "owner/myapp"
    }
  ],
  "reportSettings": {
    "defaultDays": 1,
    "autoSaveReports": true,
    "reportDirectory": "~/.claude-gh-standup/reports"
  }
}
```

## Invocation

```bash

jbang ~/.claude-gh-standup/scripts/Main.java --no-claude $ARGUMENTS
```

## Arguments Reference

### Activity & Date Flags

| Flag | Short | Type | Default | Description |
|------|-------|------|---------|-------------|
| `--days` | `-d` | Integer | `1` | Number of days to look back for activity |
| `--yesterday` | | Boolean | `false` | Shortcut for yesterday (3 days on Monday for Friday coverage) |
| `--last-week` | | Boolean | `false` | Shortcut for last 7 days |
| `--user` | `-u` | String | Authenticated user | GitHub username to analyze |
| `--repo` | `-r` | String | Config dirs / current repo | Filter to specific repository (owner/repo format) |
| `--team` | | String[] | None | Space-separated list of team members for aggregated report |

### Configuration Management Flags

| Flag | Short | Type | Default | Description |
|------|-------|------|---------|-------------|
| `--config-init` | | Boolean | `false` | Initialize new config file at ~/.claude-gh-standup/config.json |
| `--config-add` | | String | None | Add directory to tracking (use `.` for current directory) |
| `--config-remove` | | String | None | Remove directory by ID from tracking |
| `--config-list` | | Boolean | `false` | List all configured directories |
| `--id` | | String | Auto-generated | Custom ID when adding directory (use with --config-add) |

### Output & Format Flags

| Flag | Short | Type | Default | Description |
|------|-------|------|---------|-------------|
| `--format` | `-f` | String | `markdown` | Output format: `markdown`, `json`, or `html` |
| `--output` | `-o` | String | Auto-save or stdout | File path to save report (overrides auto-save) |
| `--no-claude` | | Boolean | `false` | Skip claude -p call and output raw prompt (auto-enabled in Claude Code) |

### Flag Interactions

- `--yesterday` / `--last-week` override `--days`
- `--repo` disables multi-directory mode (legacy mode for specific repo)
- `--id` requires `--config-add` to be specified
- Empty directories array = legacy mode (current directory only)

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
