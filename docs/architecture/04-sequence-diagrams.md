# Sequence Diagrams

This document shows the temporal flow of interactions in claude-gh-standup.

## Single User Standup Flow

```mermaid
sequenceDiagram
    actor User
    participant CC as Claude Code
    participant Main as Main.java
    participant Git as git CLI
    participant GH as gh CLI
    participant Collect as CollectActivity.java
    participant Analyze as AnalyzeDiffs.java
    participant Claude as claude CLI

    User->>CC: /claude-gh-standup --days 7
    CC->>Main: jbang Main.java --days 7

    rect rgb(240, 240, 255)
        Note over Main,GH: Context Detection Phase
        Main->>Main: parseArgs()
        Main->>Git: git rev-parse --git-dir
        Git-->>Main: (success - in repo)
        Main->>Git: git remote get-url origin
        Git-->>Main: https://github.com/owner/repo.git
        Main->>Main: Extract owner/repo
        Main->>GH: gh api user --jq .login
        GH-->>Main: octocat
    end

    rect rgb(255, 240, 240)
        Note over Main,Collect: Activity Collection Phase
        Main->>Collect: jbang CollectActivity.java octocat 7 owner/repo
        Collect->>GH: gh search commits --author=octocat --committer-date=>YYYY-MM-DD
        GH-->>Collect: commits JSON array
        Collect->>GH: gh search prs --author=octocat -R owner/repo
        GH-->>Collect: PRs JSON array
        Collect->>GH: gh search issues --author=octocat -R owner/repo
        GH-->>Collect: issues JSON array
        Collect->>Collect: collectAllActivity()
        Collect-->>Main: activity JSON
    end

    rect rgb(240, 255, 240)
        Note over Main,Analyze: Diff Analysis Phase
        Main->>Analyze: jbang AnalyzeDiffs.java <activity-json>
        Analyze->>Analyze: Parse PRs from JSON
        loop For each PR
            Analyze->>GH: gh pr diff <number> -R owner/repo
            GH-->>Analyze: unified diff text
            Analyze->>Analyze: parseDiff()
        end
        Analyze->>Analyze: formatDiffSummary()
        Analyze-->>Main: diff summary text
    end

    rect rgb(255, 255, 240)
        Note over Main,Claude: Report Generation Phase
        Main->>Main: Load prompts/standup.prompt.md
        Main->>Main: formatActivities(activity)
        Main->>Main: Inject data into template
        Main->>Claude: claude -p <full-prompt>
        Note over Claude: AI generates<br/>standup report
        Claude-->>User: Stream report to stdout
        Claude-->>Main: Exit code 0
    end

    Main-->>CC: Exit code 0
    CC-->>User: Done
```

## Team Standup Flow (Future)

```mermaid
sequenceDiagram
    actor User
    participant Main as Main.java
    participant Collect as CollectActivity.java
    participant Analyze as AnalyzeDiffs.java
    participant Team as TeamAggregator.java
    participant Claude as claude CLI

    User->>Main: /claude-gh-standup --team alice bob charlie --days 7

    Main->>Main: parseArgs() - detect team mode

    loop For each team member
        rect rgb(240, 240, 255)
            Note over Main,Analyze: Individual Report Generation
            Main->>Collect: Collect activity for alice
            Collect-->>Main: alice activity JSON
            Main->>Analyze: Analyze diffs for alice
            Analyze-->>Main: alice diff summary
            Main->>Main: Store individual data
        end
    end

    rect rgb(255, 240, 240)
        Note over Main,Team: Team Aggregation Phase
        Main->>Team: jbang TeamAggregator.java <team-reports-json>
        Team->>Team: Load prompts/team.prompt.md
        Team->>Team: Consolidate individual reports
        Team->>Claude: claude -p <team-prompt>
        Claude-->>User: Team standup report
        Team-->>Main: Exit code 0
    end

    Main-->>User: Done
```

## Error Handling Flow

```mermaid
sequenceDiagram
    participant Main as Main.java
    participant Git as git CLI
    participant GH as gh CLI
    participant Collect as CollectActivity.java
    participant User

    Main->>Git: git rev-parse --git-dir
    Git-->>Main: Exit code 128 (not a repo)
    Main->>User: Warning: Not in a git repository

    Main->>Collect: Continue execution
    Collect->>GH: gh search commits --author=octocat
    GH-->>Collect: Exit code 1 (GitHub API restriction)
    Collect->>User: Warning: Commit search failed
    Collect->>Collect: Continue with empty commits array

    Collect->>GH: gh search prs --author=octocat
    GH-->>Collect: PRs JSON (success)
    Collect->>GH: gh search issues --author=octocat
    GH-->>Collect: issues JSON (success)

    Collect-->>Main: activity JSON (commits=[], prs=[...], issues=[...])
    Note over Main: Continues with<br/>available data
```

