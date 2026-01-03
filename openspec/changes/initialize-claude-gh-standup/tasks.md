# Implementation Tasks

## 1. Project Setup and Infrastructure
- [ ] 1.1 Initialize git repository if not already done
- [ ] 1.2 Create directory structure (scripts/, prompts/, examples/, docs/)
- [ ] 1.3 Create .gitignore for JBang cache, IDE files, and temp files
- [ ] 1.4 Create LICENSE file (MIT with attribution to gh-standup)
- [ ] 1.5 Verify JBang installation or document installation steps

## 2. Prompt Templates
- [ ] 2.1 Copy reference prompt from `/Users/garden/projects/ai/sgoedecke/gh-standup/internal/llm/standup.prompt.yml`
- [ ] 2.2 Convert YAML prompt to Markdown format (`prompts/standup.prompt.md`)
- [ ] 2.3 Add `{{activities}}` and `{{diffs}}` placeholders to standup prompt
- [ ] 2.4 Create team aggregation prompt (`prompts/team.prompt.md`)
- [ ] 2.5 Add `{{team_reports}}` placeholder to team prompt
- [ ] 2.6 Create diff analysis section in prompts for file change context

## 3. Core Java Scripts - Activity Collection
- [ ] 3.1 Create `scripts/CollectActivity.java` with JBang shebang
- [ ] 3.2 Add Gson dependency via `//DEPS com.google.code.gson:gson:2.10.1`
- [ ] 3.3 Implement `getUserCommits()` method using `gh search commits`
- [ ] 3.4 Implement `getUserPRs()` method using `gh search prs`
- [ ] 3.5 Implement `getUserIssues()` method using `gh search issues`
- [ ] 3.6 Implement `getUserReviews()` method using `gh api`
- [ ] 3.7 Implement date range calculation using LocalDate
- [ ] 3.8 Implement `collectAllActivity()` orchestration method
- [ ] 3.9 Add error handling for API failures (graceful degradation)
- [ ] 3.10 Test independently with `jbang scripts/CollectActivity.java octocat 3`

## 4. Core Java Scripts - Diff Analysis
- [ ] 4.1 Create `scripts/AnalyzeDiffs.java` with JBang shebang
- [ ] 4.2 Implement `analyzePRDiffs()` method using `gh pr diff`
- [ ] 4.3 Implement `analyzeCommitDiffs()` method using `git diff`
- [ ] 4.4 Implement diff parsing logic to extract file paths
- [ ] 4.5 Implement line addition counter (lines starting with +)
- [ ] 4.6 Implement line deletion counter (lines starting with -)
- [ ] 4.7 Implement `summarizeDiffs()` aggregation method
- [ ] 4.8 Format diff summary for prompt injection
- [ ] 4.9 Handle empty diffs gracefully
- [ ] 4.10 Test independently with sample PR numbers

## 5. Core Java Scripts - Report Generation
- [ ] 5.1 Create `scripts/GenerateReport.java` with JBang shebang
- [ ] 5.2 Implement `loadPromptTemplate()` using `Files.readString()`
- [ ] 5.3 Implement `formatActivities()` for AI-readable activity text
- [ ] 5.4 Implement `formatDiffs()` for AI-readable diff summary
- [ ] 5.5 Implement variable injection (`.replace("{{activities}}", ...)`)
- [ ] 5.6 Implement ProcessBuilder for `claude -p` invocation
- [ ] 5.7 Use `processBuilder.inheritIO()` pattern for seamless output
- [ ] 5.8 Implement error handling for Claude invocation failures
- [ ] 5.9 Test with mock activities and diffs data
- [ ] 5.10 Verify AI output quality with real GitHub data

## 6. Core Java Scripts - Export Utilities
- [ ] 6.1 Create `scripts/ExportUtils.java` with JBang shebang
- [ ] 6.2 Implement `exportMarkdown()` method with metadata header
- [ ] 6.3 Implement `exportJSON()` method with Gson pretty printing
- [ ] 6.4 Implement `exportHTML()` method with embedded CSS
- [ ] 6.5 Implement format validation (reject invalid formats)
- [ ] 6.6 Implement stdout output (default)
- [ ] 6.7 Implement file output with `Files.writeString()`
- [ ] 6.8 Add parent directory creation if needed
- [ ] 6.9 Test all three export formats
- [ ] 6.10 Verify HTML renders correctly in browser

## 7. Core Java Scripts - Team Aggregation
- [ ] 7.1 Create `scripts/TeamAggregator.java` with JBang shebang
- [ ] 7.2 Implement iteration over team members array
- [ ] 7.3 Implement progress indicators ("Generating report for {user}...")
- [ ] 7.4 Call CollectActivity for each team member
- [ ] 7.5 Call AnalyzeDiffs for each team member
- [ ] 7.6 Call GenerateReport for each team member
- [ ] 7.7 Implement report consolidation with team prompt
- [ ] 7.8 Format individual reports with usernames and activity counts
- [ ] 7.9 Inject consolidated reports into `{{team_reports}}` placeholder
- [ ] 7.10 Test with 2-3 team members

