# Design: DDD Refactoring with Ports & Adapters

## Context

The current implementation has ProcessBuilder calls scattered throughout business logic, making unit testing impossible without real CLI invocations. Each script (~10K lines in Main.java alone) mixes orchestration, data parsing, and external calls.

**Stakeholders**: Maintainers, contributors, CI/CD pipeline
**Constraints**: Must use JBang (no Maven/Gradle), CLI compatibility required

## Goals / Non-Goals

**Goals**:
- Enable unit testing of business logic without external dependencies
- Clear separation between domain logic and infrastructure concerns
- Testable services with dependency injection
- Domain models as pure Java records (no external dependencies)

**Non-Goals**:
- Changing CLI interface
- Adding new features
- Switching build tools
- Full Clean Architecture (overkill for this scope)

## Architecture Decision: Hybrid DDD with Ports & Adapters

### Structure

```
scripts/
├── Main.java                       # Entry point, wiring, CLI parsing
├── domain/
│   ├── activity/                   # Bounded Context: Activity
│   │   ├── Activity.java           # Aggregate root
│   │   ├── Commit.java             # Entity
│   │   ├── PullRequest.java        # Entity
│   │   ├── Issue.java              # Entity
│   │   └── Review.java             # Entity
│   ├── report/                     # Bounded Context: Report
│   │   ├── StandupReport.java      # Aggregate root
│   │   ├── ReportSection.java      # Value object
│   │   └── DiffSummary.java        # Value object
│   ├── team/                       # Bounded Context: Team
│   │   ├── TeamMember.java         # Entity
│   │   └── TeamReport.java         # Aggregate root
│   └── shared/                     # Cross-cutting value objects
│       ├── DateRange.java
│       └── Repository.java
├── ports/                          # Interfaces (separate folder)
│   ├── ActivityPort.java
│   ├── DiffPort.java
│   ├── GitPort.java
│   ├── ReportGeneratorPort.java
│   └── ExportPort.java
├── infrastructure/                 # Adapters (CLI implementations)
│   ├── github/GitHubCliAdapter.java
│   ├── git/GitCliAdapter.java
│   ├── ai/ClaudeCliAdapter.java
│   └── export/{Markdown,Json,Html}Exporter.java
└── services/                       # Orchestration with injected ports
    ├── ActivityService.java
    ├── DiffService.java
    ├── ReportService.java
    └── TeamService.java
```

### Decisions

1. **Separate entity classes** (not nested records)
   - Rationale: Better testability, clearer ownership, easier to extend
   - Alternative: Single Activity aggregate with nested records (simpler but less flexible)

2. **Service classes** (not explicit Use Cases)
   - Rationale: Simpler than Clean Architecture use cases, sufficient for this scope
   - Alternative: Formal UseCases with Input/Output DTOs (over-engineering)

3. **Separate ports/ folder** (not co-located in domain)
   - Rationale: Easy to find all interfaces, clearer for newcomers
   - Alternative: Ports inside domain packages (Clean Architecture style)

4. **JBang `//SOURCES` for multi-file**
   - Rationale: Maintains single-command execution, no build setup
   - Example: `//SOURCES domain/**/*.java`

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| File count increases (~27 files) | Organized structure, clear naming |
| Learning curve for contributors | Document patterns in CLAUDE.md |
| JBang `//SOURCES` glob limitations | Test patterns thoroughly |
| Refactoring breaks existing behavior | Maintain test compatibility commands |

## Migration Plan

**Phase 1**: Domain entities + tests (TDD)
- Write tests first, then extract entities from CollectActivity.java
- Create Activity, Commit, PullRequest, Issue, Review

**Phase 2**: Ports (interfaces)
- Define ActivityPort, DiffPort, GitPort, ReportGeneratorPort, ExportPort

**Phase 3**: Mocks + service tests
- Create mock implementations for ports
- Write service tests using mocks

**Phase 4**: Infrastructure adapters
- Extract ProcessBuilder calls into GitHubCliAdapter, GitCliAdapter, ClaudeCliAdapter

**Phase 5**: Services
- Create ActivityService, DiffService, ReportService, TeamService
- Inject ports via constructors

**Phase 6**: Wire in Main.java
- Dependency injection in main()
- Remove old monolithic code

**Rollback**: Each phase can be reverted independently. Keep old scripts until Phase 6 complete.

## Resolved Questions

1. **Integration tests with real CLIs?** - No. Unit tests with mocks are sufficient. Integration testing is out of scope.
2. **Gson annotations in domain entities?** - No. Domain layer stays pure (no annotations). All JSON parsing happens in adapters, which pass primitive values to entity constructors.