## Repository Detection Flow

```mermaid
sequenceDiagram
    participant Main as Main.java
    participant Git as git CLI
    participant User

    alt Repository specified via --repo
        Main->>Main: Use --repo value
    else No --repo argument
        Main->>Git: git rev-parse --git-dir
        alt In git repository
            Git-->>Main: .git path
            Main->>Git: git remote get-url origin
            alt Has GitHub remote
                Git-->>Main: https://github.com/owner/repo.git
                Main->>Main: Extract owner/repo
                Main->>User: Detected repository: owner/repo
            else No GitHub remote
                Git-->>Main: non-GitHub URL or error
                Main->>Main: repo = null
                Main->>User: Warning: No GitHub remote found
            end
        else Not in git repository
            Git-->>Main: Exit code 128
            Main->>Main: repo = null
            Main->>User: Warning: Not in a git repository
        end
    end
```

## User Detection Flow

```mermaid
sequenceDiagram
    participant Main as Main.java
    participant GH as gh CLI
    participant User

    alt User specified via --user
        Main->>Main: Use --user value
    else Team mode via --team
        Main->>Main: Skip user detection
        Main->>Main: Use team members list
    else Auto-detect user
        Main->>User: Detecting current GitHub user...
        Main->>GH: gh api user --jq .login
        alt GitHub CLI authenticated
            GH-->>Main: octocat
            Main->>Main: user = octocat
        else Not authenticated
            GH-->>Main: Exit code 1
            Main->>User: Error: Failed to get current user
            Main->>Main: Exit with error
        end
    end
```

## ProcessBuilder Invocation Pattern

```mermaid
sequenceDiagram
    participant Java as Java Component
    participant PB as ProcessBuilder
    participant CLI as External CLI
    participant Stdout

    Java->>PB: new ProcessBuilder("gh", "search", "prs", ...)
    Java->>PB: start()
    PB->>CLI: Execute command
    activate CLI

    loop Read output
        CLI->>PB: Output line
        PB->>Java: BufferedReader.readLine()
        Java->>Java: output.append(line)
    end

    CLI-->>PB: Process exits
    deactivate CLI
    PB->>Java: waitFor() returns exit code

    alt Exit code 0
        Java->>Java: Parse output
    else Exit code != 0
        Java->>Java: Read error stream
        Java->>Stdout: System.err.println(error)
        Java->>Java: Handle error or continue
    end
```

## Claude Streaming Pattern (inheritIO)

```mermaid
sequenceDiagram
    participant Main as Main.java
    participant PB as ProcessBuilder
    participant Claude as claude CLI
    participant Stdout

    Main->>Main: Build full prompt
    Main->>PB: new ProcessBuilder("claude", "-p", fullPrompt)
    Main->>PB: inheritIO()
    Note over PB: Inherits stdin/stdout/stderr<br/>from parent process
    Main->>PB: start()
    PB->>Claude: Execute with prompt
    activate Claude

    loop Generate report
        Claude->>Stdout: Write output chunk
        Note over Stdout: User sees output<br/>in real-time
    end

    Claude-->>PB: Process exits
    deactivate Claude
    PB->>Main: waitFor() returns exit code

    alt Exit code 0
        Main->>Main: Success
    else Exit code != 0
        Main->>Stdout: Error: Claude invocation failed
        Main->>Main: Exit with error
    end
```

## Timing Characteristics

### Typical Execution Timeline

```mermaid
gantt
    title Single User Standup Generation Timeline
    dateFormat X
    axisFormat %S

    section Context Detection
    Parse args           :0, 1
    Detect repository    :1, 2
    Detect user          :2, 3

    section Activity Collection
    Search commits       :3, 6
    Search PRs           :6, 8
    Search issues        :8, 10

    section Diff Analysis
    Fetch PR diffs       :10, 15
    Parse diffs          :15, 16

    section AI Generation
    Load template        :16, 17
    Format data          :17, 18
    Claude generation    :18, 30

    section Output
    Stream to user       :18, 30
```

**Approximate timings**:
- Context detection: ~1-2 seconds
- Activity collection: ~5-7 seconds (varies by GitHub API)
- Diff analysis: ~4-6 seconds (varies by number of PRs)
- AI generation: ~10-15 seconds (varies by prompt size and Claude load)

**Total**: ~20-30 seconds for typical single-user standup

### Performance Bottlenecks

1. **GitHub API calls** - Sequential, network-dependent
2. **PR diff fetching** - One API call per PR
3. **Claude generation** - LLM inference time

### Future Optimization Opportunities

- Parallel GitHub API calls (commits + PRs + issues)
- Parallel PR diff fetching
- Caching of recent activity data
- Batch PR diff requests
