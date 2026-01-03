# Integration Patterns

This document details how claude-gh-standup integrates with external systems.

## GitHub CLI Integration

### Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant System as claude-gh-standup
    participant GH as gh CLI
    participant GitHub as GitHub API

    Note over User,GitHub: One-time setup
    User->>GH: gh auth login
    GH->>GitHub: OAuth flow
    GitHub-->>GH: Access token
    GH->>GH: Store token in ~/.config/gh/

    Note over User,GitHub: Every standup invocation
    User->>System: /claude-gh-standup
    System->>GH: gh search commits
    GH->>GH: Load token from config
    GH->>GitHub: API request with token
    GitHub-->>GH: Response
    GH-->>System: JSON output
```

**Key Benefits**:
- No API key management in code
- Leverages existing gh CLI authentication
- Token refresh handled by gh CLI
- Supports GitHub Enterprise via gh CLI config

### GitHub CLI Commands Used

```mermaid
graph TB
    subgraph "User Detection"
        API[gh api user<br/>--jq .login]
        API -->|JSON| UserLogin[Extract username]
    end

    subgraph "Activity Search"
        Commits[gh search commits<br/>--author=USER<br/>--committer-date=&gt;DATE<br/>--json sha,commit,repository]
        PRs[gh search prs<br/>--author=USER<br/>--created=&gt;DATE<br/>-R REPO<br/>--json number,title,state,repository,url]
        Issues[gh search issues<br/>--author=USER<br/>--created=&gt;DATE<br/>-R REPO<br/>--json number,title,state,repository,url]
    end

    subgraph "Diff Analysis"
        Diff[gh pr diff NUMBER<br/>-R REPO]
        Diff -->|Unified diff| Parse[Parse diff format]
    end

    style API fill:#e1f5ff,color:#000
    style Commits fill:#ffe1f5,color:#000
    style PRs fill:#ffe1f5,color:#000
    style Issues fill:#ffe1f5,color:#000
    style Diff fill:#f5e1ff,color:#000
```

### Search Query Construction

#### Commits Search
```java
// Command construction
gh search commits
  --author=octocat
  --committer-date=>2025-12-27
  --json sha,commit,repository
  --limit 1000
```

**Limitations**:
- Often fails due to GitHub API restrictions
- Not all commits are indexed for search
- System continues gracefully if this fails

#### Pull Requests Search
```java
// With repository filter
gh search prs
  --author=octocat
  --created=>2025-12-27
  -R owner/repo
  --json number,title,state,repository,url
  --limit 1000

// Without repository filter (all repos)
gh search prs
  --author=octocat
  --created=>2025-12-27
  --json number,title,state,repository,url
  --limit 1000
```

#### Issues Search
```java
// Same pattern as PRs
gh search issues
  --author=octocat
  --created=>2025-12-27
  -R owner/repo
  --json number,title,state,repository,url
  --limit 1000
```

### JSON Response Parsing

```mermaid
graph LR
    subgraph "gh CLI Output"
        JSON[Raw JSON String]
    end

    subgraph "Gson Parsing"
        Parse[JsonParser.parseString]
        Array[JsonArray]
        Objects[JsonObject instances]
    end

    subgraph "Data Extraction"
        Commits[Commits: sha, message, repo]
        PRs[PRs: number, title, state, url]
        Issues[Issues: number, title, state, url]
    end

    JSON --> Parse
    Parse --> Array
    Array --> Objects
    Objects --> Commits
    Objects --> PRs
    Objects --> Issues
```

**Example commit JSON**:
```json
{
  "sha": "abc123def456...",
  "commit": {
    "message": "Add feature X\n\nDetailed description...",
    "author": {
      "name": "Alice Developer",
      "email": "alice@example.com"
    }
  },
  "repository": {
    "fullName": "owner/repo"
  }
}
```

## Git CLI Integration

### Repository Detection

```mermaid
graph TB
    Start[Start] --> Check{git rev-parse<br/>--git-dir}

    Check -->|Exit 0| InRepo[In Git Repository]
    Check -->|Exit 128| NoRepo[Not in repository]

    InRepo --> GetRemote{git remote<br/>get-url origin}

    GetRemote -->|Success| ParseURL[Parse remote URL]
    GetRemote -->|Failure| NoRemote[No remote configured]

    ParseURL --> GitHubCheck{Contains<br/>github.com?}

    GitHubCheck -->|Yes| Extract[Extract owner/repo]
    GitHubCheck -->|No| NotGitHub[Not a GitHub repo]

    Extract --> Result[Return owner/repo]
    NoRepo --> Null[Return null]
    NoRemote --> Null
    NotGitHub --> Null

    style Result fill:#e1ffe1,color:#000
    style Null fill:#ffe1e1,color:#000
