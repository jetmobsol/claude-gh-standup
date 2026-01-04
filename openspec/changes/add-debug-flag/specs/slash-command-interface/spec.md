## ADDED Requirements

### Requirement: Debug Mode

The system SHALL provide a `--debug` / `-D` command-line flag that enables verbose logging for troubleshooting.

#### Scenario: Debug mode disabled by default
- **WHEN** the user runs `/claude-gh-standup` without `--debug`
- **THEN** no debug messages are printed to stderr
- **AND** only standard progress messages are shown

#### Scenario: Debug mode enabled
- **WHEN** the user runs `/claude-gh-standup --debug`
- **THEN** detailed debug messages are printed to stderr
- **AND** debug messages include workflow stage information
- **AND** debug messages include timing for significant operations

#### Scenario: Debug flag short form
- **WHEN** the user runs `/claude-gh-standup -D`
- **THEN** debug mode is enabled (equivalent to `--debug`)

#### Scenario: Debug output format
- **WHEN** debug mode is enabled
- **THEN** debug messages follow the format `[DEBUG] <component>: <message>`
- **AND** all debug messages are written to stderr (not stdout)

### Requirement: Debug Logging Coverage

When debug mode is enabled, the system SHALL log the following workflow stages:

#### Scenario: Argument parsing logged
- **WHEN** debug mode is enabled
- **THEN** parsed command-line arguments are logged (days, user, repo, format, team members)

#### Scenario: Configuration loading logged
- **WHEN** debug mode is enabled
- **THEN** config file path and load status are logged
- **AND** number of configured directories is logged (if multi-dir mode)

#### Scenario: Mode detection logged
- **WHEN** debug mode is enabled
- **THEN** detected mode (single-dir or multi-dir) is logged
- **AND** repository auto-detection result is logged

#### Scenario: Script execution logged
- **WHEN** debug mode is enabled
- **AND** a subsidiary script is invoked (CollectActivity, AnalyzeDiffs, etc.)
- **THEN** script path and arguments are logged
- **AND** execution time is logged upon completion

#### Scenario: GitHub API calls logged
- **WHEN** debug mode is enabled
- **AND** GitHub CLI commands are executed
- **THEN** the `gh` command being executed is logged
- **AND** number of results returned is logged

#### Scenario: Claude invocation logged
- **WHEN** debug mode is enabled
- **AND** Claude is invoked for report generation
- **THEN** prompt size (character count) is logged
- **AND** Claude invocation status is logged

### Requirement: Debug Flag Propagation

The debug flag SHALL be propagated to subsidiary scripts invoked by Main.java.

#### Scenario: Debug flag passed to CollectActivity
- **WHEN** debug mode is enabled in Main.java
- **AND** CollectActivity.java is invoked
- **THEN** CollectActivity receives and respects the debug flag

#### Scenario: Debug flag passed to ActivityAggregator
- **WHEN** debug mode is enabled in Main.java
- **AND** ActivityAggregator.java is invoked (multi-dir mode)
- **THEN** ActivityAggregator receives and respects the debug flag
- **AND** LocalChangesDetector invocations receive the debug flag
