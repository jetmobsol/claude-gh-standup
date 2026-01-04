# Change Proposal: track-multi-directory-activity

## Status
- **State**: Draft
- **Created**: 2026-01-04
- **Author**: AI Assistant (based on TODO.md)

## Summary

Enable claude-gh-standup to track multiple local directories (same repository, different branches) with intelligent deduplication of GitHub activity, local change detection (uncommitted/unpushed work), and convenient date range shortcuts (`--yesterday`, `--last-week`).

## Motivation

### Current Limitations
1. **Single-directory constraint**: Can only track one git repository/branch at a time
2. **No local work visibility**: Cannot detect uncommitted changes or unpushed commits
3. **Manual date calculation**: Users must calculate days manually (e.g., Monday morning needs `--days 3` not `--days 1`)
4. **Scattered reports**: No centralized storage for historical standup reports

### Use Cases
1. **Multi-branch developers**: Working on feature branch + maintaining main branch simultaneously
2. **Work-in-progress tracking**: Showing local uncommitted/unpushed work alongside merged GitHub activity
3. **Monday morning standups**: Automatically calculating "yesterday" as Friday (3 days back)
4. **Historical tracking**: Centralized report storage in `~/.claude-gh-standup/reports/` for reference

### Business Value
- **Time savings**: Eliminate manual branch switching and report aggregation
- **Better context**: Surface local WIP that hasn't reached GitHub yet
- **User experience**: Smart date shortcuts reduce cognitive load
- **Audit trail**: Auto-saved reports provide development history

## Proposed Solution

### High-Level Design

Transform claude-gh-standup from single-directory to multi-directory mode with:

1. **Configuration System**
   - Store directory configurations in `~/.claude-gh-standup/config.json`
   - Auto-detect git branch, remote URL, and repository name per directory
   - Enable/disable directories individually
   - Empty config = legacy single-directory mode (backward compatible)

2. **Local Change Detection**
   - Detect uncommitted changes (staged/unstaged files)
   - Detect unpushed commits (commits ahead of origin/branch)
   - Report per-directory WIP status

3. **Smart Activity Aggregation**
   - Deduplicate GitHub activity by repository (multiple branches = one GitHub fetch)
   - Parallel local change detection (3-4x faster)
   - Unified JSON structure for AI prompt

4. **Date Convenience**
   - `--yesterday`: 1 day back, or 3 days on Monday (covers Friday)
   - `--last-week`: 7 days back
   - Maintains existing `--days N` for flexibility

5. **Report Storage**
   - Auto-save reports to `~/.claude-gh-standup/reports/`
   - Filename format: `YYYY-MM-DD-repo-name.md` or `YYYY-MM-DD-multi.md`
   - Configurable via `reportSettings.autoSaveReports` flag

### Architecture Overview

```
User runs: /claude-gh-standup --yesterday

Main.java
  ├─> ConfigManager.java (load config.json)
  ├─> Mode detection (multi-dir vs legacy)
  └─> Multi-directory workflow:
      ├─> ActivityAggregator.java
      │   ├─> LocalChangesDetector.java (parallel for each dir)
      │   └─> CollectActivity.java (once per unique repo)
      ├─> GenerateReport.java (new multi-dir prompt)
      ├─> Auto-save to ~/.claude-gh-standup/reports/
      └─> Output to stdout
```

### Key Components

**New Files**:
1. `scripts/ConfigManager.java` - Configuration CRUD operations
2. `scripts/LocalChangesDetector.java` - Git change detection (uncommitted/unpushed)
3. `scripts/ActivityAggregator.java` - Multi-directory orchestration
4. `prompts/multidir-standup.prompt.md` - Enhanced prompt template
5. `config.json` - Empty config (shipped in repo)
6. `config.example.json` - Example with 2 directories
7. `install.sh` - Installation script with shell aliases

**Modified Files**:
1. `scripts/Main.java` - Mode detection, config commands, date flags
2. `.claude/commands/claude-gh-standup.md` - Updated documentation

### Backward Compatibility

**Guaranteed**:
- No config file → legacy mode (current behavior)
- Config file with empty `directories: []` → legacy mode
- Explicit `--repo` flag → overrides config, uses single-repo mode
- All existing flags work identically

**Migration Path**:
```bash
# Step 1: Continue using normally (no config)
/claude-gh-standup --days 3

# Step 2: Initialize config when ready
/claude-gh-standup --config-init

# Step 3: Add directories
/claude-gh-standup --config-add ~/projects/myapp/main --id main-branch
/claude-gh-standup --config-add ~/projects/myapp/feature --id feature-branch

# Step 4: Use multi-dir mode automatically
/claude-gh-standup --yesterday
```

## Impact Analysis

### Affected Capabilities
1. **slash-command-interface** (Main.java) - MODIFIED
   - Add config management commands (`--config-add`, `--config-list`, `--config-remove`, `--config-init`)
   - Add date shortcuts (`--yesterday`, `--last-week`)
   - Add mode detection logic (multi-dir vs legacy)
   - Add report auto-save

2. **github-activity-collection** (CollectActivity.java) - NO CHANGE
   - Remains unchanged, called once per unique repository

3. **report-generation** (GenerateReport.java) - EXTENDED
   - New multi-directory prompt template
   - Enhanced formatting for local changes section

4. **NEW: configuration-management** (ConfigManager.java)
   - New capability for managing `~/.claude-gh-standup/config.json`
   - CRUD operations, tilde expansion, git auto-detection

5. **NEW: local-change-detection** (LocalChangesDetector.java)
   - New capability for detecting uncommitted/unpushed work
   - Per-directory git status analysis

