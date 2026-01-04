# Capability: activity-aggregation

## MODIFIED Requirements

### Requirement: GitHub Activity Deduplication

The system MUST collect GitHub activity across ALL user repositories, not filtered by configured directories.

#### Scenario: Fetch ALL repositories (not just configured)
- **WHEN** multi-directory mode collects GitHub activity
- **THEN** CollectActivity.java is called ONCE with `repo = null` (no repository filter)
- **AND** GitHub returns activity from ALL repositories the user worked on
- **AND** the behavior is identical to legacy single-directory mode

#### Scenario: Multiple directories, same repository
- **GIVEN** 3 directories all point to "owner/myapp" (different branches)
- **WHEN** the system collects GitHub activity
- **THEN** CollectActivity.java is called ONCE with `repo = null`
- **AND** the activity includes ALL user repositories (not just owner/myapp)

#### Scenario: Multiple directories, different repositories
- **GIVEN** 3 directories point to "owner/app1", "owner/app2", "owner/app1" (duplicate)
- **WHEN** the system collects GitHub activity
- **THEN** CollectActivity.java is called ONCE with `repo = null`
- **AND** the activity includes app1, app2, AND any other repos the user worked on

#### Scenario: GitHub activity includes ALL repositories
- **GIVEN** user has configured directories for "owner/app1" only
- **AND** user also worked on "owner/app2" and "owner/app3" yesterday
- **WHEN** the system collects GitHub activity
- **THEN** the activity includes commits, PRs, and issues from app1, app2, AND app3
- **AND** no user activity is filtered out

#### Scenario: Performance improvement from single API call
- **GIVEN** 5 directories across 3 unique repositories
- **WHEN** the system collects GitHub activity
- **THEN** CollectActivity.java is called ONCE (not 3 times)
- **AND** total GitHub API time is reduced from 3× calls to 1× call

## MODIFIED Requirements

### Requirement: Unified JSON Output

The system MUST produce a unified JSON structure with GitHub activity as a single object (not per-repo map).

#### Scenario: Complete aggregated JSON structure
- **GIVEN** multiple directories with GitHub activity and local changes
- **WHEN** the system aggregates data
- **THEN** the output JSON structure is:
```json
{
  "githubActivity": {
    "commits": [...],
    "pull_requests": [...],
    "issues": [...]
  },
  "localChanges": [
    { /* directory 1 local changes */ },
    { /* directory 2 local changes */ }
  ],
  "metadata": {
    "user": "octocat",
    "days": 1,
    "directoryCount": 3,
    "configuredRepos": ["owner/app1", "owner/app2"]
  }
}
```

#### Scenario: Metadata includes configured repos
- **GIVEN** 5 directories with 3 unique repositories configured
- **WHEN** the system generates metadata
- **THEN** metadata.directoryCount is 5
- **AND** metadata.configuredRepos is ["owner/app1", "owner/app2", "owner/app3"]
- **AND** metadata.user is the GitHub username
- **AND** githubActivity contains ALL user repos (not just configured ones)

## ADDED Requirements

### Requirement: All-Repository Activity Collection

The system MUST provide a method to collect GitHub activity across ALL user repositories without filtering.

#### Scenario: Call collectGitHubActivityAllRepos
- **GIVEN** multi-directory mode is active
- **WHEN** aggregateActivities() is called
- **THEN** collectGitHubActivityAllRepos(user, days) is invoked
- **AND** it calls CollectActivity.java with repo parameter as null
- **AND** the result contains activity from ALL repositories

#### Scenario: Graceful error handling for GitHub API failures
- **GIVEN** CollectActivity.java fails (rate limit, network error)
- **WHEN** collectGitHubActivityAllRepos() is called
- **THEN** a warning is displayed: "⚠️ Failed to collect GitHub activity: <error>"
- **AND** an empty activity object is returned (not null)
- **AND** the system continues with local changes collection

#### Scenario: Empty activity structure on error
- **GIVEN** GitHub activity collection fails
- **WHEN** the error handler creates an empty activity object
- **THEN** the object contains:
```json
{
  "commits": [],
  "pull_requests": [],
  "issues": []
}
```

## REMOVED Requirements

### Requirement: GitHub Activity Deduplication (OLD VERSION)

**Reason**: This requirement is being replaced by the MODIFIED version above. The old version filtered GitHub activity by configured repositories, which violated the tool's purpose.

**Migration**: Code changes in ActivityAggregator.java:
- Remove `collectGitHubActivity(Map<String, List<Directory>> repoMap, ...)` method
- Add `collectGitHubActivityAllRepos(String user, int days)` method
- Update `aggregateActivities()` to call the new method
