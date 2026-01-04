# Multi-Directory Standup Report Implementation Plan

## Overview

Transform claude-gh-standup to support tracking multiple directories (same repo, different branches) with intelligent deduplication, local change detection, and convenient date range aliases.

## User Requirements

1. **Multi-directory tracking**: Configure multiple directories for the same repository (different branches)
2. **Smart reporting**: Show local uncommitted/unpushed work per directory + deduplicated GitHub activity
3. **Date convenience**: Add `--yesterday` and `--last-week` flags with smart logic (Friday on Monday)
4. **Centralized reports**: Save to `~/.claude-gh-standup/reports/`
5. **Configuration**: Store in `~/.claude-gh-standup/config.json`
6. **Auto-detection**: Slash command automatically uses configured directories when available

## Implementation Sequence

### Phase 1: Configuration Infrastructure

**1.1 Create ConfigManager.java** (`scripts/ConfigManager.java`)

New JBang script to handle configuration operations:

```java
//DEPS com.google.code.gson:gson:2.10.1

// Configuration structure:
class Config {
    String version = "1.0";
    List<Directory> directories = new ArrayList<>();
    ReportSettings reportSettings = new ReportSettings();
}

class Directory {
    String id;          // Unique identifier
    String path;        // Absolute path to directory
    String branch;      // Current branch (auto-detected)
    boolean enabled;    // Whether to include in reports
    String remoteUrl;   // Git remote URL (auto-detected)
    String repoName;    // owner/repo format (auto-detected)
}

class ReportSettings {
    int defaultDays = 1;
    boolean autoSaveReports = true;
    String reportDirectory = "~/.claude-gh-standup/reports";
}
```

**Key Methods**:
- `loadConfig()` - Load from `~/.claude-gh-standup/config.json`
- `saveConfig(Config)` - Save to config file
- `addDirectory(String path, String id)` - Add directory with git auto-detection
- `removeDirectory(String id)` - Remove by ID
- `listDirectories()` - List all configured directories
- `expandTilde(String path)` - Handle `~` in paths

**Git Detection**:
- Use `git -C <path> remote get-url origin` to detect remote
- Use `git -C <path> rev-parse --abbrev-ref HEAD` to detect branch
- Parse remote URL to extract `owner/repo` format

**1.2 Create Config Files**

Create `config.json` (empty config file):
```json
{
  "version": "1.0",
  "directories": [],
  "reportSettings": {
    "defaultDays": 1,
    "autoSaveReports": true,
    "reportDirectory": "~/.claude-gh-standup/reports"
  }
}
```

