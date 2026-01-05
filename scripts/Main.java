///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Main - Entry point for claude-gh-standup slash command
 *
 * Usage: jbang Main.java [--days N] [--user USERNAME] [--repo REPO] [--format FORMAT] [--team USERS...] [--output FILE]
 */
public class Main {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Debug flag - static so it can be accessed from all methods
    private static boolean DEBUG = false;
    private static boolean DEBUG_OVERRIDE = false;
    private static String DEBUG_SESSION_ID = null;
    private static Path DEBUG_DIR = null;
    private static int MAX_DEBUG_SESSIONS = 10;  // Can be overridden by config
    private static String DEBUG_LOG_DIRECTORY = "~/.claude-gh-standup/debug";  // Can be overridden by config
    private static boolean CAPTURE_SCRIPT_OUTPUT = true;  // From config
    private static boolean VERBOSE_GIT_COMMANDS = true;   // From config
    private static boolean VERBOSE_GITHUB_API = true;     // From config

    /**
     * Initialize debug session - creates debug directory and session log
     */
    private static void initDebugSession() {
        if (!DEBUG) return;

        try {
            DEBUG_DIR = Paths.get(expandTilde(DEBUG_LOG_DIRECTORY));
            Files.createDirectories(DEBUG_DIR);

            // Generate session ID
            if (DEBUG_OVERRIDE) {
                DEBUG_SESSION_ID = "debug";
            } else {
                DEBUG_SESSION_ID = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
            }

            // Clean up old sessions (keep last MAX_DEBUG_SESSIONS)
            if (!DEBUG_OVERRIDE) {
                cleanupOldDebugSessions();
            }

            // Create session log header
            String sessionLogPath = DEBUG_DIR + "/" + DEBUG_SESSION_ID + "-session.log";
            String header = "# Debug Session: " + DEBUG_SESSION_ID + "\n" +
                           "**Started:** " + java.time.LocalDateTime.now() + "\n" +
                           "**Working Directory:** " + System.getProperty("user.dir") + "\n\n" +
                           "## Debug Log\n\n";
            Files.writeString(Paths.get(sessionLogPath), header);

            debug("Debug session initialized: " + DEBUG_SESSION_ID);
            debug("Debug files will be saved to: " + DEBUG_DIR);
        } catch (IOException e) {
            System.err.println("Warning: Could not initialize debug session: " + e.getMessage());
        }
    }

