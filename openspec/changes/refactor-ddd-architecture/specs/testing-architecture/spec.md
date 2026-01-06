# testing-architecture Specification Delta

## ADDED Requirements

### Requirement: Unit Testing Framework

The system SHALL use JUnit 5 for unit testing via JBang dependencies.

#### Scenario: JUnit 5 dependency declaration
- **WHEN** test files are created
- **THEN** they include `//DEPS org.junit.jupiter:junit-jupiter:5.10.0`
- **AND** they include `//DEPS org.junit.platform:junit-platform-launcher:1.10.0` for test runners

#### Scenario: Tests are executable via JBang
- **WHEN** a test file is run with `jbang test/domain/activity/CommitTest.java`
- **THEN** JBang downloads dependencies and executes tests
- **AND** test results are printed to stdout

### Requirement: Mock Implementations for Ports

The system SHALL provide mock implementations of all port interfaces for testing.

#### Scenario: MockActivityPort supports stubbing
- **WHEN** MockActivityPort is used in tests
- **THEN** it provides `stubCommits()`, `stubPullRequests()`, `stubIssues()`, `stubReviews()` methods
- **AND** port methods return the stubbed data

#### Scenario: MockDiffPort supports stubbing
- **WHEN** MockDiffPort is used in tests
- **THEN** it provides `stubDiffSummary()` method
- **AND** `fetchPRDiff()` returns the stubbed DiffSummary

#### Scenario: MockGitPort supports stubbing
- **WHEN** MockGitPort is used in tests
- **THEN** it provides methods to stub branch, staged files, and repository detection

#### Scenario: MockReportGeneratorPort captures calls
- **WHEN** MockReportGeneratorPort is used in tests
- **THEN** it captures the prompt passed to `generate()`
- **AND** tests can verify the prompt content

### Requirement: Domain Entity Tests

All domain entities SHALL have unit tests covering construction and behavior.

#### Scenario: Entity JSON parsing tested
- **WHEN** entity tests are run
- **THEN** they verify `fromJson()` correctly parses valid JSON
- **AND** they verify behavior with missing optional fields
- **AND** they verify immutability of records

#### Scenario: Value object equality tested
- **WHEN** value object tests are run
- **THEN** they verify `equals()` and `hashCode()` work correctly
- **AND** they verify two value objects with same data are equal

### Requirement: Service Tests with Mocks

All service classes SHALL be tested using mock port implementations.

#### Scenario: ActivityService tested with mocks
- **WHEN** ActivityServiceTest is run
- **THEN** it uses MockActivityPort to stub GitHub responses
- **AND** it verifies the service correctly aggregates activity

#### Scenario: ReportService tested with mocks
- **WHEN** ReportServiceTest is run
- **THEN** it uses MockReportGeneratorPort
- **AND** it verifies the correct prompt is built from Activity and DiffSummary

### Requirement: Test Runner

The system SHALL provide a consolidated test runner.

#### Scenario: RunAllTests executes all tests
- **WHEN** `jbang test/RunAllTests.java` is executed
- **THEN** it discovers and runs all test classes
- **AND** it prints a summary of passed/failed tests
- **AND** it exits with code 0 on success, non-zero on failure

### Requirement: Test File Organization

Test files SHALL mirror the source directory structure.

#### Scenario: Domain tests in test/domain/
- **WHEN** tests are organized
- **THEN** domain entity tests are in `test/domain/activity/`, `test/domain/report/`, etc.

#### Scenario: Service tests in test/services/
- **WHEN** tests are organized
- **THEN** service tests are in `test/services/`

#### Scenario: Mocks in test/mocks/
- **WHEN** tests are organized
- **THEN** mock implementations are in `test/mocks/`

### Requirement: No External Dependencies in Tests

Unit tests SHALL NOT invoke external CLI tools (gh, git, claude).

#### Scenario: Tests run without CLI tools
- **WHEN** unit tests are executed
- **THEN** they complete successfully even if `gh`, `git`, or `claude` are not installed
- **AND** all external dependencies are mocked
