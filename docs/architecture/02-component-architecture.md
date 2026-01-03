# Component Architecture

This document describes the internal component structure of claude-gh-standup.

## Component Diagram

```mermaid
graph TB
    subgraph "Entry Point"
        CMD[claude-gh-standup.md<br/>Slash Command Definition]
    end

    subgraph "Core Components"
        Main[Main.java<br/>Entry Point & Orchestrator]
        Collect[CollectActivity.java<br/>GitHub Activity Collector]
        Analyze[AnalyzeDiffs.java<br/>Diff Analyzer]
        Generate[GenerateReport.java<br/>Report Generator]
        Export[ExportUtils.java<br/>Format Exporter]
        Team[TeamAggregator.java<br/>Team Report Consolidator]
    end

    subgraph "Resources"
        Prompt1[prompts/standup.prompt.md<br/>Individual Template]
        Prompt2[prompts/team.prompt.md<br/>Team Template]
    end

    subgraph "External CLIs"
        GH[gh CLI]
        Git[git CLI]
        Claude[claude CLI]
    end

    CMD -->|JBang Invokes| Main

    Main -->|Argument Parsing| Main
    Main -->|Auto-detect Repo| Git
    Main -->|Auto-detect User| GH
    Main -->|JBang Subprocess| Collect
    Main -->|JBang Subprocess| Analyze
    Main -->|Load Template| Prompt1
    Main -->|Stream Output| Claude

    Main -.->|Team Mode| Team
    Team -->|Load Template| Prompt2

    Collect -->|Search Commits| GH
    Collect -->|Search PRs| GH
    Collect -->|Search Issues| GH
    Collect -->|Detect Repo| Git

    Analyze -->|Get PR Diffs| GH
    Analyze -->|Parse Diff Stats| Analyze

    Generate -->|Load Template| Prompt1
    Generate -->|Format Data| Generate
    Generate -->|Invoke AI| Claude

    Export -.->|Future: Format| Export

    style CMD fill:#e1f5ff
    style Main fill:#fff5e1
    style Collect fill:#ffe1f5
    style Analyze fill:#f5e1ff
    style Generate fill:#e1ffe1
    style Team fill:#ffe1e1
    style Prompt1 fill:#f0f0f0
    style Prompt2 fill:#f0f0f0
```

## Component Descriptions

### Main.java
**Purpose**: Entry point and workflow orchestrator

**Responsibilities**:
- Parse command-line arguments (`--days`, `--user`, `--repo`, `--format`, `--team`, `--output`)
- Auto-detect current git repository
- Auto-detect authenticated GitHub user
- Orchestrate workflow: collect → analyze → generate
- Handle single-user vs. team modes
- Direct invocation of Claude CLI (using `inheritIO()` pattern)

**Key Methods**:
- `parseArgs()` - Command-line argument parser
- `getCurrentRepository()` - Git repository detection
- `getCurrentUser()` - GitHub user detection via `gh api user`
- `formatActivities()` - Convert JSON to readable format
- `runScript()` - JBang subprocess executor

**Dependencies**:
- Gson for JSON parsing
- ProcessBuilder for subprocess execution
- Java NIO for file I/O

### CollectActivity.java
**Purpose**: GitHub activity data collection

**Responsibilities**:
- Search for user commits via `gh search commits`
- Search for user PRs via `gh search prs`
- Search for user issues via `gh search issues`
- Apply date range filters (last N days)
- Apply repository filters (optional)
- Auto-detect repository context
- Consolidate results into JSON structure

**Output Format**:
```json
{
  "username": "octocat",
  "days": 7,
  "repository": "owner/repo",
  "commits": [...],
  "pull_requests": [...],
  "issues": [...]
}
```

**Key Methods**:
- `getUserCommits()` - Fetch commits via gh CLI
- `getUserPRs()` - Fetch pull requests
- `getUserIssues()` - Fetch issues
- `collectAllActivity()` - Aggregate all activity types
- `getCurrentRepository()` - Detect git repo

### AnalyzeDiffs.java
**Purpose**: File change analysis for PRs

**Responsibilities**:
- Fetch PR diffs via `gh pr diff`
- Parse unified diff format
- Calculate file-level statistics (additions, deletions)
- Aggregate changes across all PRs
- Format summary for AI context

