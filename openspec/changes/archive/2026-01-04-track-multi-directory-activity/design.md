# Design Document: track-multi-directory-activity

## Overview

This document captures architectural decisions, technical patterns, and trade-offs for implementing multi-directory standup tracking. It complements the spec deltas by explaining *why* certain approaches were chosen.

## Architecture Decisions

### AD-1: User-Level Configuration (Not Repository-Level)

**Decision**: Store configuration in `~/.claude-gh-standup/config.json` (user home), not in each repository.

**Rationale**:
- **Cross-repository tracking**: Users often work across multiple repositories simultaneously
- **Single source of truth**: One config manages all projects, easier to maintain
- **No repo pollution**: Avoids adding config files to every project
- **Simpler installation**: Install once, configure once

**Trade-offs**:
- ❌ Cannot share config with team (each developer configures independently)
- ❌ Config not version-controlled with code
- ✅ Works across all repos without per-repo setup
- ✅ Supports tracking multiple repos in one report

**Alternatives Considered**:
1. **Per-repo config** (`.claude-gh-standup/config.json` in each repo)
   - Rejected: Duplication, doesn't solve multi-repo tracking
2. **Environment variables** (e.g., `STANDUP_DIRS=/path1:/path2`)
   - Rejected: Cumbersome for complex config, no persistence

**Implementation**: ConfigManager.java loads from `expandTilde("~/.claude-gh-standup/config.json")`

---

### AD-2: Empty Config = Legacy Mode (Backward Compatibility)

**Decision**: If config file doesn't exist OR `directories: []` is empty, use legacy single-directory mode.

**Rationale**:
- **Zero breaking changes**: Existing users unaffected
- **Gradual migration**: Users opt-in when ready
- **Testing safety**: New code path only when explicitly enabled

**Implementation**:
```java
boolean multiDirMode = config != null
    && config.directories != null
    && !config.directories.isEmpty()
    && parsed.repo == null;  // --repo flag overrides
```

**Edge Cases**:
- No config file → `config = null` → legacy mode
- Config exists but `directories: []` → `isEmpty() = true` → legacy mode
- Config exists with directories, but `--repo owner/repo` provided → `parsed.repo != null` → legacy mode (explicit override)

---

### AD-3: Parallel Local Change Detection (Performance)

**Decision**: Use Java ExecutorService to detect local changes in parallel across directories.

**Rationale**:
- **Performance**: 5 directories sequentially = ~10-15 seconds, parallel = ~3-5 seconds (3-4x faster)
- **I/O bound operation**: Git commands wait on disk, perfect for concurrency
- **User experience**: Faster reports → better adoption

**Implementation** (ActivityAggregator.java):
```java
ExecutorService executor = Executors.newFixedThreadPool(Math.min(directories.size(), 4));
List<Future<JsonObject>> futures = new ArrayList<>();

for (Directory dir : directories) {
    futures.add(executor.submit(() -> {
        return callLocalChangesDetector(dir);
    }));
}

for (Future<JsonObject> future : futures) {
    localChanges.add(future.get());  // Blocks until complete
}
executor.shutdown();
```

**Trade-offs**:
- ✅ 3-4x faster for 5+ directories
- ✅ Simple thread pool pattern (built-in Java)
- ❌ Slightly more complex code
- ❌ Stderr output may interleave (acceptable for progress messages)

**Alternatives Considered**:
1. **Sequential processing** - Rejected: Too slow for 5+ directories
2. **CompletableFuture** - Deferred to v2.0 (overkill for v1.0)

---

### AD-4: Deduplicate GitHub Activity by Repository (Not by Directory)

**Decision**: Fetch GitHub activity once per unique `repoName` (owner/repo), not once per directory.

**Rationale**:
- **Avoid duplicate API calls**: 3 branches of same repo = 1 GitHub API call (3x faster)
- **GitHub rate limits**: Respect API quotas by minimizing requests
- **Accuracy**: User's activity is repository-scoped, not branch-scoped

