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
import java.util.List;

/**
 * Main - Entry point for claude-gh-standup slash command
 *
 * Usage: jbang Main.java [--days N] [--user USERNAME] [--repo REPO] [--format FORMAT] [--team USERS...] [--output FILE]
 */
public class Main {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static class Args {
        int days = 1;
        String user = null;
        String repo = null;
        String format = "markdown";
        List<String> team = null;
        String output = null;
        boolean noClaude = false;

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
        System.err.println("[DEBUG] runScript called for: " + scriptName);
        System.err.println("[DEBUG] Number of args: " + scriptArgs.size());
        for (int i = 0; i < scriptArgs.size(); i++) {
            System.err.println("[DEBUG] Arg " + i + " length: " + scriptArgs.get(i).length());
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
        System.err.println("[DEBUG] Script path: " + scriptPath);

        List<String> command = new ArrayList<>();
        command.add("jbang");
        command.add(scriptPath.toString());
        command.addAll(scriptArgs);

        System.err.println("[DEBUG] Creating ProcessBuilder...");
        ProcessBuilder pb = new ProcessBuilder(command);
        System.err.println("[DEBUG] Starting process...");
        Process process = pb.start();
        System.err.println("[DEBUG] Process started, reading stdout...");

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        System.err.println("[DEBUG] Finished reading stdout, waiting for exit...");

        int exitCode = process.waitFor();
        System.err.println("[DEBUG] Process exited with code: " + exitCode);

        if (exitCode != 0) {
            StringBuilder error = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }
            System.err.println("Error running " + scriptName + ":");
            System.err.println(error.toString());
            throw new RuntimeException("Script " + scriptName + " failed with exit code " + exitCode);
        }

        System.err.println("[DEBUG] runScript completed for: " + scriptName);
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
        System.err.println("Running in multi-directory mode...");

        // Calculate effective days
        int days = calculateDays(parsed);

        // Get user
        String user = parsed.user;
        if (user == null && parsed.team == null) {
            System.err.println("Detecting current GitHub user...");
            user = getCurrentUser();
        }

        // Call ActivityAggregator
        System.err.println("Aggregating activities across directories...");
        String configJson = gson.toJson(config);

        String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
        String aggregatorScript = installDir + "/scripts/ActivityAggregator.java";
        if (!Files.exists(Paths.get(aggregatorScript))) {
            aggregatorScript = "scripts/ActivityAggregator.java";  // Fallback for development
        }

        ProcessBuilder pb = new ProcessBuilder(
            "jbang", aggregatorScript,
            configJson, user, String.valueOf(days)
        );

        Process process = pb.start();
        StringBuilder aggregatedJson = new StringBuilder();

        // Capture aggregator stderr
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

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                aggregatedJson.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("ActivityAggregator failed");
            System.exit(1);
        }

        // Parse aggregated data
        JsonObject aggregated = gson.fromJson(aggregatedJson.toString(), JsonObject.class);

        // Format multi-dir prompt
        String prompt = formatMultiDirPrompt(aggregated);

        // Generate report
        String report;
        if (parsed.noClaude) {
            report = prompt;
        } else {
            report = generateReportWithClaude(prompt);
        }

        // Auto-save if enabled
        if (config.has("reportSettings")) {
            JsonObject reportSettings = config.getAsJsonObject("reportSettings");
            boolean autoSave = reportSettings.get("autoSaveReports").getAsBoolean();
            if (autoSave) {
                saveReport(report, config, aggregated);
            }
        }

        // Output to stdout
        System.out.println(report);
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

        // Format GitHub activity
        StringBuilder githubStr = new StringBuilder();
        for (String repo : githubActivity.keySet()) {
            githubStr.append("## Repository: ").append(repo).append("\n\n");
            JsonObject activity = githubActivity.getAsJsonObject(repo);
            githubStr.append(formatActivities(activity));
            githubStr.append("\n");
        }

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
            .replace("{{githubActivity}}", githubStr.toString())
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
        int repoCount = metadata.get("repoCount").getAsInt();

        String filename;
        if (repoCount == 1) {
            // Single repo: include repo name
            JsonObject githubActivity = aggregated.getAsJsonObject("githubActivity");
            String repoName = githubActivity.keySet().iterator().next();
            String sanitized = repoName.replace("/", "-");
            filename = date + "-" + sanitized + ".md";
        } else {
            // Multiple repos
            filename = date + "-multi.md";
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

            // Handle config commands first
            if (parsed.configCommand != null) {
                handleConfigCommand(parsed);
                return;
            }

            // Load configuration and determine mode
            JsonObject config = loadConfigJson();
            boolean multiDirMode = shouldUseMultiDirectoryMode(config, parsed);

            if (multiDirMode) {
                runMultiDirectoryMode(config, parsed);
                return;
            }

            // Legacy single-directory mode
            // Auto-detect repository if not specified
            if (parsed.repo == null) {
                String detectedRepo = getCurrentRepository();
                if (detectedRepo != null) {
                    parsed.repo = detectedRepo;
                    System.err.println("Detected current repository: " + detectedRepo);
                } else {
                    System.err.println("Warning: Not in a git repository or no GitHub remote found.");
                    System.err.println("Activity will be searched across all repositories.");
                    System.err.println("Use --repo owner/repo to specify a repository.");
                }
            }

            // Auto-detect user if not specified
            if (parsed.user == null && parsed.team == null) {
                System.err.println("Detecting current GitHub user...");
                parsed.user = getCurrentUser();
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
            System.err.println("Collecting activity for " + parsed.user + "...");

            // Collect activity
            int days = calculateDays(parsed);
            List<String> activityArgs = new ArrayList<>();
            activityArgs.add(parsed.user);
            activityArgs.add(String.valueOf(days));
            if (parsed.repo != null) {
                activityArgs.add(parsed.repo);
            }
            String activityJson = runScript("CollectActivity.java", activityArgs);

            System.err.println("Analyzing file changes...");

            // Analyze diffs
            List<String> diffArgs = new ArrayList<>();
            diffArgs.add(activityJson);
            String diffSummary = runScript("AnalyzeDiffs.java", diffArgs);

            System.err.println("Generating standup report...");

            // Call claude directly instead of through GenerateReport.java subprocess
            String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
            Path promptPath = Paths.get(installDir, "prompts/standup.prompt.md");
            if (!Files.exists(promptPath)) {
                promptPath = Paths.get("prompts/standup.prompt.md");  // Fallback for development
            }
            String promptTemplate = Files.readString(promptPath);

            // Parse and format activities
            JsonObject activity = com.google.gson.JsonParser.parseString(activityJson).getAsJsonObject();
            String formattedActivities = formatActivities(activity);

            // Inject data into template
            String fullPrompt = promptTemplate
                    .replace("{{activities}}", formattedActivities)
                    .replace("{{diffs}}", diffSummary);

            // Check if we should skip claude -p (when running inside Claude Code)
            if (parsed.noClaude) {
                // Output the prompt directly for Claude Code to process
                System.out.println(fullPrompt);
            } else {
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
