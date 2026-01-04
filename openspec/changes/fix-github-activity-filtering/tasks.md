# Implementation Tasks: fix-github-activity-filtering

## 1. Fix ActivityAggregator.java - GitHub Activity Collection

- [ ] 1.1 Create new method `collectGitHubActivityAllRepos(String user, int days)` in ActivityAggregator.java
  - Returns JsonObject with commits, PRs, and issues from ALL repositories
  - Calls `callCollectActivity(user, days, null)` with null repo parameter
  - Includes try-catch with graceful error handling (returns empty activity on failure)
  - Prints stderr warning on failure: "⚠️ Failed to collect GitHub activity: <message>"

- [ ] 1.2 Update `aggregateActivities()` method in ActivityAggregator.java (lines 95-119)
  - Replace `JsonObject githubActivity = collectGitHubActivity(repoMap, user, days);`
  - With `JsonObject githubActivity = collectGitHubActivityAllRepos(user, days);`
  - Keep local changes collection unchanged

- [ ] 1.3 Update `AggregatedActivity` class in ActivityAggregator.java (lines 30-34)
  - Change `JsonObject githubActivity` structure from per-repo map to single object
  - Add `JsonArray configuredRepos` field to metadata showing which repos have local tracking
  - Populate `configuredRepos` from `repoMap.keySet()`

- [ ] 1.4 Remove obsolete `collectGitHubActivity(Map<String, List<Directory>> repoMap, ...)` method
  - Delete lines 188-206 (old method)
  - Ensure no other code references this method

## 2. Update Main.java - Group GitHub Activity by Repository

- [ ] 2.1 Create new helper method `formatActivitiesGroupedByRepo(JsonObject activity)` in Main.java
  - Takes GitHubactivity JSON (commits, PRs, issues arrays)
  - Groups each item by `repository.nameWithOwner` field
  - Returns formatted string with Markdown headings per repository
  - Handles missing `repository` field gracefully (skip with stderr warning)
  - Sorts repositories alphabetically

- [ ] 2.2 Add required imports to Main.java
  - `import java.util.HashMap;`
  - `import java.util.Map;`
  - `import java.util.Collections;`
  - `import java.util.ArrayList;`

- [ ] 2.3 Update `formatMultiDirPrompt()` method in Main.java (lines 506-517)
  - Replace existing GitHub activity formatting code
  - Call `formatActivitiesGroupedByRepo(githubActivity)` instead
  - Store result in `{{githubActivity}}` placeholder

- [ ] 2.4 Test formatting with various GitHub activity structures
  - Empty activity (no commits/PRs/issues)
  - Single repository
  - Multiple repositories (3-5 repos)
  - Missing repository field in some items

## 3. Update Prompt Template

- [ ] 3.1 Update prompts/multidir-standup.prompt.md (line 26 area)
  - Change wording from "activity from GitHub across all tracked repositories"
  - To "activity from GitHub across **ALL YOUR REPOSITORIES** (not limited to configured directories)"

- [ ] 3.2 Add metadata clarification to prompts/multidir-standup.prompt.md
  - Add note in metadata section: "**Note**: GitHub activity includes ALL repositories, not just those configured above."

## 4. Update Documentation

- [ ] 4.1 Update .claude/commands/claude-gh-standup.md
  - Clarify multi-directory mode description
  - Explain two-tier approach: ALL repos (GitHub) vs configured dirs (local WIP)

- [ ] 4.2 Update README.md
  - Update feature list to clarify "ALL repositories" for GitHub activity
  - Update multi-directory example to show this behavior

- [ ] 4.3 Update CLAUDE.md (if relevant sections exist)
  - Document the two-tier approach in development guidance

## 5. Testing and Validation

- [ ] 5.1 Test multi-dir mode shows ALL GitHub activity (not just configured repos)
  - Configure 1 directory for repo A
  - Work on repo B yesterday
  - Verify report includes both repo A and repo B activities

- [ ] 5.2 Test multi-dir mode shows only configured dirs in Local WIP section
  - Configure 2 directories
  - Verify local changes section only shows those 2 directories

- [ ] 5.3 Test report clearly separates GitHub (all) from Local (config)
  - Verify heading structure makes the distinction clear

- [ ] 5.4 Test legacy mode unchanged (no config or explicit --repo)
  - Verify single-directory mode still works
  - Verify explicit --repo flag behavior unchanged

- [ ] 5.5 Test error handling graceful (no GitHub activity case)
  - Simulate GitHub API failure
  - Verify report continues with local changes

- [ ] 5.6 Test performance improved (1 API call vs N calls)
  - Time multi-dir mode with 3 repos before change
  - Time multi-dir mode with 3 repos after change
  - Verify reduction in GitHub API calls

## 6. Code Quality

- [ ] 6.1 Verify code follows project conventions
  - 4-space indentation
  - PascalCase for classes, camelCase for methods
  - Clear error messages to stderr

- [ ] 6.2 Add code comments for new methods
  - Document `collectGitHubActivityAllRepos()` purpose
  - Document `formatActivitiesGroupedByRepo()` logic

- [ ] 6.3 Ensure JBang scripts still executable
  - Test `jbang scripts/ActivityAggregator.java` directly
  - Verify dependencies correctly declared

## Dependencies

- Task 2.1 depends on Task 1.3 (needs new JSON structure)
- Task 5.1-5.6 depend on Tasks 1-4 (all implementation complete)
- Tasks can be parallelized within each section (1.x, 2.x, 3.x, 4.x can be done in parallel)
