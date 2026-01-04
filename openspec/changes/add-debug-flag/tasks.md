# Tasks: Add Debug Flag

## 1. Core Debug Infrastructure

- [ ] 1.1 Add `--debug` / `-D` flag to Args class in Main.java
- [ ] 1.2 Add debug flag parsing logic in parseArgs()
- [ ] 1.3 Update help text with debug flag description
- [ ] 1.4 Create Debug utility class or static method for conditional logging
- [ ] 1.5 Replace existing hardcoded DEBUG prints with conditional calls

## 2. Main.java Debug Logging

- [ ] 2.1 Add debug log for parsed arguments (days, user, repo, format, etc.)
- [ ] 2.2 Add debug log for config loading (found/not found, directory count)
- [ ] 2.3 Add debug log for mode detection (multi-dir vs single-dir)
- [ ] 2.4 Add debug log for repository auto-detection
- [ ] 2.5 Add debug log for user auto-detection
- [ ] 2.6 Add debug log for script invocations with timing
- [ ] 2.7 Add debug log for Claude invocation (prompt size)
- [ ] 2.8 Add debug log for report saving

## 3. Pass Debug Flag to Subprocesses

- [ ] 3.1 Update runScript() to accept and pass debug flag
- [ ] 3.2 Update ActivityAggregator call to pass debug flag
- [ ] 3.3 Update ConfigManager call to pass debug flag (if applicable)

## 4. CollectActivity.java Debug Logging

- [ ] 4.1 Add debug parameter parsing
- [ ] 4.2 Add debug log for repository detection
- [ ] 4.3 Add debug log for each GitHub API command (commits, PRs, issues)
- [ ] 4.4 Add debug log for response sizes (number of items returned)

## 5. AnalyzeDiffs.java Debug Logging

- [ ] 5.1 Add debug parameter parsing
- [ ] 5.2 Add debug log for each PR diff request
- [ ] 5.3 Add debug log for diff parsing statistics

## 6. ActivityAggregator.java Debug Logging

- [ ] 6.1 Add debug parameter parsing
- [ ] 6.2 Add debug log for directory filtering
- [ ] 6.3 Add debug log for parallel thread pool setup
- [ ] 6.4 Add debug log for each LocalChangesDetector call
- [ ] 6.5 Add debug log for GitHub activity collection

## 7. LocalChangesDetector.java Debug Logging

- [ ] 7.1 Add debug parameter parsing
- [ ] 7.2 Add debug log for git commands executed
- [ ] 7.3 Add debug log for uncommitted/unpushed change detection

## 8. ConfigManager.java Debug Logging

- [ ] 8.1 Add debug flag support (optional - may not need for config commands)
- [ ] 8.2 Add debug log for config file operations

## 9. Documentation & Testing

- [ ] 9.1 Test --debug flag in single-directory mode
- [ ] 9.2 Test --debug flag in multi-directory mode
- [ ] 9.3 Test --debug flag with team aggregation
- [ ] 9.4 Verify no debug output when flag is not used
- [ ] 9.5 Update CLAUDE.md with debug flag documentation
