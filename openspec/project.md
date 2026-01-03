# Project Context

## Purpose

**claude-gh-standup** is a Claude Code slash command that generates AI-powered standup reports from GitHub activity. It analyzes commits, pull requests, issues, and code reviews to produce professional standup summaries with minimal user effort.

**Goals**:
- Enable developers to generate daily/weekly standup reports instantly
- Provide richer context than commit messages alone through file diff analysis
- Support team aggregation for consolidated multi-developer standups
- Eliminate API key management by leveraging `gh` and `claude` CLIs
- Deliver a seamless Claude Code integration experience

**Target Users**:
- Individual developers preparing for standup meetings
- Team leads compiling team-wide activity summaries
- Remote teams documenting asynchronous progress
- Engineering managers tracking sprint contributions

## Tech Stack

**Core Technologies**:
- **Java 11+** - Implementation language for type safety and robust process management
- **JBang** - Single-file Java script execution (no compilation or build files)
- **Gson 2.10.1** - JSON parsing for GitHub API responses
- **Markdown** - Prompt template format with `{{variable}}` placeholders

**External CLIs** (required dependencies):
- **gh CLI** - GitHub API access with authentication handled by GitHub CLI
- **claude CLI** - Claude AI generation via Claude Code's CLI interface
- **git** - Diff analysis for commit-level file changes

**Development Tools**:
- Standard Java tooling (optional: IDE support for syntax highlighting)
- OpenSpec - Specification-driven development workflow

## Project Conventions

### Code Style

**Java Conventions**:
- **File Naming**: PascalCase for all `.java` files (e.g., `CollectActivity.java`, `GenerateReport.java`)
- **Class Names**: Match file names (one public class per file)
- **Method Naming**: camelCase with descriptive names (e.g., `getUserCommits()`, `analyzePRDiffs()`)
- **Constants**: UPPER_SNAKE_CASE for static final variables
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Soft limit of 120 characters

**JBang Shebang Pattern**:
Every `.java` file must start with:
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
```

**Dependency Declaration**:
Use JBang `//DEPS` comments at top of file:
```java
//DEPS com.google.code.gson:gson:2.10.1
```

**Imports**:
- Standard library imports first
- Third-party imports second
- Blank line between groups
- Alphabetical within groups

**Error Handling**:
- Use try-catch for I/O operations (file reading, process execution)
- Print clear error messages to stderr with context
- Exit with code 1 on fatal errors
- Graceful degradation for optional data (e.g., commit search failures)

### Architecture Patterns

**Capability-Based Decomposition**:
Split functionality into 6 independent capabilities, each with its own Java script:
1. `github-activity-collection` → `CollectActivity.java`
2. `diff-analysis` → `AnalyzeDiffs.java`
3. `report-generation` → `GenerateReport.java`
4. `export-formats` → `ExportUtils.java`
5. `team-aggregation` → `TeamAggregator.java`
6. `slash-command-interface` → `Main.java`

**ProcessBuilder Pattern**:
```java
ProcessBuilder pb = new ProcessBuilder("claude", "-p", fullPrompt);
pb.inheritIO();  // Pipes stdout/stderr to current process
Process process = pb.start();
int exitCode = process.waitFor();
```

**Prompt Template Injection**:
- Load Markdown files using `Files.readString(Paths.get(path))`
- Replace `{{placeholders}}` with `String.replace()`
- No complex templating engine needed

**Data Flow**:
```
User Input → Main.java (orchestration)
  → CollectActivity.java (GitHub data)
  → AnalyzeDiffs.java (file changes)
  → GenerateReport.java (AI prompt + claude -p)
  → ExportUtils.java (format conversion)
  → Output (stdout or file)
```

**Separation of Concerns**:
- Each Java script is independently testable with JBang
- No shared state between scripts (communicate via stdin/stdout or return values)
- Main.java is the only orchestrator

### Testing Strategy

**Independent Script Testing**:
Each Java script can be tested directly with JBang:
```bash
# Test activity collection
jbang scripts/CollectActivity.java octocat 3

# Test diff analysis
jbang scripts/AnalyzeDiffs.java owner/repo 123

# Test report generation
jbang scripts/GenerateReport.java activities.json diffs.json

# Test export utilities
jbang scripts/ExportUtils.java report-data.json markdown
```

**Integration Testing**:
- Test full workflow via Main.java entry point
- Use real GitHub data for realistic testing
- Verify all export formats (Markdown, JSON, HTML)
- Test team aggregation with 2-3 users

**Manual Testing Checklist**:
- Basic single-user report (`/claude-gh-standup`)
- Multi-day reports (`--days 7`)
- Team aggregation (`--team alice bob`)
- Repository filtering (`--repo owner/repo`)
- All export formats (`--format json|markdown|html`)
- File output (`--output standup.md`)
- Error cases (no activity, auth failures, missing deps)

**Test Data**:
- Use personal GitHub account for development testing
- Create example outputs in `examples/` directory
- Document expected behavior in spec files

### Git Workflow