**Implementation** (ActivityAggregator.java):
```java
// Group directories by repoName
Map<String, List<Directory>> repoMap = directories.stream()
    .collect(Collectors.groupingBy(d -> d.repoName));

// Fetch GitHub activity once per unique repo
for (String repoName : repoMap.keySet()) {
    JsonObject activity = callCollectActivity(user, days, repoName);
    githubActivity.add(repoName, activity);
}
```

**Example**:
```
Directories:
1. ~/projects/myapp/main (branch: main, repo: owner/myapp)
2. ~/projects/myapp/feature (branch: feature/new-ui, repo: owner/myapp)
3. ~/projects/other/main (branch: main, repo: owner/other)

GitHub API Calls:
1. CollectActivity.java for owner/myapp (covers directories 1 & 2)
2. CollectActivity.java for owner/other (covers directory 3)

Result: 2 API calls instead of 3
```

**Trade-offs**:
- ✅ Faster (fewer API calls)
- ✅ Lower rate limit consumption
- ✅ Simpler GitHub activity section (per-repo, not per-branch)
- ❌ Cannot filter activity by branch (acceptable - PRs/issues are repo-scoped anyway)

---

### AD-5: Monday-Aware "Yesterday" Logic (UX)

**Decision**: `--yesterday` returns 3 days on Monday (covers Friday), 1 day otherwise.

**Rationale**:
- **Real-world usage**: Monday standup needs Friday's work, not Sunday's (no work)
- **Reduce cognitive load**: Users don't calculate "3 days back" on Monday
- **Industry standard**: Many standup tools do this automatically

**Implementation** (Main.java):
```java
private static int calculateDays(Args parsed) {
    if (parsed.yesterday) {
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            return 3;  // Friday + Saturday + Sunday
        }
        return 1;
    }
    if (parsed.lastWeek) {
        return 7;
    }
    return parsed.days;  // Explicit --days flag
}
```

**Edge Cases**:
- Holiday Monday: Still returns 3 (acceptable - covers previous workday)
- Tuesday after long weekend: Returns 1 (user can use `--days 4` explicitly)
- `--yesterday --days 5`: `--yesterday` takes precedence (first flag wins)

**Trade-offs**:
- ✅ Better UX for 90% of Monday standup cases
- ✅ Aligns with user mental model
- ❌ Opinionated (assumes 5-day work week)
- ❌ No override if user truly wants 1 day on Monday (can use `--days 1` explicitly)

**Alternatives Considered**:
1. **Always return 1 day** - Rejected: Poor Monday UX
2. **Add `--weekend-aware` flag** - Rejected: Too complex, most users want smart default
3. **Check calendar API for holidays** - Rejected: Over-engineering, regional differences

---

### AD-6: Auto-Save Reports (Default On, Configurable)

**Decision**: Reports auto-save to `~/.claude-gh-standup/reports/` by default, configurable via `reportSettings.autoSaveReports`.

**Rationale**:
- **User value**: Historical reports useful for performance reviews, retrospectives
- **Low friction**: Happens automatically, no extra user action
- **Auditable**: Can reference past standups weeks/months later

**Filename Logic**:
```java
String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);  // YYYY-MM-DD

if (uniqueRepoCount == 1) {
    // Single repo: include repo name for clarity
    filename = date + "-" + sanitizeRepoName(singleRepo) + ".md";
    // Example: 2026-01-04-owner-myapp.md
} else {
    // Multiple repos: generic name
    filename = date + "-multi.md";
    // Example: 2026-01-04-multi.md
}
```

**Trade-offs**:
- ✅ Useful default behavior (most users want this)
- ✅ Easily disabled (`autoSaveReports: false` in config)
- ✅ Doesn't interfere with stdout output (can still pipe to clipboard)
- ❌ Creates files user might not want (mitigated by stderr message showing path)
- ❌ No automatic cleanup (user manages old reports manually)

**Alternatives Considered**:
1. **Default off** - Rejected: Requires users to discover feature
2. **Prompt on first run** - Rejected: Interrupts workflow
3. **Save only on explicit flag `--save`** - Rejected: Most users want this always

**Future Enhancement** (v2.0):
- Report retention policy (auto-delete reports older than N days)
- Git-commit reports to dedicated repo (opt-in)

---

### AD-7: Fixed Installation Path with Symlink (Not Relative)

