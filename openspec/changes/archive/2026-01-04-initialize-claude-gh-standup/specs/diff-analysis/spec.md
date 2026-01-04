# Capability: Diff Analysis

## ADDED Requirements

### Requirement: Pull Request Diff Collection
The system SHALL collect detailed file diffs for pull requests using `gh pr diff`.

#### Scenario: PR diff collected successfully
- **WHEN** system has PR number 123 from activity collection
- **THEN** executes `gh pr diff 123 -R owner/repo`
- **AND** captures unified diff output
- **AND** parses to extract changed files, additions, deletions

#### Scenario: PR diff unavailable
- **WHEN** `gh pr diff` returns non-zero exit code
- **THEN** logs warning and skips that PR
- **AND** continues processing remaining PRs

### Requirement: Commit Diff Collection
The system SHALL collect file diffs for individual commits using `git diff`.

#### Scenario: Commit diff collected
- **WHEN** system has commit SHA from activity collection
- **THEN** executes `git diff {sha}^..{sha}`
- **AND** parses diff output for file changes

### Requirement: Diff Parsing
The system SHALL parse unified diff format to extract file change statistics.

#### Scenario: Parse file paths from diff
- **WHEN** diff line starts with `diff --git a/file.java b/file.java`
- **THEN** extracts file path "file.java"
- **AND** initializes counters for additions/deletions

#### Scenario: Count line additions
- **WHEN** diff line starts with `+` but not `+++`
- **THEN** increments additions counter for current file

#### Scenario: Count line deletions
- **WHEN** diff line starts with `-` but not `---`
- **THEN** increments deletions counter for current file

### Requirement: Diff Summary Statistics
The system SHALL aggregate file change statistics across all diffs.

#### Scenario: Summarize changes
- **WHEN** all diffs are parsed
- **THEN** returns summary with:
  - Total files changed
  - Total lines added
  - Total lines deleted
  - List of file paths with per-file stats

#### Scenario: Empty diff handling
- **WHEN** no diffs available (no PRs or commits)
- **THEN** returns empty summary:
  - `{"files_changed": 0, "total_additions": 0, "total_deletions": 0, "files": []}`

### Requirement: Diff Context for AI
The system SHALL format diff summaries for Claude AI prompt injection.

#### Scenario: Format for prompt template
- **WHEN** diff summary is ready
- **THEN** formats as human-readable text:
```
Files changed: 12
Lines added: 347
Lines deleted: 89

Modified files:
- src/Main.java (+45, -12)
- src/CollectActivity.java (+123, -34)
- ...
```
- **AND** injects into `{{diffs}}` placeholder in prompt template
