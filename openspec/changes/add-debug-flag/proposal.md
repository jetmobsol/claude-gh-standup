# Change: Add Debug Flag for Workflow Visibility

## Why

Currently, DEBUG log statements are hardcoded to always print to stderr (Main.java:408-464), making the output noisy for end users. Users need a way to enable verbose logging to troubleshoot issues and understand the workflow execution. Additionally, there is no consistent logging across all scripts in the pipeline (CollectActivity, AnalyzeDiffs, ActivityAggregator, LocalChangesDetector, ConfigManager), making it difficult to diagnose problems in multi-step workflows.

Issue #23 requests the ability to enable debugging to see what is happening during execution, including saving script outputs to files for later analysis.

## What Changes

### 1. Console Debug Output
- Add `--debug` / `-D` CLI flag to enable verbose logging
- Introduce a lightweight `debug()` utility method for conditional logging across all scripts
- Add debug logging to key workflow stages:
  - Argument parsing and mode detection
  - Config loading and validation
  - Script invocation with timing information
  - GitHub API calls (commands being executed, response sizes)
  - Claude invocation (prompt size, streaming status)
  - Report generation and saving
- Pass debug flag through subprocess invocations
- Ensure DEBUG messages only print when flag is enabled

### 2. File-based Debug Logging
- Create `~/.claude-gh-standup/debug/` directory for debug logs
- Save session log with all debug messages: `YYYY-MM-DD-HH-mm-ss-session.log`
- Save each script's stdout/stderr to separate markdown files:
  - `YYYY-MM-DD-HH-mm-ss-CollectActivity.md`
  - `YYYY-MM-DD-HH-mm-ss-AnalyzeDiffs.md`
  - `YYYY-MM-DD-HH-mm-ss-ActivityAggregator.md`
  - `YYYY-MM-DD-HH-mm-ss-LocalChangesDetector-<dirId>.md`

### 3. Debug Override Mode
- Add `--debug-override` flag for fixed filenames (no timestamps)
- Useful for CI/CD or development where you want to overwrite previous logs

### 4. Auto-cleanup
- Keep only last 10 debug sessions (configurable via MAX_DEBUG_SESSIONS)
- Automatic cleanup when new sessions are created

### 5. Config Integration (debugSettings)
- Add `debugSettings` section to config.json for persistent debug configuration
- Settings include: `enabled`, `logDirectory`, `maxSessions`, `captureScriptOutput`, `verboseGitCommands`, `verboseGitHubAPICalls`
- Config settings act as defaults; CLI flags override config values
- Add `DebugSettings` class to ConfigManager.java

## Impact

- Affected specs: `slash-command-interface` (new capability)
- Affected code:
  - `scripts/Main.java` - Add --debug flag, debug logging, file saving infrastructure
  - `scripts/CollectActivity.java` - Add debug logging for GitHub API calls
  - `scripts/AnalyzeDiffs.java` - Add debug logging for diff analysis
  - `scripts/ActivityAggregator.java` - Add debug logging for multi-dir orchestration
  - `scripts/LocalChangesDetector.java` - Add debug logging for git operations
  - `scripts/ConfigManager.java` - Add DebugSettings class
  - `config.json` - Add debugSettings section
  - `CLAUDE.md` - Documentation updates
- User experience: No visible change unless `--debug` is used or `debugSettings.enabled` is true
