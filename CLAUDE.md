# claude-gh-standup

AI-powered GitHub standup report generator using Claude AI. Analyzes commits, pull requests, issues, and code reviews with file diff context to produce professional standup summaries via Claude Code slash command.

**Tech Stack**: Java (JBang), GitHub CLI, Claude CLI
**Project Type**: Claude Code slash command / development tool
**Team Size**: Solo/Small team
**Development Phase**: Active development

## Project Overview

This is a Claude Code slash command that generates AI-powered standup reports from GitHub activity. It's a Java/JBang port of [gh-standup](https://github.com/sgoedecke/gh-standup) designed specifically for Claude Code integration.

**Key Capabilities**:
- Repository-aware activity detection (auto-detects current git repo)
- Rich context analysis (parses file diffs, not just commit messages)
- Team aggregation (multi-user consolidated reports)
- Multiple export formats (Markdown, JSON, HTML)
- Zero API key management (uses `gh` and `claude` CLI tools)

**Installation**:
- Project files: `~/.claude-gh-standup/`
- Command symlink: `~/.claude/commands/claude-gh-standup.md` → points to project's `.claude/commands/claude-gh-standup.md`

## Project Structure

```
claude-gh-standup/
├── scripts/                      # JBang Java scripts (single-file executables)
│   ├── Main.java                 # Entry point, CLI arg parsing, workflow orchestration
│   ├── CollectActivity.java      # GitHub API integration via gh CLI
│   ├── AnalyzeDiffs.java         # PR diff analysis for file change statistics
│   ├── GenerateReport.java       # Claude AI integration via claude -p
│   ├── ExportUtils.java          # Format conversion (Markdown/JSON/HTML)
│   └── TeamAggregator.java       # Multi-user report consolidation
├── prompts/                      # Claude prompt templates
│   ├── standup.prompt.md         # Individual standup report template
│   └── team.prompt.md            # Team aggregation prompt template
├── .claude/commands/             # Slash command definition
│   └── claude-gh-standup.md      # Command metadata and invocation
├── .github/workflows/            # CI/CD automation
│   ├── pr-into-main.yml          # PR validation (branch, title, linked issues)
│   ├── reusable-pr-checks.yml    # Quality checks (Java syntax, scripts)
│   ├── pr-status-sync.yml        # Auto-sync issue/PR status labels
│   └── claude-plan-to-issues.yml # Convert OpenSpec plans to GitHub issues
├── openspec/                     # OpenSpec change proposals
│   ├── AGENTS.md                 # OpenSpec workflow and conventions
│   └── changes/                  # Archived change proposals
├── examples/                     # Sample output files
├── docs/                         # Additional documentation
├── CONTRIBUTING.md               # Development workflow and branching rules
├── README.md                     # User-facing documentation
├── TODO.md                       # Implementation plans (multi-directory feature)
└── CLAUDE.md                     # This file - AI assistant guidance

**File Purpose Explanations**:
- **scripts/*.java**: Self-contained JBang scripts with shebang headers
- **prompts/*.md**: Template files with `{{variable}}` placeholders for dynamic content injection
- **.claude/commands/**: Claude Code slash command discovery location
- **openspec/**: Architectural change management using OpenSpec specification
```

## Development Setup

### Prerequisites

**Required Tools**:
1. **JBang** (Java script runner):
   ```bash
   curl -Ls https://sh.jbang.dev | bash -s - app setup
   ```

2. **GitHub CLI** (authenticated):
   ```bash
   brew install gh  # macOS
   # OR: https://github.com/cli/cli#installation
   gh auth login
   ```

3. **Claude CLI** (via Claude Code):
   - Authenticated via Claude Code installation
   - Verify: `claude --version`

**Optional Tools**:
- Java 17+ (for native javac compilation testing)
- Git (for repository operations)

### Local Development Workflow

**1. Clone and Install**:
```bash
# Clone to dedicated directory
git clone https://github.com/jetmobsol/claude-gh-standup.git ~/.claude-gh-standup
cd ~/.claude-gh-standup

# Symlink the command file to Claude Code
mkdir -p ~/.claude/commands
ln -sf ~/.claude-gh-standup/.claude/commands/claude-gh-standup.md ~/.claude/commands/claude-gh-standup.md
```

**2. Test Individual Scripts**:
```bash
# Test activity collection (requires authenticated gh CLI)
jbang scripts/CollectActivity.java octocat 3

# Test diff analysis (pass JSON from previous step)
jbang scripts/AnalyzeDiffs.java '{"pull_requests":[...]}'

# Test report generation
jbang scripts/GenerateReport.java '<activity-json>' '<diff-summary>'

# Test team aggregation
jbang scripts/TeamAggregator.java alice bob charlie 7
```

