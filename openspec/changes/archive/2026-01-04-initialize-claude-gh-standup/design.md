# Design Document: claude-gh-standup

## Context

This project reimplements the core functionality of [gh-standup](https://github.com/sgoedecke/gh-standup) (a Go-based GitHub CLI extension using GitHub Models LLM API) as a Claude Code slash command using Java/JBang and the `claude -p` CLI pattern.

**Background**:
- Original gh-standup requires GitHub Models API access and API key management
- Claude Code ecosystem benefits from slash commands that integrate with existing `claude` CLI
- JBang pattern enables single-file Java scripts without compilation

**Constraints**:
- Must use `gh` CLI (GitHub CLI) for all GitHub API access (no direct API calls)
- Must use `claude -p` for AI generation (no Anthropic API SDK)
- Must follow Claude Code slash command conventions
- Must be installable via git clone to `.claude/commands/` directory

**Stakeholders**:
- Individual developers generating personal standup reports
- Team leads generating consolidated team standups
- Users of Claude Code CLI who want GitHub integration

## Goals / Non-Goals

### Goals
1. **Zero API Key Management**: Use `gh` and `claude` CLIs which handle authentication
2. **Rich Context**: Analyze file diffs beyond commit messages for better AI understanding
3. **Team Collaboration**: Support multi-user aggregation for team standups
4. **Flexible Output**: Export to Markdown, JSON, HTML for different use cases
5. **Claude Code Integration**: First-class slash command experience
6. **Single-File Simplicity**: JBang pattern avoids build complexity

### Non-Goals
1. Real-time GitHub webhooks or continuous monitoring
2. Database persistence of historical reports
3. Web UI or dashboard (CLI-only)
4. Direct GitHub API access (must use `gh` CLI)
5. Local LLM support (requires `claude` CLI)
6. Multi-repository analysis beyond filtering

## Decisions

### Decision 1: Java/JBang Over Python
**Choice**: Use Java with JBang instead of Python

**Rationale**:
- **Type Safety**: Java's strong typing prevents runtime errors in activity/diff parsing
- **ProcessBuilder API**: More robust than Python's subprocess module for CLI integration
- **Proven Pattern**: JBang + ProcessBuilder + inheritIO()
- **Performance**: Better for subprocess-heavy workloads
- **Cross-Platform**: JBang handles Java installation automatically
- **Dependency Management**: `//DEPS` comments vs. requirements.txt

**Alternatives Considered**:
- Python: Simpler syntax but weaker typing, less robust process management
- Go: Would match original gh-standup but adds compilation complexity
- Shell scripts: Too fragile for complex JSON parsing and error handling

**Trade-offs**:
- ✅ Fewer runtime errors due to type safety
- ✅ Better CLI process management
- ❌ Slightly more verbose than Python
- ❌ Less common in Claude Code slash commands (most use Python)

### Decision 2: `claude -p` Over Anthropic API
**Choice**: Use `claude -p` CLI instead of Anthropic SDK

**Rationale**:
- **No API Key Setup**: Users already authenticated with Claude Code
- **Consistent UX**: Matches how Claude Code itself uses Claude
- **Simpler Installation**: No API key environment variables
- **InheritIO Pattern**: Seamless output piping using ProcessBuilder.inheritIO()

**Alternatives Considered**:
- Anthropic API SDK: More control but requires ANTHROPIC_API_KEY management
- GitHub Models API: Matches original gh-standup but adds dependency

**Trade-offs**:
- ✅ Zero configuration for users
- ✅ Simpler error handling (exit codes)
- ❌ Less control over model parameters
- ❌ Output parsing complexity (stdout capture)

### Decision 3: File Diff Analysis
**Choice**: Include detailed file diff analysis using `gh pr diff` and `git diff`

**Rationale**:
- **Better Context**: Commit messages often vague or missing
- **Scope Understanding**: Lines changed indicates work magnitude
- **AI Enhancement**: Claude can infer complexity from file changes
- **User Requirement**: Explicitly requested in implementation plan

**Alternatives Considered**:
- Commit messages only: Simpler but loses context
- Full diff content: Too verbose for AI prompt

**Trade-offs**:
- ✅ Richer AI-generated reports
- ✅ Better understanding of work scope
- ❌ Additional API calls (gh pr diff)
- ❌ Parsing complexity for diff format

### Decision 4: Capability Decomposition
**Choice**: Split into 6 distinct capabilities instead of monolithic design

**Rationale**:
- **Separation of Concerns**: Each capability has single responsibility
- **Testability**: Can test each Java script independently with JBang
- **Maintainability**: Clear boundaries for future enhancements
- **Reusability**: Export utilities can be reused for different report types

**Capabilities**:
1. **github-activity-collection**: GitHub API data gathering
2. **diff-analysis**: File change statistics
3. **report-generation**: AI prompt construction and invocation
4. **export-formats**: Output formatting (MD/JSON/HTML)
5. **team-aggregation**: Multi-user consolidation
6. **slash-command-interface**: CLI argument parsing and orchestration

**Trade-offs**:
- ✅ Clear module boundaries
- ✅ Easy to add new capabilities
- ❌ More files to maintain
- ❌ Orchestration complexity in Main.java

### Decision 5: Prompt Template Format
**Choice**: Use Markdown files with `{{variable}}` placeholders

**Rationale**:
- **Readability**: Markdown is human-readable for prompt editing
- **Simplicity**: String replacement is straightforward in Java
- **Version Control**: Easy to diff prompt changes
- **Reference Pattern**: Matches approach in original gh-standup (though they use YAML)

**Alternatives Considered**:
- YAML with structured fields: More structured but harder to edit
- Hardcoded strings: No flexibility for prompt iteration
- External prompt management service: Overkill for this use case

**Trade-offs**:
- ✅ Easy prompt iteration
- ✅ No special parsing library needed
- ❌ No schema validation for placeholders
- ❌ Manual tracking of required variables

## Architecture

### Component Diagram

```
User Input
    ↓
┌────────────────────────────────────────┐
│  Main.java (Entry Point)              │
│  - Parse arguments                     │
│  - Orchestrate workflow                │
│  - Handle errors                       │
└───────────┬────────────────────────────┘
            ↓
┌───────────────────────────────────────────────────────┐
│  Workflow Orchestration                               │
│  ┌─────────────────┐    ┌──────────────────┐        │
│  │ Single User     │ OR │ Team Aggregation │        │
│  │ - User provided │    │ - Multiple users │        │
│  │ - Auto-detected │    │ - Loop per user  │        │
│  └─────────────────┘    └──────────────────┘        │
└───────────┬───────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────────────────┐
│  Data Collection Layer                               │
│  ┌──────────────────────┐  ┌────────────────────┐  │
│  │ CollectActivity.java │  │ AnalyzeDiffs.java  │  │
│  │ - gh search commits  │  │ - gh pr diff       │  │
│  │ - gh search prs      │  │ - git diff         │  │
│  │ - gh search issues   │  │ - Parse diff stats │  │
│  │ - gh api reviews     │  │                    │  │
│  └──────────────────────┘  └────────────────────┘  │
└───────────┬──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────────────────┐
│  Report Generation Layer                             │
│  ┌────────────────────────────────────────────────┐ │
│  │ GenerateReport.java                            │ │
│  │ - Load prompts/standup.prompt.md               │ │
│  │ - Inject {{activities}} and {{diffs}}          │ │
│  │ - ProcessBuilder: claude -p "prompt"           │ │
│  │ - Capture AI output via inheritIO()            │ │
│  └────────────────────────────────────────────────┘ │
└───────────┬──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────────────────┐
│  Export Layer                                        │
│  ┌────────────────────────────────────────────────┐ │
│  │ ExportUtils.java                               │ │
│  │ - exportMarkdown() → MD with headers           │ │
│  │ - exportJSON() → Structured data + Gson        │ │
│  │ - exportHTML() → Styled report + CSS           │ │
│  └────────────────────────────────────────────────┘ │
└───────────┬──────────────────────────────────────────┘
            ↓
        Output
   (stdout or file)
```

### Data Flow - Single User

```
1. User: `/claude-gh-standup --days 3 --format json`
2. Main.java: Parse { days: 3, format: "json", user: auto-detect }
3. Main.java → CollectActivity.java(user, days)
   → gh search commits/prs/issues → JSON arrays
4. Main.java → AnalyzeDiffs.java(activities)
   → gh pr diff → { files_changed, additions, deletions }
5. Main.java → GenerateReport.java(activities, diffs)
   → Load prompts/standup.prompt.md
   → Inject data
   → claude -p "full_prompt"
   → AI report text
6. Main.java → ExportUtils.exportJSON(report_data)
   → JSON with metadata + report
7. Output to stdout
```

### Data Flow - Team Aggregation

```
1. User: `/claude-gh-standup --team alice bob --days 7`
2. Main.java: Detect team mode
3. For each user in [alice, bob]:
   a. CollectActivity.java(user, 7)
   b. AnalyzeDiffs.java(activities)
   c. GenerateReport.java(activities, diffs) → individual_report
4. Main.java → TeamAggregator.java(individual_reports)
   → Consolidate reports
   → Load prompts/team.prompt.md
   → Inject {{team_reports}}
   → claude -p "team_prompt"
   → Team summary
5. Main.java → ExportUtils.exportMarkdown(team_report)
6. Output to stdout
```

## Risks / Trade-offs

### Risk: GitHub API Rate Limiting
**Mitigation**:
- Use `gh` CLI which handles auth tokens and respects limits
- Document `--days` parameter to limit query scope
- Implement graceful degradation if commit search fails (common restriction)
- Consider caching activity data for development/testing

### Risk: `claude -p` Invocation Failures
**Mitigation**:
- Check `claude` is in PATH before execution
- Validate Claude Code CLI authentication
- Provide clear error messages with troubleshooting steps
- Document prerequisite: Claude Code CLI installed and authenticated

### Risk: Large Diff Content Overwhelming Prompts
**Mitigation**:
- Summarize diffs (file paths + stats) instead of full diff content
- Limit to top N changed files if needed
- Use line counts and file paths, not actual diff lines

### Risk: JBang Not Installed
**Mitigation**:
- Document JBang installation in Prerequisites
- Provide installation command: `curl -Ls https://sh.jbang.dev | bash -s - app setup`
- JBang auto-installs Java 11+ if missing
- Test on clean system to validate setup steps

### Risk: Team Reports Taking Too Long
**Mitigation**:
- Print progress indicators: "Generating report for {user}..."
- Consider parallel processing in future (v2.0)
- Document expected time for large teams (N users × ~10 seconds)

### Risk: Prompt Template Maintenance
**Mitigation**:
- Version prompt files in git
- Document `{{placeholder}}` conventions
- Provide example prompts in repository
- Consider prompt testing framework in future

## Migration Plan

**N/A - New Project Installation**

Users install by:
1. Clone repository: `git clone <repo-url> ~/.claude/commands/claude-gh-standup/`
2. Verify prerequisites: `gh auth status`, `jbang version`, `which claude`
3. Test command: `/claude-gh-standup --help`

No migration from existing systems required.

## Performance Considerations

**Expected Latency** (single user, 7 days):
- GitHub activity collection: ~2-5 seconds (gh CLI calls)
- Diff analysis: ~1-3 seconds (gh pr diff calls)
- Claude AI generation: ~5-15 seconds (depends on activity volume)
- Total: ~10-25 seconds

**Scaling Characteristics**:
- Linear with `--days` parameter (more days → more API calls)
- Linear with team size (N users × single-user latency)
- Bottleneck: Claude AI generation time (sequential)

**Optimization Opportunities** (future):
- Parallel team member processing (Java CompletableFuture)
- Cache GitHub activity data during development
- Batch diff analysis instead of per-PR calls

## Open Questions

1. **Q**: Should we support organization-wide reports (all members)?
   **A**: Defer to v2.0 - focus on explicit user lists for v1.0

2. **Q**: How to handle private repositories?
   **A**: Rely on `gh` CLI authentication - if user has access via `gh`, it works

3. **Q**: Should we support custom prompt templates via flags?
   **A**: Defer to v2.0 - users can edit `prompts/*.md` directly for now

4. **Q**: What if user has no GitHub activity?
   **A**: Generate report noting "No activity found" - let AI explain gracefully

5. **Q**: Should we add caching for development iterations?
   **A**: Not in MVP - can redirect to file: `/claude-gh-standup --format json > cache.json`

## Future Enhancements (v2.0+)

1. **Parallel Processing**: Use Java CompletableFuture for team aggregation
2. **Custom Prompts**: `--prompt path/to/prompt.md` flag
3. **Activity Caching**: `--cache` flag to store/reuse GitHub data
4. **Filtering**: `--exclude-repos`, `--activity-types` (commits only, PRs only)
5. **Organization Reports**: `--org company-name` for all org members
6. **Historical Comparison**: Compare current week vs. previous week
7. **Metrics Dashboard**: Generate charts from JSON export data
8. **Git Blame Integration**: Identify code ownership for team reports

## References

**Pattern Sources**:
- `/Users/garden/projects/ai/sgoedecke/gh-standup` - Prompt templates and GitHub API patterns

**External Documentation**:
- [JBang Documentation](https://www.jbang.dev/)
- [GitHub CLI Manual](https://cli.github.com/manual/)
- [Claude Code Slash Commands](https://docs.anthropic.com/claude/docs/claude-code)
- [ProcessBuilder JavaDoc](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ProcessBuilder.html)
