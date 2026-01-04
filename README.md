# claude-gh-standup

> AI-powered GitHub standup reports for Claude Code

Generate professional standup reports from your GitHub activity using Claude AI. Supports both single-repository mode and multi-directory tracking across multiple branches/repos with local work-in-progress detection.

## Features

- **Multi-Directory Support** - Track multiple branches/repos simultaneously with local WIP detection
- **Local Change Detection** - Shows uncommitted and unpushed work alongside GitHub activity
- **Smart Date Shortcuts** - `--yesterday` (Friday-aware on Monday), `--last-week`
- **GitHub Activity Deduplication** - Fetches activity once per unique repository
- **Report Auto-Save** - Saves to `~/.claude-gh-standup/reports/` by default
- **Repository-Aware** - Automatically detects current repository or uses configured directories
- **Zero API Key Management** - Uses `gh` CLI and `claude -p` (no GitHub or Anthropic API keys needed)
- **Rich Context** - Analyzes file diffs beyond commit messages for better AI understanding
- **Team Aggregation** - Generate consolidated reports for multiple team members
- **Multiple Formats** - Export to Markdown, JSON, or HTML
- **Claude Code Integration** - First-class slash command experience

## Installation

### Prerequisites

1. **GitHub CLI** - Install and authenticate:
   ```bash
   # Install gh CLI (if not already installed)
   brew install gh  # macOS
   # OR: https://github.com/cli/cli#installation

   # Authenticate
   gh auth login
   ```

2. **JBang** - Java script runner:
   ```bash
   curl -Ls https://sh.jbang.dev | bash -s - app setup
   ```

3. **Claude Code** - Ensure `claude` CLI is available

### Quick Install (Recommended)

Run the automated installer:
```bash
curl -fsSL https://raw.githubusercontent.com/jetmobsol/claude-gh-standup/main/install.sh | bash
```

The installer will:
- Install to `~/.claude-gh-standup/`
- Create symlink for Claude Code slash command
- Optionally set up shell aliases (`standup-yesterday`, `standup-week`, `standup`)
- Optionally initialize configuration file

### Manual Install

**User-level** (available in all projects):
```bash
git clone https://github.com/jetmobsol/claude-gh-standup.git ~/.claude/commands/claude-gh-standup/
```

**Project-level** (available only in current project):
```bash
git clone https://github.com/jetmobsol/claude-gh-standup.git .claude/commands/claude-gh-standup/
```

After installation, restart Claude Code or reload commands.

## Usage

### Quick Start (Legacy Mode)

```bash
# Yesterday's activity in current repository
/claude-gh-standup

# Use smart date shortcuts
/claude-gh-standup --yesterday        # Friday if Monday, otherwise yesterday
/claude-gh-standup --last-week        # Last 7 days

# Specific user or repository
/claude-gh-standup --user octocat --days 3
/claude-gh-standup --repo owner/repo-name --days 7
```

**Note**: By default, the command analyzes activity in the **current git repository**. If you're not in a git repository or want to analyze a different one, use `--repo owner/repo`.

### Multi-Directory Mode

Track multiple branches/repositories with local work-in-progress detection:

```bash
# 1. Initialize configuration
/claude-gh-standup --config-init

# 2. Add directories to track
cd ~/projects/myapp
/claude-gh-standup --config-add .

cd ~/projects/myapp-feature
/claude-gh-standup --config-add . --id feature-branch

# 3. List configured directories
/claude-gh-standup --config-list

# 4. Generate multi-directory report
/claude-gh-standup --yesterday
```

**Multi-directory reports include:**
- GitHub activity deduplicated across repositories
- Local uncommitted changes per directory
- Unpushed commits per directory
- Context switching analysis

**Configuration management:**
```bash
/claude-gh-standup --config-add PATH [--id ID]   # Add directory
/claude-gh-standup --config-list                 # List directories
/claude-gh-standup --config-remove ID            # Remove directory
```

### Team Reports

```bash
# Team standup for 3 developers
/claude-gh-standup --team alice bob charlie --days 7
```

### Export Formats