```

### URL Parsing Logic

```java
// Supports multiple GitHub URL formats
String remoteUrl = "https://github.com/owner/repo.git";
// OR
String remoteUrl = "git@github.com:owner/repo.git";

// Regex pattern
if (remoteUrl.contains("github.com")) {
    String[] parts = remoteUrl.split("github.com[:/]");
    if (parts.length > 1) {
        String repoPath = parts[1].replaceAll("\\.git$", "");
        return repoPath; // "owner/repo"
    }
}
```

**Supported formats**:
- `https://github.com/owner/repo.git`
- `https://github.com/owner/repo`
- `git@github.com:owner/repo.git`
- `git@github.com:owner/repo`

## Claude CLI Integration

### Prompt Mode Invocation

```mermaid
sequenceDiagram
    participant Main as Main.java
    participant Template as Prompt Template
    participant Claude as claude CLI
    participant API as Anthropic API
    participant User

    Main->>Template: Read prompts/standup.prompt.md
    Template-->>Main: Template string
    Main->>Main: formatActivities(activity)
    Main->>Main: Replace {{activities}} and {{diffs}}
    Main->>Main: fullPrompt string

    Main->>Claude: ProcessBuilder("claude", "-p", fullPrompt)
    Note over Main,Claude: inheritIO() - stdout piped directly

    Claude->>API: Send prompt to Claude API
    activate API

    loop Generate tokens
        API->>Claude: Stream response tokens
        Claude->>User: Write to stdout
        Note over User: Sees output in real-time
    end

    API-->>Claude: Generation complete
    deactivate API
    Claude-->>Main: Exit code 0
```

### Template Injection Pattern

```java
// Load template file
String promptTemplate = Files.readString(
    Paths.get("prompts/standup.prompt.md")
);

// Format activity data
String formattedActivities = formatActivities(activity);

// Inject data via string replacement
String fullPrompt = promptTemplate
    .replace("{{activities}}", formattedActivities)
    .replace("{{diffs}}", diffSummary);

// Invoke Claude
ProcessBuilder pb = new ProcessBuilder("claude", "-p", fullPrompt);
pb.inheritIO();
Process process = pb.start();
```

**Template placeholders**:
- `{{activities}}` - Formatted commits, PRs, issues
- `{{diffs}}` - File change statistics and summary

### Streaming Output (inheritIO Pattern)

```mermaid
graph TB
    subgraph "Traditional Buffering"
        T1[Java Process] -->|Read stdout| T2[Buffer in memory]
        T2 -->|Parse| T3[Process]
        T3 -->|Print| T4[User sees output]
    end

    subgraph "inheritIO Streaming"
        S1[Java Process] -.->|inheritIO| S2[Direct pipe]
        S2 -->|Real-time| S3[User sees output]
    end

    style S2 fill:#e1ffe1,color:#000
    style T2 fill:#ffe1e1,color:#000
```

**Benefits of inheritIO**:
- User sees output as it's generated
- No memory buffering overhead
- Natural streaming experience
- No intermediate parsing required

**Trade-off**:
- Cannot capture output for post-processing
- Cannot modify or filter output
- Exit code is only feedback mechanism

### Error Handling

```java
int exitCode = claudeProcess.waitFor();

if (exitCode != 0) {
    System.err.println("Claude invocation failed with exit code: " + exitCode);
    System.exit(exitCode);
}
```

**Possible exit codes**:
- `0` - Success
- `1` - General error (invalid prompt, API error, etc.)
- Other - System errors

## JBang Process Orchestration

### Subprocess Execution Pattern

```mermaid
graph TB
    subgraph "Main.java Process"
        Main[Main Process<br/>PID 1234]
    end

    subgraph "Child Processes"
        Collect[CollectActivity.java<br/>PID 1235]
        Analyze[AnalyzeDiffs.java<br/>PID 1236]
        Claude[claude CLI<br/>PID 1237]
    end

    subgraph "Grandchild Processes"
        GH1[gh search commits<br/>PID 1238]
        GH2[gh search prs<br/>PID 1239]
        GH3[gh pr diff<br/>PID 1240]
    end

    Main -->|JBang subprocess| Collect
    Main -->|JBang subprocess| Analyze
    Main -->|Direct exec| Claude

    Collect -->|ProcessBuilder| GH1
    Collect -->|ProcessBuilder| GH2
    Analyze -->|ProcessBuilder| GH3

    style Main fill:#fff5e1,color:#000
    style Collect fill:#ffe1f5,color:#000
    style Analyze fill:#f5e1ff,color:#000
```

### JBang Subprocess Invocation

