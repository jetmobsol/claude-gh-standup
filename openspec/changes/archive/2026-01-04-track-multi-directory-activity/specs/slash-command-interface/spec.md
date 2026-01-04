# Capability: slash-command-interface

## ADDED Requirements

### Requirement: Configuration Management Commands

The system MUST provide CLI commands for managing multi-directory configuration.

#### Scenario: Initialize empty configuration
```
GIVEN no config.json exists at ~/.claude-gh-standup/
WHEN the user runs /claude-gh-standup --config-init
THEN config.json is created with empty directories array
AND default reportSettings are set
AND a confirmation message is displayed
```

#### Scenario: Add directory to configuration
```
GIVEN the user is in a git repository
WHEN they run /claude-gh-standup --config-add ~/projects/myapp --id main-branch
THEN the directory is added to config.json
AND git branch, remote URL, and repo name are auto-detected
AND a confirmation message is displayed with detected values
```

#### Scenario: List configured directories
```
GIVEN config.json contains 3 directories
WHEN the user runs /claude-gh-standup --config-list
THEN all directories are displayed in a table format
AND each row shows: ID, path, branch, repository, enabled status
```

#### Scenario: Remove directory from configuration
```
GIVEN config.json contains a directory with ID "feature-branch"
WHEN the user runs /claude-gh-standup --config-remove feature-branch
THEN the directory is removed from config.json
AND a confirmation message is displayed
```

---

### Requirement: Date Convenience Flags

The system MUST provide convenient date range shortcuts.

#### Scenario: Yesterday flag on Tuesday-Friday
```
GIVEN today is Tuesday (not Monday)
WHEN the user runs /claude-gh-standup --yesterday
THEN the system queries for 1 day of activity
```

#### Scenario: Yesterday flag on Monday
```
GIVEN today is Monday
WHEN the user runs /claude-gh-standup --yesterday
THEN the system queries for 3 days of activity (covering Friday-Sunday)
```

#### Scenario: Last week flag
```
GIVEN the user runs /claude-gh-standup --last-week
WHEN the system processes the flag
THEN the system queries for 7 days of activity
```

#### Scenario: Flag precedence (yesterday overrides days)
```
GIVEN the user runs /claude-gh-standup --yesterday --days 5
WHEN the system calculates the date range
THEN --yesterday takes precedence (1 or 3 days depending on day of week)
AND --days 5 is ignored
```

---

### Requirement: Multi-Directory Mode Detection

The system MUST detect when to use multi-directory mode vs legacy single-directory mode.

#### Scenario: Multi-directory mode (config exists with directories)
```
GIVEN config.json exists with 3 directories in the directories array
AND no --repo flag is provided
WHEN the user runs /claude-gh-standup
THEN the system uses multi-directory mode
AND all 3 directories are processed
```

#### Scenario: Legacy mode (no config file)
```
GIVEN config.json does not exist
WHEN the user runs /claude-gh-standup
THEN the system uses legacy single-directory mode
AND behaves identically to the current implementation
```

#### Scenario: Legacy mode (empty directories array)
```
GIVEN config.json exists but directories array is empty []
WHEN the user runs /claude-gh-standup
THEN the system uses legacy single-directory mode
AND behaves identically to the current implementation
```

#### Scenario: Legacy mode (explicit --repo override)
```
GIVEN config.json exists with 3 directories
AND the user runs /claude-gh-standup --repo owner/myapp
WHEN the system processes the command
THEN the system uses legacy single-directory mode
AND only the specified repository is queried
AND configured directories are ignored
```

---

### Requirement: Help Text Updates

The system MUST include new flags in the --help output.

#### Scenario: Display configuration commands in help
```
GIVEN the user runs /claude-gh-standup --help
WHEN the help text is displayed
THEN it includes a "Configuration:" section with:
  --config-add PATH [--id ID]  Add directory to config
  --config-list                List configured directories
  --config-remove ID           Remove directory from config
  --config-init                Initialize configuration file
```

#### Scenario: Display date shortcuts in help
```
GIVEN the user runs /claude-gh-standup --help
WHEN the help text is displayed
THEN it includes a "Date Shortcuts:" section with:
  --yesterday                  Yesterday's work (Friday if Monday)
  --last-week                  Last 7 days of activity
```

---

## MODIFIED Requirements

### Requirement: Argument Parsing (MODIFIED)

The argument parsing logic MUST be extended to support new flags.

#### Scenario: Parse configuration flags
```
GIVEN the user runs a command with --config-add ~/path --id myid
WHEN the system parses arguments
THEN parsed.configCommand is "add"
AND parsed.configPath is "~/path"
AND parsed.configId is "myid"
```

