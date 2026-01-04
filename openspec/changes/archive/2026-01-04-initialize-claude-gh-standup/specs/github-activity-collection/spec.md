# Capability: GitHub Activity Collection

## ADDED Requirements

### Requirement: Commit Search via gh CLI
The system SHALL collect user commits using `gh search commits` API for a specified date range.

#### Scenario: Single user commits collected
- **WHEN** user requests commits for username "octocat" for last 3 days
- **THEN** system executes `gh search commits --author=octocat --committer-date=>{date} --json sha,commit,repository --limit 1000`
- **AND** returns JSON array of commit objects with sha, message, repository

#### Scenario: No commits found
- **WHEN** user has no commits in specified date range
- **THEN** system returns empty JSON array `[]`
- **AND** continues processing without error

#### Scenario: GitHub API failure
- **WHEN** `gh search commits` returns non-zero exit code
- **THEN** system logs warning "Commit search failed"
- **AND** returns empty array to allow graceful degradation

### Requirement: Pull Request Collection
The system SHALL collect user pull requests using `gh search prs` for a specified date range.

#### Scenario: User PRs collected
- **WHEN** user requests PRs for username "octocat" for last 7 days
- **THEN** system executes `gh search prs --author=octocat --created=>{date} --json number,title,state,repository`
- **AND** returns JSON array of PR objects

#### Scenario: Filter by repository
- **WHEN** user specifies `--repo owner/repo` flag
- **THEN** system adds `-R owner/repo` to gh command
- **AND** returns only PRs from that repository

### Requirement: Issue Collection
The system SHALL collect user issues using `gh search issues` for a specified date range.

#### Scenario: User issues collected
- **WHEN** user requests issues for username "octocat"
- **THEN** system executes `gh search issues --author=octocat --created=>{date} --json number,title,state,repository`
- **AND** returns JSON array of issue objects

### Requirement: Code Review Activity
The system SHALL collect user code review activity using `gh api` for pull request reviews.

#### Scenario: Review activity collected
- **WHEN** user requests review activity
- **THEN** system executes `gh api user/repos` to get repositories
- **AND** for each repository executes `gh api repos/{owner}/{repo}/pulls/{pr}/reviews`
- **AND** filters reviews by username and date range
- **AND** returns JSON array of review objects

### Requirement: Authenticated User Detection
The system SHALL detect the currently authenticated GitHub user when no `--user` flag is provided.

#### Scenario: Auto-detect current user
- **WHEN** user invokes `/claude-gh-standup` without `--user` flag
- **THEN** system executes `gh api user --jq .login`
- **AND** uses returned username for activity collection

### Requirement: Date Range Calculation
The system SHALL calculate ISO 8601 date strings for GitHub API queries based on `--days` parameter.

#### Scenario: Calculate 3 days ago
- **WHEN** user specifies `--days 3`
- **THEN** system calculates `LocalDate.now().minusDays(3)`
- **AND** formats as ISO date (e.g., "2026-01-01")
- **AND** uses in `--committer-date=>2026-01-01` queries

### Requirement: JSON Output Format
The system SHALL return all activity data as valid JSON for downstream processing.

#### Scenario: Valid JSON structure
- **WHEN** activity collection completes
- **THEN** output is parsable by Gson library
- **AND** contains arrays of commits, PRs, issues, reviews
- **AND** each object has consistent field names
