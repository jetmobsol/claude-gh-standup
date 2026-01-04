# Change: Add Debug Flag for Workflow Visibility

## Why

Currently, DEBUG log statements are hardcoded to always print to stderr (Main.java:408-464), making the output noisy for end users. Users need a way to enable verbose logging to troubleshoot issues and understand the workflow execution. Additionally, there is no consistent logging across all scripts in the pipeline (CollectActivity, AnalyzeDiffs, ActivityAggregator, LocalChangesDetector, ConfigManager), making it difficult to diagnose problems in multi-step workflows.

Issue #23 requests the ability to enable debugging to see what is happening during execution.

## What Changes

- Add `--debug` / `-D` CLI flag to enable verbose logging
- Introduce a lightweight `Debug` utility pattern for conditional logging across all scripts
- Add debug logging to key workflow stages:
  - Argument parsing and mode detection
  - Config loading and validation
  - Script invocation with timing information
  - GitHub API calls (commands being executed, response sizes)
  - Claude invocation (prompt size, streaming status)
  - Report generation and saving
- Pass debug flag through subprocess invocations
- Ensure DEBUG messages only print when flag is enabled (fix current always-on DEBUG logs)

## Impact

- Affected specs: `slash-command-interface` (new capability)
- Affected code:
  - `scripts/Main.java` - Add --debug flag, conditional debug output
  - `scripts/CollectActivity.java` - Add debug logging for GitHub API calls
  - `scripts/AnalyzeDiffs.java` - Add debug logging for diff analysis
  - `scripts/ActivityAggregator.java` - Add debug logging for multi-dir orchestration
  - `scripts/LocalChangesDetector.java` - Add debug logging for git operations
  - `scripts/ConfigManager.java` - Add debug logging for config operations
- User experience: No visible change unless `--debug` is used