**Decision**: Install to fixed `~/.claude-gh-standup/`, symlink to `~/.claude/commands/claude-gh-standup` for Claude Code discovery.

**Rationale**:
- **Slash command requirement**: Claude Code slash commands execute via `.md` file in `.claude/commands/`
- **Invocation path problem**: Slash command needs absolute path to `Main.java` (cannot use `$COMMAND_DIR` for JBang)
- **Shell alias compatibility**: Shell aliases also need fixed path
- **Single source of truth**: One installation location, multiple access methods

**Directory Structure**:
```
~/.claude-gh-standup/              # Fixed installation (git repo)
├── scripts/Main.java
├── prompts/
├── config.json
└── .claude/commands/claude-gh-standup.md

~/.claude/commands/
└── claude-gh-standup/  → symlink to ~/.claude-gh-standup/

~/.claude-gh-standup/reports/      # Auto-generated reports
```

**Slash Command Invocation** (`.claude/commands/claude-gh-standup.md`, line 77):
```bash
# OLD (broken): jbang $COMMAND_DIR/scripts/Main.java
# NEW (fixed): jbang ~/.claude-gh-standup/scripts/Main.java --no-claude $ARGUMENTS
```

**Shell Aliases** (`~/.zshrc`):
```bash
alias standup='jbang ~/.claude-gh-standup/scripts/Main.java'
alias standup-yesterday='jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week='jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
```

**Trade-offs**:
- ✅ Slash command and shell aliases work reliably
- ✅ Update via `git pull` in `~/.claude-gh-standup/` (single location)
- ✅ Symlink enables Claude Code discovery
- ❌ Opinionated installation path (user cannot choose)
- ❌ Requires symlink management in install.sh

**Why Not Relative Paths?**:
- `$COMMAND_DIR` resolves to symlink target, causing path confusion
- JBang requires absolute paths for reliable execution
- Shell aliases executed from any directory need fixed path

---

### AD-8: Minimal Shell Aliases (3 Total, Not 10+)

**Decision**: Install only 3 essential aliases: `standup`, `standup-yesterday`, `standup-week`.

**Rationale**:
- **Simplicity**: Fewer aliases = easier to remember
- **Main command covers all**: `standup --config-add`, `standup --days 3`, etc.
- **Common cases**: 90% of usage is "yesterday" or "last week"
- **Namespace pollution**: Too many aliases clutter shell environment

**Implementation** (install.sh):
```bash
alias standup-yesterday='jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week='jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
alias standup='jbang ~/.claude-gh-standup/scripts/Main.java'
```

**Usage Examples**:
```bash
# Common cases (aliases)
standup-yesterday
standup-week

# Configuration (via main command)
standup --config-add ~/projects/myapp --id main-branch
standup --config-list
standup --config-remove main-branch

# Custom queries (via main command)
standup --days 3
standup --team alice bob --days 7
standup --format json --output report.json
```

**Trade-offs**:
- ✅ Clean, minimal shell environment
- ✅ All functionality accessible via `standup` command
- ✅ Intuitive shortcuts for 90% use cases
- ❌ No `standup-config-add` shortcut (acceptable - infrequent operation)

**Alternatives Considered**:
1. **10+ aliases** (one per common operation) - Rejected: Too many to remember
2. **No aliases** (only slash command) - Rejected: Shell users want direct access
3. **Single alias `gs` (GitHub Standup)** - Rejected: Not descriptive enough

---

### AD-9: Graceful Degradation for Missing Directories

**Decision**: If configured directory doesn't exist or has git errors, skip with warning and continue.

**Rationale**:
- **Resilience**: Don't fail entire report because one branch was deleted
- **User experience**: Partial data better than no data
- **Common scenario**: User deletes feature branch after merge

**Implementation** (ActivityAggregator.java):
```java
List<Directory> enabledDirs = config.directories.stream()
    .filter(d -> d.enabled)
    .filter(d -> {
        Path path = Paths.get(expandTilde(d.path));
        if (!Files.exists(path)) {
            System.err.println("⚠️  Directory not found: " + d.path + " (skipping)");
            return false;
        }
        return true;
    })
    .collect(Collectors.toList());
```

