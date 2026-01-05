# Tasks: Add Debug Flag

## 1. Core Debug Infrastructure

- [x] 1.1 Add `--debug` / `-D` flag to Args class in Main.java
- [x] 1.2 Add debug flag parsing logic in parseArgs()
- [x] 1.3 Update help text with debug flag description
- [x] 1.4 Create debug() utility method for conditional logging
- [x] 1.5 Replace existing hardcoded DEBUG prints with conditional calls

## 2. File-based Debug Logging

- [x] 2.1 Create initDebugSession() to initialize debug directory and session
- [x] 2.2 Generate timestamped session ID (YYYY-MM-DD-HH-mm-ss)
- [x] 2.3 Create session log file with header
- [x] 2.4 Update debug() to write to session log file
- [x] 2.5 Create saveScriptDebugLog() for script execution logs
- [x] 2.6 Add --debug-override flag for fixed filenames
- [x] 2.7 Implement cleanupOldDebugSessions() (keep last 10)

## 3. Main.java Debug Logging

- [x] 3.1 Add debug log for parsed arguments (days, user, repo, format, etc.)
- [x] 3.2 Add debug log for config loading (found/not found, directory count)
- [x] 3.3 Add debug log for mode detection (multi-dir vs single-dir)
- [x] 3.4 Add debug log for repository auto-detection
- [x] 3.5 Add debug log for user auto-detection
- [x] 3.6 Add debug log for script invocations with timing
- [x] 3.7 Add debug log for Claude invocation (prompt size)

## 4. Pass Debug Flag to Subprocesses

- [x] 4.1 Update runScript() to pass debug flag and capture stderr
- [x] 4.2 Update runScript() to call saveScriptDebugLog()
- [x] 4.3 Update ActivityAggregator call to capture outputs and save debug log

## 5. CollectActivity.java Debug Logging

- [x] 5.1 Add debug parameter parsing
- [x] 5.2 Add debug log for repository detection
- [x] 5.3 Add debug log for each GitHub API command (commits, PRs, issues)
- [x] 5.4 Add debug log for response sizes (number of items returned)

## 6. AnalyzeDiffs.java Debug Logging

- [x] 6.1 Add debug parameter parsing
- [x] 6.2 Add debug log for each PR diff request
- [x] 6.3 Add debug log for diff parsing statistics

## 7. ActivityAggregator.java Debug Logging

- [x] 7.1 Add debug parameter parsing
- [x] 7.2 Add debug log for directory filtering
- [x] 7.3 Add debug log for parallel thread pool setup
- [x] 7.4 Add debug log for each LocalChangesDetector call
- [x] 7.5 Add debug log for GitHub activity collection

## 8. LocalChangesDetector.java Debug Logging

- [x] 8.1 Add debug parameter parsing
- [x] 8.2 Add debug log for git commands executed
- [x] 8.3 Add debug log for uncommitted/unpushed change detection

## 9. Documentation & Testing

- [x] 9.1 Update CLAUDE.md with --debug flag in Standard Flags
- [x] 9.2 Update CLAUDE.md with --debug-override flag
- [x] 9.3 Add "Debugging workflow issues" troubleshooting section
- [x] 9.4 Update CLAUDE.md with debugSettings config documentation
- [ ] 9.5 Test --debug flag in single-directory mode (manual)
- [ ] 9.6 Test --debug flag in multi-directory mode (manual)
- [ ] 9.7 Verify debug files are created in ~/.claude-gh-standup/debug/
- [ ] 9.8 Verify auto-cleanup of old sessions works

## 10. Config Integration (debugSettings)

- [x] 10.1 Add DebugSettings class to ConfigManager.java
- [x] 10.2 Update Config class to include debugSettings field
- [x] 10.3 Update config.json with debugSettings section
- [x] 10.4 Add applyDebugSettings() method in Main.java
- [x] 10.5 Load config early and apply debugSettings before CLI args override
- [x] 10.6 Make debug variables configurable (MAX_DEBUG_SESSIONS, logDirectory)
- [x] 10.7 Add CAPTURE_SCRIPT_OUTPUT, VERBOSE_GIT_COMMANDS, VERBOSE_GITHUB_API settings
