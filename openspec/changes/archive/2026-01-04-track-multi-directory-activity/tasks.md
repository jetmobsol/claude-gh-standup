# Implementation Tasks: track-multi-directory-activity

## Overview

Implementation of multi-directory standup tracking with local change detection, GitHub activity deduplication, and date convenience shortcuts. Tasks ordered for incremental delivery with minimal dependencies.

## Task List

### Phase 1: Configuration Infrastructure (Foundation)

- [ ] **T1.1**: Create empty `config.json` and example `config.example.json` schema files
  - Deliverable: Two JSON files in repository root
  - Validation: Files parse with `jq .`
  - Dependencies: None

- [ ] **T1.2**: Implement `ConfigManager.java` with CRUD operations
  - Deliverable: JBang script with `loadConfig()`, `saveConfig()`, `addDirectory()`, `removeDirectory()`, `listDirectories()` methods
  - Validation: `jbang scripts/ConfigManager.java --test` (manual test mode)
  - Dependencies: T1.1

- [ ] **T1.3**: Add git auto-detection to ConfigManager (remote URL, branch, repo name)
  - Deliverable: `detectGitInfo(String path)` method
  - Validation: Run in test git repo, verify JSON output contains branch/remote/repoName
  - Dependencies: T1.2

- [ ] **T1.4**: Add tilde expansion support (`expandTilde()` method)
  - Deliverable: Path normalization for `~/.claude-gh-standup/`
  - Validation: Test with `~/projects/test` → `/Users/username/projects/test`
  - Dependencies: T1.2

- [ ] **T1.5**: Add config command flags to Main.java (`--config-add`, `--config-list`, `--config-remove`, `--config-init`)
  - Deliverable: Updated `Args` class and `parseArgs()` method
  - Validation: `jbang scripts/Main.java --config-init` creates config file
  - Dependencies: T1.2

- [ ] **T1.6**: Implement config command handlers in Main.java (`handleConfigCommand()`)
  - Deliverable: Call ConfigManager operations from Main.java
  - Validation: Add directory, list directories, remove directory workflow
  - Dependencies: T1.5

### Phase 2: Local Change Detection (User Visibility)

- [ ] **T2.1**: Implement `LocalChangesDetector.java` for uncommitted changes (staged/unstaged)
  - Deliverable: JBang script detecting `git diff` and `git diff --cached`
  - Validation: Test in repo with uncommitted files, verify JSON output
  - Dependencies: None (independent)

- [ ] **T2.2**: Add unpushed commit detection to LocalChangesDetector
  - Deliverable: `git log origin/<branch>..HEAD` parsing
  - Validation: Test with local commits, verify commit list in JSON
  - Dependencies: T2.1

- [ ] **T2.3**: Handle edge cases (no remote branch, detached HEAD, permission errors)
  - Deliverable: Graceful error handling with warnings to stderr
  - Validation: Test in non-tracked branch, verify no crash
  - Dependencies: T2.2

- [ ] **T2.4**: Add JSON output format with directory metadata
  - Deliverable: Structured output with `directoryId`, `path`, `branch`, `uncommitted`, `unpushed`
  - Validation: Parse output with `jq` and verify schema
  - Dependencies: T2.2

### Phase 3: Date Range Convenience (UX Improvement)

- [ ] **T3.1**: Add `--yesterday` and `--last-week` flags to Main.java Args class
  - Deliverable: Updated `Args` class with boolean fields
  - Validation: Flags parse without error
  - Dependencies: None (independent)

- [ ] **T3.2**: Implement `calculateDays()` logic with Monday-Friday detection
  - Deliverable: Method that returns 3 on Monday for `--yesterday`, else 1
  - Validation: Test on Monday (return 3) and Tuesday (return 1)
  - Dependencies: T3.1

- [ ] **T3.3**: Integrate date calculation into Main.java workflow
  - Deliverable: Replace `parsed.days` with `calculateDays(parsed)` throughout
  - Validation: Run `--yesterday` on Monday, verify GitHub query uses 3 days
  - Dependencies: T3.2

### Phase 4: Activity Aggregation & Deduplication (Core Logic)

- [ ] **T4.1**: Implement `ActivityAggregator.java` scaffold with directory grouping
  - Deliverable: JBang script that groups directories by `repoName`
  - Validation: Pass 3 directories (2 same repo), verify grouping
  - Dependencies: T1.3 (needs git detection)

- [ ] **T4.2**: Add parallel local change detection with ExecutorService
  - Deliverable: Concurrent calls to LocalChangesDetector for all directories
  - Validation: 5 directories complete in <10 seconds
  - Dependencies: T2.4, T4.1