Create `config.example.json` (example with 2 directories):
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
    },
    {
      "id": "myapp-feature",
      "path": "~/projects/myapp-feature",
      "branch": "feature/new-ui",
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

**IMPORTANT**: If `config.json` exists but has empty `directories` array `[]`, the tool will fall back to **legacy single-directory mode** (current behavior).

**1.3 Update Main.java - Config Commands**

Add new CLI flags:
```java
case "--config-add":
    // Add directory to config
case "--config-list":
    // List configured directories
case "--config-remove":
    // Remove directory from config
case "--config-init":
    // Initialize empty config file
```

Handle config commands before main execution:
```java
if (parsed.configCommand != null) {
    handleConfigCommand(parsed);
    return;
}
```

### Phase 2: Local Change Detection

**2.1 Create LocalChangesDetector.java** (`scripts/LocalChangesDetector.java`)

New JBang script to detect local git changes:

**Git Commands**:
```bash
# Uncommitted changes (unstaged)
git -C <path> diff --stat

# Uncommitted changes (staged)
git -C <path> diff --cached --stat

# Unpushed commits
git -C <path> log origin/<branch>..HEAD --oneline --format="%h %s"

# Check remote branch exists
git -C <path> rev-parse --verify origin/<branch>
```

**Output JSON Structure**:
```json
{
  "directoryId": "project-main",
  "path": "/Users/garden/projects/myapp",
  "branch": "main",
  "uncommitted": {
    "hasChanges": true,
    "filesChanged": 3,
    "staged": ["file1.java", "file2.java"],
    "unstaged": ["file3.java"],
    "summary": "3 files changed, 45 insertions(+), 12 deletions(-)"
  },
  "unpushed": {
    "hasCommits": true,
    "count": 2,
    "commits": [
      "abc1234 Fix bug in login flow",
      "def5678 Add validation"
    ]
  }
}
```

### Phase 3: Date Range Convenience

**3.1 Update Main.java - Date Flags**

Add new flags:
```java
case "--yesterday":
    parsed.yesterday = true;
    break;
case "--last-week":
    parsed.lastWeek = true;
    break;
```

Add date calculation logic:
```java
private static int calculateDays(Args parsed) {
    if (parsed.yesterday) {
        LocalDate today = LocalDate.now();
        // If Monday, look back to Friday (3 days)
        if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            return 3;
        }
        return 1;
    }
    if (parsed.lastWeek) {
        return 7;
    }
    return parsed.days;
}
```

### Phase 4: Activity Aggregation & Deduplication

**4.1 Create ActivityAggregator.java** (`scripts/ActivityAggregator.java`)

Orchestrate multi-directory data collection:

**Process**:
1. Group directories by `repoName` (deduplicate repos)
2. For each unique repo → call `CollectActivity.java` ONCE
3. For each directory → call `LocalChangesDetector.java`
4. Merge into unified JSON structure

**Output Structure**:
```json
{
  "githubActivity": {
    "owner/myapp": { /* commits, PRs, issues */ },
    "owner/other": { /* commits, PRs, issues */ }
  },
  "localChanges": [
    { /* directory 1 local changes */ },
    { /* directory 2 local changes */ }
  ],
  "metadata": {
    "user": "octocat",
    "days": 1,
    "directoryCount": 3,
    "repoCount": 2
  }
}
```

**Parallel Processing**:
- Use `ExecutorService` to collect local changes in parallel (3-4x faster)
- Fetch GitHub activity serially (already fast via `gh` CLI)

### Phase 5: Enhanced Report Generation

**5.1 Create New Prompt Template** (`prompts/multidir-standup.prompt.md`)

Structure:
```markdown
# Multi-Directory Standup Report Generator

Guidelines:
- Synthesize GitHub activity across all repositories
- Highlight local work-in-progress per directory/branch
- Group related activities logically
- Emphasize context switching if relevant

## GitHub Activity (Deduplicated)

{{githubActivity}}

## Local Changes by Directory

{{localChanges}}

Generate report with:
1. Yesterday's Accomplishments (GitHub)
2. Work in Progress (Local changes per branch)
3. Today's Plans
4. Blockers/Challenges
```

**5.2 Update Main.java - Report Formatting**

Add `formatMultiDirActivities()` method:
- Format GitHub activity grouped by repo
- Format local changes grouped by directory
- Inject into new prompt template

### Phase 6: Report Storage

**6.1 Implement Auto-Save** (in Main.java)

```java
private static void saveReport(String report, Config config, Args parsed) {
    String reportDir = expandTilde(config.reportSettings.reportDirectory);
    Files.createDirectories(Paths.get(reportDir));

    String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    String filename;

    if (uniqueRepoCount == 1) {
        filename = date + "-" + sanitizeRepoName(singleRepo) + ".md";
    } else {
        filename = date + "-multi.md";
    }

    Path filepath = Paths.get(reportDir, filename);
    Files.writeString(filepath, report);
    System.err.println("Report saved to: " + filepath);
}
```

### Phase 7: Multi-Directory Workflow Integration

**7.1 Update Main.java - Mode Detection**

```java
public static void main(String... args) {
    Args parsed = parseArgs(args);

    // Handle config commands first
    if (parsed.configCommand != null) {
        handleConfigCommand(parsed);
        return;
    }

    // Load configuration
    Config config = ConfigManager.loadConfig();

    // Determine execution mode
    // Empty directories array [] = legacy mode (backward compatibility)
    boolean multiDirMode = config != null
        && config.directories != null
        && !config.directories.isEmpty()
        && parsed.repo == null;  // Allow --repo to override

    if (multiDirMode) {
        runMultiDirectoryMode(config, parsed);
    } else {
        runSingleDirectoryMode(parsed);  // Legacy mode
    }
}
```

**7.2 Implement Multi-Directory Workflow**

```java
private static void runMultiDirectoryMode(Config config, Args parsed) {
    // 1. Filter enabled directories
    List<Directory> enabledDirs = config.directories.stream()
        .filter(d -> d.enabled)
        .filter(d -> Files.exists(Paths.get(expandTilde(d.path))))
        .collect(Collectors.toList());

    // 2. Calculate effective days
    int days = calculateDays(parsed);

    // 3. Get user
    String user = parsed.user != null ? parsed.user : getCurrentUser();

    // 4. Aggregate activities (calls ActivityAggregator.java)
    JsonObject aggregated = ActivityAggregator.aggregateActivities(
        enabledDirs, user, days
    );

    // 5. Format prompt with multi-dir template
    String prompt = formatMultiDirPrompt(aggregated);

    // 6. Generate report via claude or output prompt
    String report;
    if (parsed.noClaude) {
        report = prompt;
    } else {
        report = callClaude(prompt);
    }

    // 7. Auto-save if enabled
    if (config.reportSettings.autoSaveReports) {
        saveReport(report, config, parsed);
    }

    // 8. Output to stdout
    System.out.println(report);
}
```

### Phase 8: Documentation & Help

**8.1 Update --help Output** (in Main.java)

Add new flags to help:
```
Configuration:
  --config-add PATH [--id ID]  Add directory to config
  --config-list                List configured directories
  --config-remove ID           Remove directory from config
  --config-init                Initialize configuration file

Date Shortcuts:
  --yesterday                  Yesterday's work (Friday if Monday)
  --last-week                  Last 7 days of activity
```

**8.2 Update Slash Command Documentation** (`.claude/commands/claude-gh-standup.md`)

**IMPORTANT: Update invocation path** (line 77):
```bash
# Change from:
jbang $COMMAND_DIR/scripts/Main.java --no-claude $ARGUMENTS

# To fixed installation path:
jbang ~/.claude-gh-standup/scripts/Main.java --no-claude $ARGUMENTS
```

Add new sections:
- Multi-Directory Setup
- Configuration Management
- Date Range Shortcuts
- Examples with configured directories

## Critical Files to Modify

### New Files
1. `scripts/ConfigManager.java` - Configuration management
2. `scripts/LocalChangesDetector.java` - Local git change detection
3. `scripts/ActivityAggregator.java` - Multi-directory orchestration
4. `prompts/multidir-standup.prompt.md` - Enhanced prompt template
5. `config.json` - Empty config file (shipped in repo)
6. `config.example.json` - Example config with 2 directories

### Modified Files
1. `scripts/Main.java` - Core workflow changes:
   - Add config command handling (lines ~80-90)
   - Add date range flags (lines ~80-90)
   - Add mode detection logic (lines ~240-280)
   - Add `runMultiDirectoryMode()` method
   - Add report save logic
2. `.claude/commands/claude-gh-standup.md` - Documentation updates

## Backward Compatibility

**Guaranteed**:
- If no config file exists → behaves exactly as current version (legacy mode)
- If config file exists but `directories` array is empty `[]` → legacy mode
- If config exists but `--repo` flag provided → overrides config, uses single-repo mode
- All existing flags continue to work identically

**Migration Path**:
```bash
# Step 1: Continue using normally (no config)
/claude-gh-standup --days 3

# Step 2: Initialize config when ready
/claude-gh-standup --config-init

# Step 3: Add directories
/claude-gh-standup --config-add ~/projects/myapp/main --id main-branch
/claude-gh-standup --config-add ~/projects/myapp/feature --id feature-branch

# Step 4: Now slash command automatically uses multi-dir mode
/claude-gh-standup --yesterday
```

## Error Handling

1. **Missing directories**: Skip with warning, continue with remaining
2. **Git permission issues**: Catch exception, log warning, continue
3. **Remote branch doesn't exist**: Skip unpushed commits check (local-only branch)
4. **No enabled directories**: Fall back to single-directory mode
5. **Config file corruption**: Warn user, use defaults

## Performance Expectations

- Single directory (legacy): No change (~3-5 seconds)
- 3 directories (same repo): ~5-10 seconds (parallel local change detection)
- 3 directories (different repos): ~10-15 seconds (parallel processing)
- 10 directories: ~15-25 seconds (with parallel ExecutorService)

## Installation Architecture

### Directory Structure

```
~/.claude-gh-standup/              # Fixed installation location
├── scripts/
│   ├── Main.java
│   ├── ConfigManager.java
│   ├── LocalChangesDetector.java
│   └── ActivityAggregator.java
├── prompts/
│   ├── standup.prompt.md
│   └── multidir-standup.prompt.md
├── .claude/
│   └── commands/
│       └── claude-gh-standup.md
├── install.sh
├── config.json                    # Empty config (shipped in repo)
└── config.example.json            # Example with 2 directories

~/.claude/commands/
└── claude-gh-standup/  → symlink to ~/.claude-gh-standup/

~/.claude-gh-standup/reports/      # Auto-generated reports
└── 2026-01-04-owner-repo.md
```

### Why This Architecture

**Problem**: Need to support both slash command and direct shell aliases (both need fixed path)

**Solution**: Install to fixed location + symlink for Claude Code
- **Slash command**: Uses fixed path `~/.claude-gh-standup/` directly (line 77 in claude-gh-standup.md)
- **Shell aliases**: Reference same fixed path `~/.claude-gh-standup/`
- **Symlink**: `~/.claude/commands/claude-gh-standup` → `~/.claude-gh-standup/` (for Claude Code discovery)
- **Single source**: All scripts in one place, no duplication
- **Git updates**: `git pull` in `~/.claude-gh-standup/` updates everything

### Shell Aliases

Add to `~/.zshrc` or `~/.bashrc` (minimal set):

```bash
# === claude-gh-standup aliases ===
alias standup-yesterday='jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week='jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
alias standup='jbang ~/.claude-gh-standup/scripts/Main.java'
```

**Note**: Configuration management uses the main command:
- `standup --config-add ~/path`
- `standup --config-list`
- `standup --config-remove id`

### Install Script (`install.sh`)

Create this script in the repository root:

```bash
#!/bin/bash
# install.sh - Install claude-gh-standup to ~/.claude-gh-standup

set -e

INSTALL_DIR="$HOME/.claude-gh-standup"
COMMAND_LINK="$HOME/.claude/commands/claude-gh-standup"

echo "Installing claude-gh-standup..."
echo ""

# 1. Check prerequisites
echo "Checking prerequisites..."
command -v jbang >/dev/null 2>&1 || {
    echo "❌ Error: jbang not found"
    echo "   Install: curl -Ls https://sh.jbang.dev | bash -s - app setup"
    exit 1
}
command -v gh >/dev/null 2>&1 || {
    echo "❌ Error: gh CLI not found"
    echo "   Install: https://cli.github.com"
    exit 1
}
command -v git >/dev/null 2>&1 || {
    echo "❌ Error: git not found"
    exit 1
}
echo "✓ Prerequisites satisfied"
echo ""

# 2. Determine installation source
if [ -d ".git" ]; then
    # Running from cloned repo
    SOURCE_DIR="$(pwd)"
    echo "Installing from current directory: $SOURCE_DIR"
else
    # Need to clone
    echo "Cloning repository..."
    TEMP_DIR=$(mktemp -d)
    git clone https://github.com/YOUR-ORG/claude-gh-standup.git "$TEMP_DIR"
    SOURCE_DIR="$TEMP_DIR"
fi

# 3. Install to fixed location
if [ -d "$INSTALL_DIR" ]; then
    echo "Existing installation found at $INSTALL_DIR"
    read -p "Update existing installation? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Updating installation..."
        rsync -av --exclude='.git' --exclude='config.json' "$SOURCE_DIR/" "$INSTALL_DIR/"
        echo "✓ Updated successfully!"
    else
        echo "Installation cancelled"
        exit 0
    fi
else
    echo "Installing to $INSTALL_DIR..."
    mkdir -p "$INSTALL_DIR"
    rsync -av --exclude='.git' "$SOURCE_DIR/" "$INSTALL_DIR/"
    echo "✓ Installed to $INSTALL_DIR"
fi

# 4. Create symlink for Claude Code
echo ""
echo "Creating symlink for Claude Code slash command..."
mkdir -p "$HOME/.claude/commands"
if [ -L "$COMMAND_LINK" ] || [ -e "$COMMAND_LINK" ]; then
    rm -rf "$COMMAND_LINK"
fi
ln -s "$INSTALL_DIR" "$COMMAND_LINK"
echo "✓ Symlink created: $COMMAND_LINK → $INSTALL_DIR"

# 5. Make scripts executable
chmod +x "$INSTALL_DIR/scripts"/*.java 2>/dev/null || true

# 6. Offer to install shell aliases
echo ""
echo "═══════════════════════════════════════════════"
echo "Shell Aliases Installation"
echo "═══════════════════════════════════════════════"
echo ""
echo "This will add convenient aliases to your shell:"
echo "  • standup-yesterday - Quick yesterday report (Friday if Monday)"
echo "  • standup-week      - Last week's activity"
echo "  • standup           - General command with all flags"
echo ""
read -p "Install shell aliases? (y/n) " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Detect shell
    SHELL_RC=""
    if [ -n "$ZSH_VERSION" ]; then
        SHELL_RC="$HOME/.zshrc"
    elif [ -n "$BASH_VERSION" ]; then
        if [ -f "$HOME/.bashrc" ]; then
            SHELL_RC="$HOME/.bashrc"
        elif [ -f "$HOME/.bash_profile" ]; then
            SHELL_RC="$HOME/.bash_profile"
        fi
    fi

    if [ -z "$SHELL_RC" ]; then
        echo ""
        echo "Could not detect shell config file."
        echo "Please manually add these aliases to your shell config:"
        echo ""
        cat <<'EOF'
# === claude-gh-standup aliases ===
alias standup-yesterday='jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week='jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
alias standup='jbang ~/.claude-gh-standup/scripts/Main.java'
EOF
    else
        echo "Adding aliases to $SHELL_RC..."

        # Check if aliases already exist
        if grep -q "claude-gh-standup aliases" "$SHELL_RC" 2>/dev/null; then
            echo "⚠ Aliases already exist in $SHELL_RC (skipping)"
        else
            cat >> "$SHELL_RC" <<'EOF'

# === claude-gh-standup aliases ===
alias standup-yesterday='jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week='jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
alias standup='jbang ~/.claude-gh-standup/scripts/Main.java'
EOF
            echo "✓ Aliases added to $SHELL_RC"
            echo ""
            echo "Run to activate aliases: source $SHELL_RC"
        fi
    fi
fi

# 7. Initialize config if desired
echo ""
echo "═══════════════════════════════════════════════"
echo "Configuration Setup"
echo "═══════════════════════════════════════════════"
echo ""
read -p "Initialize configuration file now? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    jbang "$INSTALL_DIR/scripts/Main.java" --config-init
    echo "✓ Config initialized at ~/.claude-gh-standup/config.json"
fi

# Clean up temp directory if used
if [ -n "$TEMP_DIR" ] && [ -d "$TEMP_DIR" ]; then
    rm -rf "$TEMP_DIR"
fi

# 8. Success summary
echo ""
echo "═══════════════════════════════════════════════"
echo "✓ Installation Complete!"
echo "═══════════════════════════════════════════════"
echo ""
echo "Three ways to use claude-gh-standup:"
echo ""
echo "1. Claude Code slash command:"
echo "   /claude-gh-standup --yesterday"
echo ""
echo "2. Shell aliases (if installed):"
echo "   standup-yesterday"
echo "   standup-week"
echo "   standup --days 3"
echo ""
echo "3. Direct jbang call:"
echo "   jbang ~/.claude-gh-standup/scripts/Main.java --yesterday"
echo ""
echo "Next steps:"
if [ -n "$SHELL_RC" ]; then
    echo "  1. Activate aliases: source $SHELL_RC"
fi
echo "  2. Add directories: standup --config-add ~/projects/my-repo"
echo "  3. List directories: standup --config-list"
echo "  4. Generate report: standup-yesterday"
echo ""
echo "For help: standup --help"
echo ""
```

### Usage Workflow

**Installation**:
```bash
# Clone and run install script
git clone https://github.com/YOUR-ORG/claude-gh-standup.git
cd claude-gh-standup
chmod +x install.sh
./install.sh

# Or one-liner
curl -sSL https://raw.githubusercontent.com/YOUR-ORG/claude-gh-standup/main/install.sh | bash
```

**First-time setup**:
```bash
# Initialize config
standup --config-init

# Add your project directories
standup --config-add ~/projects/myapp/main --id main-branch
standup --config-add ~/projects/myapp/feature --id feature-branch

# Verify configuration
standup --config-list
```

**Daily usage - three options**:
```bash
# Option 1: Shell alias (fastest)
standup-yesterday
standup-week
standup --days 3

# Option 2: Claude Code slash command
/claude-gh-standup --yesterday
/claude-gh-standup --last-week

# Option 3: Direct jbang
jbang ~/.claude-gh-standup/scripts/Main.java --yesterday
```

## Testing Strategy

**Manual Testing Checklist**:
1. ✓ No config file (legacy mode)
2. ✓ Config with single directory
3. ✓ Config with multiple directories (same repo, different branches)
4. ✓ Config with multiple directories (different repos)
5. ✓ `--yesterday` on Monday vs other days
6. ✓ `--last-week` functionality
7. ✓ Report auto-save to `~/.claude-gh-standup/reports/`
8. ✓ Config management commands (add/remove/list)
9. ✓ GitHub activity deduplication
10. ✓ Local change detection (uncommitted/unpushed)
11. ✓ Install script works on fresh system
12. ✓ Shell aliases work correctly
13. ✓ Slash command works with symlink
14. ✓ Update via `git pull` in `~/.claude-gh-standup/`

## Implementation Order

1. **Create config files** - config.json (empty) and config.example.json (2 directories)
2. **ConfigManager.java** - Foundation for all other work
3. **Main.java config commands** - Ability to manage config
4. **LocalChangesDetector.java** - Local change detection
5. **ActivityAggregator.java** - Orchestration logic
6. **Main.java date flags** - Date convenience
7. **New prompt template** - Enhanced reporting
8. **Main.java multi-dir mode** - Integration (with empty array = legacy mode)
9. **Report storage** - Auto-save functionality
10. **install.sh** - Installation script with minimal aliases
11. **Documentation updates** - Help and slash command docs (update invocation path)
12. **Testing & refinement** - Comprehensive testing

---

**Estimated Total Implementation Time**: 15-20 hours