**Output Format**:
```
Files changed: 12
Lines added: 245
Lines deleted: 89

Modified files:
- src/Main.java (+45, -12)
- src/CollectActivity.java (+89, -23)
- README.md (+111, -54)
```

**Key Methods**:
- `analyzePRDiff()` - Fetch diff for single PR
- `parseDiff()` - Parse unified diff format
- `analyzePRDiffs()` - Aggregate multiple PR diffs
- `formatDiffSummary()` - Create readable summary

**Data Structures**:
- `FileStat` - Per-file change statistics
- `DiffSummary` - Aggregated change summary

### GenerateReport.java
**Purpose**: AI-powered report generation

**Responsibilities**:
- Load prompt template from file
- Format activity JSON into readable text
- Inject data into template placeholders
- Invoke Claude CLI with full prompt
- Stream output directly to stdout

**Template Variables**:
- `{{activities}}` - Formatted commit/PR/issue list
- `{{diffs}}` - File change summary

**Key Methods**:
- `loadPromptTemplate()` - Read template file
- `formatActivities()` - Convert JSON to text
- `generateStandupReport()` - Full workflow
- ProcessBuilder with `inheritIO()` for streaming

**Notable Pattern**:
```java
ProcessBuilder pb = new ProcessBuilder("claude", "-p", fullPrompt);
pb.inheritIO();  // Stream directly to stdout
Process process = pb.start();
```

### ExportUtils.java
**Purpose**: Format conversion (Markdown/JSON/HTML)

**Status**: Placeholder for future export functionality

**Planned Responsibilities**:
- Convert Markdown to JSON
- Convert Markdown to HTML
- Write to output files
- Handle different format specifications

### TeamAggregator.java
**Purpose**: Multi-user report consolidation

**Status**: Placeholder for team mode functionality

**Planned Responsibilities**:
- Collect individual reports for team members
- Consolidate into team standup format
- Use team-specific prompt template
- Generate aggregated insights

## Prompt Templates

### standup.prompt.md
Individual developer standup report template

**Structure**:
- System instructions for AI
- Output format guidelines (Yesterday/Today/Blockers)
- Context about file changes
- Template placeholders for data injection

### team.prompt.md
Team aggregation template (future)

**Purpose**: Generate team-level insights from individual reports

## Design Patterns

### JBang Single-File Executables
Each component is a standalone executable:
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.google.code.gson:gson:2.10.1
```

### ProcessBuilder Pattern
Consistent external tool invocation:
```java
ProcessBuilder pb = new ProcessBuilder("gh", "search", "commits", ...);
Process process = pb.start();
// Read output, check exit code
```

### inheritIO Streaming Pattern
Direct output piping (tac-1 pattern):
```java
ProcessBuilder claudeBuilder = new ProcessBuilder("claude", "-p", fullPrompt);
claudeBuilder.inheritIO();  // No buffering
Process claudeProcess = claudeBuilder.start();
```

### Template-Based Prompt Engineering
Separation of concerns:
- Logic in Java
- AI instructions in Markdown templates
- Data injection via simple string replacement

## Component Dependencies

```mermaid
graph LR
    Main -->|Uses| Collect
    Main -->|Uses| Analyze
    Main -->|Direct Call| Claude[claude CLI]
    Main -.->|Future| Team
    Main -.->|Future| Export

    Collect -->|Uses| GH[gh CLI]
    Collect -->|Uses| Git[git CLI]

    Analyze -->|Uses| GH

    Generate -->|Uses| Claude

    Team -.->|Future| Generate

    style Main fill:#fff5e1
    style Collect fill:#ffe1f5
    style Analyze fill:#f5e1ff
    style Generate fill:#e1ffe1
```

## Error Handling Strategy

### Graceful Degradation
- Commit search fails → Continue with PRs and issues
- PR diff unavailable → Skip that PR
- Repository not detected → Search across all repos

### User-Friendly Warnings
```
Warning: Commit search failed (this is common due to GitHub restrictions)
Warning: Not in a git repository or no GitHub remote found.
```

### Exit Codes
- `0` - Success
- `1` - Invalid arguments or execution failure
- Propagates subprocess exit codes

## Data Flow Summary

1. **Main.java** receives arguments and detects context
2. **CollectActivity.java** gathers GitHub data → JSON
3. **AnalyzeDiffs.java** analyzes PR diffs → Summary text
4. **Main.java** loads template, injects data
5. **claude CLI** generates report → stdout
6. User sees formatted standup report
