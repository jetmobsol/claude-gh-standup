# GitHub Workflow Setup

This project has been integrated with GitHub Actions workflows from [claude-code-github-workflow](https://github.com/alirezarezvani/claude-code-github-workflow), adapted for a **Simple Branching Strategy** (Small Teams).

## Branching Strategy

```
feature/* → master
fix/* → master
hotfix/* → master
```

No `dev` or `staging` branches - all changes merge directly to `master` (or `main`) after passing quality gates.

**Note:** The workflows support both `master` and `main` branches automatically.

## Installed Workflows

### 1. Bootstrap Repository (`bootstrap.yml`)

**Triggers:** Manual only (`workflow_dispatch`)

**What it does:**
- 🏷️ Creates all required labels (status, type, priority, meta)
- 🎯 Optionally creates initial milestone
- 📋 Optionally validates project board (if configured)
- ✅ Idempotent - safe to run multiple times

**When to run:**
- **Once** when setting up the repository for the first time
- Run again if you need to add missing labels

**How to run:**
```bash
# Via GitHub UI:
# Actions → Bootstrap Repository → Run workflow

# Via gh CLI:
gh workflow run bootstrap.yml
```

**Created Labels:**
- **Status:** `status:to-triage`, `status:ready`, `status:in-progress`, `status:in-review`, `status:done`
- **Type:** `type:feature`, `type:fix`, `type:hotfix`, `type:docs`, `type:refactor`, `type:test`
- **Priority:** `priority:critical`, `priority:high`, `priority:medium`, `priority:low`
- **Meta:** `claude-code`, `dependencies`, `good first issue`, `help wanted`

### 2. Claude Plan to Issues (`claude-plan-to-issues.yml`)

**Triggers:** Manual only (`workflow_dispatch`)

**What it does:**
- 📋 Converts Claude Code plans (JSON) into GitHub issues
- 🏷️ Auto-labels issues based on type and priority
- 🎯 Creates/assigns to milestones
- 🔗 Links dependencies between issues
- ✅ Idempotent - skips existing issues with same title
- ⚡ Max 10 tasks per plan (for performance)

**How to use:**

1. **In Claude Code, create a plan:**
   ```
   "Create a plan for adding JSON export feature with 3 tasks"
   ```

2. **Claude generates a plan JSON** (simplified example):
   ```json
   {
     "tasks": [
       {
         "title": "Add JSON serializer",
         "description": "Implement JSON serialization for activity data",
         "type": "feature",
         "priority": "high",
         "acceptanceCriteria": ["Serialize all activity types", "Handle edge cases"]
       },
       {
         "title": "Add CLI flag for JSON export",
         "description": "Update CLI to support --format=json",
         "type": "feature",
         "priority": "medium"
       },
       {
         "title": "Add tests for JSON export",
         "description": "Test JSON export functionality",
         "type": "test",
         "priority": "high"
       }
     ]
   }
   ```

3. **Run the workflow:**
   ```bash
   # Via GitHub UI:
   # Actions → Claude Plan to Issues → Run workflow → Paste JSON

   # Via gh CLI:
   gh workflow run claude-plan-to-issues.yml \
     -f plan_json='{"tasks":[...]}' \
     -f milestone_title="v1.2 Release" \
     -f milestone_due_date="2026-02-01"
   ```

4. **Result:** Creates 3 GitHub issues with:
   - Proper labels (`claude-code`, `type:feature`, `priority:high`, etc.)
   - Acceptance criteria as task lists
   - All assigned to milestone "v1.2 Release"

**Plan JSON Format:**
```json
{
  "tasks": [
    {
      "title": "Issue title",
      "description": "Issue description",
      "type": "feature|fix|docs|refactor|test|hotfix",
      "priority": "low|medium|high|critical",
      "acceptanceCriteria": ["Criterion 1", "Criterion 2"],
      "dependencies": [1, 2]  // Issue numbers this depends on
    }
  ]
}
```

### 3. PR into Main (`pr-into-main.yml`)

**Triggers:** When a PR is opened, synchronized, or marked ready for review to `master` or `main`

**What it does:**
- ✅ Validates branch name (must be `feature/`, `fix/`, or `hotfix/`)
- ✅ Validates PR title (conventional commit format)
- ✅ Ensures PR links to at least one issue
- ✅ Runs quality checks (Java syntax, script validation)
- ✅ Fork-safe (read-only for fork PRs)
- ✅ Rate limit protection

**Required PR Title Format:**
```
<type>(<scope>): <subject>

Examples:
feat(standup): Add team aggregation support
fix(api): Resolve null pointer exception
docs: Update README with setup instructions
```

**Valid types:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`

**Required Branch Names:**
- `feature/<description>` - New features
- `fix/<description>` - Bug fixes
- `hotfix/<description>` - Critical fixes

**Required Issue Linking:**
Add to your PR description:
```markdown
Closes #123
Fixes #456
Relates to #789
```

### 4. PR Status Sync (`pr-status-sync.yml`)

**Triggers:** PR lifecycle events (opened, closed, merged, draft conversion)

**What it does:**
- 📝 PR opened → Comments on linked issues: "In Review"
- ✅ PR merged → Comments on linked issues: "Done" + deletes branch
- 🔄 PR closed (not merged) → Comments on linked issues: "In Progress"
- 📋 PR draft → Comments on linked issues: "In Progress"
- 🗑️ Auto-deletes merged branches (except protected branches)

### 5. Reusable PR Quality Checks (`reusable-pr-checks.yml`)

**Used by:** `pr-into-main.yml`

**What it does:**
- ✅ Java syntax validation (JBang compatible)
- ✅ Shell script syntax check
- ✅ Path-based filtering (only runs when code changes)
- ✅ Skips on docs-only changes

## Composite Actions

Reusable building blocks:

### Fork Safety (`fork-safety`)
- Detects fork PRs for write protection
- Prevents malicious actions from forked PRs

### Rate Limit Check (`rate-limit-check`)
- Circuit breaker for API exhaustion
- Minimum 50 API calls required
- Warns/fails if rate limit too low

## Branch Protection (Recommended)

To fully benefit from these workflows, configure branch protection on `master`:

1. Go to **Settings** → **Branches** → **Add rule**
2. Branch name pattern: `master`
3. Enable:
   - ✅ Require pull request reviews before merging
   - ✅ Require status checks to pass before merging
   - ✅ Required status checks:
     - `Validate Branch Name`
     - `Validate PR Title`
     - `Validate Linked Issue`
     - `Quality Check Summary`
   - ✅ Require conversation resolution before merging
   - ✅ Do not allow bypassing the above settings

## Getting Started

### Step 1: Run Bootstrap (One-Time Setup)

```bash
# Via GitHub UI:
# Go to Actions → Bootstrap Repository → Run workflow

# Via gh CLI:
gh workflow run bootstrap.yml
```

This creates all the labels you'll need for issue management.

### Step 2: Create Issues (Optional - Using Claude Plan)

If you have a plan from Claude Code, convert it to issues:

```bash
gh workflow run claude-plan-to-issues.yml \
  -f plan_json='{"tasks":[...]}'
```

Or create issues manually with the labels from bootstrap.

## First PR Workflow

1. **Create a branch:**
   ```bash
   git checkout -b feature/my-new-feature
   ```

2. **Make your changes and commit**

3. **Push to GitHub:**
   ```bash
   git push -u origin feature/my-new-feature
   ```

4. **Create PR on GitHub:**
   - Title: `feat: Add my new feature`
   - Description: Include `Closes #<issue-number>`
   - Link to at least one issue

5. **Automated checks run:**
   - Branch name validation ✅
   - PR title validation ✅
   - Linked issue validation ✅
   - Java syntax check ✅
   - Script validation ✅

6. **After approval and merge:**
   - Linked issues automatically commented "Done"
   - Feature branch automatically deleted

## Example PR

**Branch:** `feature/add-json-export`

**Title:** `feat(export): Add JSON export format`

**Description:**
```markdown
## Summary
This PR adds JSON export format to the standup report generator.

## Changes
- Add JSON serialization for activity data
- Update CLI to support --format=json flag
- Add tests for JSON export

Closes #42
```

**Result:**
- ✅ All validations pass
- ✅ Quality checks run
- ✅ Issue #42 gets "In Review" comment when PR opens
- ✅ Issue #42 gets "Done" comment when PR merges
- ✅ Branch `feature/add-json-export` auto-deleted after merge

## Troubleshooting

### "Branch name validation failed"
**Fix:** Rename your branch to start with `feature/`, `fix/`, or `hotfix/`
```bash
git branch -m new-branch-name
git push -u origin new-branch-name
```

### "PR title validation failed"
**Fix:** Update PR title to follow conventional commits:
- Start with valid type: `feat:`, `fix:`, `docs:`, etc.
- Subject must start with uppercase letter
- Use imperative mood: "Add" not "Added"

### "No linked issue found"
**Fix:** Add to PR description:
```markdown
Closes #123
```

### "Quality checks failed"
**Fix:** Check the workflow logs for specific errors and fix them:
- Java syntax errors in `.java` files
- Shell script syntax errors in `.sh` files

## What's Different from the Original Blueprint?

This integration is **simplified** for your Java/JBang project:

1. ✅ **No dev branch** - Direct to master (simple branching)
2. ✅ **No Node.js/pnpm** - Adapted for Java/JBang
3. ✅ **Streamlined quality checks** - Java syntax + scripts only
4. ✅ **No project board integration** - Can add later if needed
5. ✅ **No milestone creation** - Can add later if needed

## Next Steps (Optional)

Want even more automation? You can add:

1. **Create Branch on Issue** - Auto-create branches from labeled issues
2. **Project Board Integration** - Sync with GitHub Projects v2
3. **Dependabot** - Automated dependency updates

Let me know if you'd like to integrate any of these!

## Credits

Workflows adapted from [claude-code-github-workflow](https://github.com/alirezarezvani/claude-code-github-workflow) by Alireza Rezvani.