**3. Test Slash Command**:
```bash
# In Claude Code, test the command:
/claude-gh-standup --days 3
/claude-gh-standup --team user1 user2 --days 7
/claude-gh-standup --format json --output test.json
```

**4. Verify Installation**:
```bash
# Check if command appears in Claude Code
/help  # Look for claude-gh-standup in the list

# If not visible, restart Claude Code or check installation path
```

### Testing Scripts Individually

Each Java script can be tested independently with JBang:

```bash
# CollectActivity.java - Fetches GitHub activity
jbang scripts/CollectActivity.java <username> <days> [repo]

# Example:
jbang scripts/CollectActivity.java octocat 3
jbang scripts/CollectActivity.java octocat 7 owner/repo

# AnalyzeDiffs.java - Parses PR diffs for file statistics
jbang scripts/AnalyzeDiffs.java '<activity-json>'

# GenerateReport.java - Calls Claude AI
jbang scripts/GenerateReport.java '<activity-json>' '<diff-summary>'

# ExportUtils.java - Format conversion
jbang scripts/ExportUtils.java '<report-content>' <format> [output-file]

# TeamAggregator.java - Multi-user consolidation
jbang scripts/TeamAggregator.java user1 user2 user3 <days> [repo]
```

## Code Patterns & Conventions

### JBang Script Pattern

All scripts follow this standard structure:

```java
///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

public class ScriptName {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String... args) {
        // Implementation
    }
}
```

**Key Elements**:
- **Shebang**: `///usr/bin/env jbang "$0" "$@" ; exit $?` - Makes script executable
- **Dependencies**: `//DEPS groupId:artifactId:version` - JBang downloads automatically
- **Gson**: Standard JSON library across all scripts

### CLI Integration Pattern

**Claude CLI Integration** (`GenerateReport.java`):
```java
ProcessBuilder pb = new ProcessBuilder("claude", "-p", fullPrompt);
pb.inheritIO();  // Pipes claude output directly to stdout (seamless streaming)
Process process = pb.start();
int exitCode = process.waitFor();
```

**GitHub CLI Integration** (`CollectActivity.java`):
```java
ProcessBuilder pb = new ProcessBuilder("gh", "pr", "list", "--author", user, "--json", "title,url");
Process process = pb.start();
BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
String json = reader.lines().collect(Collectors.joining());
```

**Why ProcessBuilder over direct API calls?**
- Leverages existing `gh` and `claude` authentication (no API key management)
- Inherits user's configured defaults and preferences
- Simpler error handling (delegates to CLI tools)

### Error Handling Philosophy

**Continue on Partial Failures**:
```java
// Commit search often fails due to GitHub API restrictions
// → Continue with PRs, issues, and reviews (graceful degradation)

try {
    commits = fetchCommits(user, days);
} catch (Exception e) {
    System.err.println("⚠️  Commit search failed (common) - continuing with PRs/issues");
    commits = new JsonArray();  // Empty array, not fatal
}
```

**Fail Fast on Critical Errors**:
```java
// Missing dependencies → fail immediately with clear message
if (!commandExists("gh")) {
    System.err.println("❌ Error: gh CLI not found");
    System.err.println("   Install: https://github.com/cli/cli#installation");
    System.exit(1);
}
```

### Argument Parsing Convention

All flags support both long and short forms:

```java
case "--days":
case "-d":
    parsed.days = Integer.parseInt(args[++i]);
    break;
```

**Standard Flags**:
- `--days, -d` - Number of days to look back
- `--user, -u` - GitHub username
- `--repo, -r` - Repository (owner/repo format)
- `--format, -f` - Output format (markdown|json|html)
- `--output, -o` - Output file path
- `--team` - List of team members (no short form)
- `--no-claude` - Skip Claude AI (output raw prompt)
- `--help, -h` - Show help message

## Testing Strategy

### Manual Testing Workflow

**1. Unit Testing (Individual Scripts)**:
```bash
# Test each script independently with known inputs
jbang scripts/CollectActivity.java octocat 3
jbang scripts/AnalyzeDiffs.java '{"pull_requests":[]}'
```

**2. Integration Testing (Full Workflow)**:
```bash
# Test in a git repository
cd /path/to/some/repo
/claude-gh-standup --days 3

# Test repository auto-detection
git remote get-url origin  # Should match detected repo
```

