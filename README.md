# claude-gh-standup

> AI-powered GitHub standup reports for Claude Code

Generate professional standup reports from your GitHub activity using Claude AI. Analyzes commits, pull requests, issues, and code reviews with file diff context to produce meaningful summaries.

## Features

- **Repository-Aware** - Automatically detects and analyzes activity in the current git repository
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

### Install Command

**User-level** (available in all projects):
```bash
git clone <repo-url> ~/.claude/commands/claude-gh-standup/
```

**Project-level** (available only in current project):
```bash
git clone <repo-url> .claude/commands/claude-gh-standup/
```

After installation, restart Claude Code or reload commands.

## Usage

### Basic Examples

```bash
# Yesterday's activity in current repository
/claude-gh-standup

# Last 7 days in current repository
/claude-gh-standup --days 7

# Specific user in current repository
/claude-gh-standup --user octocat --days 3

# Override repository (analyze different repo)
/claude-gh-standup --repo owner/repo-name --days 7
```

**Note**: By default, the command analyzes activity in the **current git repository**. If you're not in a git repository or want to analyze a different one, use `--repo owner/repo`.

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

1. **Collect Activity** - Uses `gh` CLI to search commits, PRs, issues, reviews
2. **Analyze Diffs** - Parses `gh pr diff` output for file change statistics
3. **Generate Report** - Injects data into prompt template and calls `claude -p`
4. **Format Output** - Exports to chosen format (Markdown/JSON/HTML)

## Architecture

Built with **Java + JBang** for single-file executable scripts:

```
scripts/
├── Main.java              # Entry point, argument parsing, workflow orchestration
├── CollectActivity.java   # GitHub activity collection via gh CLI
├── AnalyzeDiffs.java      # File diff analysis
├── GenerateReport.java    # Claude AI integration
├── ExportUtils.java       # Format conversion (MD/JSON/HTML)
└── TeamAggregator.java    # Multi-user report consolidation

prompts/
├── standup.prompt.md      # Individual standup prompt template
└── team.prompt.md         # Team aggregation prompt template
```

**Key Pattern** (from [tac-1](https://github.com/tac-1)):
```java
ProcessBuilder pb = new ProcessBuilder("claude", "-p", fullPrompt);
pb.inheritIO();  // Seamlessly pipes claude output to stdout
Process process = pb.start();
```

## Attribution

This project is inspired by and adapted from **[gh-standup](https://github.com/sgoedecke/gh-standup)** by [@sgoedecke](https://github.com/sgoedecke).

**Key Differences**:
- Original: Go-based GitHub CLI extension using GitHub Models LLM API
- This version: Java/JBang implementation using `claude -p` for Claude Code integration
- Original: Requires GitHub Models API access
- This version: Uses existing `gh` and `claude` CLI authentication

## Development

### Testing Individual Scripts

Each Java script can be tested independently with JBang:

```bash
# Test activity collection
jbang scripts/CollectActivity.java octocat 3

# Test diff analysis (pass activity JSON as argument)
jbang scripts/AnalyzeDiffs.java '{"pull_requests":[]}'

# Test report generation
jbang scripts/GenerateReport.java '<activity-json>' '<diff-summary>'
```

### Project Structure

```
claude-gh-standup/
├── claude-gh-standup.md   # Slash command definition (REQUIRED)
├── README.md              # This file
├── LICENSE                # MIT License with attribution
├── CONTRIBUTING.md        # Contribution guidelines
├── CLAUDE.md              # Claude Code project guidance
├── scripts/               # JBang Java scripts
├── prompts/               # Prompt templates
├── examples/              # Sample output files
└── openspec/              # OpenSpec specifications
```

## Troubleshooting

### "Command not found"
- Ensure repository is in `~/.claude/commands/claude-gh-standup/` or `.claude/commands/claude-gh-standup/`
- Run `/help` in Claude Code to verify command appears in list
- Restart Claude Code to reload commands

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
- [tac-1](https://github.com/tac-1) - JBang + ProcessBuilder pattern reference
- [Claude Code](https://docs.anthropic.com/claude/docs/claude-code) - Official Claude Code documentation

---

**Made with Claude Code**
