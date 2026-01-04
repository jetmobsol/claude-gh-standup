# claude-gh-standup

> AI-powered GitHub standup reports for Claude Code

Generate professional standup reports from your GitHub activity using Claude AI. Supports both single-repository mode and multi-directory tracking across multiple branches/repos with local work-in-progress detection.

## Features

- **Multi-Directory Support** - Track multiple branches/repos simultaneously with local WIP detection
- **Local Change Detection** - Shows uncommitted and unpushed work alongside GitHub activity
- **Smart Date Shortcuts** - `--yesterday` (Friday-aware on Monday), `--last-week`
- **Complete GitHub Activity** - Shows ALL repositories you worked on (multi-directory mode adds local WIP, not filters activity)
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

```bash
# 1. Clone to a dedicated directory (NOT inside .claude/commands/)
git clone https://github.com/jetmobsol/claude-gh-standup.git ~/.claude-gh-standup

# 2. Symlink ONLY the command file to Claude Code
mkdir -p ~/.claude/commands
ln -sf ~/.claude-gh-standup/.claude/commands/claude-gh-standup.md ~/.claude/commands/claude-gh-standup.md
```

After installation, restart Claude Code or reload commands.

> **Why this approach?** Cloning the entire repo into `~/.claude/commands/` causes all markdown files (docs, specs, etc.) to appear as commands. Symlinking just the command file keeps it clean.

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
- GitHub activity from ALL repositories you worked on (not just configured ones)
- Local uncommitted changes per configured directory
- Unpushed commits per configured directory
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
3. **ALL-Repository GitHub Activity** - Fetches activity from ALL user repositories (single API call)
4. **Activity Aggregation** - Combines local WIP (configured dirs) + GitHub (all repos) into unified JSON
5. **Generate Report** - Uses multi-directory prompt template with `claude -p`
6. **Auto-Save** - Saves report to `~/.claude-gh-standup/reports/YYYY-MM-DD-*.md`

**Two-Tier Approach:**
- **GitHub Activity**: Shows ALL repositories you worked on (same as legacy mode)
- **Local WIP**: Shows only configured directories' uncommitted/unpushed work

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

*ALL-Repository GitHub Activity:*
```java
// Fetch ALL user activity (same as legacy mode - no filtering by config)
JsonObject activity = collectGitHubActivityAllRepos(user, days);
// Single API call, grouped by repository in output
```

## Attribution

This project is inspired by and adapted from **[gh-standup](https://github.com/sgoedecke/gh-standup)** by [@sgoedecke](https://github.com/sgoedecke).

**Key Differences**:
- Original: Go-based GitHub CLI extension using GitHub Models LLM API
- This version: Java/JBang implementation using `claude -p` for Claude Code integration
- Original: Requires GitHub Models API access
- This version: Uses existing `gh` and `claude` CLI authentication

## Development

### Testing Scripts

Test individual scripts directly with JBang:

```bash
# Test GitHub activity collection
jbang scripts/CollectActivity.java octocat 3

# Test configuration management
jbang scripts/ConfigManager.java list

# Test full workflow
jbang scripts/Main.java --yesterday
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

### "Command not found"
- Verify symlink exists: `ls -la ~/.claude/commands/claude-gh-standup.md`
- Check it points to: `~/.claude-gh-standup/.claude/commands/claude-gh-standup.md`
- Run `/help` in Claude Code to verify command appears
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
