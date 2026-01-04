# Capability: activity-aggregation

## ADDED Requirements

### Requirement: Multi-Directory Orchestration

The system MUST orchestrate data collection across multiple directories and aggregate results into a unified structure.

#### Scenario: Collect data from multiple directories
```
GIVEN config contains 3 enabled directories
WHEN the system aggregates activities
THEN local changes are collected for all 3 directories
AND GitHub activity is collected for each unique repository
AND results are combined into a single JSON output
```

#### Scenario: Skip disabled directories
```
GIVEN config contains 5 directories but 2 are disabled
WHEN the system aggregates activities
THEN only 3 enabled directories are processed
AND disabled directories are ignored
```

#### Scenario: Handle missing directories gracefully
```
GIVEN config contains a directory that no longer exists
WHEN the system aggregates activities
THEN a warning is displayed: "⚠️ Directory not found: <path> (skipping)"
AND the missing directory is excluded
AND remaining directories are processed normally
```

---

### Requirement: GitHub Activity Deduplication

The system MUST deduplicate GitHub activity by repository to minimize API calls.

#### Scenario: Multiple directories, same repository
```
GIVEN 3 directories all point to "owner/myapp" (different branches)
WHEN the system collects GitHub activity
THEN CollectActivity.java is called ONCE for "owner/myapp"
AND the activity is shared across all 3 directories
```

#### Scenario: Multiple directories, different repositories
```
GIVEN 3 directories point to "owner/app1", "owner/app2", "owner/app1" (duplicate)
WHEN the system collects GitHub activity
THEN CollectActivity.java is called TWICE (once for app1, once for app2)
AND duplicate repository calls are avoided
```

#### Scenario: Grouping by repoName
```
GIVEN directories with repoName: ["owner/app1", "owner/app2", "owner/app1"]
WHEN the system groups directories by repository
THEN the groups are: {"owner/app1": [dir1, dir3], "owner/app2": [dir2]}
AND each group is processed once for GitHub activity
```

#### Scenario: GitHub activity includes ALL branches (backward compatibility)
```
GIVEN 2 directories configured for "owner/myapp" on branches "main" and "feature/new-ui"
AND the user has commits on branch "hotfix/bug-123" (NOT in configured directories)
WHEN the system collects GitHub activity for "owner/myapp"
THEN the activity includes commits from ALL branches (main, feature/new-ui, hotfix/bug-123)
AND the activity includes PRs from ANY branch
AND the activity includes issues (repository-scoped, not branch-scoped)
AND the behavior is IDENTICAL to legacy single-directory mode
```

---

### Requirement: Parallel Local Change Detection

The system MUST detect local changes in parallel across directories for performance.

#### Scenario: Parallel execution with ExecutorService
```
GIVEN 5 directories to process
WHEN the system detects local changes
THEN an ExecutorService with thread pool size min(5, 4) is created
AND LocalChangesDetector is called for each directory in parallel
AND results are collected as they complete
```

#### Scenario: Sequential GitHub activity collection
```
GIVEN 3 unique repositories to process
WHEN the system collects GitHub activity
THEN repositories are processed sequentially (not in parallel)
AND each CollectActivity.java call completes before the next starts
```

#### Scenario: Performance improvement from parallelization
```
GIVEN 5 directories, each taking ~2 seconds for local changes
WHEN processed in parallel (4 threads)
THEN total time is ~3 seconds (not 10 seconds sequential)
```

---

### Requirement: Unified JSON Output

The system MUST produce a unified JSON structure combining GitHub activity and local changes.

#### Scenario: Complete aggregated JSON structure
```
GIVEN multiple directories with GitHub activity and local changes
WHEN the system aggregates data
THEN the output JSON structure is:
{
  "githubActivity": {
    "owner/myapp": { /* commits, PRs, issues */ },
    "owner/other": { /* commits, PRs, issues */ }
  },
  "localChanges": [
    { /* directory 1 local changes */ },
    { /* directory 2 local changes */ }
  ],
  "metadata": {
    "user": "octocat",
    "days": 1,
    "directoryCount": 3,
    "repoCount": 2
  }
}
```