- [ ] **T4.3**: Add deduplicated GitHub activity fetching (once per repo)
  - Deliverable: Call CollectActivity.java once per unique repository
  - Validation: 2 directories same repo = 1 GitHub API call
  - Dependencies: T4.1

- [ ] **T4.4**: Create unified JSON output structure
  - Deliverable: Single JSON with `githubActivity`, `localChanges`, `metadata` sections
  - Validation: Parse with `jq` and verify schema matches proposal
  - Dependencies: T4.2, T4.3

### Phase 5: Enhanced Report Generation (AI Integration)

- [ ] **T5.1**: Create `prompts/multidir-standup.prompt.md` template
  - Deliverable: New prompt with sections for GitHub activity and local changes
  - Validation: Template contains `{{githubActivity}}` and `{{localChanges}}` placeholders
  - Dependencies: None (independent)

- [ ] **T5.2**: Add `formatMultiDirActivities()` method to Main.java
  - Deliverable: Format aggregated JSON into prompt template
  - Validation: Test with sample JSON, verify placeholder replacement
  - Dependencies: T5.1, T4.4

- [ ] **T5.3**: Update GenerateReport.java to accept multi-dir prompt (or keep as-is if compatible)
  - Deliverable: Verify existing GenerateReport.java works with new prompt
  - Validation: End-to-end test with real data
  - Dependencies: T5.2

### Phase 6: Report Storage (User Value)

- [ ] **T6.1**: Add `reportSettings` to config.json schema
  - Deliverable: `autoSaveReports`, `reportDirectory` fields with defaults
  - Validation: Load config with new fields, verify defaults
  - Dependencies: T1.1

- [ ] **T6.2**: Implement `saveReport()` method in Main.java
  - Deliverable: Auto-save to `~/.claude-gh-standup/reports/` with date-based filename
  - Validation: Generate report, verify file created with correct name
  - Dependencies: T6.1

- [ ] **T6.3**: Add filename logic (single repo vs multi-repo)
  - Deliverable: `YYYY-MM-DD-repo-name.md` or `YYYY-MM-DD-multi.md`
  - Validation: Test with 1 repo and 3 repos, verify filenames
  - Dependencies: T6.2

- [ ] **T6.4**: Create reports directory on first run
  - Deliverable: `Files.createDirectories()` before saving
  - Validation: First run creates `~/.claude-gh-standup/reports/`
  - Dependencies: T6.2

### Phase 7: Multi-Directory Workflow Integration (Orchestration)

- [ ] **T7.1**: Add mode detection logic to Main.java (`multiDirMode` check)
  - Deliverable: Detect config with non-empty directories → multi-dir mode
  - Validation: Empty config → legacy mode, populated config → multi-dir mode
  - Dependencies: T1.2

- [ ] **T7.2**: Implement `runMultiDirectoryMode()` method in Main.java
  - Deliverable: Orchestrate: load config → aggregate → format → generate → save → output
  - Validation: End-to-end test with 2 directories
  - Dependencies: T7.1, T4.4, T5.2, T6.2

- [ ] **T7.3**: Preserve `runSingleDirectoryMode()` for backward compatibility
  - Deliverable: Existing logic moved to dedicated method
  - Validation: Run with `--repo` flag, verify legacy mode
  - Dependencies: T7.1

- [ ] **T7.4**: Add `--repo` flag override (forces single-dir mode)
  - Deliverable: Explicit `--repo` bypasses config
  - Validation: Config exists but `--repo owner/repo` uses single-dir mode
  - Dependencies: T7.1

### Phase 8: Installation & Documentation (User Onboarding)

- [ ] **T8.1**: Create `install.sh` script with prerequisite checks
  - Deliverable: Bash script checking `jbang`, `gh`, `git`, `claude`
  - Validation: Run on fresh system, verify error messages
  - Dependencies: None (independent)

- [ ] **T8.2**: Add installation logic (clone or rsync to `~/.claude-gh-standup/`)
  - Deliverable: Copy files to fixed location, preserve existing config
  - Validation: Install, verify files in place, config not overwritten
  - Dependencies: T8.1

- [ ] **T8.3**: Add symlink creation for Claude Code (`~/.claude/commands/claude-gh-standup`)
  - Deliverable: Symlink to fixed installation directory
  - Validation: Claude Code `/help` shows command
  - Dependencies: T8.2

- [ ] **T8.4**: Add shell alias installation (optional prompt)
  - Deliverable: Add `standup-yesterday`, `standup-week`, `standup` to shell RC
  - Validation: Source RC file, run `standup-yesterday`
  - Dependencies: T8.2

