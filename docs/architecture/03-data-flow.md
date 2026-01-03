# Data Flow Architecture

This document describes how data flows through the claude-gh-standup system from user input to final output.

## End-to-End Data Flow

```mermaid
flowchart TB
    Start([User invokes<br/>/claude-gh-standup]) --> Parse[Parse Arguments]

    Parse --> Detect{Auto-detect<br/>Context}

    Detect -->|git remote| GetRepo[Get Repository<br/>owner/repo]
    Detect -->|gh api user| GetUser[Get GitHub<br/>Username]

    GetRepo --> Mode{Execution<br/>Mode?}
    GetUser --> Mode

    Mode -->|Single User| SingleFlow[Single User Workflow]
    Mode -->|Team| TeamFlow[Team Workflow]

    SingleFlow --> Collect[Collect GitHub Activity]

    Collect -->|JSON| Activity{Activity<br/>Data}

    Activity -->|commits| CommitData[Commits Array]
    Activity -->|pull_requests| PRData[PRs Array]
    Activity -->|issues| IssueData[Issues Array]

    PRData --> AnalyzeDiffs[Analyze PR Diffs]
    CommitData --> Format
    IssueData --> Format

    AnalyzeDiffs -->|Stats| DiffSummary[Diff Summary<br/>Text]

    Format[Format Activities] -->|Text| FormattedAct[Formatted<br/>Activities]

    FormattedAct --> LoadTemplate[Load Prompt<br/>Template]
    DiffSummary --> LoadTemplate

    LoadTemplate --> Inject[Inject Data into<br/>Template]

    Inject -->|Full Prompt| Claude[Claude CLI<br/>claude -p]

    Claude -->|Stream| Output[Standup Report<br/>Markdown]

    Output --> Display([Display to User])

    TeamFlow -.->|Loop for each member| Collect
    TeamFlow -.->|Future| TeamAgg[Team Aggregator]
    TeamAgg -.-> Display

    style Start fill:#e1f5ff,color:#000
    style Display fill:#e1ffe1,color:#000
    style Claude fill:#ffe1e1,color:#000
    style Activity fill:#fff5e1,color:#000
```

## Data Transformation Pipeline

### Stage 1: Input Processing

```mermaid
graph LR
    subgraph "User Input"
        Args[Command Arguments<br/>--days 7<br/>--user octocat<br/>--repo owner/repo]
    end

    subgraph "Context Detection"
        GitRemote[git remote get-url origin<br/>→ https://github.com/owner/repo.git]
        GhUser[gh api user --jq .login<br/>→ octocat]
    end

    subgraph "Processed Context"
        Context[Execution Context<br/>user: octocat<br/>repo: owner/repo<br/>days: 7]
    end

    Args --> Context
    GitRemote --> Context
    GhUser --> Context

    style Context fill:#fff5e1,color:#000
```

### Stage 2: Activity Collection

```mermaid
graph TB
    subgraph "GitHub CLI Queries"
        Q1[gh search commits<br/>--author=octocat<br/>--committer-date=&gt;2025-12-27]
        Q2[gh search prs<br/>--author=octocat<br/>--created=&gt;2025-12-27<br/>-R owner/repo]
        Q3[gh search issues<br/>--author=octocat<br/>--created=&gt;2025-12-27<br/>-R owner/repo]
    end

    subgraph "Raw JSON Responses"
        R1[Commits JSON Array]
        R2[PRs JSON Array]
        R3[Issues JSON Array]
    end

    subgraph "Consolidated Activity JSON"
        Activity["{\n  username: 'octocat',\n  days: 7,\n  repository: 'owner/repo',\n  commits: [...],\n  pull_requests: [...],\n  issues: [...]\n}"]
    end

    Q1 --> R1
    Q2 --> R2
    Q3 --> R3

    R1 --> Activity
    R2 --> Activity
    R3 --> Activity

    style Activity fill:#e1ffe1,color:#000
```

### Stage 3: Diff Analysis

```mermaid
graph TB
    subgraph "Input"
        PRs[Pull Requests Array<br/>#123, #456, #789]
    end

    subgraph "Per-PR Processing"
        Loop[For each PR]
        GetDiff[gh pr diff 123<br/>-R owner/repo]
        Parse[Parse Unified Diff]
        Stats[Calculate Stats<br/>+additions<br/>-deletions<br/>files changed]
    end

    subgraph "Aggregation"
        Aggregate[Sum all PR stats]
        Format[Format Summary Text]
    end

    subgraph "Output"
        Summary["Files changed: 12\nLines added: 245\nLines deleted: 89\n\nModified files:\n- src/Main.java (+45, -12)\n- README.md (+111, -54)"]
    end

    PRs --> Loop
    Loop --> GetDiff
    GetDiff --> Parse
    Parse --> Stats
    Stats --> Aggregate
    Aggregate --> Format
    Format --> Summary

    style Summary fill:#f5e1ff,color:#000
```

### Stage 4: Report Generation

