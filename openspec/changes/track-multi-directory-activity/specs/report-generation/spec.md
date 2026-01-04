# Capability: report-generation

## ADDED Requirements

### Requirement: Multi-Directory Prompt Template

The system MUST provide a prompt template specifically for multi-directory standup reports.

#### Scenario: Multi-directory template structure
```
GIVEN a new file prompts/multidir-standup.prompt.md exists
WHEN the template is loaded
THEN it contains placeholders for:
  - {{githubActivity}} - GitHub activity grouped by repository
  - {{localChanges}} - Local changes grouped by directory/branch
  - {{user}} - GitHub username
  - {{days}} - Number of days queried
  - {{directoryCount}} - Number of directories tracked
  - {{repoCount}} - Number of unique repositories
```

#### Scenario: Template emphasizes local work-in-progress
```
GIVEN the multi-directory template
WHEN it is used to generate a report
THEN the prompt instructs Claude to:
  1. Synthesize GitHub activity across all repositories
  2. Highlight local work-in-progress per directory/branch
  3. Group related activities logically
  4. Emphasize context switching if relevant
```

---

### Requirement: Multi-Directory Activity Formatting

The system MUST format aggregated activity data for the multi-directory prompt.

#### Scenario: Format GitHub activity by repository
```
GIVEN aggregated JSON with GitHub activity for 2 repositories
WHEN the system formats the activity
THEN the output is grouped by repository:

## GitHub Activity (owner/myapp)
- 3 commits pushed
- 2 pull requests merged
- 1 issue closed

## GitHub Activity (owner/other)
- 1 commit pushed
- 1 pull request opened
```

#### Scenario: Format local changes by directory
```
GIVEN aggregated JSON with local changes for 3 directories
WHEN the system formats the local changes
THEN the output is grouped by directory:

## Local Changes (~/projects/myapp/main - branch: main)
### Uncommitted Changes
- Staged: file1.java, file2.java
- Unstaged: file3.java
- Summary: 3 files changed, 45 insertions(+), 12 deletions(-)

### Unpushed Commits
- abc1234 Fix bug in login flow
- def5678 Add validation

## Local Changes (~/projects/myapp/feature - branch: feature/new-ui)
### Uncommitted Changes
- None

### Unpushed Commits
- None
```

#### Scenario: Format metadata section
```
GIVEN aggregated JSON with metadata
WHEN the system formats metadata
THEN the output includes:

## Report Metadata
- User: octocat
- Date Range: Last 3 days (2026-01-01 to 2026-01-04)
- Directories Tracked: 3
- Repositories: 2 (owner/myapp, owner/other)
```

---

### Requirement: Enhanced Report Sections

The system MUST structure multi-directory reports with enhanced sections.

#### Scenario: Report includes all standard sections
```
GIVEN a multi-directory report is generated
WHEN Claude AI produces the output
THEN the report contains sections:
  1. Yesterday's Accomplishments (GitHub activity)
  2. Work in Progress (Local changes per branch)
  3. Today's Plans
  4. Blockers/Challenges
```

#### Scenario: WIP section highlights branch context
```
GIVEN local changes exist in 2 directories on different branches
WHEN the WIP section is generated
THEN each branch's work is clearly labeled:

## Work in Progress

### Main Branch (~/projects/myapp/main)
- Refactoring authentication module (uncommitted)
- 3 files changed, 45 insertions(+)

### Feature Branch (~/projects/myapp/feature)
- Working on new UI component (2 unpushed commits)
- Commits: abc1234, def5678
```

#### Scenario: Cross-branch collaboration highlighted
```
GIVEN changes in multiple branches relate to the same feature
WHEN the report is generated
THEN Claude identifies the relationship:

## Work in Progress
- **Cross-branch work**: Authentication refactor spans main and feature branches
  - Main: Backend changes (uncommitted)
  - Feature: Frontend integration (2 unpushed commits)
```

---

## MODIFIED Requirements

### Requirement: Template Selection Logic (MODIFIED)

The system MUST select the appropriate prompt template based on execution mode.

#### Scenario: Use multi-directory template in multi-dir mode
```
GIVEN multi-directory mode is active
WHEN the system generates a report
THEN prompts/multidir-standup.prompt.md is used
```

#### Scenario: Use single-directory template in legacy mode
```
GIVEN legacy single-directory mode is active
WHEN the system generates a report
THEN prompts/standup.prompt.md is used (existing template)
```

---

### Requirement: Prompt Injection (MODIFIED)

The prompt injection logic MUST handle both single-directory and multi-directory data.

#### Scenario: Inject multi-directory data
```
GIVEN aggregated JSON from ActivityAggregator
WHEN the system injects data into the prompt
THEN {{githubActivity}} is replaced with formatted GitHub activity per repo
AND {{localChanges}} is replaced with formatted local changes per directory
AND {{user}}, {{days}}, {{directoryCount}}, {{repoCount}} are replaced with metadata values
```

#### Scenario: Inject single-directory data (unchanged)
```
GIVEN activity JSON from CollectActivity and AnalyzeDiffs
WHEN the system injects data into the prompt
THEN {{activities}}, {{diffs}}, {{user}}, {{days}} are replaced (existing behavior)
```

---

### Requirement: Claude AI Integration (UNCHANGED)

The existing Claude AI integration via `claude -p` MUST continue to work with new prompt templates.

#### Scenario: Generate report with multi-directory prompt
```
GIVEN a multi-directory prompt is prepared
WHEN the system calls Claude AI
THEN it executes: claude -p '<full-prompt>'
AND the output is streamed to stdout in real-time
AND the report is formatted as Markdown
```

#### Scenario: Streaming output works identically
```
GIVEN GenerateReport.java is called with a multi-directory prompt
WHEN Claude AI generates the report
THEN output is streamed line-by-line to stdout (unchanged behavior)
AND users see progress in real-time
```

---

### Requirement: Report Content Quality (ADDED)

The multi-directory reports MUST provide clear, actionable insights.

#### Scenario: Report identifies context switching
```
GIVEN the user worked on 3 different branches in one day
WHEN the report is generated
THEN Claude identifies context switching as a potential blocker or challenge
```

#### Scenario: Report groups related work
```
GIVEN commits in multiple repos relate to the same feature
WHEN the report is generated
THEN Claude groups them logically under a single accomplishment:

## Yesterday's Accomplishments
- **Implemented authentication flow** (cross-repo work)
  - Backend API (owner/api-service): 3 commits
  - Frontend (owner/web-app): 2 commits, 1 PR merged
```

#### Scenario: Report highlights WIP across branches
```
GIVEN uncommitted changes in 2 branches
WHEN the report is generated
THEN Claude surfaces the WIP prominently in the "Work in Progress" section
AND provides clear next steps if inferable from commit messages
```