**Error Scenarios**:
1. **Directory deleted**: Skip with warning, continue with remaining
2. **Git permission error**: Skip with warning (e.g., "Permission denied")
3. **Remote branch deleted**: Skip unpushed commits check, show uncommitted only
4. **No enabled directories**: Fall back to legacy single-directory mode

**Trade-offs**:
- ✅ Robust against configuration drift
- ✅ Clear user feedback (warnings on stderr)
- ✅ Partial data still useful
- ❌ Silent skipping might hide issues (mitigated by warnings)

**User Experience**:
```
⚠️  Directory not found: ~/projects/old-feature (skipping)
✓ Collecting activity for 2 directories...
✓ Report saved to ~/.claude-gh-standup/reports/2026-01-04-owner-repo.md
```

---

### AD-10: JSON as Inter-Script Communication Format

**Decision**: All scripts communicate via JSON (stdout), not shared memory or files.

**Rationale**:
- **JBang constraint**: Each script is independent process (no shared JVM)
- **Testability**: Easy to test scripts with JSON input/output
- **Debugging**: Can inspect intermediate JSON with `jq`
- **Type safety**: Gson provides structured parsing

**Data Flow**:
```
ConfigManager.java     → JSON config
LocalChangesDetector   → JSON per directory
ActivityAggregator     → Unified JSON
Main.java              → Format into prompt → GenerateReport.java
```

**Example** (LocalChangesDetector output):
```json
{
  "directoryId": "myapp-main",
  "path": "/Users/garden/projects/myapp",
  "branch": "main",
  "uncommitted": {
    "hasChanges": true,
    "filesChanged": 3,
    "staged": ["file1.java", "file2.java"],
    "unstaged": ["file3.java"]
  },
  "unpushed": {
    "hasCommits": true,
    "count": 2,
    "commits": ["abc1234 Fix bug", "def5678 Add feature"]
  }
}
```

**Trade-offs**:
- ✅ Clean separation of concerns
- ✅ Independently testable scripts
- ✅ Human-readable intermediate format
- ❌ JSON parsing overhead (minimal with Gson)
- ❌ No compile-time type checking across scripts (mitigated by schemas)

**Alternative** (rejected): Shared Java library with common classes
- Problem: JBang doesn't easily share code across scripts
- Problem: Breaks "single-file executable" pattern

---

## Performance Characteristics

### Expected Latency

**Single Directory (Legacy Mode)**:
- GitHub activity: 2-3 seconds (via `gh` CLI)
- Diff analysis: 0.5-1 second
- Claude AI: 5-10 seconds (streaming)
- **Total**: 8-15 seconds (unchanged from current)

**Multi-Directory Mode (3 directories, same repo)**:
- Config load: <0.1 seconds
- Local changes (parallel): 1-2 seconds (3 git commands per dir × 3 dirs / 4 threads)
- GitHub activity (deduplicated): 2-3 seconds (1 API call)
- Activity aggregation: <0.5 seconds
- Claude AI: 7-12 seconds (larger prompt)
- Report save: <0.1 seconds
- **Total**: 10-18 seconds (acceptable)

**Multi-Directory Mode (5 directories, 3 different repos)**:
- Local changes (parallel): 2-3 seconds
- GitHub activity: 6-9 seconds (3 API calls)
- Activity aggregation: <0.5 seconds
- Claude AI: 10-15 seconds
- **Total**: 18-28 seconds (acceptable for comprehensive report)

### Bottlenecks

1. **Claude AI generation**: Largest single operation (5-15 seconds)
   - Mitigation: Streaming output provides progress feedback
2. **GitHub API calls**: Linear with unique repos (2-3 seconds each)
   - Mitigation: Deduplication reduces calls significantly
3. **Git commands**: I/O bound, mitigated by parallelization

### Optimization Opportunities (v2.0)

- Cache GitHub activity for 15 minutes (during development iterations)
- Batch diff analysis across directories
- Use GitHub GraphQL API for fewer requests

---

## Security Considerations

### Sensitive Data Handling

