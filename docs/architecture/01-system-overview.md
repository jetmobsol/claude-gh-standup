# System Overview

This document provides a high-level overview of the claude-gh-standup system architecture.

## System Context

```mermaid
graph TB
    subgraph "External Systems"
        GH[GitHub API]
        Git[Git Repository]
        Claude[Claude AI CLI]
    end

    subgraph "claude-gh-standup System"
        CLI[Claude Code<br/>Slash Command<br/>/claude-gh-standup]
        Main[Main.java<br/>Orchestrator]
    end

    subgraph "CLI Dependencies"
        GHCLI[gh CLI<br/>GitHub CLI]
        GitCLI[git CLI]
        ClaudeCLI[claude CLI]
    end

    User([Developer]) -->|Invokes| CLI
    CLI -->|JBang Execute| Main

    Main -->|Calls| GHCLI
    Main -->|Calls| GitCLI
    Main -->|Calls| ClaudeCLI

    GHCLI -->|API Requests| GH
    GitCLI -->|Read Config| Git
    ClaudeCLI -->|Generate Report| Claude

    Claude -->|Standup Report| User

    style User fill:#e1f5ff,color:#000
    style CLI fill:#ffe1f5,color:#000
    style Main fill:#fff5e1,color:#000
    style Claude fill:#e1ffe1,color:#000
```

## Key Characteristics

### Zero API Key Management
- **No GitHub API Keys**: Uses authenticated `gh` CLI
- **No Anthropic API Keys**: Uses `claude -p` (prompt mode)
- **Seamless Integration**: Leverages existing CLI authentication

### Repository-Aware
- Auto-detects current git repository
- Filters activity to the active project context
- Falls back to all repositories if not in a git repo

### Single-File Executables
- Built with JBang for zero-setup deployment
- Each component is a standalone `.java` file with shebang
- Dependencies declared inline with `//DEPS` comments

### Streaming Output
- Uses `ProcessBuilder.inheritIO()` for direct output piping
- Claude responses stream directly to user's terminal
- No intermediate buffering or storage required

## Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Runtime** | JBang | Single-file Java script execution |
| **Language** | Java 11+ | Cross-platform compatibility |
| **CLI Integration** | ProcessBuilder | External tool invocation |
| **JSON Processing** | Gson 2.10.1 | GitHub API response parsing |
| **Git Operations** | git CLI | Repository detection |
| **GitHub Data** | gh CLI | Activity collection |
| **AI Generation** | claude CLI | Report generation |

## System Boundaries

### What the System Does
- Collects GitHub activity (commits, PRs, issues, reviews)
- Analyzes file changes from PR diffs
- Generates natural language standup reports
- Supports team aggregation
- Exports to multiple formats

### What the System Does NOT Do
- Does not store data persistently
- Does not require network APIs directly
- Does not manage authentication
- Does not modify GitHub data
- Does not cache results

## Integration Points

### Input
- Command-line arguments (days, user, repo, format, team, output)
- Git repository configuration (remote URL)
- GitHub CLI authentication state

### Output
- Formatted standup reports (Markdown/JSON/HTML)
- Console output (stdout/stderr)
- Optional file export

### External Dependencies
- GitHub CLI (`gh`) - must be authenticated
- Git CLI (`git`) - for repository detection
- Claude CLI (`claude`) - for AI generation
- JBang - for script execution
- Java 11+ - managed by JBang