```mermaid
graph TB
    subgraph "Inputs"
        Activity[Formatted Activities<br/>Text]
        Diffs[Diff Summary<br/>Text]
        Template[Prompt Template<br/>prompts/standup.prompt.md]
    end

    subgraph "Template Processing"
        Load[Load Template File]
        Replace1[Replace '{{activities}}'<br/>with formatted activities]
        Replace2[Replace '{{diffs}}'<br/>with diff summary]
        FullPrompt[Complete Prompt]
    end

    subgraph "AI Generation"
        Claude[claude -p &lt;prompt&gt;]
        Stream[Stream Output<br/>via inheritIO]
    end

    subgraph "Output"
        Report[Standup Report<br/>Markdown]
    end

    Template --> Load
    Load --> Replace1
    Activity --> Replace1
    Replace1 --> Replace2
    Diffs --> Replace2
    Replace2 --> FullPrompt
    FullPrompt --> Claude
    Claude --> Stream
    Stream --> Report

    style Report fill:#e1ffe1,color:#000
```

## Data Formats

### Activity JSON Structure

```json
{
  "username": "octocat",
  "days": 7,
  "repository": "owner/repo",
  "commits": [
    {
      "sha": "abc123...",
      "commit": {
        "message": "Add feature X",
        "author": {...},
        "committer": {...}
      },
      "repository": {
        "fullName": "owner/repo"
      }
    }
  ],
  "pull_requests": [
    {
      "number": 123,
      "title": "Add feature Y",
      "state": "OPEN",
      "url": "https://github.com/owner/repo/pull/123",
      "repository": {
        "fullName": "owner/repo"
      }
    }
  ],
  "issues": [
    {
      "number": 456,
      "title": "Fix bug Z",
      "state": "CLOSED",
      "url": "https://github.com/owner/repo/issues/456",
      "repository": {
        "fullName": "owner/repo"
      }
    }
  ]
}
```

### Formatted Activities Text

```
COMMITS:
- [owner/repo] Add feature X (abc123)
- [owner/repo] Update documentation (def456)

PULL REQUESTS:
- [owner/repo] #123: Add feature Y (OPEN)
- [owner/repo] #124: Fix bug in auth (MERGED)

ISSUES:
- [owner/repo] #456: Fix bug Z (CLOSED)
```

### Diff Summary Text

```
Files changed: 12
Lines added: 245
Lines deleted: 89

Modified files:
- src/Main.java (+45, -12)
- src/CollectActivity.java (+89, -23)
- README.md (+111, -54)
- src/AnalyzeDiffs.java (+0, -0)
```

### Final Prompt Structure

```markdown
# Standup Report Generator

You are an AI assistant helping to generate professional standup reports...

## Activity Data

COMMITS:
- [owner/repo] Add feature X (abc123)

PULL REQUESTS:
- [owner/repo] #123: Add feature Y (OPEN)

## File Changes

Files changed: 12
Lines added: 245
Lines deleted: 89

Modified files:
- src/Main.java (+45, -12)
```

### Output Report Format

```markdown
**Yesterday's Accomplishments**
- Implemented feature X for the authentication module
- Opened PR #123 to add feature Y with 245 lines of new code
- Closed issue #456 related to bug Z

**Today's Plans**
- Review and address feedback on PR #123
- Continue work on feature X implementation
- Begin investigating performance optimization

**Blockers**
- None at this time
```

## Data Flow Characteristics

### Streaming vs. Buffering

| Stage | Strategy | Reason |
|-------|----------|--------|
| GitHub API calls | Buffered | Need complete JSON for parsing |
| Diff analysis | Buffered | Need complete diff for stats |
| Claude output | **Streamed** | Real-time user feedback |

### Error Propagation

```mermaid
graph LR
    A[Git detection fails] -->|null repo| B[Warn user]
    B --> C[Continue with all repos]

    D[Commit search fails] -->|empty array| E[Warn user]
    E --> F[Continue with PRs/issues]

    G[PR diff unavailable] -->|skip PR| H[Process next PR]

    I[Claude fails] -->|exit code| J[Propagate error]
    J --> K[Exit with failure]

    style C fill:#ffe1e1,color:#000
    style F fill:#ffe1e1,color:#000
    style K fill:#ff9999,color:#000
```

### Memory Efficiency

- **No persistent storage**: All data is transient
- **Subprocess isolation**: Each JBang script runs in separate JVM
- **Streaming output**: Claude results go directly to stdout
- **JSON parsing**: Only in-memory, no file I/O

### Parallelization Opportunities

Current: **Sequential execution**
```
Collect → Analyze → Generate
```

Future potential:
```
Collect (parallel: commits + PRs + issues)
  ↓
Analyze (parallel: PR diffs)
  ↓
Generate (streaming)
```

## Data Validation

### Input Validation
- `--days`: Must be valid integer
- `--user`: String (no validation)
- `--repo`: Format `owner/repo` (no validation)
- `--format`: Enum validation (future)

### Output Validation
- GitHub CLI exit codes checked
- JSON parsing exceptions caught
- Claude exit code checked
- No output content validation

### Data Sanitization
- No SQL injection (no database)
- No XSS (no HTML in current impl)
- Shell injection protected by ProcessBuilder array API
- JSON escaping handled by Gson