**No Secrets in Config**:
- Config stores only paths, IDs, and repository names (public info)
- No API keys or tokens (authentication via `gh` and `claude` CLIs)
- Config file readable by user only (inherited umask permissions)

**Git Credential Exposure**:
- Uses existing `gh auth` and git credentials (not ours to manage)
- No password prompts or credential storage

**Report Content**:
- Reports may contain commit messages and code context
- Stored locally in user home (`~/.claude-gh-standup/reports/`)
- User controls report retention and deletion

### File System Permissions

**Config Directory**: `~/.claude-gh-standup/`
- Created with user's default umask (typically 755 for dirs, 644 for files)
- No need for special permissions (not a daemon, no root)

**Reports Directory**: `~/.claude-gh-standup/reports/`
- Same permissions as parent
- User can manually `chmod 700` if desired

### Process Isolation

**No Shared State**:
- Each JBang script runs as separate process
- No shared memory or IPC (only JSON via pipes)
- No daemon or background service

**Subprocess Safety**:
- ProcessBuilder escapes arguments (no shell injection)
- Git commands use `-C <path>` (no directory traversal)

---

## Error Handling Strategy

### Categories

1. **Recoverable Errors** (warn and continue):
   - Directory not found → skip with warning
   - Git permission denied → skip local changes for that directory
   - Remote branch doesn't exist → skip unpushed commits check
   - Commit search fails → continue with PRs/issues (existing pattern)

2. **Fatal Errors** (exit with code 1):
   - Missing `gh` CLI → exit with installation instructions
   - Missing `jbang` → exit with installation instructions
   - Config file corrupted (invalid JSON) → exit with error message
   - No directories enabled and no `--repo` flag → exit with usage hint

3. **User Errors** (exit with usage hint):
   - Invalid flag combination → print usage
   - Invalid `--format` value → list valid formats
   - `--team` with no users → print usage

### Error Message Philosophy

**Clear and Actionable**:
```bash
# Good:
❌ Error: gh CLI not found
   Install: brew install gh (macOS) or https://github.com/cli/cli#installation

# Bad:
Error: command not found: gh
```

**Progressive Disclosure**:
```bash
# First error shows basic info
⚠️  Directory not found: ~/projects/old-feature (skipping)

# Multiple errors show summary at end
⚠️  Skipped 2 directories due to errors. See messages above.
```

---

## Future Enhancement Paths

### v1.1: Configuration Enhancements
- Per-directory custom labels (beyond just `id`)
- Exclude patterns (skip certain files in uncommitted changes)
- Repository groups (e.g., "frontend", "backend")

### v2.0: Advanced Features
- CompletableFuture for parallel GitHub API calls
- GitHub GraphQL API (fewer requests)
- Report retention policy (auto-delete old reports)
- Git-commit reports to dedicated repo (opt-in)
- Multi-remote support (not just `origin`)

### v2.1: Team Features
- Shared configuration (team config repo)
- Slack/Discord integration (post reports automatically)
- Report templates per team/project

---

## Open Technical Questions

1. **Q**: Should ConfigManager use file locking for concurrent writes?
   **A**: Not in v1.0 - Low risk (users rarely run multiple instances concurrently)

2. **Q**: Should we validate git remote URLs (e.g., reject invalid SSH/HTTPS)?
   **A**: Not in v1.0 - Git commands will fail anyway, error message sufficient

3. **Q**: Should ExecutorService thread pool size be configurable?
   **A**: Not in v1.0 - `Math.min(directories.size(), 4)` works for 99% of cases

4. **Q**: Should reports be Markdown, HTML, or JSON by default?
   **A**: Markdown (existing default) - most readable for AI and humans

5. **Q**: Should install.sh support Windows (PowerShell)?
   **A**: Not in v1.0 - Focus on Unix-like systems (macOS, Linux), Windows support in v2.0

---

## References

- **ProcessBuilder Pattern**: Java 11 ProcessBuilder API docs
- **ExecutorService**: Java 11 Concurrent utilities
- **Gson**: Google Gson 2.10.1 documentation
- **Git Commands**: git-diff(1), git-log(1), git-rev-parse(1) man pages
- **JBang**: https://www.jbang.dev/documentation/guide/latest/