## 8. Main Entry Point
- [ ] 8.1 Create `scripts/Main.java` with JBang shebang
- [ ] 8.2 Implement argument parsing for all flags (--days, --user, --repo, --format, --team, --output)
- [ ] 8.3 Implement default value handling
- [ ] 8.4 Implement auto-detection of current user via `gh api user --jq .login`
- [ ] 8.5 Implement single-user workflow orchestration
- [ ] 8.6 Implement team workflow orchestration (if --team flag present)
- [ ] 8.7 Add progress indicators for long-running operations
- [ ] 8.8 Implement error handling and stderr output
- [ ] 8.9 Test all flag combinations
- [ ] 8.10 Verify exit codes (0 for success, 1 for errors)

## 9. Slash Command Definition
- [ ] 9.1 Create `claude-gh-standup.md` in repository root
- [ ] 9.2 Add YAML frontmatter (description, argument-hint)
- [ ] 9.3 Write Overview section
- [ ] 9.4 Write Prerequisites section (gh CLI, JBang, Claude Code)
- [ ] 9.5 Write Usage Examples section (basic, team, export formats)
- [ ] 9.6 Write Workflow section explaining execution steps
- [ ] 9.7 Write Invocation section with JBang command
- [ ] 9.8 Write Arguments Reference table
- [ ] 9.9 Write Troubleshooting section
- [ ] 9.10 Add Related Commands and Credits sections

## 10. Documentation
- [ ] 10.1 Create README.md with project overview
- [ ] 10.2 Add Attribution section crediting @sgoedecke/gh-standup
- [ ] 10.3 Document installation instructions (user-level and project-level)
- [ ] 10.4 Add usage examples for all major features
- [ ] 10.5 Document prerequisites and setup steps
- [ ] 10.6 Create CONTRIBUTING.md with contribution guidelines
- [ ] 10.7 Add architecture diagram or explanation
- [ ] 10.8 Create example outputs in `examples/` directory
- [ ] 10.9 Add troubleshooting section to README
- [ ] 10.10 Document JBang pattern

## 11. Example Outputs
- [ ] 11.1 Generate sample Markdown report and save to `examples/sample_output.md`
- [ ] 11.2 Generate sample JSON report and save to `examples/sample_output.json`
- [ ] 11.3 Generate sample HTML report and save to `examples/sample_output.html`
- [ ] 11.4 Ensure examples demonstrate realistic GitHub activity

## 12. Testing and Validation
- [ ] 12.1 Test basic single-user report generation
- [ ] 12.2 Test with different --days values (1, 3, 7)
- [ ] 12.3 Test with specific --user (not authenticated user)
- [ ] 12.4 Test with --repo filter
- [ ] 12.5 Test team aggregation with 2-3 users
- [ ] 12.6 Test all export formats (markdown, json, html)
- [ ] 12.7 Test file output with --output flag
- [ ] 12.8 Test error cases (no activity, API failures, missing dependencies)
- [ ] 12.9 Test graceful degradation when commit search fails
- [ ] 12.10 Verify command appears in `/help` after installation

## 13. Installation Testing
- [ ] 13.1 Test project-level installation (`.claude/commands/claude-gh-standup/`)
- [ ] 13.2 Verify command recognition by Claude Code
- [ ] 13.3 Test user-level installation (`~/.claude/commands/claude-gh-standup/`)
- [ ] 13.4 Document minimum Claude Code version if needed
- [ ] 13.5 Test on clean system to verify dependency handling

## 14. Repository Publication (Optional)
- [ ] 14.1 Create GitHub repository jetmobsol/claude-gh-standup
- [ ] 14.2 Push all code to main branch
- [ ] 14.3 Create initial release tag (v0.1.0)
- [ ] 14.4 Set up CI/CD workflow for testing (optional)
- [ ] 14.5 Add topics/tags for discoverability (claude-code, standup, github-cli)

## Validation Checkpoints

After Step 10 (Main Entry Point):
- Run `jbang scripts/Main.java --help` to verify argument parsing
- Run `jbang scripts/Main.java --days 1` to verify end-to-end flow

After Step 11 (Slash Command Definition):
- Copy repository to `.claude/commands/claude-gh-standup/`
- Run `/help` in Claude Code to verify command appears
- Run `/claude-gh-standup` to verify command executes

After Step 14 (Documentation):
- Read README.md as a new user would
- Follow installation steps verbatim
- Verify all examples work as documented

## Dependencies

Sequential dependencies:
- Steps 1-2 must complete before Step 3-7 (need prompts for testing)
- Steps 3-7 can run in parallel (independent scripts)
- Step 8 depends on Steps 3-7 (Main.java orchestrates all scripts)
- Step 9 depends on Step 8 (command file references Main.java)
- Steps 10-11 can run in parallel with Steps 12-13
- Step 14 depends on all previous steps

Parallelizable work:
- CollectActivity.java, AnalyzeDiffs.java, GenerateReport.java, ExportUtils.java, TeamAggregator.java can be developed independently
- Documentation (README, CONTRIBUTING) can be written while code is being developed
- Example outputs can be generated as soon as export utilities are ready