```java
// Build command
List<String> command = new ArrayList<>();
command.add("jbang");
command.add("scripts/CollectActivity.java");
command.add(username);
command.add(String.valueOf(days));
if (repo != null) {
    command.add(repo);
}

// Execute
ProcessBuilder pb = new ProcessBuilder(command);
Process process = pb.start();

// Read output
StringBuilder output = new StringBuilder();
try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {
    String line;
    while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
    }
}

// Wait and check
int exitCode = process.waitFor();
if (exitCode != 0) {
    // Handle error
}
```

### Process Communication

```mermaid
graph LR
    subgraph "Main.java"
        M1[Prepare arguments]
        M2[Invoke subprocess]
        M3[Read stdout]
        M4[Check exit code]
    end

    subgraph "CollectActivity.java"
        C1[Parse args]
        C2[Call gh CLI]
        C3[Build JSON]
        C4[Print to stdout]
    end

    M1 --> M2
    M2 --> C1
    C1 --> C2
    C2 --> C3
    C3 --> C4
    C4 --> M3
    M3 --> M4

    style M2 fill:#e1f5ff,color:#000
    style C4 fill:#ffe1f5,color:#000
```

**Communication protocol**:
- **Input**: Command-line arguments
- **Output**: JSON to stdout
- **Errors**: Messages to stderr
- **Status**: Process exit code

## Error Handling Strategies

### Graceful Degradation

```mermaid
graph TB
    Start[Start Execution] --> Repo{Repository<br/>Detection}

    Repo -->|Success| User{User<br/>Detection}
    Repo -->|Failure| WarnRepo[Warn: No repo<br/>Continue]
    WarnRepo --> User

    User -->|Success| Commits{Commit<br/>Search}
    User -->|Failure| Error[Error: Cannot proceed<br/>Exit 1]

    Commits -->|Success| PRs
    Commits -->|Failure| WarnCommits[Warn: Commits failed<br/>Continue]

    WarnCommits --> PRs{PR Search}
    PRs -->|Success| Issues
    PRs -->|Failure| WarnPRs[Warn: PRs failed<br/>Continue]

    WarnPRs --> Issues{Issue Search}
    Issues -->|Success| Diffs
    Issues -->|Failure| WarnIssues[Warn: Issues failed<br/>Continue]

    WarnIssues --> Diffs{Diff Analysis}
    Diffs -->|Success| Generate
    Diffs -->|Failure| WarnDiffs[Warn: Some diffs failed<br/>Continue]

    WarnDiffs --> Generate{Claude<br/>Generation}
    Generate -->|Success| Success[Success<br/>Exit 0]
    Generate -->|Failure| Error

    style Success fill:#e1ffe1,color:#000
    style Error fill:#ffe1e1,color:#000
```

### Warning vs. Error Policy

**Warnings (continue execution)**:
- Repository not detected
- Commit search failed
- Individual PR diff unavailable

**Errors (stop execution)**:
- User detection failed
- Cannot read prompt template
- Claude invocation failed

### Example Error Messages

```java
// Repository detection warning
System.err.println("Warning: Not in a git repository or no GitHub remote found.");
System.err.println("Activity will be searched across all repositories.");
System.err.println("Use --repo owner/repo to specify a repository.");

// Commit search warning
System.err.println("Warning: Commit search failed (this is common due to GitHub restrictions)");

// PR diff warning
System.err.println("Warning: Could not analyze diff for PR #" + prNumber + ": " + e.getMessage());

// Claude failure error
System.err.println("Claude invocation failed with exit code: " + claudeExitCode);
System.exit(claudeExitCode);
```

## Integration Architecture Summary

```mermaid
graph TB
    subgraph "claude-gh-standup"
        Main[Main.java]
        Collect[CollectActivity.java]
        Analyze[AnalyzeDiffs.java]
    end

    subgraph "CLI Tools"
        Git[git CLI<br/>Repository detection]
        GH[gh CLI<br/>Data collection]
        Claude[claude CLI<br/>Report generation]
    end

    subgraph "External Services"
        GitHub[GitHub API<br/>Activity data]
        Anthropic[Anthropic API<br/>Claude AI]
    end

    Main -->|Execute| Collect
    Main -->|Execute| Analyze
    Main -->|Execute| Claude

    Collect -->|Call| GH
    Analyze -->|Call| GH
    Main -->|Call| Git

    GH <-->|Authenticated| GitHub
    Claude <-->|Authenticated| Anthropic

    style Main fill:#fff5e1,color:#000
    style Git fill:#e1f5ff,color:#000
    style GH fill:#e1f5ff,color:#000
    style Claude fill:#e1ffe1,color:#000
```

**Key integration principles**:
1. **Delegate authentication** to CLI tools
2. **Use ProcessBuilder** for all external invocations
3. **Stream output** when possible (inheritIO)
4. **Parse JSON** only when needed
5. **Fail gracefully** on non-critical errors
6. **Propagate errors** from critical operations
