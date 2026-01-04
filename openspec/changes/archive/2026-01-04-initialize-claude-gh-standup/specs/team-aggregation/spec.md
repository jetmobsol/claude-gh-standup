# Capability: Team Aggregation

## ADDED Requirements

### Requirement: Multi-User Activity Collection
The system SHALL collect activity for multiple team members when `--team` flag is provided.

#### Scenario: Team members specified
- **WHEN** user invokes `/claude-gh-standup --team alice bob charlie --days 7`
- **THEN** iterates through each username: alice, bob, charlie
- **AND** collects activity for each user independently

#### Scenario: Team member has no activity
- **WHEN** one team member has no activity in date range
- **THEN** generates report noting "No activity found"
- **AND** includes them in team aggregation with zero activities

### Requirement: Individual Report Generation
The system SHALL generate individual standup reports for each team member before aggregation.

#### Scenario: Generate individual reports
- **WHEN** processing team member "alice"
- **THEN** collects her commits, PRs, issues, reviews
- **AND** analyzes file diffs for her activity
- **AND** generates standup report using `standup.prompt.md`
- **AND** stores result with username

#### Scenario: Progress indication
- **WHEN** generating team report
- **THEN** prints status messages:
  - "Generating report for alice..."
  - "Generating report for bob..."
- **AND** provides user feedback during processing

### Requirement: Report Consolidation
The system SHALL consolidate individual reports into unified team standup using AI.

#### Scenario: Consolidate with team prompt
- **WHEN** all individual reports are generated
- **THEN** constructs team summary with format:
```
## alice (15 activities)
[Alice's individual report]

## bob (8 activities)
[Bob's individual report]

## charlie (23 activities)
[Charlie's individual report]
```
- **AND** loads `prompts/team.prompt.md`
- **AND** injects `{{team_reports}}` with consolidated summary
- **AND** calls `claude -p` with team prompt

#### Scenario: Team-level insights
- **WHEN** team prompt is processed by Claude
- **THEN** AI identifies:
  - Common themes across team members
  - Cross-team dependencies
  - Collaborative work
  - Team-level accomplishments
  - Blockers affecting multiple people

### Requirement: Team Report Metadata
The system SHALL include team-level statistics in team reports.

#### Scenario: Team metadata in output
- **WHEN** team report is generated
- **THEN** includes metadata:
  - Team members list
  - Total activities across team
  - Date range
  - Generation timestamp

### Requirement: Team Export Formats
The system SHALL support all export formats (Markdown, JSON, HTML) for team reports.

#### Scenario: Team report as JSON
- **WHEN** user specifies `--team alice bob --format json`
- **THEN** exports JSON with structure:
```json
{
  "metadata": {
    "generated_at": "2026-01-02T10:30:00Z",
    "team_members": ["alice", "bob"],
    "days": 7,
    "total_activities": 23
  },
  "team_report": "AI-generated team summary",
  "individual_reports": [
    {"user": "alice", "report": "...", "activity_count": 15},
    {"user": "bob", "report": "...", "activity_count": 8}
  ]
}
```

#### Scenario: Team report as HTML
- **WHEN** team report exported as HTML
- **THEN** includes collapsible sections for individual reports
- **AND** highlights team summary at top

### Requirement: Team Prompt Template
The system SHALL provide team aggregation prompt template for AI consolidation.

#### Scenario: Team prompt structure
- **WHEN** `team.prompt.md` is loaded
- **THEN** instructs AI to:
  - Synthesize individual reports into team summary
  - Identify collaboration and dependencies
  - Highlight team achievements
  - Note cross-cutting blockers
  - Keep output concise but comprehensive