```bash
# JSON output
/claude-gh-standup --format json

# HTML report saved to file
/claude-gh-standup --format html --output standup.html

# Markdown to file (default format)
/claude-gh-standup --output standup.md
```

## How It Works

### Legacy Mode (Single Repository)
1. **Collect Activity** - Uses `gh` CLI to search commits, PRs, issues, reviews
2. **Analyze Diffs** - Parses `gh pr diff` output for file change statistics
3. **Generate Report** - Injects data into prompt template and calls `claude -p`
4. **Format Output** - Exports to chosen format (Markdown/JSON/HTML)

### Multi-Directory Mode
1. **Load Configuration** - Reads `~/.claude-gh-standup/config.json`
2. **Parallel Local Detection** - Detects uncommitted/unpushed changes (4 threads)
3. **Deduplicated GitHub Activity** - Fetches activity once per unique repository
4. **Activity Aggregation** - Combines local and GitHub data into unified JSON
5. **Generate Report** - Uses multi-directory prompt template with `claude -p`
6. **Auto-Save** - Saves report to `~/.claude-gh-standup/reports/YYYY-MM-DD-*.md`

## Architecture

Built with **Java + JBang** for single-file executable scripts:

```
scripts/
├── Main.java                  # Entry point, mode detection, workflow orchestration
├── ConfigManager.java         # Configuration CRUD (add/remove/list directories)
├── LocalChangesDetector.java  # Git change detection (uncommitted/unpushed)
├── ActivityAggregator.java    # Multi-directory orchestration & deduplication
├── CollectActivity.java       # GitHub activity collection via gh CLI
├── AnalyzeDiffs.java          # File diff analysis
├── GenerateReport.java        # Claude AI integration
├── ExportUtils.java           # Format conversion (MD/JSON/HTML)
└── TeamAggregator.java        # Multi-user report consolidation

prompts/
├── standup.prompt.md          # Individual standup prompt template
├── multidir-standup.prompt.md # Multi-directory prompt template
└── team.prompt.md             # Team aggregation prompt template

config/
├── config.json                # User configuration (empty by default)
└── config.example.json        # Example with 2 directories
```

**Key Patterns**:

*Claude AI Integration:*
```java
ProcessBuilder pb = new ProcessBuilder("claude", "-p", fullPrompt);
pb.inheritIO();  // Seamlessly pipes claude output to stdout
Process process = pb.start();
```

*Parallel Local Change Detection:*
```java
ExecutorService executor = Executors.newFixedThreadPool(4);
for (Directory dir : directories) {
    futures.add(executor.submit(() -> detectChanges(dir)));
}
```

*GitHub Activity Deduplication:*
```java
Map<String, List<Directory>> repoMap = directories.stream()
    .collect(Collectors.groupingBy(d -> d.repoName));
// Fetch activity once per unique repository
```

## Attribution