#### Scenario: Metadata accuracy
```
GIVEN 5 directories with 3 unique repositories
WHEN the system generates metadata
THEN metadata.directoryCount is 5
AND metadata.repoCount is 3
AND metadata.user is the GitHub username used for activity collection
AND metadata.days is the number of days queried
```

---

### Requirement: Error Aggregation

The system MUST collect errors from child processes and report them clearly.

#### Scenario: Local change detection fails for one directory
```
GIVEN LocalChangesDetector fails for directory "~/old-project" (not found)
WHEN the system aggregates results
THEN a warning is displayed: "⚠️ Failed to collect local changes for ~/old-project"
AND the error is logged to stderr
AND other directories are processed successfully
AND the aggregated output excludes the failed directory
```

#### Scenario: GitHub activity collection fails for one repository
```
GIVEN CollectActivity.java fails for "owner/private-repo" (permission denied)
WHEN the system aggregates results
THEN an error is displayed: "❌ Failed to collect GitHub activity for owner/private-repo"
AND githubActivity for that repo is null or empty
AND other repositories are processed successfully
```

#### Scenario: All directories fail
```
GIVEN all configured directories fail (e.g., all deleted)
WHEN the system aggregates results
THEN an error is displayed: "❌ No valid directories to process"
AND the system falls back to legacy single-directory mode (if --repo provided)
OR exits with error if no fallback available
```

---

### Requirement: Process Management

The system MUST manage child processes (LocalChangesDetector, CollectActivity) reliably.

#### Scenario: Call LocalChangesDetector via ProcessBuilder
```
GIVEN a directory to process
WHEN the system calls LocalChangesDetector
THEN it executes: jbang scripts/LocalChangesDetector.java <directoryId> <path> <branch>
AND captures stdout for JSON output
AND captures stderr for warnings/errors
AND checks exit code (0 = success, non-zero = failure)
```

#### Scenario: Call CollectActivity via ProcessBuilder
```
GIVEN a repository to query
WHEN the system calls CollectActivity
THEN it executes: jbang scripts/CollectActivity.java <user> <days> <repo>
AND captures stdout for JSON output
AND captures stderr for warnings/errors
AND checks exit code for success
```

#### Scenario: Timeout handling for child processes
```
GIVEN a child process hangs for >60 seconds
WHEN the system waits for completion
THEN the process is terminated forcibly
AND a warning is displayed: "⚠️ Process timeout (skipping)"
AND aggregation continues with remaining directories
```

---

### Requirement: Configuration Integration

The system MUST load directories from ConfigManager and filter them appropriately.

#### Scenario: Load directories from config
```
GIVEN config.json contains 5 directories
WHEN the system aggregates activities
THEN ConfigManager.loadConfig() is called
AND all directories are loaded from config.directories
```

#### Scenario: Filter enabled directories only
```
GIVEN config contains 3 enabled and 2 disabled directories
WHEN the system filters directories
THEN only 3 enabled directories are processed
AND disabled directories are skipped silently (no warning)
```

#### Scenario: Validate directory paths exist
```
GIVEN config contains a directory path "~/projects/deleted"
AND that path does not exist on disk
WHEN the system filters directories
THEN a warning is displayed: "⚠️ Directory not found: ~/projects/deleted (skipping)"
AND that directory is excluded from processing
```

---

### Requirement: Performance Requirements

The system MUST meet performance targets for multi-directory aggregation.

#### Scenario: 3 directories (same repo) complete in <15 seconds
```
GIVEN 3 directories for the same repository
WHEN the system aggregates activities
THEN total time is <15 seconds
AND GitHub activity is fetched ONCE (2-3 seconds)
AND local changes are detected in parallel (~2-3 seconds)
AND Claude AI generation takes the remaining time (7-12 seconds)
```

#### Scenario: 5 directories (3 repos) complete in <30 seconds
```
GIVEN 5 directories across 3 unique repositories
WHEN the system aggregates activities
THEN total time is <30 seconds
AND GitHub activity is fetched 3 times (~6-9 seconds total)
AND local changes are detected in parallel (~2-3 seconds)
```

#### Scenario: Parallel speedup verification
```
GIVEN N directories to process
WHEN local changes are detected in parallel
THEN speedup is approximately min(N, 4) / 1 (e.g., 4x for 4+ directories)
```
