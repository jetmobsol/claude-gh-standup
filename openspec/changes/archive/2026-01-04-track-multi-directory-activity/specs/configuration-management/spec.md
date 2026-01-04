# Capability: configuration-management

## ADDED Requirements

### Requirement: Configuration File Storage

The system MUST store multi-directory configuration in a JSON file at `~/.claude-gh-standup/config.json`.

#### Scenario: User initializes configuration
```
GIVEN the user has installed claude-gh-standup
WHEN they run /claude-gh-standup --config-init
THEN a config.json file is created at ~/.claude-gh-standup/config.json
AND the file contains an empty directories array
AND the file contains default reportSettings
```

#### Scenario: Configuration file already exists
```
GIVEN config.json already exists at ~/.claude-gh-standup/
WHEN the user runs --config-init
THEN the system prompts for confirmation before overwriting
AND existing configuration is preserved if user declines
```

---

### Requirement: Configuration Schema

The configuration file MUST follow this JSON schema structure:

```json
{
  "version": "1.0",
  "directories": [
    {
      "id": "string (unique identifier)",
      "path": "string (absolute path to directory)",
      "branch": "string (git branch name, auto-detected)",
      "enabled": "boolean (whether to include in reports)",
      "remoteUrl": "string (git remote URL, auto-detected)",
      "repoName": "string (owner/repo format, auto-detected)"
    }
  ],
  "reportSettings": {
    "defaultDays": "integer (default: 1)",
    "autoSaveReports": "boolean (default: true)",
    "reportDirectory": "string (default: ~/.claude-gh-standup/reports)"
  }
}
```

#### Scenario: Loading valid configuration
```
GIVEN a config.json file exists with valid schema
WHEN the system loads the configuration
THEN all directories are parsed correctly
AND all reportSettings are loaded with correct types
AND invalid fields are ignored with warnings
```

#### Scenario: Loading configuration with missing optional fields
```
GIVEN a config.json with only required fields (version, directories)
WHEN the system loads the configuration
THEN default values are used for reportSettings
AND no errors are raised
```

---

### Requirement: Directory Management

The system MUST provide commands to add, remove, and list configured directories.

#### Scenario: Add directory with auto-detection
```
GIVEN the user is in a git repository at ~/projects/myapp
WHEN they run /claude-gh-standup --config-add ~/projects/myapp --id main-branch
THEN the directory is added to config.json
AND the branch name is auto-detected from git
AND the remoteUrl is auto-detected from git remote
AND the repoName is parsed from remoteUrl (e.g., "owner/myapp")
AND the directory is enabled by default
```

#### Scenario: Add directory with explicit ID
```
GIVEN the user wants to add a directory
WHEN they run --config-add PATH --id custom-id
THEN the directory is added with the specified ID
AND the ID is used for future reference
```

#### Scenario: Add directory that is not a git repo
```
GIVEN the user provides a path that is not a git repository
WHEN they run --config-add /not/a/git/repo
THEN an error is displayed: "Not a git repository"
AND the directory is NOT added to config
```

#### Scenario: Add directory with duplicate ID
```
GIVEN a directory with ID "main-branch" already exists in config
WHEN the user runs --config-add PATH --id main-branch
THEN an error is displayed: "ID already exists"
AND the duplicate is NOT added
```

#### Scenario: List configured directories
```
GIVEN config.json contains 3 directories
WHEN the user runs --config-list
THEN all directories are displayed with: ID, path, branch, repo, enabled status
AND the output is formatted as a table or list
```

#### Scenario: Remove directory by ID
```
GIVEN config.json contains a directory with ID "feature-branch"
WHEN the user runs --config-remove feature-branch
THEN the directory is removed from config.json
AND the config file is saved
AND a confirmation message is displayed
```

#### Scenario: Remove non-existent directory
```
GIVEN config.json does not contain ID "nonexistent"
WHEN the user runs --config-remove nonexistent
THEN an error is displayed: "Directory not found"
AND no changes are made to config
```

---

### Requirement: Git Auto-Detection

The system MUST auto-detect git branch, remote URL, and repository name when adding directories.

#### Scenario: Detect git branch
```
GIVEN a directory at ~/projects/myapp on branch "feature/new-ui"
WHEN the system auto-detects git info
THEN the branch field is set to "feature/new-ui"
```

#### Scenario: Detect remote URL (SSH format)
```
GIVEN a directory with git remote "git@github.com:owner/myapp.git"
WHEN the system auto-detects git info
THEN the remoteUrl is "git@github.com:owner/myapp.git"
AND the repoName is parsed as "owner/myapp"
```

#### Scenario: Detect remote URL (HTTPS format)
```
GIVEN a directory with git remote "https://github.com/owner/myapp.git"
WHEN the system auto-detects git info
THEN the remoteUrl is "https://github.com/owner/myapp.git"
AND the repoName is parsed as "owner/myapp"
```

#### Scenario: Multiple remotes (use origin by default)
```
GIVEN a directory with remotes "origin" and "upstream"
WHEN the system auto-detects git info
THEN the remoteUrl is from "origin" (not "upstream")
```

#### Scenario: No remote configured
```
GIVEN a directory with no git remotes
WHEN the system auto-detects git info
THEN an error is displayed: "No remote configured"
AND the directory is NOT added
```

---

### Requirement: Tilde Expansion

The system MUST expand tilde (~) in file paths to the user's home directory.

#### Scenario: Expand tilde in directory path
```
GIVEN the user adds directory "~/projects/myapp"
WHEN the system processes the path
THEN "~" is expanded to the user's home directory (e.g., "/Users/username")
AND the full path is stored in config
```

#### Scenario: Expand tilde in report directory setting
```
GIVEN reportDirectory is set to "~/.claude-gh-standup/reports"
WHEN the system saves a report
THEN "~" is expanded to the user's home directory
AND the report is saved to the correct absolute path
```

---

### Requirement: Configuration Validation

The system MUST validate configuration on load and provide clear error messages for invalid data.

#### Scenario: Corrupted JSON file
```
GIVEN config.json contains invalid JSON syntax
WHEN the system attempts to load the configuration
THEN an error is displayed: "Invalid JSON in config file"
AND the system exits with code 1
AND the error message includes the JSON syntax error location
```

#### Scenario: Missing version field
```
GIVEN config.json is missing the "version" field
WHEN the system loads the configuration
THEN a warning is displayed
AND the system continues with default version "1.0"
```

#### Scenario: Invalid directory object (missing required field)
```
GIVEN a directory object is missing the "path" field
WHEN the system loads the configuration
THEN a warning is displayed: "Directory missing required field 'path' (skipping)"
AND that directory is skipped
AND other valid directories are loaded
```

---

### Requirement: Configuration Persistence

The system MUST save configuration changes immediately and atomically.

#### Scenario: Save after adding directory
```
GIVEN the user adds a directory with --config-add
WHEN the operation completes successfully
THEN config.json is updated on disk
AND the new directory is immediately available for reports
```

#### Scenario: Atomic write (prevent corruption)
```
GIVEN the user is modifying configuration
WHEN a write operation is in progress
THEN the system writes to a temporary file first
AND atomically renames the temp file to config.json
AND the original file is not corrupted if the process crashes
```

#### Scenario: Preserve formatting and comments (if any)
```
GIVEN config.json contains user comments (// or /* */)
WHEN the system saves configuration
THEN comments are preserved where possible (JSON allows trailing commas in some parsers)
OR a warning is displayed that comments will be lost
```
