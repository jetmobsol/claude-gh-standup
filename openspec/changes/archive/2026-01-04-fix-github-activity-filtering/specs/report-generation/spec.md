# Capability: report-generation

## MODIFIED Requirements

### Requirement: Multi-Directory Activity Formatting

The system MUST format GitHub activity grouped by repository name (extracted from activity data, not from config).

#### Scenario: Group GitHub activities by repository
- **GIVEN** aggregated JSON with GitHub activity from 3 repositories
- **WHEN** the system formats the activity for the prompt
- **THEN** activities are grouped by repository name extracted from each activity item
- **AND** the output includes a heading for each repository:
```markdown
### owner/app1

- Commit: Fix authentication bug (abc1234)
- PR #123: Add user validation

### owner/app2

- Commit: Update dependencies (def5678)
- Issue #45: Database migration fails

### owner/app3

- PR #67: Refactor API handlers
```

#### Scenario: Extract repository from commit data
- **GIVEN** a commit object with `repository.nameWithOwner = "owner/myapp"`
- **WHEN** the system groups activities
- **THEN** this commit is added to the "owner/myapp" group
- **AND** the repository name is extracted from the commit data (not from config)

#### Scenario: Extract repository from PR data
- **GIVEN** a PR object with `repository.nameWithOwner = "owner/other"`
- **WHEN** the system groups activities
- **THEN** this PR is added to the "owner/other" group

#### Scenario: Extract repository from issue data
- **GIVEN** an issue object with `repository.nameWithOwner = "owner/tools"`
- **WHEN** the system groups activities
- **THEN** this issue is added to the "owner/tools" group

#### Scenario: Sort repositories alphabetically
- **GIVEN** activities from 4 repositories: "c/repo", "a/repo", "b/repo", "a/other"
- **WHEN** the system formats the output
- **THEN** repositories are displayed in order: "a/other", "a/repo", "b/repo", "c/repo"

#### Scenario: Handle repositories not in config
- **GIVEN** GitHub activity includes "owner/untracked-repo" (not in configured directories)
- **WHEN** the system formats activities
- **THEN** "owner/untracked-repo" is included in the grouped output
- **AND** it appears alongside configured repositories

## MODIFIED Requirements

### Requirement: Multi-Directory Prompt Template

The system MUST clarify that GitHub activity includes ALL user repositories, not just configured ones.

#### Scenario: Template clarifies scope
- **GIVEN** the multi-directory prompt template
- **WHEN** the template is rendered
- **THEN** it contains text: "GitHub activity from **ALL YOUR REPOSITORIES** (not limited to configured directories)"
- **AND** it explains that local WIP is limited to configured directories

#### Scenario: Metadata section notes repository scope
- **GIVEN** the template's metadata section
- **WHEN** the template is rendered
- **THEN** it includes a note: "**Note**: GitHub activity includes ALL repositories, not just those configured above."

## ADDED Requirements

### Requirement: Repository Grouping Helper

The system MUST provide a helper method to group activities by repository name.

#### Scenario: formatActivitiesGroupedByRepo method
- **GIVEN** a JsonObject containing commits, PRs, and issues
- **WHEN** formatActivitiesGroupedByRepo(activity) is called
- **THEN** it returns a formatted string with activities grouped by repository
- **AND** each repository has its own section with heading

#### Scenario: Combine all activity types per repository
- **GIVEN** "owner/myapp" has 2 commits, 1 PR, and 1 issue
- **WHEN** the grouping helper processes the data
- **THEN** all 4 items appear under the "owner/myapp" heading
- **AND** they are ordered: commits first, then PRs, then issues

#### Scenario: Handle empty activity
- **GIVEN** GitHub activity is empty (no commits, PRs, or issues)
- **WHEN** formatActivitiesGroupedByRepo(activity) is called
- **THEN** it returns "No GitHub activity in the last N days"

#### Scenario: Handle missing repository field gracefully
- **GIVEN** an activity item is missing the `repository.nameWithOwner` field
- **WHEN** the grouping helper processes it
- **THEN** it skips that item with a warning to stderr
- **AND** other items are processed normally