- [ ] **T8.5**: Add config initialization prompt in install.sh
  - Deliverable: Offer to run `--config-init` during installation
  - Validation: User accepts → config created
  - Dependencies: T1.5

- [ ] **T8.6**: Update `--help` output in Main.java with new flags
  - Deliverable: Help text includes config commands, date shortcuts
  - Validation: `jbang scripts/Main.java --help` shows all options
  - Dependencies: T1.5, T3.1

- [ ] **T8.7**: Update `.claude/commands/claude-gh-standup.md` documentation
  - Deliverable: Multi-directory setup guide, examples, troubleshooting
  - Validation: Review documentation clarity
  - Dependencies: T8.6

- [ ] **T8.8**: Fix slash command invocation path (line 77) to use `~/.claude-gh-standup/`
  - Deliverable: Update from `$COMMAND_DIR` to fixed path
  - Validation: Slash command works after installation
  - Dependencies: T8.2

### Phase 9: Testing & Refinement (Quality Assurance)

- [ ] **T9.1**: Manual test: No config file (legacy mode)
  - Validation: Behaves identically to current version
  - Dependencies: T7.3

- [ ] **T9.2**: Manual test: Config with single directory
  - Validation: Works like legacy but uses config
  - Dependencies: T7.2

- [ ] **T9.3**: Manual test: Config with 2 directories (same repo, different branches)
  - Validation: GitHub activity deduplicated, local changes shown per branch
  - Dependencies: T7.2

- [ ] **T9.4**: Manual test: Config with 3 directories (different repos)
  - Validation: GitHub activity per repo, local changes per directory
  - Dependencies: T7.2

- [ ] **T9.5**: Manual test: `--yesterday` on Monday vs Tuesday
  - Validation: Monday = 3 days, Tuesday = 1 day
  - Dependencies: T3.3

- [ ] **T9.6**: Manual test: `--last-week` (7 days)
  - Validation: Correct date range
  - Dependencies: T3.3

- [ ] **T9.7**: Manual test: Report auto-save verification
  - Validation: File created in `~/.claude-gh-standup/reports/` with correct name
  - Dependencies: T6.3

- [ ] **T9.8**: Manual test: Config commands workflow (add → list → remove)
  - Validation: CRUD operations work correctly
  - Dependencies: T1.6

- [ ] **T9.9**: Manual test: Error scenarios (missing directory, git errors, no activity)
  - Validation: Graceful degradation, clear error messages
  - Dependencies: T2.3, T7.2

- [ ] **T9.10**: Manual test: Install script on fresh system
  - Validation: Installation succeeds, slash command works, aliases work
  - Dependencies: T8.4

- [ ] **T9.11**: Performance test: 3 directories complete in <15 seconds
  - Validation: Meets performance requirement
  - Dependencies: T4.2

- [ ] **T9.12**: Backward compatibility test: Existing flags work identically
  - Validation: `--days`, `--user`, `--repo`, `--format`, `--output` unchanged
  - Dependencies: T7.3

## Parallel Work Opportunities

**Can be done in parallel**:
- Phase 1 (Config) + Phase 2 (Local Changes) + Phase 3 (Date) + Phase 5 (Prompt) + Phase 8.1-8.3 (Install script structure)
- T2.1-T2.4 independent of all other work
- T3.1-T3.3 independent of all other work
- T5.1 independent of all other work
- T8.1-8.3 independent of configuration logic

**Must be sequential**:
- Phase 4 depends on Phase 1 (needs config) and Phase 2 (needs LocalChangesDetector)
- Phase 7 depends on Phases 1-6 (orchestration needs all components)
- Phase 9 depends on Phase 7 (testing needs complete implementation)

## Definition of Done

Each task is complete when:
1. ✅ Code is written and runs without errors
2. ✅ Validation criteria met (see per-task validation)
3. ✅ Manual testing performed (where applicable)
4. ✅ Error handling added for edge cases
5. ✅ stderr messages are clear and actionable

Overall change is complete when:
1. ✅ All 60+ tasks checked off
2. ✅ All manual tests (T9.1-T9.12) pass
3. ✅ Documentation updated (`--help`, slash command docs)
4. ✅ `openspec validate track-multi-directory-activity --strict` passes
5. ✅ End-to-end workflow tested with real GitHub data

## Notes

- **Incremental Delivery**: After Phase 2, local change detection can be used independently
- **Early User Feedback**: After Phase 3, date shortcuts can be tested without multi-dir
- **Risk Mitigation**: Backward compatibility (Phase 7.3-7.4) ensures no breaking changes
- **Performance**: Phase 4.2 parallelization is critical for good UX with 5+ directories
