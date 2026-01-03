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

### 1. PR into Main (`pr-into-main.yml`)

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

### 2. PR Status Sync (`pr-status-sync.yml`)

**Triggers:** PR lifecycle events (opened, closed, merged, draft conversion)

**What it does:**
- 📝 PR opened → Comments on linked issues: "In Review"
- ✅ PR merged → Comments on linked issues: "Done" + deletes branch
- 🔄 PR closed (not merged) → Comments on linked issues: "In Progress"
- 📋 PR draft → Comments on linked issues: "In Progress"
- 🗑️ Auto-deletes merged branches (except protected branches)

### 3. Reusable PR Quality Checks (`reusable-pr-checks.yml`)

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

Want more automation? You can add:

1. **Claude Plan to Issues** - Convert Claude plans to GitHub issues
2. **Bootstrap Workflow** - One-time repository setup
3. **Create Branch on Issue** - Auto-create branches from issues
4. **Project Board Integration** - Sync with GitHub Projects v2

Let me know if you'd like to integrate any of these!

## Credits

Workflows adapted from [claude-code-github-workflow](https://github.com/alirezarezvani/claude-code-github-workflow) by Alireza Rezvani.
