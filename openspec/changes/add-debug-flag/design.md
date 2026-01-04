# Design: Debug Flag Implementation

## Context

The claude-gh-standup tool orchestrates multiple JBang scripts (CollectActivity, AnalyzeDiffs, etc.) via ProcessBuilder. Users need visibility into this multi-step workflow for troubleshooting. Currently, some DEBUG statements exist but always print, making output noisy.

**Stakeholders**: End users troubleshooting issues, developers debugging the tool

**Constraints**:
- JBang scripts communicate via stdout (JSON) and stderr (status messages)
- Scripts are invoked as subprocesses - debug flag must be passed through
- Minimal code changes preferred (no new dependencies)
- Debug output must go to stderr (stdout reserved for actual output)

## Goals / Non-Goals

**Goals**:
- Enable verbose logging via `--debug` flag
- Provide visibility into: arg parsing, config loading, script execution, API calls, timing
- Pass debug flag through subprocess chain
- Keep debug output informative but not overwhelming

**Non-Goals**:
- Full structured logging framework (out of scope - keep it simple)
- Log levels beyond on/off (no DEBUG/INFO/WARN hierarchy)
- Log file output (stderr is sufficient for CLI tool)
- Performance metrics beyond simple timing

## Decisions

### Decision 1: Use static boolean flag in each script

**What**: Each script will have a `private static boolean DEBUG = false;` field set from command-line args

**Why**: Simplest approach that works with JBang's single-file model. No shared dependencies needed.

**Alternatives considered**:
- Environment variable (`DEBUG=1`) - Rejected: less explicit, harder to control per-invocation
- Shared Debug.java utility - Rejected: complicates JBang's single-file execution model
- Java logging framework - Rejected: overkill for simple CLI tool

### Decision 2: Pass debug flag as last positional argument

**What**: Scripts will accept debug flag as additional argument: `jbang Script.java <args...> --debug`

**Why**: Maintains backward compatibility with existing argument parsing. Flag parsing at end is simple.

**Alternative considered**:
- Environment variable for subprocess - Rejected: less transparent, harder to trace

### Decision 3: Debug log format

**What**: Use format `[DEBUG] <component>: <message>` printed to stderr

**Why**: Clear, grep-able, consistent across all scripts. Example:
```
[DEBUG] Main: Parsed arguments: days=3, user=octocat, repo=null
[DEBUG] Main: Loading config from ~/.claude-gh-standup/config.json
[DEBUG] CollectActivity: Executing: gh search prs --author=octocat --created=>2026-01-01
[DEBUG] CollectActivity: Found 5 pull requests
```

### Decision 4: Timing information

**What**: Add elapsed time for significant operations (script calls, API calls)

**Why**: Helps identify performance bottlenecks

**Format**: `[DEBUG] Main: runScript(CollectActivity.java) completed in 2.3s`

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Debug output too verbose | Keep messages concise, one line per event |
| Performance impact | Minimal - string concatenation only when DEBUG=true |
| Subprocess debug flag parsing | Standardize flag position (last arg) across all scripts |

## Migration Plan

1. Implement debug flag in Main.java (fix existing always-on DEBUG logs)
2. Add debug flag passing to subprocess calls
3. Implement debug logging in each subsidiary script
4. Test end-to-end with and without --debug
5. Update documentation

**Rollback**: Remove --debug flag parsing, revert to current behavior (always-on DEBUG logs can remain as-is for testing)

## Open Questions

1. Should ConfigManager commands (--config-add, --config-list) support --debug?
   - Recommendation: Optional, lower priority since these are simpler operations
