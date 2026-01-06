# Tasks: DDD Refactoring

## 1. Domain Entities (TDD)

### 1.1 Activity Bounded Context
- [ ] 1.1.1 Write CommitTest.java with JSON parsing and equality tests
- [ ] 1.1.2 Create Commit.java record to pass tests
- [ ] 1.1.3 Write PullRequestTest.java
- [ ] 1.1.4 Create PullRequest.java record
- [ ] 1.1.5 Write IssueTest.java
- [ ] 1.1.6 Create Issue.java record
- [ ] 1.1.7 Write ReviewTest.java
- [ ] 1.1.8 Create Review.java record
- [ ] 1.1.9 Write ActivityTest.java (aggregate tests)
- [ ] 1.1.10 Create Activity.java aggregate

### 1.2 Report Bounded Context
- [ ] 1.2.1 Write DiffSummaryTest.java
- [ ] 1.2.2 Create DiffSummary.java value object
- [ ] 1.2.3 Create ReportSection.java value object
- [ ] 1.2.4 Write StandupReportTest.java
- [ ] 1.2.5 Create StandupReport.java aggregate

### 1.3 Shared Value Objects
- [ ] 1.3.1 Write DateRangeTest.java
- [ ] 1.3.2 Create DateRange.java value object
- [ ] 1.3.3 Write RepositoryTest.java
- [ ] 1.3.4 Create Repository.java value object

### 1.4 Team Bounded Context
- [ ] 1.4.1 Create TeamMember.java entity
- [ ] 1.4.2 Write TeamReportTest.java
- [ ] 1.4.3 Create TeamReport.java aggregate

## 2. Ports (Interfaces)

- [ ] 2.1 Create ActivityPort.java interface
- [ ] 2.2 Create DiffPort.java interface
- [ ] 2.3 Create GitPort.java interface
- [ ] 2.4 Create ReportGeneratorPort.java interface
- [ ] 2.5 Create ExportPort.java interface

## 3. Mocks and Service Tests

### 3.1 Mock Implementations
- [ ] 3.1.1 Create MockActivityPort.java
- [ ] 3.1.2 Create MockDiffPort.java
- [ ] 3.1.3 Create MockGitPort.java
- [ ] 3.1.4 Create MockReportGeneratorPort.java
- [ ] 3.1.5 Create MockExportPort.java

### 3.2 Service Tests (TDD)
- [ ] 3.2.1 Write ActivityServiceTest.java
- [ ] 3.2.2 Write DiffServiceTest.java
- [ ] 3.2.3 Write ReportServiceTest.java
- [ ] 3.2.4 Write TeamServiceTest.java

## 4. Infrastructure Adapters

- [ ] 4.1 Create GitHubCliAdapter.java (implements ActivityPort, DiffPort)
- [ ] 4.2 Create GitCliAdapter.java (implements GitPort)
- [ ] 4.3 Create ClaudeCliAdapter.java (implements ReportGeneratorPort)
- [ ] 4.4 Create MarkdownExporter.java (implements ExportPort)
- [ ] 4.5 Create JsonExporter.java (implements ExportPort)
- [ ] 4.6 Create HtmlExporter.java (implements ExportPort)

## 5. Services

- [ ] 5.1 Create ActivityService.java
- [ ] 5.2 Create DiffService.java
- [ ] 5.3 Create ReportService.java
- [ ] 5.4 Create TeamService.java

## 6. Integration

### 6.1 Main.java Refactoring
- [ ] 6.1.1 Update JBang `//SOURCES` directives
- [ ] 6.1.2 Add dependency wiring in main()
- [ ] 6.1.3 Replace inline code with service calls
- [ ] 6.1.4 Remove old monolithic methods

### 6.2 Test Runner
- [ ] 6.2.1 Create RunAllTests.java with JUnit Platform Launcher
- [ ] 6.2.2 Verify all tests pass

### 6.3 Backward Compatibility
- [ ] 6.3.1 Test: `jbang scripts/Main.java --days 1 --no-claude`
- [ ] 6.3.2 Test: `jbang scripts/Main.java --days 3 --user octocat --repo cli/cli`
- [ ] 6.3.3 Test: `jbang scripts/Main.java --team user1 user2 --days 7`
- [ ] 6.3.4 Test: All export formats (markdown, json, html)

## 7. Cleanup

- [ ] 7.1 Remove deprecated code from old scripts
- [ ] 7.2 Update CLAUDE.md with new architecture patterns
- [ ] 7.3 Update project.md with new conventions

## Dependencies

- Phase 1 (Domain) has no dependencies
- Phase 2 (Ports) depends on Phase 1 entities
- Phase 3 (Mocks) depends on Phases 1 and 2
- Phase 4 (Adapters) depends on Phases 1 and 2
- Phase 5 (Services) depends on Phases 1, 2, 3
- Phase 6 (Integration) depends on all previous phases
- Phase 7 (Cleanup) depends on Phase 6

## Parallelizable Work

- 1.1 (Activity) and 1.2 (Report) and 1.3 (Shared) can be done in parallel
- 2.x (Ports) can be done in parallel after respective domain entities
- 3.1 (Mocks) can be done in parallel after ports
- 4.x (Adapters) can be done in parallel after ports
