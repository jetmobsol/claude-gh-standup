# domain-model Specification Delta

## ADDED Requirements

### Requirement: Activity Domain Entities

The system SHALL provide domain entities for GitHub activity organized in an Activity bounded context.

#### Scenario: Commit entity stores commit data
- **WHEN** a Commit entity is created
- **THEN** it contains SHA, message, author, date, and URL fields
- **AND** it can be constructed from JSON via `Commit.fromJson()`
- **AND** it is immutable (Java record)

#### Scenario: PullRequest entity stores PR data
- **WHEN** a PullRequest entity is created
- **THEN** it contains number, title, state, URL, additions, and deletions fields
- **AND** it can be constructed from JSON via `PullRequest.fromJson()`
- **AND** it is immutable (Java record)

#### Scenario: Issue entity stores issue data
- **WHEN** an Issue entity is created
- **THEN** it contains number, title, state, URL, and labels fields
- **AND** it can be constructed from JSON via `Issue.fromJson()`

#### Scenario: Review entity stores review data
- **WHEN** a Review entity is created
- **THEN** it contains PR number, state, body, and URL fields
- **AND** it can be constructed from JSON via `Review.fromJson()`

#### Scenario: Activity aggregate contains all activity types
- **WHEN** an Activity aggregate is created
- **THEN** it contains lists of Commit, PullRequest, Issue, and Review entities
- **AND** it provides accessors for each entity type

### Requirement: Report Domain Entities

The system SHALL provide domain entities for report generation organized in a Report bounded context.

#### Scenario: DiffSummary value object stores file change stats
- **WHEN** a DiffSummary is created
- **THEN** it contains files changed count, additions, and deletions
- **AND** it can be constructed from raw diff text via `DiffSummary.fromDiff()`

#### Scenario: ReportSection value object stores report content
- **WHEN** a ReportSection is created
- **THEN** it contains a title and content string
- **AND** it is immutable

#### Scenario: StandupReport aggregate contains report sections
- **WHEN** a StandupReport aggregate is created
- **THEN** it contains a list of ReportSection value objects
- **AND** it contains a timestamp

### Requirement: Shared Value Objects

The system SHALL provide cross-cutting value objects in a shared context.

#### Scenario: DateRange value object represents time periods
- **WHEN** a DateRange is created
- **THEN** it contains start and end dates
- **AND** it can be constructed via `DateRange.lastDays(int)`
- **AND** it provides date formatting for GitHub API queries

#### Scenario: Repository value object represents owner/repo
- **WHEN** a Repository is created
- **THEN** it contains owner and repo name strings
- **AND** it can be parsed from "owner/repo" format via `Repository.parse()`
- **AND** it provides `toString()` returning "owner/repo"

### Requirement: Team Domain Entities

The system SHALL provide domain entities for team aggregation in a Team bounded context.

#### Scenario: TeamMember entity stores member info
- **WHEN** a TeamMember entity is created
- **THEN** it contains username and associated Activity

#### Scenario: TeamReport aggregate contains team data
- **WHEN** a TeamReport aggregate is created
- **THEN** it contains a list of TeamMember entities
- **AND** it provides team-level statistics

### Requirement: Domain Layer Purity

The domain layer SHALL have zero external dependencies.

#### Scenario: Domain entities use only standard library
- **WHEN** domain entity classes are analyzed
- **THEN** they import only `java.*` packages
- **AND** they do not import Gson, ProcessBuilder, or other external dependencies

#### Scenario: JSON parsing is static factory methods
- **WHEN** entities need to be constructed from JSON
- **THEN** the Gson instance is passed to `fromJson()` method
- **AND** the entity class does not hold or create Gson instances