    /**
     * Clean up old debug sessions, keeping only the most recent MAX_DEBUG_SESSIONS
     */
    private static void cleanupOldDebugSessions() {
        try {
            List<Path> sessionLogs = Files.list(DEBUG_DIR)
                .filter(p -> p.getFileName().toString().endsWith("-session.log"))
                .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                .collect(java.util.stream.Collectors.toList());

            if (sessionLogs.size() >= MAX_DEBUG_SESSIONS) {
                // Get sessions to delete (all except most recent MAX_DEBUG_SESSIONS - 1)
                for (int i = MAX_DEBUG_SESSIONS - 1; i < sessionLogs.size(); i++) {
                    String sessionId = sessionLogs.get(i).getFileName().toString().replace("-session.log", "");
                    // Delete all files for this session
                    Files.list(DEBUG_DIR)
                        .filter(p -> p.getFileName().toString().startsWith(sessionId))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                                debug("Cleaned up old debug file: " + p.getFileName());
                            } catch (IOException e) {
                                // Ignore cleanup errors
                            }
                        });
                }
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }

    /**
     * Conditional debug logging - prints to stderr and optionally saves to file
     */
    private static void debug(String message) {
        if (DEBUG) {
            String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            String logLine = "[DEBUG] Main: " + message;
            System.err.println(logLine);

            // Also write to session log file
            if (DEBUG_DIR != null && DEBUG_SESSION_ID != null) {
                try {
                    String sessionLogPath = DEBUG_DIR + "/" + DEBUG_SESSION_ID + "-session.log";
                    Files.writeString(Paths.get(sessionLogPath),
                        timestamp + " " + logLine + "\n",
                        java.nio.file.StandardOpenOption.APPEND);
                } catch (IOException e) {
                    // Ignore file write errors
                }
            }
        }
    }

    /**
     * Save script execution details to a markdown file
     */
    private static void saveScriptDebugLog(String scriptName, List<String> args,
                                            String stdout, String stderr,
                                            int exitCode, long durationMs,
                                            String directorySuffix) {
        if (!DEBUG || !CAPTURE_SCRIPT_OUTPUT || DEBUG_DIR == null || DEBUG_SESSION_ID == null) return;

        try {
            String suffix = directorySuffix != null ? "-" + directorySuffix : "";
            String filename = DEBUG_SESSION_ID + "-" + scriptName.replace(".java", "") + suffix + ".md";
            Path filePath = DEBUG_DIR.resolve(filename);

            StringBuilder content = new StringBuilder();
            content.append("# Script Execution: ").append(scriptName).append("\n\n");
            content.append("**Session ID:** ").append(DEBUG_SESSION_ID).append("\n");
            content.append("**Arguments:** ").append(String.join(" ", args)).append("\n");
            content.append("**Timestamp:** ").append(java.time.LocalDateTime.now()).append("\n");
            content.append("**Duration:** ").append(durationMs).append("ms\n");
            content.append("**Exit Code:** ").append(exitCode).append("\n\n");

            content.append("## Standard Output\n\n```json\n");
            content.append(stdout.length() > 10000 ? stdout.substring(0, 10000) + "\n... (truncated)" : stdout);
            content.append("\n```\n\n");

            if (stderr != null && !stderr.isEmpty()) {
                content.append("## Standard Error\n\n```\n");
                content.append(stderr.length() > 5000 ? stderr.substring(0, 5000) + "\n... (truncated)" : stderr);
                content.append("\n```\n");
            }

            Files.writeString(filePath, content.toString());
            debug("Saved script log to: " + filename);
        } catch (IOException e) {
            debug("Warning: Could not save script debug log: " + e.getMessage());
        }
    }

    /**
     * Apply debug settings from config.json - called before CLI args override
     */
    private static void applyDebugSettings(JsonObject config) {
        if (config == null || !config.has("debugSettings")) return;

        JsonObject debugSettings = config.getAsJsonObject("debugSettings");

        // Apply config settings (CLI args will override these later)
        if (debugSettings.has("enabled") && debugSettings.get("enabled").getAsBoolean()) {
            DEBUG = true;
        }
        if (debugSettings.has("logDirectory")) {
            DEBUG_LOG_DIRECTORY = debugSettings.get("logDirectory").getAsString();
        }
        if (debugSettings.has("maxSessions")) {
            MAX_DEBUG_SESSIONS = debugSettings.get("maxSessions").getAsInt();
        }
        if (debugSettings.has("captureScriptOutput")) {
            CAPTURE_SCRIPT_OUTPUT = debugSettings.get("captureScriptOutput").getAsBoolean();
        }
        if (debugSettings.has("verboseGitCommands")) {
            VERBOSE_GIT_COMMANDS = debugSettings.get("verboseGitCommands").getAsBoolean();
        }
        if (debugSettings.has("verboseGitHubAPICalls")) {
            VERBOSE_GITHUB_API = debugSettings.get("verboseGitHubAPICalls").getAsBoolean();
        }
    }

    static class Args {
        int days = 1;
        String user = null;
        String repo = null;
        String format = "markdown";
        List<String> team = null;
        String output = null;
        boolean noClaude = false;
        boolean debug = false;
        boolean debugOverride = false;

        // Date shortcuts
        boolean yesterday = false;
        boolean lastWeek = false;

        // Config commands
        String configCommand = null;  // init, add, remove, list
        String configPath = null;
        String configId = null;
    }

    public static Args parseArgs(String[] args) {
        Args parsed = new Args();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--days":
                case "-d":
                    if (i + 1 < args.length) {
                        parsed.days = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--user":
                case "-u":
                    if (i + 1 < args.length) {
                        parsed.user = args[++i];
                    }
                    break;
                case "--repo":
                case "-r":
                    if (i + 1 < args.length) {
                        parsed.repo = args[++i];
                    }
                    break;
                case "--format":
                case "-f":
                    if (i + 1 < args.length) {
                        parsed.format = args[++i];
                    }
                    break;
                case "--team":
                    parsed.team = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        parsed.team.add(args[++i]);
                    }
                    break;
                case "--output":
                case "-o":
                    if (i + 1 < args.length) {
                        parsed.output = args[++i];
                    }
                    break;
                case "--no-claude":
                    parsed.noClaude = true;
                    break;
                case "--yesterday":
                    parsed.yesterday = true;
                    break;
                case "--last-week":
                    parsed.lastWeek = true;
                    break;
                case "--config-init":
                    parsed.configCommand = "init";
                    break;
                case "--config-add":
                    parsed.configCommand = "add";
                    if (i + 1 < args.length) {
                        parsed.configPath = args[++i];
                    }
                    // Check for optional --id flag
                    if (i + 1 < args.length && args[i + 1].equals("--id")) {
                        i++; // Skip --id
                        if (i + 1 < args.length) {
                            parsed.configId = args[++i];
                        }
                    }
                    break;
                case "--config-list":
                    parsed.configCommand = "list";
                    break;
                case "--config-remove":
                    parsed.configCommand = "remove";
                    if (i + 1 < args.length) {
                        parsed.configId = args[++i];
                    }
                    break;
                case "--id":
                    // Handled inline with --config-add
                    break;
                case "--debug":
                case "-D":
                    parsed.debug = true;
                    break;
                case "--debug-override":
                    parsed.debugOverride = true;
                    break;
                case "--help":
                case "-h":
                    printHelp();
                    System.exit(0);
                    break;
            }
        }

        return parsed;
    }

    public static void printHelp() {
        System.out.println("claude-gh-standup - Generate AI-powered GitHub standup reports");
        System.out.println();
        System.out.println("Usage: /claude-gh-standup [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --days, -d N        Number of days to look back (default: 1)");
        System.out.println("  --user, -u USER     GitHub username (default: current user)");
        System.out.println("  --repo, -r REPO     Filter by repository (format: owner/repo)");
        System.out.println("  --format, -f FMT    Output format: markdown, json, html (default: markdown)");
        System.out.println("  --team USERS...     Generate team report for multiple users");
        System.out.println("  --output, -o FILE   Write to file instead of stdout");
        System.out.println("  --no-claude         Skip claude -p call and output prompt directly");
        System.out.println("  --debug, -D         Enable verbose debug logging (saves to ~/.claude-gh-standup/debug/)");
        System.out.println("  --debug-override    Use fixed debug filenames (overwrite previous, no timestamps)");
        System.out.println("  --help, -h          Show this help message");
        System.out.println();
        System.out.println("Date Shortcuts:");
        System.out.println("  --yesterday         Yesterday's work (Friday if Monday)");
        System.out.println("  --last-week         Last 7 days of activity");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  --config-add PATH [--id ID]  Add directory to config");
        System.out.println("  --config-list                List configured directories");
        System.out.println("  --config-remove ID           Remove directory from config");
        System.out.println("  --config-init                Initialize configuration file");
    }

    public static String getCurrentUser() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("gh", "api", "user", "--jq", ".login");
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to get current user from gh CLI");
        }

        return output.toString().trim();
    }

    public static String formatActivities(JsonObject activity) {
        StringBuilder sb = new StringBuilder();

        // Format commits
        JsonArray commits = activity.getAsJsonArray("commits");
        if (commits != null && commits.size() > 0) {
            sb.append("COMMITS:\n");
            for (JsonElement commitElement : commits) {
                JsonObject commit = commitElement.getAsJsonObject();
                JsonObject commitData = commit.getAsJsonObject("commit");
                JsonObject repo = commit.getAsJsonObject("repository");

                if (repo == null || repo.get("nameWithOwner") == null) {
                    continue; // Skip commits without repository info
                }

                String repoName = repo.get("nameWithOwner").getAsString();
                String message = commitData.get("message").getAsString().split("\n")[0]; // First line only
                String sha = commit.get("sha").getAsString().substring(0, 7);

                sb.append("- [").append(repoName).append("] ").append(message)
                        .append(" (").append(sha).append(")\n");
            }
            sb.append("\n");
        }

        // Format pull requests
        JsonArray prs = activity.getAsJsonArray("pull_requests");
        if (prs != null && prs.size() > 0) {
            sb.append("PULL REQUESTS:\n");
            for (JsonElement prElement : prs) {
                JsonObject pr = prElement.getAsJsonObject();
                JsonObject repo = pr.getAsJsonObject("repository");

                if (repo == null || repo.get("nameWithOwner") == null) {
                    continue; // Skip PRs without repository info
                }

                String repoName = repo.get("nameWithOwner").getAsString();
                int number = pr.get("number").getAsInt();
                String title = pr.get("title").getAsString();
                String state = pr.get("state").getAsString();

                sb.append("- [").append(repoName).append("] #").append(number).append(": ")
                        .append(title).append(" (").append(state).append(")\n");
            }
            sb.append("\n");
        }

        // Format issues
        JsonArray issues = activity.getAsJsonArray("issues");
        if (issues != null && issues.size() > 0) {
            sb.append("ISSUES:\n");
            for (JsonElement issueElement : issues) {
                JsonObject issue = issueElement.getAsJsonObject();
                JsonObject repo = issue.getAsJsonObject("repository");

                if (repo == null || repo.get("nameWithOwner") == null) {
                    continue; // Skip issues without repository info
                }

                String repoName = repo.get("nameWithOwner").getAsString();
                int number = issue.get("number").getAsInt();
                String title = issue.get("title").getAsString();
                String state = issue.get("state").getAsString();

                sb.append("- [").append(repoName).append("] #").append(number).append(": ")
                        .append(title).append(" (").append(state).append(")\n");
            }
            sb.append("\n");
        }

        if (sb.length() == 0) {
            return "No GitHub activity found for this period.";
        }

        return sb.toString();
    }

    /**
     * Groups GitHub activities by repository name for multi-directory mode display.
     * Activities are extracted from the single githubActivity object and grouped by repo.
     */
    public static String formatActivitiesGroupedByRepo(JsonObject activity, int days) {
        // Use TreeMap to sort repositories alphabetically
        Map<String, List<String>> repoActivities = new TreeMap<>();

        // Process commits
        JsonArray commits = activity.getAsJsonArray("commits");
        if (commits != null) {
            for (JsonElement commitElement : commits) {
                JsonObject commit = commitElement.getAsJsonObject();
                JsonObject commitData = commit.getAsJsonObject("commit");
                JsonObject repo = commit.getAsJsonObject("repository");

                if (repo == null || repo.get("nameWithOwner") == null) {
                    String sha = commit.has("sha") ? commit.get("sha").getAsString().substring(0, 7) : "unknown";
                    String msg = commitData != null && commitData.has("message")
                        ? commitData.get("message").getAsString().split("\n")[0]
                        : "unknown";
                    System.err.println("⚠️ Skipping commit without repository info: " + sha + " - " + msg);
                    continue;
                }

                String repoName = repo.get("nameWithOwner").getAsString();
                String message = commitData.get("message").getAsString().split("\n")[0];
                String sha = commit.get("sha").getAsString().substring(0, 7);

                repoActivities.computeIfAbsent(repoName, k -> new ArrayList<>())
                    .add("- Commit: " + message + " (" + sha + ")");
            }
        }

        // Process pull requests
        JsonArray prs = activity.getAsJsonArray("pull_requests");
        if (prs != null) {
            for (JsonElement prElement : prs) {
                JsonObject pr = prElement.getAsJsonObject();
                JsonObject repo = pr.getAsJsonObject("repository");

                if (repo == null || repo.get("nameWithOwner") == null) {
                    System.err.println("⚠️ Skipping PR without repository info");
                    continue;
                }

                String repoName = repo.get("nameWithOwner").getAsString();
                int number = pr.get("number").getAsInt();
                String title = pr.get("title").getAsString();
                String state = pr.get("state").getAsString();

                repoActivities.computeIfAbsent(repoName, k -> new ArrayList<>())
                    .add("- PR #" + number + ": " + title + " (" + state + ")");
            }
        }

        // Process issues
        JsonArray issues = activity.getAsJsonArray("issues");
        if (issues != null) {
            for (JsonElement issueElement : issues) {
                JsonObject issue = issueElement.getAsJsonObject();
                JsonObject repo = issue.getAsJsonObject("repository");

                if (repo == null || repo.get("nameWithOwner") == null) {
                    System.err.println("⚠️ Skipping issue without repository info");
                    continue;
                }

                String repoName = repo.get("nameWithOwner").getAsString();
                int number = issue.get("number").getAsInt();
                String title = issue.get("title").getAsString();
                String state = issue.get("state").getAsString();

                repoActivities.computeIfAbsent(repoName, k -> new ArrayList<>())
                    .add("- Issue #" + number + ": " + title + " (" + state + ")");
            }
        }

        // Build output string grouped by repository
        if (repoActivities.isEmpty()) {
            return "No GitHub activity in the last " + days + " day(s).";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : repoActivities.entrySet()) {
            sb.append("### ").append(entry.getKey()).append("\n\n");
            for (String activity_item : entry.getValue()) {
                sb.append(activity_item).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String getCurrentRepository() throws Exception {
        // Check if we're in a git repository
        ProcessBuilder checkGit = new ProcessBuilder("git", "rev-parse", "--git-dir");
        Process checkProcess = checkGit.start();
        int checkExit = checkProcess.waitFor();

        if (checkExit != 0) {
            return null; // Not in a git repository
        }

        // Get remote URL
        ProcessBuilder pb = new ProcessBuilder("git", "remote", "get-url", "origin");
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            return null; // No remote configured
        }

        String remoteUrl = output.toString().trim();

        // Extract owner/repo from GitHub URL
        // Supports: https://github.com/owner/repo.git or git@github.com:owner/repo.git
        if (remoteUrl.contains("github.com")) {
            String[] parts = remoteUrl.split("github.com[:/]");
            if (parts.length > 1) {
                String repoPath = parts[1].replaceAll("\\.git$", "");
                return repoPath;
            }
        }

        return null;
    }

    public static String runScript(String scriptName, List<String> scriptArgs) throws Exception {
        return runScript(scriptName, scriptArgs, null);
    }

    public static String runScript(String scriptName, List<String> scriptArgs, String directorySuffix) throws Exception {
        long startTime = System.currentTimeMillis();
        debug("runScript called for: " + scriptName);
        debug("Number of args: " + scriptArgs.size());
        for (int i = 0; i < scriptArgs.size(); i++) {
            debug("Arg " + i + " length: " + scriptArgs.get(i).length());
        }

        // Determine the installation directory (where Main.java is located)
        String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
        Path scriptPath = Paths.get(installDir, "scripts", scriptName);

        if (!Files.exists(scriptPath)) {
            // Fallback: try relative to current directory (for development)
            scriptPath = Paths.get("scripts", scriptName);
            if (!Files.exists(scriptPath)) {
                // Last resort: just the script name
                scriptPath = Paths.get(scriptName);
            }
        }
        debug("Script path: " + scriptPath);

        List<String> command = new ArrayList<>();
        command.add("jbang");
        command.add(scriptPath.toString());
        command.addAll(scriptArgs);
        // Pass debug flag to subprocess if enabled
        if (DEBUG) {
            command.add("--debug");
        }

        debug("Command: " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        debug("Starting process...");
        Process process = pb.start();
        debug("Process started, reading stdout...");

        // Capture stdout
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // Capture stderr
        StringBuilder stderrOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stderrOutput.append(line).append("\n");
                // Also print to stderr in real-time for user visibility
                System.err.println(line);
            }
        }

        debug("Finished reading stdout/stderr, waiting for exit...");

        int exitCode = process.waitFor();
        long elapsed = System.currentTimeMillis() - startTime;
        debug("Process exited with code: " + exitCode + " (elapsed: " + elapsed + "ms)");

        // Save debug log for this script execution
        saveScriptDebugLog(scriptName, scriptArgs, output.toString(), stderrOutput.toString(),
                          exitCode, elapsed, directorySuffix);

        if (exitCode != 0) {
            System.err.println("Error running " + scriptName + ":");
            System.err.println(stderrOutput.toString());
            throw new RuntimeException("Script " + scriptName + " failed with exit code " + exitCode);
        }

        debug("runScript completed for: " + scriptName + " in " + elapsed + "ms");
        return output.toString();
    }

    private static int calculateDays(Args parsed) {
        if (parsed.yesterday) {
            LocalDate today = LocalDate.now();
            if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
                return 3;  // Friday + Saturday + Sunday
            }
            return 1;
        }
        if (parsed.lastWeek) {
            return 7;
        }
        return parsed.days;  // Explicit --days flag
    }

    private static String expandTilde(String path) {
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    private static JsonObject loadConfigJson() {
        try {
            String configPath = expandTilde("~/.claude-gh-standup/config.json");
            if (!Files.exists(Paths.get(configPath))) {
                return null;
            }
            String json = Files.readString(Paths.get(configPath));
            return gson.fromJson(json, JsonObject.class);
        } catch (Exception e) {
            System.err.println("Warning: Could not load config: " + e.getMessage());
            return null;
        }
    }

    private static boolean shouldUseMultiDirectoryMode(JsonObject config, Args parsed) {
        if (config == null) return false;
        if (parsed.repo != null) return false;  // Explicit --repo overrides config
        if (!config.has("directories")) return false;

        JsonArray dirs = config.getAsJsonArray("directories");
        return dirs != null && dirs.size() > 0;
    }

    private static void runMultiDirectoryMode(JsonObject config, Args parsed) throws Exception {
        debug("Entering multi-directory mode");
        System.err.println("Running in multi-directory mode...");

        // Calculate effective days
        int days = calculateDays(parsed);
        debug("Effective days: " + days);

        // Get user
        String user = parsed.user;
        if (user == null && parsed.team == null) {
            debug("No --user specified, detecting from gh CLI");
            System.err.println("Detecting current GitHub user...");
            user = getCurrentUser();
            debug("User detected: " + user);
        }

        // Call ActivityAggregator
        System.err.println("Aggregating activities across directories...");
        String configJson = gson.toJson(config);
        debug("Config JSON length: " + configJson.length() + " chars");

        String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
        String aggregatorScript = installDir + "/scripts/ActivityAggregator.java";
        if (!Files.exists(Paths.get(aggregatorScript))) {
            aggregatorScript = "scripts/ActivityAggregator.java";  // Fallback for development
        }
        debug("ActivityAggregator script: " + aggregatorScript);

        List<String> command = new ArrayList<>();
        command.add("jbang");
        command.add(aggregatorScript);
        command.add(configJson);
        command.add(user);
        command.add(String.valueOf(days));
        if (DEBUG) {
            command.add("--debug");
        }
        debug("ActivityAggregator command args: configJson=" + configJson.length() + "chars, user=" + user + ", days=" + days);

        ProcessBuilder pb = new ProcessBuilder(command);
        long startTime = System.currentTimeMillis();

        Process process = pb.start();
        StringBuilder aggregatedJson = new StringBuilder();
        StringBuilder stderrCapture = new StringBuilder();

        // Capture aggregator stderr in a separate thread
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderrCapture.append(line).append("\n");
                    System.err.println(line);
                }
            } catch (IOException e) {
                // Ignore
            }
        });
        stderrThread.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                aggregatedJson.append(line);
            }
        }

        int exitCode = process.waitFor();
        stderrThread.join(1000);  // Wait for stderr thread to finish
        long elapsed = System.currentTimeMillis() - startTime;
        debug("ActivityAggregator exited with code: " + exitCode + " (elapsed: " + elapsed + "ms)");

        // Save debug log for ActivityAggregator
        List<String> aggregatorArgs = Arrays.asList(configJson.length() + " chars", user, String.valueOf(days));
        saveScriptDebugLog("ActivityAggregator.java", aggregatorArgs,
                          aggregatedJson.toString(), stderrCapture.toString(),
                          exitCode, elapsed, null);

        if (exitCode != 0) {
            System.err.println("ActivityAggregator failed");
            System.exit(1);
        }

        // Parse aggregated data
        debug("Aggregated JSON length: " + aggregatedJson.length() + " chars");
        JsonObject aggregated = gson.fromJson(aggregatedJson.toString(), JsonObject.class);

        // Format multi-dir prompt
        debug("Formatting multi-directory prompt");
        String prompt = formatMultiDirPrompt(aggregated);
        debug("Multi-dir prompt length: " + prompt.length() + " chars");

        // Generate report
        String report;
        if (parsed.noClaude) {
            debug("--no-claude flag set, returning prompt directly");
            report = prompt;
        } else {
            debug("Invoking Claude for report generation");
            report = generateReportWithClaude(prompt);
            debug("Report generated, length: " + report.length() + " chars");
        }

        // Auto-save if enabled
        if (config.has("reportSettings")) {
            JsonObject reportSettings = config.getAsJsonObject("reportSettings");
            boolean autoSave = reportSettings.get("autoSaveReports").getAsBoolean();
            if (autoSave) {
                saveReport(report, config, aggregated);
            }
        }

        // Note: report is already printed in real-time by generateReportWithClaude()
        // Only print here if --no-claude was used (prompt mode)
        if (parsed.noClaude) {
            System.out.println(report);
        }
    }

    private static String formatMultiDirPrompt(JsonObject aggregated) throws IOException {
        // Load template
        String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
        Path templatePath = Paths.get(installDir, "prompts/multidir-standup.prompt.md");
        if (!Files.exists(templatePath)) {
            templatePath = Paths.get("prompts/multidir-standup.prompt.md");  // Fallback for development
        }
        String template = Files.readString(templatePath);

        // Extract data
        JsonObject githubActivity = aggregated.getAsJsonObject("githubActivity");
        JsonArray localChanges = aggregated.getAsJsonArray("localChanges");
        JsonObject metadata = aggregated.getAsJsonObject("metadata");
        int days = metadata.get("days").getAsInt();

        // Format GitHub activity (now a single object with commits/PRs/issues arrays, grouped by repo)
        String githubStr = formatActivitiesGroupedByRepo(githubActivity, days);

        // Format local changes
        StringBuilder localStr = new StringBuilder();
        for (JsonElement elem : localChanges) {
            JsonObject change = elem.getAsJsonObject();
            String dirId = change.get("directoryId").getAsString();
            String path = change.get("path").getAsString();
            String branch = change.get("branch").getAsString();

            localStr.append("## ").append(dirId).append(" (").append(path).append(" - branch: ").append(branch).append(")\n\n");

            JsonObject uncommitted = change.getAsJsonObject("uncommitted");
            JsonObject unpushed = change.getAsJsonObject("unpushed");

            if (uncommitted != null && uncommitted.has("hasChanges") && uncommitted.get("hasChanges").getAsBoolean()) {
                localStr.append("### Uncommitted Changes\n");

                JsonArray staged = uncommitted.getAsJsonArray("staged");
                if (staged.size() > 0) {
                    localStr.append("- Staged: ");
                    for (int i = 0; i < staged.size(); i++) {
                        if (i > 0) localStr.append(", ");
                        localStr.append(staged.get(i).getAsString());
                    }
                    localStr.append("\n");
                }

                JsonArray unstaged = uncommitted.getAsJsonArray("unstaged");
                if (unstaged.size() > 0) {
                    localStr.append("- Unstaged: ");
                    for (int i = 0; i < unstaged.size(); i++) {
                        if (i > 0) localStr.append(", ");
                        localStr.append(unstaged.get(i).getAsString());
                    }
                    localStr.append("\n");
                }

                String summary = uncommitted.get("summary").getAsString();
                if (!summary.isEmpty()) {
                    localStr.append("- Summary: ").append(summary).append("\n");
                }
            } else {
                localStr.append("### Uncommitted Changes\n- None\n");
            }

            localStr.append("\n");

            if (unpushed != null && unpushed.has("hasCommits") && unpushed.get("hasCommits").getAsBoolean()) {
                localStr.append("### Unpushed Commits (").append(unpushed.get("count").getAsInt()).append(")\n");
                JsonArray commits = unpushed.getAsJsonArray("commits");
                for (JsonElement commit : commits) {
                    localStr.append("- ").append(commit.getAsString()).append("\n");
                }
            } else {
                localStr.append("### Unpushed Commits\n- None\n");
            }

            localStr.append("\n");
        }

        // Replace placeholders
        String formatted = template
            .replace("{{githubActivity}}", githubStr)
            .replace("{{localChanges}}", localStr.toString())
            .replace("{{user}}", metadata.get("user").getAsString())
            .replace("{{days}}", metadata.get("days").getAsString())
            .replace("{{directoryCount}}", metadata.get("directoryCount").getAsString())
            .replace("{{repoCount}}", metadata.get("repoCount").getAsString());

        return formatted;
    }

    private static String generateReportWithClaude(String prompt) throws Exception {
        // Use stdin to pass prompt (avoids command-line length limits)
        ProcessBuilder pb = new ProcessBuilder("claude", "-p", "-");
        Process process = pb.start();

        StringBuilder reportOutput = new StringBuilder();

        // Write prompt to claude's stdin in a separate thread
        new Thread(() -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(prompt);
                writer.flush();
            } catch (IOException e) {
                System.err.println("Error writing to claude stdin: " + e.getMessage());
            }
        }).start();

        // Stream stderr to System.err in real-time (for status messages)
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            } catch (IOException e) {
                // Ignore
            }
        }).start();

        // Capture stdout and print in real-time
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);  // Print to user
                reportOutput.append(line).append("\n");  // Capture for saving
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Claude generation failed with exit code: " + exitCode);
        }

        return reportOutput.toString();
    }

    private static void saveReport(String report, JsonObject config, JsonObject aggregated) throws IOException {
        JsonObject reportSettings = config.getAsJsonObject("reportSettings");
        String reportDir = expandTilde(reportSettings.get("reportDirectory").getAsString());

        // Create directory if needed
        Files.createDirectories(Paths.get(reportDir));

        // Generate filename
        LocalDate today = LocalDate.now();
        String date = today.toString();  // YYYY-MM-DD format

        JsonObject metadata = aggregated.getAsJsonObject("metadata");

        // Get configured repos from metadata (added by ActivityAggregator)
        String filename;
        if (metadata.has("configuredRepos") && metadata.getAsJsonArray("configuredRepos").size() == 1) {
            // Single configured repo: include repo name
            String repoName = metadata.getAsJsonArray("configuredRepos").get(0).getAsString();
            String sanitized = repoName.replace("/", "-");
            filename = date + "-" + sanitized + ".md";
        } else if (metadata.has("configuredRepos") && metadata.getAsJsonArray("configuredRepos").size() > 1) {
            // Multiple configured repos
            filename = date + "-multi.md";
        } else {
            // Fallback: all repos
            filename = date + "-all-repos.md";
        }

        Path filepath = Paths.get(reportDir, filename);
        Files.writeString(filepath, report);
        System.err.println("✓ Report saved to: " + filepath);
    }

    private static void saveLegacyReport(String report, JsonObject reportSettings, String repo) throws IOException {
        String reportDir = expandTilde(reportSettings.get("reportDirectory").getAsString());

        // Create directory if needed
        Files.createDirectories(Paths.get(reportDir));

        // Generate filename
        LocalDate today = LocalDate.now();
        String date = today.toString();  // YYYY-MM-DD format

        String filename;
        if (repo != null) {
            // Specific repo: include repo name
            String sanitized = repo.replace("/", "-");
            filename = date + "-" + sanitized + ".md";
        } else {
            // All repos
            filename = date + "-all-repos.md";
        }

        Path filepath = Paths.get(reportDir, filename);
        Files.writeString(filepath, report);
        System.err.println("✓ Report saved to: " + filepath);
    }

    private static void handleConfigCommand(Args parsed) throws Exception {
        List<String> cmArgs = new ArrayList<>();
        cmArgs.add(parsed.configCommand);

        switch (parsed.configCommand) {
            case "init":
                // No additional args
                break;
            case "add":
                if (parsed.configPath == null) {
                    System.err.println("Error: --config-add requires a path");
                    System.err.println("Usage: --config-add PATH [--id ID]");
                    System.exit(1);
                }
                cmArgs.add(parsed.configPath);

                // Auto-generate ID if not provided
                String id = parsed.configId;
                if (id == null) {
                    // Generate ID from path (last directory name)
                    Path path = Paths.get(parsed.configPath);
                    id = path.getFileName().toString();
                }
                cmArgs.add(id);
                break;
            case "remove":
                if (parsed.configId == null) {
                    System.err.println("Error: --config-remove requires an ID");
                    System.err.println("Usage: --config-remove ID");
                    System.exit(1);
                }
                cmArgs.add(parsed.configId);
                break;
            case "list":
                // No additional args
                break;
        }

        // Run ConfigManager with direct stdout (inheritIO)
        String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
        String configScript = installDir + "/scripts/ConfigManager.java";
        if (!Files.exists(Paths.get(configScript))) {
            configScript = "scripts/ConfigManager.java";  // Fallback for development
        }

        List<String> command = new ArrayList<>();
        command.add("jbang");
        command.add(configScript);
        command.addAll(cmArgs);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();  // Pass through stdout/stderr directly
        Process process = pb.start();
        int exitCode = process.waitFor();
        System.exit(exitCode);
    }

    public static void main(String... args) {
        try {
            Args parsed = parseArgs(args);

            // Load configuration early to get debugSettings
            JsonObject config = loadConfigJson();

            // Apply debug settings from config (as defaults)
            applyDebugSettings(config);

            // CLI args override config settings
            if (parsed.debug) {
                DEBUG = true;
            }
            if (parsed.debugOverride) {
                DEBUG_OVERRIDE = true;
            }

            // Initialize debug session (creates debug directory and session log)
            initDebugSession();

            debug("Debug mode enabled");
            debug("Config loaded: " + (config != null ? "found with debugSettings" : "not found"));
            debug("Debug settings: logDir=" + DEBUG_LOG_DIRECTORY +
                  ", maxSessions=" + MAX_DEBUG_SESSIONS +
                  ", captureScriptOutput=" + CAPTURE_SCRIPT_OUTPUT +
                  ", verboseGit=" + VERBOSE_GIT_COMMANDS +
                  ", verboseGitHub=" + VERBOSE_GITHUB_API);
            debug("Parsed arguments: days=" + parsed.days + ", user=" + parsed.user +
                  ", repo=" + parsed.repo + ", format=" + parsed.format +
                  ", noClaude=" + parsed.noClaude + ", yesterday=" + parsed.yesterday +
                  ", lastWeek=" + parsed.lastWeek);
            if (parsed.team != null) {
                debug("Team members: " + String.join(", ", parsed.team));
            }

            // Handle config commands first
            if (parsed.configCommand != null) {
                debug("Handling config command: " + parsed.configCommand);
                handleConfigCommand(parsed);
                return;
            }

            debug("Mode detection starting...");
            boolean multiDirMode = shouldUseMultiDirectoryMode(config, parsed);
            debug("Mode detection: " + (multiDirMode ? "multi-directory" : "single-directory"));

            if (multiDirMode) {
                runMultiDirectoryMode(config, parsed);
                return;
            }

            // Legacy single-directory mode
            // Auto-detect repository if not specified
            if (parsed.repo == null) {
                debug("No --repo specified, attempting auto-detection");
                String detectedRepo = getCurrentRepository();
                if (detectedRepo != null) {
                    parsed.repo = detectedRepo;
                    debug("Repository auto-detected: " + detectedRepo);
                    System.err.println("Detected current repository: " + detectedRepo);
                } else {
                    debug("Repository auto-detection failed (not in git repo or no origin remote)");
                    System.err.println("Warning: Not in a git repository or no GitHub remote found.");
                    System.err.println("Activity will be searched across all repositories.");
                    System.err.println("Use --repo owner/repo to specify a repository.");
                }
            } else {
                debug("Using explicitly specified repo: " + parsed.repo);
            }

            // Auto-detect user if not specified
            if (parsed.user == null && parsed.team == null) {
                debug("No --user specified, detecting from gh CLI");
                System.err.println("Detecting current GitHub user...");
                parsed.user = getCurrentUser();
                debug("User detected: " + parsed.user);
            } else if (parsed.user != null) {
                debug("Using explicitly specified user: " + parsed.user);
            }

            // Team mode
            if (parsed.team != null) {
                System.err.println("Generating team report for " + parsed.team.size() + " members...");
                StringBuilder teamReports = new StringBuilder();

                for (String member : parsed.team) {
                    System.err.println("Generating report for " + member + "...");

                    // Collect activity
                    int days = calculateDays(parsed);
                    List<String> activityArgs = new ArrayList<>();
                    activityArgs.add(member);
                    activityArgs.add(String.valueOf(days));
                    if (parsed.repo != null) {
                        activityArgs.add(parsed.repo);
                    }
                    String activityJson = runScript("CollectActivity.java", activityArgs);

                    // Analyze diffs
                    List<String> diffArgs = new ArrayList<>();
                    diffArgs.add(activityJson);
                    String diffSummary = runScript("AnalyzeDiffs.java", diffArgs);

                    // Generate individual report (capture stdout differently for team mode)
                    // For simplicity, we'll generate reports and consolidate them
                    teamReports.append("## ").append(member).append("\n\n");
                    teamReports.append("[Individual report would go here]\n\n");
                }

                // Generate team report
                List<String> teamArgs = new ArrayList<>();
                teamArgs.add(teamReports.toString());
                runScript("TeamAggregator.java", teamArgs);

                System.exit(0);
            }

            // Single user mode
            debug("Starting single user mode for: " + parsed.user);
            System.err.println("Collecting activity for " + parsed.user + "...");

            // Collect activity
            int days = calculateDays(parsed);
            debug("Effective days: " + days);
            List<String> activityArgs = new ArrayList<>();
            activityArgs.add(parsed.user);
            activityArgs.add(String.valueOf(days));
            if (parsed.repo != null) {
                activityArgs.add(parsed.repo);
            }
            debug("Calling CollectActivity with args: " + activityArgs);
            String activityJson = runScript("CollectActivity.java", activityArgs);
            debug("Activity JSON received, length: " + activityJson.length() + " chars");

            System.err.println("Analyzing file changes...");

            // Analyze diffs
            List<String> diffArgs = new ArrayList<>();
            diffArgs.add(activityJson);
            debug("Calling AnalyzeDiffs");
            String diffSummary = runScript("AnalyzeDiffs.java", diffArgs);
            debug("Diff summary received, length: " + diffSummary.length() + " chars");

            System.err.println("Generating standup report...");

            // Call claude directly instead of through GenerateReport.java subprocess
            String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
            Path promptPath = Paths.get(installDir, "prompts/standup.prompt.md");
            if (!Files.exists(promptPath)) {
                promptPath = Paths.get("prompts/standup.prompt.md");  // Fallback for development
            }
            debug("Loading prompt template from: " + promptPath);
            String promptTemplate = Files.readString(promptPath);
            debug("Prompt template loaded, length: " + promptTemplate.length() + " chars");

            // Parse and format activities
            JsonObject activity = com.google.gson.JsonParser.parseString(activityJson).getAsJsonObject();
            String formattedActivities = formatActivities(activity);
            debug("Formatted activities length: " + formattedActivities.length() + " chars");

            // Inject data into template
            String fullPrompt = promptTemplate
                    .replace("{{activities}}", formattedActivities)
                    .replace("{{diffs}}", diffSummary)
                    .replace("{{days}}", String.valueOf(days));
            debug("Full prompt assembled, length: " + fullPrompt.length() + " chars");

            // Check if we should skip claude -p (when running inside Claude Code)
            if (parsed.noClaude) {
                debug("--no-claude flag set, outputting prompt directly");
                // Output the prompt directly for Claude Code to process
                System.out.println(fullPrompt);
            } else {
                debug("Invoking Claude CLI with prompt via stdin");
                // Call claude -p and pipe prompt via stdin (avoids command-line length limits)
                ProcessBuilder claudeBuilder = new ProcessBuilder("claude", "-p", "-");
                Process claudeProcess = claudeBuilder.start();

                // Capture output for auto-save
                StringBuilder reportOutput = new StringBuilder();

                // Write prompt to claude's stdin in a separate thread
                new Thread(() -> {
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(claudeProcess.getOutputStream()))) {
                        writer.write(fullPrompt);
                        writer.flush();
                    } catch (IOException e) {
                        System.err.println("Error writing to claude stdin: " + e.getMessage());
                    }
                }).start();

                // Stream stderr to System.err in real-time (for status messages)
                new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(claudeProcess.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.err.println(line);
                        }
                    } catch (IOException e) {
                        // Ignore
                    }
                }).start();

                // Capture stdout and print in real-time
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(claudeProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);  // Print to user
                        reportOutput.append(line).append("\n");  // Capture for saving
                    }
                }

                int claudeExitCode = claudeProcess.waitFor();

                if (claudeExitCode != 0) {
                    System.err.println("Claude invocation failed with exit code: " + claudeExitCode);
                    System.exit(claudeExitCode);
                }

                // Auto-save if enabled in config
                if (config != null && config.has("reportSettings")) {
                    JsonObject reportSettings = config.getAsJsonObject("reportSettings");
                    if (reportSettings.has("autoSaveReports") && reportSettings.get("autoSaveReports").getAsBoolean()) {
                        saveLegacyReport(reportOutput.toString(), reportSettings, parsed.repo);
                    }
                }
            }

        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid number format");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