#### Scenario: Parse date convenience flags
```
GIVEN the user runs --yesterday
WHEN the system parses arguments
THEN parsed.yesterday is true
AND parsed.lastWeek is false
```

#### Scenario: Parse --last-week flag
```
GIVEN the user runs --last-week
WHEN the system parses arguments
THEN parsed.lastWeek is true
AND parsed.yesterday is false
```

---

### Requirement: Workflow Orchestration (MODIFIED)

The main workflow MUST be split into multi-directory and single-directory modes.

#### Scenario: Multi-directory workflow execution
```
GIVEN multi-directory mode is enabled
WHEN the user runs /claude-gh-standup --yesterday
THEN the system:
  1. Loads config from ConfigManager
  2. Filters enabled directories
  3. Calculates effective days (3 on Monday)
  4. Calls ActivityAggregator to collect data
  5. Formats multi-directory prompt
  6. Generates report via Claude AI
  7. Auto-saves report to ~/.claude-gh-standup/reports/
  8. Outputs report to stdout
```

#### Scenario: Single-directory workflow execution (unchanged)
```
GIVEN legacy single-directory mode is enabled
WHEN the user runs /claude-gh-standup --days 3
THEN the system:
  1. Detects current repository OR uses --repo flag
  2. Calls CollectActivity.java
  3. Calls AnalyzeDiffs.java
  4. Formats single-directory prompt
  5. Generates report via Claude AI
  6. Outputs report to stdout (no auto-save in legacy mode)
```

---

### Requirement: Report Auto-Save (ADDED)

The system MUST auto-save reports to a configured directory when in multi-directory mode.

#### Scenario: Auto-save enabled (default)
```
GIVEN reportSettings.autoSaveReports is true (default)
AND multi-directory mode is active
WHEN a report is generated
THEN the report is saved to ~/.claude-gh-standup/reports/
AND the filename is YYYY-MM-DD-repo-name.md (single repo) or YYYY-MM-DD-multi.md (multiple repos)
AND a message is displayed: "Report saved to <path>"
```

#### Scenario: Auto-save disabled
```
GIVEN reportSettings.autoSaveReports is false
WHEN a report is generated
THEN the report is NOT saved to disk
AND only stdout output is produced
```

#### Scenario: Create reports directory if missing
```
GIVEN ~/.claude-gh-standup/reports/ does not exist
AND auto-save is enabled
WHEN a report is generated
THEN the reports/ directory is created automatically
AND the report is saved successfully
```

#### Scenario: Single repository filename
```
GIVEN all directories point to the same repository "owner/myapp"
WHEN a report is saved
THEN the filename is 2026-01-04-owner-myapp.md (using ISO date format)
```

#### Scenario: Multiple repositories filename
```
GIVEN directories point to 3 different repositories
WHEN a report is saved
THEN the filename is 2026-01-04-multi.md
```

---

## MODIFIED Requirements (Backward Compatibility)

### Requirement: Backward Compatibility (MODIFIED)

All existing flags MUST continue to work identically when configuration is not used.

#### Scenario: Existing --days flag works in legacy mode
```
GIVEN no config.json exists
WHEN the user runs /claude-gh-standup --days 7
THEN the system queries for 7 days of activity (unchanged behavior)
```

#### Scenario: Existing --user flag works in both modes
```
GIVEN the user runs /claude-gh-standup --user octocat
WHEN the system processes the command
THEN activity is collected for user "octocat" (works in both modes)
```

#### Scenario: Existing --repo flag works in legacy mode
```
GIVEN the user runs /claude-gh-standup --repo owner/myapp --days 3
WHEN the system processes the command
THEN legacy single-directory mode is used
AND activity is collected for "owner/myapp" only
```

#### Scenario: Existing --format and --output flags work in both modes
```
GIVEN the user runs /claude-gh-standup --format json --output report.json
WHEN the system generates a report
THEN the output is JSON format and saved to report.json (unchanged behavior)
```

#### Scenario: Existing --team flag works in legacy mode
```
GIVEN the user runs /claude-gh-standup --team alice bob --days 7
WHEN the system processes the command
THEN team aggregation is performed in legacy mode (unchanged)
```

#### Scenario: Existing --no-claude flag works in both modes
```
GIVEN the user runs /claude-gh-standup --no-claude --yesterday
WHEN the system processes the command
THEN the raw prompt is output to stdout (no Claude AI call)
AND this works identically in both multi-dir and single-dir modes
```