**3. Format Testing**:
```bash
/claude-gh-standup --format json --output test.json
/claude-gh-standup --format html --output test.html
/claude-gh-standup --format markdown --output test.md

# Verify file contents
cat test.json | jq .  # Validate JSON
open test.html        # View HTML
```

**4. Team Aggregation Testing**:
```bash
/claude-gh-standup --team alice bob charlie --days 7
```

**5. Error Scenario Testing**:
```bash
# Non-git directory
cd /tmp
/claude-gh-standup  # Should prompt for --repo

# Invalid user
/claude-gh-standup --user nonexistent-user-12345 --days 3

# Invalid repo
/claude-gh-standup --repo invalid/nonexistent --days 3
```

### Common Test Cases

**Repository Detection**:
- ✅ Run in git repo → auto-detects `owner/repo`
- ✅ Run outside git repo → prompts for `--repo` flag
- ✅ Explicit `--repo` override → uses specified repo

**User Detection**:
- ✅ No `--user` flag → uses current authenticated GitHub user
- ✅ Explicit `--user` flag → analyzes specified user's activity

**Error Handling**:
- ✅ Commit search fails → continues with PRs/issues/reviews
- ✅ Missing `gh` CLI → fails with installation instructions
- ✅ Missing `jbang` → fails with installation instructions

**Output Validation**:
- ✅ Markdown output contains proper headers and sections
- ✅ JSON output is valid (test with `jq`)
- ✅ HTML output renders correctly in browser

## Common Development Tasks

### Adding a New CLI Flag

**1. Update Args class** (`Main.java`):
```java
static class Args {
    // ... existing fields ...
    boolean myNewFlag = false;  // Add your field
}
```

**2. Add parsing logic** (`parseArgs()` method):
```java
case "--my-new-flag":
    parsed.myNewFlag = true;
    break;
```

**3. Update help text** (`printHelp()` method):
```java
System.out.println("  --my-new-flag       Description of what this flag does");
```

**4. Pass to relevant script** (if needed):
```java
String[] scriptArgs = new String[]{
    user,
    String.valueOf(parsed.days),
    parsed.myNewFlag ? "true" : "false"  // Pass as string argument
};
```

**5. Test**:
```bash
jbang scripts/Main.java --my-new-flag --days 3
```

### Adding a New Export Format

**1. Add format handler** (`ExportUtils.java`):
```java
public static String convertToMyFormat(String content) {
    // Convert markdown content to new format
    return formattedContent;
}
```

**2. Update format validation** (`Main.java`):
```java
if (!parsed.format.matches("markdown|json|html|myformat")) {
    System.err.println("Invalid format. Use: markdown, json, html, myformat");
    System.exit(1);
}
```

**3. Add format switch** (`ExportUtils.java` or `Main.java`):
```java
case "myformat":
    output = ExportUtils.convertToMyFormat(report);
    break;
```

**4. Test**:
```bash
/claude-gh-standup --format myformat --output test.myext
```

### Modifying Prompt Templates

**1. Edit template file**:
```bash
# Individual standup
vim prompts/standup.prompt.md

# Team aggregation
vim prompts/team.prompt.md
```

**2. Templates use `{{variable}}` placeholders**:
```markdown
# Standup Report for {{user}}

## Activity Summary (Last {{days}} days)

{{activity}}

{{diffs}}
```

**3. Test with direct script call**:
```bash
jbang scripts/GenerateReport.java '<json-activity>' '<diff-summary>'
```

**4. Verify in full workflow**:
```bash
/claude-gh-standup --days 3 --no-claude  # See raw prompt without AI call
```

### Adding a New Script

**1. Create new JBang script**:
```bash
touch scripts/MyNewScript.java
chmod +x scripts/MyNewScript.java
```

**2. Add JBang header**:
```java
///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.*;
import java.io.*;

public class MyNewScript {
    public static void main(String... args) {
        // Implementation
    }
}
```

**3. Test independently**:
```bash
jbang scripts/MyNewScript.java arg1 arg2
```

**4. Integrate into Main.java workflow** (if needed):
```java
ProcessBuilder pb = new ProcessBuilder("jbang", scriptPath, arg1, arg2);
Process process = pb.start();
```

### Working with OpenSpec Changes

**When to create an OpenSpec proposal**:
- New features (e.g., multi-directory support)
- Breaking changes (e.g., changing CLI flags)
- Architecture shifts (e.g., switching from JBang to native Java)
- Performance/security work

**Workflow**:
1. **Create proposal**: Use `/openspec:proposal` slash command
2. **Review and refine**: Edit proposal in `openspec/changes/<id>/`
3. **Get approval**: Discuss with team/maintainers
4. **Implement**: Use `/openspec:apply` to start implementation
5. **Archive**: Use `/openspec:archive` when deployed