**Branching Strategy**:
- **main** - Production-ready code (stable releases)
- **feature/*** - Feature development branches
- **fix/*** - Bug fix branches

**Commit Conventions**:
Follow Conventional Commits format:
```
type(scope): description

feat(activity-collection): add PR collection via gh search prs
fix(diff-analysis): handle empty diff gracefully
docs(readme): add team aggregation examples
test(export): verify HTML styling
```

**Types**: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`

**Scopes**: Match capability names (activity-collection, diff-analysis, report-generation, etc.)

**Pull Requests**:
- One feature/capability per PR when possible
- Include tests and documentation updates
- Reference OpenSpec proposal/spec if applicable

**Releases**:
- Semantic versioning (v1.0.0, v1.1.0, v2.0.0)
- Tag releases in git
- Create GitHub release with changelog

## Domain Context

**GitHub Activity Types**:
1. **Commits**: Code changes pushed to repositories
2. **Pull Requests**: Proposed code changes awaiting review
3. **Issues**: Bug reports, feature requests, tasks
4. **Code Reviews**: Comments and approvals on PRs

**Standup Report Structure**:
Traditional standup format adapted to GitHub activity:
- **Yesterday's Accomplishments**: Completed PRs, merged commits, closed issues
- **Today's Plans**: Open PRs, in-progress work (inferred from recent activity)
- **Blockers/Challenges**: Awaiting review, CI failures, dependencies

**File Diff Analysis**:
- **Purpose**: Provides context beyond commit messages
- **Metrics**: Lines added, lines deleted, files changed
- **Format**: Unified diff format from git/gh
- **Usage**: Injected into AI prompt for better understanding of work scope

**Team Aggregation**:
- Consolidates individual reports into team summary
- Identifies cross-team collaboration
- Highlights dependencies and blockers affecting multiple people
- Provides team-level metrics (total commits, PRs, etc.)

**AI Prompt Engineering**:
- Prompts guide Claude to write in first person (for individual reports)
- Team prompts synthesize across reports to find themes
- File changes section provides implementation context
- Emphasis on professional but conversational tone

## Important Constraints

**Technical Constraints**:
1. **No Direct API Calls**: MUST use `gh` CLI for all GitHub API access (no REST/GraphQL SDKs)
2. **No API Key Management**: MUST use `gh` and `claude` CLIs which handle authentication
3. **Single-File Scripts**: Each `.java` file must be executable via JBang without compilation
4. **Claude Code Integration**: Must follow slash command conventions (`.md` file in repo root)
5. **Java 11+ Compatibility**: Cannot use language features newer than Java 11

**Functional Constraints**:
1. **Graceful Degradation**: If commit search fails (common GitHub restriction), continue with PRs/issues
2. **Rate Limiting**: Respect GitHub API rate limits via `gh` CLI's built-in handling
3. **Prompt Size**: Keep diff summaries concise (file paths + stats, not full diff content)
4. **Team Size**: Practical limit ~10 users (sequential processing takes time)

**User Experience Constraints**:
1. **Progress Indicators**: Print status messages for operations >2 seconds
2. **Clear Error Messages**: Include troubleshooting hints in error output
3. **Sensible Defaults**: `--days 1`, `--format markdown`, auto-detect user
4. **No Interactivity**: Pure CLI, no TUI or prompts (can run in scripts)

**Licensing Constraints**:
- MIT License with attribution to original gh-standup project
- Credit @sgoedecke in README and documentation
- Preserve original prompt template attribution if adapted

## External Dependencies

**Required CLIs** (must be in PATH):
1. **gh** (GitHub CLI)
   - Version: 2.0+
   - Purpose: GitHub API access (search, PRs, issues, reviews)
   - Authentication: `gh auth login`
   - Docs: https://cli.github.com/manual/

2. **claude** (Claude Code CLI)
   - Version: Compatible with Claude Code installation
   - Purpose: AI report generation via `claude -p`
   - Authentication: Handled by Claude Code
   - Docs: https://docs.anthropic.com/claude/docs/claude-code

3. **git**
   - Version: 2.0+
   - Purpose: Commit diff analysis (`git diff`)
   - Typically pre-installed on developer machines

4. **jbang**
   - Version: 0.100+
   - Purpose: Java script execution without build files
   - Installation: `curl -Ls https://sh.jbang.dev | bash -s - app setup`
   - Docs: https://www.jbang.dev/

**Optional Dependencies**:
- **Java 11+**: Auto-installed by JBang if not present
- **Gson**: Managed by JBang via `//DEPS` (no manual install)

**External Services**:
- **GitHub API**: Accessed indirectly via `gh` CLI
  - Endpoints: `/search/commits`, `/search/issues`, `/repos/{owner}/{repo}/pulls`
  - Rate limits: 30 requests/minute (unauthenticated), 5000/hour (authenticated via gh)
- **Claude AI**: Accessed via `claude -p` CLI
  - Model: Sonnet 4.5 (or user's configured model)
  - Prompt size limits apply (stay under ~100K tokens)

**Reference Projects**:
1. **sgoedecke/gh-standup** (`/Users/garden/projects/ai/sgoedecke/gh-standup`)
   - Purpose: Prompt template inspiration
   - Language: Go
   - Architecture: GitHub CLI extension using GitHub Models API

## Installation Paths

**User-Level Installation**:
```bash
git clone <repo-url> ~/.claude/commands/claude-gh-standup/
```
- Available to all projects for that user
- Listed in `/help` as "user" command

**Project-Level Installation**:
```bash
git clone <repo-url> .claude/commands/claude-gh-standup/
```
- Available only in current project
- Listed in `/help` as "project" command

**Command Invocation**:
After installation, users run:
```bash
/claude-gh-standup [flags]
```

## Performance Expectations

**Typical Latency** (single user, 7 days):
- GitHub activity collection: 2-5 seconds
- Diff analysis: 1-3 seconds
- Claude AI generation: 5-15 seconds
- **Total**: 10-25 seconds

**Scaling**:
- Linear with `--days` parameter
- Linear with team size (N × single-user time)
- Bottleneck: AI generation (sequential)

**Future Optimization** (v2.0):
- Parallel processing for team members (CompletableFuture)
- Activity caching for development iterations
- Batch diff analysis