6. **NEW: activity-aggregation** (ActivityAggregator.java)
   - New capability for orchestrating multi-directory data collection
   - Deduplication, parallel processing, unified JSON output

### Breaking Changes
**NONE** - Feature is opt-in via configuration file.

### Dependencies
- No new external dependencies (uses existing `git`, `gh`, `jbang`)
- Gson 2.10.1 (already in use)

### Performance Implications
- **Single directory (legacy)**: No change (~3-5 seconds)
- **3 directories (same repo)**: ~5-10 seconds (parallel local detection)
- **3 directories (different repos)**: ~10-15 seconds
- **10 directories**: ~15-25 seconds (with ExecutorService parallelization)

### Security Considerations
- Config file stored in user home directory (`~/.claude-gh-standup/`)
- No secrets stored (relies on existing `gh` CLI authentication)
- File permissions inherited from user umask

### User Experience Changes
**Additions**:
- New CLI flags: `--config-*`, `--yesterday`, `--last-week`
- New shell aliases: `standup-yesterday`, `standup-week`, `standup`
- Auto-saved reports in `~/.claude-gh-standup/reports/`

**No Changes**:
- Existing flags behave identically
- Default behavior unchanged when no config present

## Success Criteria

### Functional Requirements
- [ ] Multi-directory configuration stored in `~/.claude-gh-standup/config.json`
- [ ] Auto-detect git branch, remote URL, repository name per directory
- [ ] Detect uncommitted changes (staged/unstaged files)
- [ ] Detect unpushed commits (ahead of origin)
- [ ] Deduplicate GitHub activity across directories of same repository
- [ ] `--yesterday` flag: 1 day back, or 3 days on Monday
- [ ] `--last-week` flag: 7 days back
- [ ] Reports auto-save to `~/.claude-gh-standup/reports/`
- [ ] Backward compatible: empty config = legacy mode
- [ ] Config management: `--config-add`, `--config-list`, `--config-remove`, `--config-init`

### Performance Requirements
- [ ] 3 directories complete in <15 seconds
- [ ] Local change detection runs in parallel
- [ ] GitHub activity fetched once per unique repository

### Documentation Requirements
- [ ] Updated `--help` output with new flags
- [ ] Shell alias examples in install.sh
- [ ] Multi-directory setup guide in `.claude/commands/claude-gh-standup.md`
- [ ] Example `config.example.json` with 2 directories

### Testing Requirements
- [ ] Manual test: No config file (legacy mode)
- [ ] Manual test: Config with single directory
- [ ] Manual test: Config with multiple directories (same repo)
- [ ] Manual test: Config with multiple directories (different repos)
- [ ] Manual test: `--yesterday` on Monday vs other days
- [ ] Manual test: `--last-week` functionality
- [ ] Manual test: Report auto-save verification
- [ ] Manual test: Config commands (add/remove/list)

## Alternatives Considered

### Alternative 1: Git Worktrees
**Description**: Use git worktrees instead of separate directory clones
**Pros**: Native git feature, less disk space
**Cons**: Requires users to restructure repositories, steep learning curve
**Decision**: Rejected - Too invasive, incompatible with existing workflows

### Alternative 2: Repository-Level Config
**Description**: Store config in `.claude-gh-standup/config.json` within each repo
**Pros**: Per-project configuration
**Cons**: Duplication across repos, doesn't solve multi-repo tracking
**Decision**: Rejected - User-level config (`~/.claude-gh-standup/`) fits better

### Alternative 3: Cloud-Based Storage
**Description**: Store reports in GitHub Gists or cloud service
**Pros**: Accessible from multiple machines
**Cons**: Adds network dependency, authentication complexity
**Decision**: Rejected for v1.0 - Local storage simpler, can add later

## Open Questions

1. **Q**: Should configuration support repository-level overrides (e.g., `.claude-gh-standup/config.json` in each repo)?
   **A**: Not in v1.0 - Keep it simple with user-level config only. Can add later if requested.

2. **Q**: Should we support remote branch tracking for unpushed commits (not just origin)?
   **A**: v1.0 uses `origin/<branch>` only. Multi-remote support deferred to v2.0.

3. **Q**: Should install.sh offer to clone user's repos automatically?
   **A**: No - Installation should be lightweight. Users manually add directories via `--config-add`.

4. **Q**: Should reports be git-committed to a dedicated repo?
   **A**: Not in v1.0 - Too opinionated. Users can manually commit from `~/.claude-gh-standup/reports/` if desired.

## Timeline

**Estimated Effort**: 15-20 hours (based on TODO.md)

**Phases**:
1. **Configuration Infrastructure** (3-4 hours)
   - ConfigManager.java
   - Config file schemas
   - Main.java config commands

2. **Local Change Detection** (2-3 hours)
   - LocalChangesDetector.java
   - Git command integration
   - JSON output structure

3. **Date & Aggregation** (3-4 hours)
   - Date calculation logic
   - ActivityAggregator.java
   - Parallel processing

4. **Enhanced Reporting** (2-3 hours)
   - Multi-dir prompt template
   - Report formatting
   - Auto-save logic

5. **Installation & Docs** (2-3 hours)
   - install.sh script
   - Shell aliases
   - Documentation updates

6. **Testing & Refinement** (3-4 hours)
   - Manual testing checklist
   - Bug fixes
   - Performance tuning

## References

- **Source**: `TODO.md` (comprehensive implementation plan)
- **Related**: Issue #22 (multi-directory support request)
- **Inspiration**: Original gh-standup project (single-directory model)
