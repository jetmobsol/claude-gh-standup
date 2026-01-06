# ports-adapters Specification Delta

## ADDED Requirements

### Requirement: Port Interfaces for External Dependencies

The system SHALL define port interfaces that abstract external CLI dependencies.

#### Scenario: ActivityPort interface for GitHub activity
- **WHEN** the ActivityPort interface is defined
- **THEN** it declares methods to fetch commits, pull requests, issues, and reviews
- **AND** methods accept username, DateRange, and optional Repository parameters
- **AND** methods return domain entity lists

#### Scenario: DiffPort interface for PR diffs
- **WHEN** the DiffPort interface is defined
- **THEN** it declares a method to fetch PR diff content
- **AND** the method accepts Repository and PR number parameters
- **AND** the method returns a DiffSummary value object

#### Scenario: GitPort interface for local git operations
- **WHEN** the GitPort interface is defined
- **THEN** it declares methods for getCurrentBranch, getStagedFiles, getUnstagedFiles, getUnpushedCommits
- **AND** it declares a method to detectRepository from a path
- **AND** methods accept Path parameters

#### Scenario: ReportGeneratorPort interface for AI generation
- **WHEN** the ReportGeneratorPort interface is defined
- **THEN** it declares a method to generate reports from a prompt
- **AND** the method streams output to stdout

#### Scenario: ExportPort interface for format conversion
- **WHEN** the ExportPort interface is defined
- **THEN** it declares a method to export StandupReport to a string
- **AND** different implementations provide different formats

### Requirement: GitHub CLI Adapter

The system SHALL provide an adapter implementing ActivityPort and DiffPort via the `gh` CLI.

#### Scenario: GitHubCliAdapter fetches commits
- **WHEN** fetchCommits is called on GitHubCliAdapter
- **THEN** it executes `gh search commits --author=<user> --json ...`
- **AND** it parses the JSON response into Commit entities
- **AND** it handles API errors gracefully (empty list on failure)

#### Scenario: GitHubCliAdapter fetches pull requests
- **WHEN** fetchPullRequests is called on GitHubCliAdapter
- **THEN** it executes `gh search prs --author=<user> --json ...`
- **AND** it parses the JSON response into PullRequest entities

#### Scenario: GitHubCliAdapter fetches PR diffs
- **WHEN** fetchPRDiff is called on GitHubCliAdapter
- **THEN** it executes `gh pr diff <number> --repo <owner/repo>`
- **AND** it parses the diff output into a DiffSummary

### Requirement: Git CLI Adapter

The system SHALL provide an adapter implementing GitPort via the `git` CLI.

#### Scenario: GitCliAdapter detects repository
- **WHEN** detectRepository is called with a git repository path
- **THEN** it executes `git remote get-url origin`
- **AND** it parses the output into a Repository value object
- **AND** it returns Optional.empty() for non-git directories

#### Scenario: GitCliAdapter gets current branch
- **WHEN** getCurrentBranch is called
- **THEN** it executes `git branch --show-current`
- **AND** it returns the branch name string

### Requirement: Claude CLI Adapter

The system SHALL provide an adapter implementing ReportGeneratorPort via the `claude` CLI.

#### Scenario: ClaudeCliAdapter streams report generation
- **WHEN** generate is called with a prompt
- **THEN** it executes `claude -p <prompt>` via ProcessBuilder
- **AND** it inherits IO to stream output directly to stdout
- **AND** it waits for process completion

### Requirement: Export Adapters

The system SHALL provide adapters implementing ExportPort for each output format.

#### Scenario: MarkdownExporter produces markdown
- **WHEN** export is called on MarkdownExporter
- **THEN** it returns the StandupReport content as Markdown string

#### Scenario: JsonExporter produces JSON
- **WHEN** export is called on JsonExporter
- **THEN** it serializes the StandupReport to JSON using Gson
- **AND** the output is pretty-printed

#### Scenario: HtmlExporter produces HTML
- **WHEN** export is called on HtmlExporter
- **THEN** it wraps the report content in HTML structure
- **AND** it includes basic styling

### Requirement: Adapter Location

All adapter implementations SHALL be located in the `infrastructure/` directory.

#### Scenario: Infrastructure directory structure
- **WHEN** adapters are organized
- **THEN** GitHubCliAdapter is in `infrastructure/github/`
- **AND** GitCliAdapter is in `infrastructure/git/`
- **AND** ClaudeCliAdapter is in `infrastructure/ai/`
- **AND** exporters are in `infrastructure/export/`