**See**: `openspec/AGENTS.md` for detailed workflow

## Dependencies & External Tools

### External CLI Dependencies

**gh CLI (GitHub CLI)**:
- **Purpose**: All GitHub API interactions
- **Commands Used**:
  - `gh pr list --author USER --json ...` - List pull requests
  - `gh issue list --author USER --json ...` - List issues
  - `gh pr diff NUMBER` - Get file diffs
  - `gh api repos/OWNER/REPO/...` - Direct API calls
- **Authentication**: `gh auth login`
- **Installation**: https://github.com/cli/cli#installation

**claude CLI (Claude Code)**:
- **Purpose**: AI report generation
- **Usage**: `claude -p '<prompt>'` (prompt mode)
- **Authentication**: Via Claude Code installation
- **Why prompt mode?**: Seamless streaming output to stdout

**jbang (Java Script Runner)**:
- **Purpose**: Run Java scripts without build setup
- **Features**:
  - Automatic dependency management (`//DEPS`)
  - Shebang support (scripts as executables)
  - Caching for fast execution
- **Installation**: `curl -Ls https://sh.jbang.dev | bash -s - app setup`

### Java Library Dependencies

**Gson (2.10.1)**:
- **Purpose**: JSON parsing and serialization
- **Usage**: `//DEPS com.google.code.gson:gson:2.10.1`
- **Common Patterns**:
  ```java
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  JsonObject obj = gson.fromJson(jsonString, JsonObject.class);
  String json = gson.toJson(obj);
  ```

### Why No Direct API Calls?

**Design Decision**: Use CLI tools instead of direct GitHub/Anthropic APIs

**Reasons**:
1. **Zero API key management** - Leverages existing user authentication
2. **Simpler error handling** - Delegates to battle-tested CLI tools
3. **Respects user config** - Inherits gh/claude defaults and preferences
4. **Lower maintenance** - No need to track API version changes

**Trade-off**: Requires `gh` and `claude` to be installed and authenticated

## Change Management

This project uses **OpenSpec** for managing architectural changes and proposals.

**When to use OpenSpec**:
- Planning new features (e.g., multi-directory support in TODO.md)
- Proposing breaking changes
- Architectural discussions
- Large refactoring work

**Workflow**:
See `openspec/AGENTS.md` for detailed instructions on creating and applying change proposals.

<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Development workflow (issue → branch → PR)
- Branch naming conventions (`feature/issue-N-description`)
- Commit message format (Conventional Commits)
- PR requirements (linked issues, status checks)
- Labels and project management

**Quick Reference**:
```bash
# 1. Create issue on GitHub
gh issue create --title "feat: Description"

# 2. Create feature branch
git checkout -b feature/issue-42-description

# 3. Make changes and commit
git commit -m "feat(scope): Description"

# 4. Push and create PR
git push -u origin feature/issue-42-description
gh pr create --title "feat: Description" --body "Closes #42"
```

## Troubleshooting

### "Command not found" in Claude Code
- Verify symlink: `ls -la ~/.claude/commands/claude-gh-standup.md`
- Should point to: `~/.claude-gh-standup/.claude/commands/claude-gh-standup.md`
- Run `/help` in Claude Code to check if command appears
- Restart Claude Code to reload commands

### "Commit search failed"
- **Expected behavior** - GitHub API often restricts commit searches
- Tool continues with PRs, issues, and reviews (graceful degradation)
- Use `--days` to limit scope if needed

### "gh: command not found"
- Install GitHub CLI: https://github.com/cli/cli#installation
- Authenticate: `gh auth login`
- Verify: `gh --version`

### "jbang: command not found"
- Install JBang: `curl -Ls https://sh.jbang.dev | bash -s - app setup`
- Verify: `jbang version`
- Add to PATH if needed (JBang setup does this automatically)

### "Repository not detected"
- Run in a git repository, OR
- Use explicit `--repo owner/repo` flag
- Verify: `git remote get-url origin`

### JSON parsing errors
- Check Gson dependency is loaded: `jbang --deps scripts/Main.java`
- Verify JSON structure from `gh` CLI matches expected format
- Test with: `gh pr list --json title,url | jq .`

### Claude CLI errors
- Verify authentication: `claude --version`
- Check quota/rate limits in Claude Code
- Test prompt mode: `echo "Test prompt" | claude -p`

---

**Last Updated**: 2026-01-04
**Maintained By**: claude-md-guardian agent (auto-sync on major changes)
