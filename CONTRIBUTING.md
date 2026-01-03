# Contributing Guide

Thank you for contributing to claude-gh-standup! This guide will help you understand our workflow.

## Development Workflow

### 1. Create an Issue

Before making changes, create an issue describing:
- What you want to add/fix
- Why it's needed
- Acceptance criteria

### 2. Create a Feature Branch

Branch names must follow this pattern:
```
feature/<issue-number>-brief-description
fix/<issue-number>-brief-description
hotfix/<issue-number>-brief-description
```

Example:
```bash
git checkout -b feature/issue-42-add-json-export
```

### 3. Make Your Changes

- Write clean, documented code
- Follow existing code style
- Add tests if applicable

### 4. Commit with Conventional Format

Commit messages must follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

Examples:
feat(export): Add JSON export format
fix(api): Resolve null pointer exception
docs: Update README with setup instructions
```

**Valid types:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`

### 5. Push and Create PR

```bash
git push -u origin feature/issue-42-add-json-export
```

Then create a PR with:
- **Title:** Conventional commit format (e.g., `feat: Add JSON export`)
- **Body:** Must include `Closes #<issue-number>` or `Fixes #<issue-number>`

### 6. Wait for Checks

Your PR will be automatically validated:
- ✅ Branch name format
- ✅ PR title format
- ✅ Linked issue
- ✅ Quality checks (Java syntax, scripts)

### 7. Merge

Once all checks pass:
- Merge the PR (squash merge recommended)
- The linked issue will be automatically closed
- Your branch will be automatically deleted

## Branch Protection

The `main` branch is protected:
- No direct pushes allowed
- All changes via PR
- All status checks must pass
- All conversations must be resolved

## Labels

Use these labels on issues:

**Status:**
- `status:to-triage` - Needs triage
- `status:ready` - Ready to work on
- `status:in-progress` - Currently being worked on
- `status:in-review` - Under code review
- `status:done` - Completed

**Type:**
- `type:feature` - New feature
- `type:fix` - Bug fix
- `type:hotfix` - Critical fix
- `type:docs` - Documentation
- `type:refactor` - Code refactoring
- `type:test` - Test addition/update

**Priority:**
- `priority:critical` - Blocking issue
- `priority:high` - High priority
- `priority:medium` - Medium priority
- `priority:low` - Low priority

## Questions?

Open an issue with the `question` label!