This project is inspired by and adapted from **[gh-standup](https://github.com/sgoedecke/gh-standup)** by [@sgoedecke](https://github.com/sgoedecke).

**Key Differences**:
- Original: Go-based GitHub CLI extension using GitHub Models LLM API
- This version: Java/JBang implementation using `claude -p` for Claude Code integration
- Original: Requires GitHub Models API access
- This version: Uses existing `gh` and `claude` CLI authentication

## Development

### Development Mode Setup (Best of Both Worlds)

When developing features, the standard installation copies files to `~/.claude-gh-standup/`, which means every code change requires reinstalling. For fast iteration, use **symlink mode** to make your development code immediately available to the slash command.

#### Quick Setup: Development Mode

**1. Clone repository for development:**
```bash
git clone https://github.com/jetmobsol/claude-gh-standup.git ~/projects/claude-gh-standup
cd ~/projects/claude-gh-standup
git checkout -b feature/my-feature
```

**2. Create symlink installation (instead of copying files):**
```bash
# Remove existing installation if present
rm -rf ~/.claude-gh-standup

# Create symlink to your dev directory
ln -s ~/projects/claude-gh-standup ~/.claude-gh-standup

# Create slash command symlink for Claude Code
mkdir -p ~/.claude/commands
rm -rf ~/.claude/commands/claude-gh-standup
ln -s ~/.claude-gh-standup ~/.claude/commands/claude-gh-standup
```

**3. Test your changes instantly:**
```bash
# Edit code in your dev directory
vim ~/projects/claude-gh-standup/scripts/Main.java

# Test immediately - no reinstall needed!
/claude-gh-standup --yesterday

# Or test directly with JBang for even faster iteration
jbang scripts/Main.java --yesterday --debug
```

#### Development Workflow Comparison

| Method | Speed | Use Case | Invocation |
|--------|-------|----------|------------|
| **JBang Direct** | ⚡ Fastest | Quick script testing | `jbang scripts/Main.java --args` |
| **Symlink Mode** | ⚡ Fast | Full slash command testing | `/claude-gh-standup --args` |
| **Install Script** | 🐌 Slow | Production installation | Requires `./install.sh` on every change |

#### Switching Between Dev and Production

**Switch to production (stable main branch):**
```bash
# Remove symlinks
rm ~/.claude-gh-standup
rm ~/.claude/commands/claude-gh-standup

# Run normal installation (copies files from main branch)
curl -fsSL https://raw.githubusercontent.com/jetmobsol/claude-gh-standup/main/install.sh | bash
```

**Switch back to development:**
```bash
# Remove copied files
rm -rf ~/.claude-gh-standup

# Recreate symlinks
ln -s ~/projects/claude-gh-standup ~/.claude-gh-standup
ln -s ~/.claude-gh-standup ~/.claude/commands/claude-gh-standup
```

#### Verifying Development Mode

**Check symlinks are correct:**
```bash
# Should point to your dev directory
ls -la ~/.claude-gh-standup
# Example output: ~/.claude-gh-standup -> /Users/you/projects/claude-gh-standup

ls -la ~/.claude/commands/claude-gh-standup
# Example output: ~/.claude/commands/claude-gh-standup -> /Users/you/.claude-gh-standup
```

**Verify slash command uses dev code:**
```bash
# Add a debug statement to Main.java
echo 'System.err.println("🚧 DEV MODE ACTIVE");' >> scripts/Main.java

# Run slash command - should see your debug message
/claude-gh-standup --help

# Clean up
git checkout scripts/Main.java
```

#### Development Best Practices

1. **Fast Iteration**: Use `jbang scripts/Main.java` for quick testing
2. **Integration Testing**: Use `/claude-gh-standup` to test the full slash command
3. **Branching**: Always develop on feature branches, never on main
4. **Symlinks**: Keep symlinks during development, reinstall for production use
5. **Config Isolation**: Test multi-directory with separate config.json in dev directory

#### Testing Individual Scripts

Each Java script can be tested independently with JBang:

```bash
# Test configuration management
jbang scripts/ConfigManager.java init
jbang scripts/ConfigManager.java add ~/projects/myapp myapp-main
jbang scripts/ConfigManager.java list

# Test local change detection
jbang scripts/LocalChangesDetector.java myapp-id ~/projects/myapp main

# Test activity aggregation (requires config JSON)
jbang scripts/ActivityAggregator.java '{"directories":[...]}' username 3

# Test GitHub activity collection
jbang scripts/CollectActivity.java octocat 3

# Test diff analysis
jbang scripts/AnalyzeDiffs.java '{"pull_requests":[]}'

# Test report generation
jbang scripts/GenerateReport.java '<activity-json>' '<diff-summary>'
```

### Project Structure

```
claude-gh-standup/
├── .claude/
│   ├── commands/
│   │   └── claude-gh-standup.md  # Slash command definition (REQUIRED)
│   └── settings.json             # Claude Code permissions
├── install.sh                    # Installation script
├── config.json                   # Empty config (shipped in repo)
├── config.example.json           # Example with 2 directories
├── README.md                     # This file
├── LICENSE                       # MIT License with attribution
├── CONTRIBUTING.md               # Contribution guidelines
├── CLAUDE.md                     # Claude Code project guidance
├── scripts/                      # JBang Java scripts
│   ├── Main.java
│   ├── ConfigManager.java
│   ├── LocalChangesDetector.java
│   ├── ActivityAggregator.java
│   ├── CollectActivity.java
│   ├── AnalyzeDiffs.java
│   ├── GenerateReport.java
│   ├── ExportUtils.java
│   └── TeamAggregator.java
├── prompts/                      # Prompt templates
│   ├── standup.prompt.md
│   ├── multidir-standup.prompt.md
│   └── team.prompt.md
├── examples/                     # Sample output files
└── openspec/                     # OpenSpec specifications
```

### Configuration File Format

Multi-directory mode uses `~/.claude-gh-standup/config.json`:

```json
{
  "version": "1.0",
  "directories": [
    {
      "id": "myapp-main",
      "path": "~/projects/myapp",
      "branch": "main",
      "enabled": true,
      "remoteUrl": "git@github.com:owner/myapp.git",
      "repoName": "owner/myapp"
    }
  ],
  "reportSettings": {
    "defaultDays": 1,
    "autoSaveReports": true,
    "reportDirectory": "~/.claude-gh-standup/reports"
  }
}
```

**Notes:**
- Empty `directories` array = legacy mode (current directory only)
- Git info (branch, remoteUrl, repoName) auto-detected via `--config-add`
- Reports auto-saved to `reportDirectory` with filename `YYYY-MM-DD-repo.md`

## Troubleshooting

### Development Mode Issues

**"Changes not reflected in slash command"**
- Verify symlinks exist: `ls -la ~/.claude-gh-standup`
- Ensure symlink points to correct dev directory
- If using copied installation, you need to reinstall after each change
- Switch to symlink mode (see Development section)

**"Using old version of code"**
- Check if `~/.claude-gh-standup` is a symlink or copied directory:
  - Symlink: `ls -la ~/.claude-gh-standup` shows `->` pointing to dev directory
  - Copied: Shows as regular directory - requires reinstallation for updates
- Recreate symlinks if needed (see Development > Switching Between Dev and Production)

**"Testing on wrong branch"**
- Symlink mode uses whatever branch you're on in dev directory
- Check current branch: `cd ~/.claude-gh-standup && git branch`
- Switch branch in dev directory: `cd ~/projects/claude-gh-standup && git checkout main`

### "Command not found"
- Ensure repository is in `~/.claude/commands/claude-gh-standup/` or `.claude/commands/claude-gh-standup/`
- Run `/help` in Claude Code to verify command appears in list
- Restart Claude Code to reload commands

### Multi-Directory Mode Issues

**"No directories configured"**
- Run `--config-init` to create configuration file
- Use `--config-add` to add directories
- Check `~/.claude-gh-standup/config.json` exists

**"Directory not found" when adding**
- Ensure the path exists and is a valid git repository
- Use absolute paths or `~/` for home directory
- Current directory can be added with `--config-add .`

**Reports show only legacy mode despite having config**
- Ensure `directories` array in config is not empty
- Check that at least one directory has `"enabled": true`
- Use `--config-list` to verify configuration

**Local changes not detected**
- Ensure directory path is correct (not symlink)
- Verify git repository has a configured remote (`git remote -v`)
- Check that branch exists on remote (`git branch -r`)

### "Commit search failed"
- This is common due to GitHub API restrictions
- The tool continues with PRs, issues, and reviews
- Use `--days` to limit scope if needed

### "gh: command not found"
- Install GitHub CLI: https://github.com/cli/cli#installation
- Authenticate: `gh auth login`

### "jbang: command not found"
- Install JBang: `curl -Ls https://sh.jbang.dev | bash -s - app setup`
- Verify: `jbang version`

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

MIT License - See [LICENSE](LICENSE) file.

This project includes attribution to the original [gh-standup](https://github.com/sgoedecke/gh-standup) project by Sean Goedecke.

## Related Projects

- [gh-standup](https://github.com/sgoedecke/gh-standup) - Original Go-based GitHub CLI extension
- [Claude Code](https://docs.anthropic.com/claude/docs/claude-code) - Official Claude Code documentation

---

**Made with Claude Code**
