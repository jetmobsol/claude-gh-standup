# Capability: local-change-detection

## ADDED Requirements

### Requirement: Uncommitted Change Detection

The system MUST detect uncommitted changes (staged and unstaged files) in each configured directory.

#### Scenario: Detect unstaged changes
```
GIVEN a directory has 2 modified files that are unstaged
WHEN the system detects local changes
THEN the output includes "unstaged": ["file1.java", "file2.java"]
AND hasChanges is true
```

#### Scenario: Detect staged changes
```
GIVEN a directory has 1 file staged for commit
WHEN the system detects local changes
THEN the output includes "staged": ["file3.java"]
AND hasChanges is true
```

#### Scenario: Detect both staged and unstaged changes
```
GIVEN a directory has 2 staged files and 1 unstaged file
WHEN the system detects local changes
THEN "staged" contains the 2 staged files
AND "unstaged" contains the 1 unstaged file
AND filesChanged is 3
```

#### Scenario: No uncommitted changes
```
GIVEN a directory has a clean working tree
WHEN the system detects local changes
THEN hasChanges is false
AND staged is an empty array
AND unstaged is an empty array
```

---

### Requirement: Unpushed Commit Detection

The system MUST detect commits that exist locally but have not been pushed to the remote branch.

#### Scenario: Detect unpushed commits
```
GIVEN a directory has 2 commits ahead of origin/main
WHEN the system detects local changes
THEN unpushed.hasCommits is true
AND unpushed.count is 2
AND unpushed.commits contains the commit hashes and messages
```

#### Scenario: No unpushed commits
```
GIVEN a directory is up-to-date with origin/main
WHEN the system detects local changes
THEN unpushed.hasCommits is false
AND unpushed.count is 0
AND unpushed.commits is an empty array
```

#### Scenario: Local-only branch (no remote tracking)
```
GIVEN a directory is on a branch with no remote counterpart
WHEN the system detects local changes
THEN a warning is displayed: "No remote branch 'origin/branch-name'"
AND unpushed.hasCommits is false (cannot determine)
AND the script continues without error
```

#### Scenario: Detached HEAD state
```
GIVEN a directory is in detached HEAD state
WHEN the system detects local changes
THEN a warning is displayed: "Detached HEAD state"
AND unpushed check is skipped
AND uncommitted changes are still detected
```

---

### Requirement: Change Summary Statistics

The system MUST provide a human-readable summary of changes.

#### Scenario: Generate summary for uncommitted changes
```
GIVEN a directory has 3 files changed with 45 insertions and 12 deletions
WHEN the system generates a summary
THEN summary is "3 files changed, 45 insertions(+), 12 deletions(-)"
```

#### Scenario: Summary for only additions
```
GIVEN a directory has 2 new files with 100 insertions and 0 deletions
WHEN the system generates a summary
THEN summary is "2 files changed, 100 insertions(+)"
```

#### Scenario: Summary for only deletions
```
GIVEN a directory has 1 file with 0 insertions and 50 deletions
WHEN the system generates a summary
THEN summary is "1 file changed, 50 deletions(-)"
```

---

### Requirement: JSON Output Format

The system MUST output local change information in a structured JSON format.

#### Scenario: Complete JSON output structure
```
GIVEN a directory has uncommitted and unpushed changes
WHEN the system outputs local change data
THEN the JSON structure is:
{
  "directoryId": "project-main",
  "path": "/Users/garden/projects/myapp",
  "branch": "main",
  "uncommitted": {
    "hasChanges": true,
    "filesChanged": 3,
    "staged": ["file1.java", "file2.java"],
    "unstaged": ["file3.java"],
    "summary": "3 files changed, 45 insertions(+), 12 deletions(-)"
  },
  "unpushed": {
    "hasCommits": true,
    "count": 2,
    "commits": [
      "abc1234 Fix bug in login flow",
      "def5678 Add validation"
    ]
  }
}
```

#### Scenario: JSON output with no changes
```
GIVEN a directory has no uncommitted or unpushed changes
WHEN the system outputs local change data
THEN the JSON structure is:
{
  "directoryId": "project-main",
  "path": "/Users/garden/projects/myapp",
  "branch": "main",
  "uncommitted": {
    "hasChanges": false,
    "filesChanged": 0,
    "staged": [],
    "unstaged": [],
    "summary": ""
  },
  "unpushed": {
    "hasCommits": false,
    "count": 0,
    "commits": []
  }
}
```

---

### Requirement: Git Command Integration

The system MUST use standard git commands to detect changes.

#### Scenario: Use git diff for unstaged changes
```
GIVEN a directory with unstaged changes
WHEN the system detects local changes
THEN it executes: git -C <path> diff --stat
AND parses the output for file names and statistics
```

#### Scenario: Use git diff --cached for staged changes
```
GIVEN a directory with staged changes
WHEN the system detects local changes
THEN it executes: git -C <path> diff --cached --stat
AND parses the output for file names and statistics
```

#### Scenario: Use git log for unpushed commits
```
GIVEN a directory with unpushed commits
WHEN the system detects local changes
THEN it executes: git -C <path> log origin/<branch>..HEAD --oneline --format="%h %s"
AND parses the output for commit hashes and messages
```

#### Scenario: Check remote branch existence
```
GIVEN a directory on a branch with remote tracking
WHEN the system checks for unpushed commits
THEN it first executes: git -C <path> rev-parse --verify origin/<branch>
AND only checks unpushed commits if the remote branch exists
```

---

### Requirement: Error Handling

The system MUST handle git errors gracefully without crashing.

#### Scenario: Git permission denied
```
GIVEN a directory where git commands fail with "Permission denied"
WHEN the system detects local changes
THEN a warning is displayed: "⚠️ Permission denied for <path> (skipping local changes)"
AND the directory is excluded from the report
AND the script continues with remaining directories
```

#### Scenario: Not a git repository
```
GIVEN a configured directory that is not a git repository
WHEN the system detects local changes
THEN a warning is displayed: "⚠️ Not a git repository: <path> (skipping)"
AND the directory is excluded from the report
```

#### Scenario: Git command timeout
```
GIVEN a git command hangs for >30 seconds
WHEN the system detects local changes
THEN the command is terminated
AND a warning is displayed: "⚠️ Git command timeout for <path> (skipping)"
AND the script continues with remaining directories
```

---

### Requirement: Performance Optimization

The system MUST execute local change detection efficiently for multiple directories.

#### Scenario: Parallel execution support
```
GIVEN ActivityAggregator calls LocalChangesDetector for 5 directories
WHEN local change detection is performed
THEN each directory is processed independently (no shared state)
AND the script can be called in parallel from different processes
AND results are output to stdout without interference
```

#### Scenario: Fast execution for clean directories
```
GIVEN a directory with no changes (clean working tree)
WHEN the system detects local changes
THEN the check completes in <1 second
AND only essential git commands are executed
```

#### Scenario: Efficient parsing of git output
```
GIVEN git commands return large output (100+ files changed)
WHEN the system parses the output
THEN only file names are extracted (not full diff content)
AND memory usage remains low (<100MB per directory)
```
