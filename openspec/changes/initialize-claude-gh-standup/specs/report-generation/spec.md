# Capability: Report Generation

## ADDED Requirements

### Requirement: Prompt Template Loading
The system SHALL load Markdown prompt templates from the `prompts/` directory.

#### Scenario: Load standup prompt template
- **WHEN** generating single-user report
- **THEN** reads `prompts/standup.prompt.md` using `Files.readString()`
- **AND** returns template content as string

#### Scenario: Load team prompt template
- **WHEN** generating team aggregated report
- **THEN** reads `prompts/team.prompt.md`
- **AND** returns team template content

#### Scenario: Prompt file not found
- **WHEN** prompt template file does not exist
- **THEN** throws IOException with clear error message
- **AND** exits with code 1

### Requirement: Variable Injection
The system SHALL inject activity data and diff analysis into prompt templates.

#### Scenario: Inject activity data
- **WHEN** prompt template contains `{{activities}}` placeholder
- **THEN** replaces with formatted activity JSON or text
- **AND** preserves template structure

#### Scenario: Inject diff analysis
- **WHEN** prompt template contains `{{diffs}}` placeholder
- **THEN** replaces with formatted diff summary
- **AND** includes file paths and statistics

#### Scenario: Multiple placeholders
- **WHEN** template has multiple `{{variable}}` placeholders
- **THEN** replaces all occurrences in order
- **AND** produces complete prompt with all data injected

### Requirement: Claude CLI Integration
The system SHALL invoke `claude -p` with the fully constructed prompt using ProcessBuilder.

#### Scenario: Execute claude -p command
- **WHEN** prompt is ready with all data injected
- **THEN** creates ProcessBuilder with command `["claude", "-p", fullPrompt]`
- **AND** calls `processBuilder.inheritIO()` to pipe output to stdout
- **AND** executes `process.start()` and `process.waitFor()`

#### Scenario: Claude invocation succeeds
- **WHEN** `claude -p` exits with code 0
- **THEN** AI-generated report is written to stdout
- **AND** system returns success

#### Scenario: Claude invocation fails
- **WHEN** `claude -p` exits with non-zero code
- **THEN** throws RuntimeException with exit code
- **AND** displays error message to user

### Requirement: Activity Formatting
The system SHALL format collected activities into human-readable text for AI consumption.

#### Scenario: Format commits for prompt
- **WHEN** activity data contains commits array
- **THEN** formats as:
```
COMMITS:
- [repo-name] commit-message (sha: abcd123)
- [repo-name] another-commit (sha: def4567)
```

#### Scenario: Format PRs for prompt
- **WHEN** activity data contains PRs array
- **THEN** formats as:
```
PULL REQUESTS:
- [repo-name] #123: PR title (state: open)
- [repo-name] #456: Another PR (state: merged)
```

#### Scenario: Format issues and reviews
- **WHEN** activity data contains issues and reviews
- **THEN** formats similarly with clear section headers
- **AND** includes relevant metadata (state, numbers, titles)

### Requirement: Prompt Template Structure
The system SHALL provide base prompt templates adapted from gh-standup with file diff context.

#### Scenario: Standup prompt template structure
- **WHEN** `standup.prompt.md` is loaded
- **THEN** contains instructions for AI to:
  - Write in first person
  - Create sections: Yesterday's Work, Today's Plans, Blockers
  - Use file changes for context beyond commit messages
  - Keep professional but conversational tone

#### Scenario: Team prompt template structure
- **WHEN** `team.prompt.md` is loaded
- **THEN** contains instructions for AI to:
  - Consolidate individual reports
  - Identify common themes and dependencies
  - Highlight cross-team collaboration
  - Summarize team-level accomplishments
