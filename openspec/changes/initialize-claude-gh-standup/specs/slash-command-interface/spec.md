# Capability: Slash Command Interface

## ADDED Requirements

### Requirement: Command Definition File
The system SHALL provide `claude-gh-standup.md` file defining the slash command for Claude Code.

#### Scenario: Command file structure
- **WHEN** command file is read by Claude Code
- **THEN** contains YAML frontmatter with:
  - `description`: Command purpose
  - `argument-hint`: Usage pattern
- **AND** contains Markdown documentation sections:
  - Overview
  - Prerequisites
  - Usage Examples
  - Workflow
  - Arguments Reference
  - Troubleshooting

#### Scenario: Command invocation definition
- **WHEN** command file specifies invocation
- **THEN** uses JBang pattern: `jbang $COMMAND_DIR/scripts/Main.java $ARGUMENTS`
- **AND** passes all user flags to Java script

### Requirement: Argument Parsing
The system SHALL parse command-line arguments in Main.java using standard patterns.

#### Scenario: Parse --days flag
- **WHEN** user provides `--days 7`
- **THEN** parses as integer 7
- **AND** uses for date range calculation

#### Scenario: Parse --user flag
- **WHEN** user provides `--user octocat`
- **THEN** uses "octocat" as target username
- **AND** overrides auto-detection

#### Scenario: Parse --repo flag
- **WHEN** user provides `--repo owner/repo-name`
- **THEN** extracts "owner/repo-name"
- **AND** filters activity to that repository

#### Scenario: Parse --format flag
- **WHEN** user provides `--format json|markdown|html`
- **THEN** validates against allowed values
- **AND** passes to export formatter

#### Scenario: Parse --team flag
- **WHEN** user provides `--team alice bob charlie`
- **THEN** parses as string array: ["alice", "bob", "charlie"]
- **AND** triggers team aggregation mode

#### Scenario: Parse --output flag
- **WHEN** user provides `--output path/to/file.md`
- **THEN** uses as file destination
- **AND** creates file instead of stdout

### Requirement: Command Discovery
The system SHALL be discoverable in Claude Code when installed to correct directory.

#### Scenario: User-level installation
- **WHEN** repository cloned to `~/.claude/commands/claude-gh-standup/`
- **THEN** `/claude-gh-standup` appears in `/help` command list
- **AND** is available in all projects for that user

#### Scenario: Project-level installation
- **WHEN** repository cloned to `.claude/commands/claude-gh-standup/`
- **THEN** `/claude-gh-standup` available only in that project
- **AND** listed in `/help` as "project" command

#### Scenario: Command not found
- **WHEN** repository not in correct location
- **THEN** `/claude-gh-standup` not recognized
- **AND** Claude Code suggests running `/help` to see available commands

### Requirement: Default Values
The system SHALL use sensible defaults when optional flags are omitted.

#### Scenario: Default days
- **WHEN** no `--days` flag provided
- **THEN** defaults to 1 (yesterday's activity)

#### Scenario: Default user
- **WHEN** no `--user` flag provided
- **THEN** auto-detects from `gh api user --jq .login`

#### Scenario: Default format
- **WHEN** no `--format` flag provided
- **THEN** defaults to "markdown"

#### Scenario: Default output
- **WHEN** no `--output` flag provided
- **THEN** outputs to stdout

### Requirement: Help Documentation
The system SHALL provide comprehensive help documentation in command definition file.

#### Scenario: Usage examples section
- **WHEN** user reads command documentation
- **THEN** finds examples for:
  - Basic single-user report
  - Multi-day reports
  - Team aggregation
  - Export formats
  - File output

#### Scenario: Arguments reference table
- **WHEN** user reads arguments section
- **THEN** sees table with:
  - Flag names (long and short)
  - Type (Integer, String, String[])
  - Default value
  - Description

#### Scenario: Troubleshooting section
- **WHEN** user encounters errors
- **THEN** finds solutions for:
  - Command not found
  - GitHub authentication errors
  - JBang not installed
  - No activity found
  - Claude invocation failures

### Requirement: Workflow Orchestration
The system SHALL orchestrate all capabilities in Main.java following defined workflow.

#### Scenario: Single-user workflow
- **WHEN** user invokes `/claude-gh-standup --days 3`
- **THEN** executes in order:
  1. Parse arguments
  2. Detect or use specified username
  3. Collect GitHub activity (CollectActivity.java)
  4. Analyze file diffs (AnalyzeDiffs.java)
  5. Generate report (GenerateReport.java)
  6. Export in specified format (ExportUtils.java)
  7. Output to stdout or file

#### Scenario: Team workflow
- **WHEN** user invokes `/claude-gh-standup --team alice bob --days 7`
- **THEN** executes in order:
  1. Parse arguments
  2. For each team member:
     a. Collect activity
     b. Analyze diffs
     c. Generate individual report
  3. Consolidate reports (TeamAggregator.java)
  4. Export in specified format
  5. Output to stdout or file

### Requirement: Error Handling and User Feedback
The system SHALL provide clear error messages and progress indicators.

#### Scenario: Progress messages
- **WHEN** command is running
- **THEN** prints status updates:
  - "Collecting activity for {user}..."
  - "Analyzing file changes..."
  - "Generating standup report..."

#### Scenario: Error message format
- **WHEN** error occurs
- **THEN** prints to stderr with format: "Error: {clear description}"
- **AND** exits with non-zero code
- **AND** suggests remediation when applicable

#### Scenario: Graceful degradation
- **WHEN** optional data unavailable (e.g., commit search fails)
- **THEN** logs warning but continues
- **AND** generates report with available data
