# Change: Fix Multi-Directory Mode GitHub Activity Filtering

## Why

Multi-directory mode currently filters GitHub activity to ONLY repositories configured in directories, missing activity from other repos the user worked on. This violates the tool's core purpose: showing ALL user activity across ALL repositories.

**Example of Current Broken Behavior:**
- User has `tac-2` configured in multi-directory mode
- User also worked on `claude-gh-standup` repo yesterday
- Report shows: "No commits or pull requests" (only checked tac-2)
- **Missing**: All activity from claude-gh-standup repo

**Root Cause:**
`ActivityAggregator.java:109` passes `repoMap` (repos from config) to `collectGitHubActivity()`, which filters GitHub queries to only those repositories.

## What Changes

- **BREAKING**: Change `ActivityAggregator.collectGitHubActivity()` to fetch ALL user activity (not filtered by configured repos)
- Add new method `collectGitHubActivityAllRepos()` that calls `CollectActivity` WITHOUT repo parameter
- Update `AggregatedActivity` JSON structure: `githubActivity` becomes a single object (not per-repo map)
- Update `Main.java` to group GitHub activities by repository name for display
- Update `prompts/multidir-standup.prompt.md` to clarify scope (ALL repos vs configured dirs)
- Update documentation to explain the two-tier approach:
  - **GitHub Activity**: ALL repositories (like legacy mode)
  - **Local WIP**: Only configured directories

## Impact

**Affected Specs:**
- `activity-aggregation` - Core logic change to GitHub activity collection
- `report-generation` - Formatting change for grouped-by-repo display

**Affected Code:**
- `scripts/ActivityAggregator.java` (lines 95-119, 188-206) - New method and logic change
- `scripts/Main.java` (lines 496-588, new method) - Formatting change for multi-dir reports
- `prompts/multidir-standup.prompt.md` (lines 24-42) - Template clarification
- `scripts/CollectActivity.java` - NO CHANGE (already supports `repo == null`)

**Benefits:**
- Restores tool's original purpose (show all user activity)
- Multi-directory mode ADDS local visibility without SUBTRACTING GitHub visibility
- Better performance (1 API call vs N calls when N > 1)
- Consistent with legacy single-directory mode behavior

**Risks:**
- Larger prompts with more repos (mitigated by Claude's large context window)
- Need to ensure Main.java grouping logic handles all repo formats correctly

**Backward Compatibility:**
- Config format: NO CHANGE
- CLI flags: NO CHANGE
- Output format: ENHANCED (more complete, not breaking)
- Legacy mode: NO CHANGE (doesn't use ActivityAggregator)
